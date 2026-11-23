package com.qlsv.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qlsv.database.DatabaseManager;
import com.qlsv.database.dao.AuditDAO;
import com.qlsv.model.*;
import com.qlsv.network.PacketType;
import com.qlsv.network.UDPPacket;

import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Máy chủ RUDP 2.0 Phân Tán Đa Luồng:
 * - Xác thực chữ ký số gói tin HMAC-SHA256 & Chống tấn công phát lại (Anti-Replay)
 * - Chống Brute-force Rate-Limiting & Phân quyền RBAC + Khóa sổ điểm học kỳ (Gradebook Lock)
 * - Lưu vết Thanh tra biến động điểm số (Immutable Grade Audit Trail: OldScore -> NewScore)
 * - Kênh đẩy thông báo thời gian thực Full-Duplex (UDP Server-Push Notification)
 * - Bộ giả lập mạng chập chờn (Network Chaos Simulator: Packet Loss & Latency)
 */
public class UDPServer {

    public interface ServerLogListener {
        void onLog(String message);
        void onDataUpdated();
    }

    private static final Gson gson = new Gson();
    private final int port;
    private DatagramSocket socket;
    private volatile boolean running = false;
    private Thread serverThread;
    private ServerLogListener logListener;
    private ExecutorService threadPool;

    // Network Chaos Simulator Parameters
    private volatile int simulatedDropRatePercent = 0; // 0 - 50%
    private volatile int simulatedLatencyMs = 0;       // 0 - 500ms
    private final Set<String> chaosDroppedOnce = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Telemetry Counters
    private final AtomicLong totalPacketsReceived = new AtomicLong(0);
    private final AtomicLong totalPacketsDroppedByChaos = new AtomicLong(0);
    private final AtomicLong totalPushEventsSent = new AtomicLong(0);

    // Active Client Push Subscribers (Key: "ip:pushPort" -> InetSocketAddress)
    private final Map<String, InetSocketAddress> pushSubscribers = new ConcurrentHashMap<>();

    // RUDP Deduplication Cache (LRU cache of 500 recent packetIds)
    private final Map<String, Boolean> seenPackets = Collections.synchronizedMap(
            new LinkedHashMap<String, Boolean>(100, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > 500;
                }
            }
    );

    public UDPServer(int port) {
        this.port = port;
    }

    public void setLogListener(ServerLogListener listener) {
        this.logListener = listener;
    }

    public int getSimulatedDropRatePercent() {
        return simulatedDropRatePercent;
    }

    public void setSimulatedDropRatePercent(int simulatedDropRatePercent) {
        this.simulatedDropRatePercent = Math.max(0, Math.min(80, simulatedDropRatePercent));
        log(String.format("[CHAOS CONFIG] Đã đặt tỷ lệ giả lập rớt gói UDP = %d%%, Độ trễ = %dms",
                this.simulatedDropRatePercent, this.simulatedLatencyMs));
    }

    public int getSimulatedLatencyMs() {
        return simulatedLatencyMs;
    }

    public void setSimulatedLatencyMs(int simulatedLatencyMs) {
        this.simulatedLatencyMs = Math.max(0, Math.min(2000, simulatedLatencyMs));
        log(String.format("[CHAOS CONFIG] Đã đặt độ trễ mạng giả lập = %dms (Drop Rate = %d%%)",
                this.simulatedLatencyMs, this.simulatedDropRatePercent));
    }

    public long getTotalPacketsReceived() {
        return totalPacketsReceived.get();
    }

    public long getTotalPacketsDroppedByChaos() {
        return totalPacketsDroppedByChaos.get();
    }

    public int getActivePushSubscriberCount() {
        return pushSubscribers.size();
    }

    public synchronized void start() throws Exception {
        if (running) return;
        socket = new DatagramSocket(port);
        running = true;
        threadPool = Executors.newFixedThreadPool(20);

        log(String.format("Server RUDP 2.0 khởi chạy trên cổng %d (20 Workers, HMAC-SHA256, Anti-Replay, Server-Push)", port));

        serverThread = new Thread(() -> {
            if (!DatabaseManager.getInstance().isConnected()) {
                try {
                    SqlConfig config = com.qlsv.database.DatabaseConfigLoader.loadConfig();
                    DatabaseManager.getInstance().connect(config);
                    log("Tự động kết nối CSDL (" + config.getDbType() + ") từ db.properties qua HikariCP thành công.");
                    if (logListener != null) {
                        logListener.onDataUpdated();
                    }
                } catch (Exception e) {
                    log("Thông báo: CSDL chưa kết nối sẵn: " + e.getMessage());
                }
            }

            byte[] buffer = new byte[65507];
            while (running && !socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    byte[] data = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                    InetAddress clientAddress = packet.getAddress();
                    int clientPort = packet.getPort();

                    threadPool.submit(() -> processPacket(data, data.length, clientAddress, clientPort));
                } catch (Exception e) {
                    if (running) {
                        log("Lỗi nhận gói tin UDP: " + e.getMessage());
                    }
                }
            }
        });
        serverThread.start();
    }

    /**
     * Phát thông báo thời gian thực (UDP Server-Push) xuống toàn bộ các Client đang trực tuyến.
     */
    public void broadcastPushEvent(String eventType, String message, String jsonPayload) {
        if (socket == null || socket.isClosed() || pushSubscribers.isEmpty()) return;
        try {
            UDPPacket pushPacket = UDPPacket.createPushEvent(eventType, message, jsonPayload);
            byte[] bytes = pushPacket.toBytes();
            for (Map.Entry<String, InetSocketAddress> entry : pushSubscribers.entrySet()) {
                try {
                    InetSocketAddress target = entry.getValue();
                    DatagramPacket dp = new DatagramPacket(bytes, bytes.length, target.getAddress(), target.getPort());
                    socket.send(dp);
                    totalPushEventsSent.incrementAndGet();
                } catch (Exception ignored) {}
            }
            log(String.format("[UDP SERVER-PUSH] Đã phát sự kiện [%s] tới %d Client trực tuyến: %s",
                    eventType, pushSubscribers.size(), message));
        } catch (Exception e) {
            log("Lỗi phát Push Event: " + e.getMessage());
        }
    }

    private void processPacket(byte[] data, int length, InetAddress clientAddress, int clientPort) {
        try {
            totalPacketsReceived.incrementAndGet();
            UDPPacket packet = UDPPacket.fromBytes(data, length);
            if (packet == null) {
                log("Bỏ qua gói tin UDP không đúng định dạng từ " + clientAddress.getHostAddress());
                return;
            }

            String clientIp = clientAddress.getHostAddress();
            int seq = packet.getSequenceNum();

            // 1. Network Chaos Simulator: Artificial Latency & Packet Drop
            if (simulatedLatencyMs > 0) {
                try {
                    Thread.sleep(simulatedLatencyMs);
                } catch (InterruptedException ignored) {}
            }

            if (simulatedDropRatePercent > 0 && packet.getType() != PacketType.PING && packet.getType() != PacketType.PUSH_SUBSCRIBE) {
                String pId = packet.getPacketId() != null ? packet.getPacketId() : String.valueOf(seq);
                // Chỉ làm rớt lần truyền đầu tiên của gói tin để Client luôn khôi phục thành công ở lần Retry thứ 2
                if (!chaosDroppedOnce.contains(pId) && (Math.random() * 100.0) < simulatedDropRatePercent) {
                    chaosDroppedOnce.add(pId);
                    totalPacketsDroppedByChaos.incrementAndGet();
                    log(String.format("[CHAOS DROP %d%%] Giả lập RỚT GÓI TIN [Seq#%d | %s] từ %s:%d -> Chờ Client ARQ Retransmit!",
                            simulatedDropRatePercent, seq, packet.getType(), clientIp, clientPort));
                    return; // Cố ý không phản hồi để kích hoạt Timeout & Retransmission phía Client
                }
            }

            // 2. Kiểm tra chữ ký toàn vẹn HMAC-SHA256 & Chống Replay Attack
            if (!packet.verifyIntegrity()) {
                log(String.format("[CẢNH BÁO AN NINH] Chữ ký HMAC-SHA256 KHÔNG HỢP LỆ trên gói [Seq#%d | %s] từ %s:%d! Từ chối xử lý.",
                        seq, packet.getType(), clientIp, clientPort));
                sendPacket(UDPPacket.createError("BẢO MẬT RUDP: Chữ ký số HMAC-SHA256 không hợp lệ (Gói tin có dấu hiệu bị can thiệp)!"),
                        seq, clientAddress, clientPort);
                return;
            }

            if (!packet.isReplayValid()) {
                log(String.format("[CẢNH BÁO AN NINH] Phát hiện gói tin quá hạn Timestamp (Replay Attack) [Seq#%d] từ %s:%d!",
                        seq, clientIp, clientPort));
                sendPacket(UDPPacket.createError("BẢO MẬT RUDP: Gói tin đã hết hạn Timestamp (Chống tấn công phát lại - Anti-Replay)!"),
                        seq, clientAddress, clientPort);
                return;
            }

            log(String.format("Nhận [Seq#%d | %s | HMAC:OK] từ Client %s:%d", seq, packet.getType(), clientIp, clientPort));

            // 3. Deduplication Check
            if (packet.getPacketId() != null && seenPackets.containsKey(packet.getPacketId())) {
                log(String.format("[ARQ RETRANSMIT] Phát hiện gói tin truyền lại #%d [%s]. Khôi phục phản hồi an toàn.",
                        seq, packet.getPacketId().substring(0, 8)));
            } else if (packet.getPacketId() != null) {
                seenPackets.put(packet.getPacketId(), Boolean.TRUE);
            }

            UDPPacket responsePacket;

            switch (packet.getType()) {
                case PING:
                    responsePacket = UDPPacket.createPong();
                    break;

                case PUSH_SUBSCRIBE:
                    try {
                        String payload = packet.getPayload();
                        String[] parts = payload != null ? payload.split("\\|") : new String[0];
                        int pushPort = parts.length > 0 ? Integer.parseInt(parts[0].trim()) : clientPort;
                        String key = clientIp + ":" + pushPort;
                        pushSubscribers.put(key, new InetSocketAddress(clientAddress, pushPort));
                        log(String.format("Đăng ký kênh UDP Server-Push cho Client %s (Tổng: %d kết nối Push)", key, pushSubscribers.size()));
                        responsePacket = UDPPacket.createGradeLockStatus(SessionManager.getInstance().isGradebookLocked(),
                                "Đã đăng ký kênh Push thời gian thực");
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi đăng ký kênh Push: " + ex.getMessage());
                    }
                    break;

                case LOGIN:
                    try {
                        String[] creds = gson.fromJson(packet.getPayload(), String[].class);
                        String username = creds[0];
                        String password = creds[1];

                        long lockRemain = SessionManager.getInstance().getRemainingLockoutSeconds(username);
                        if (lockRemain > 0) {
                            DatabaseManager.getInstance().logAudit(username, "LOGIN_LOCKED",
                                    "Tài khoản đang bị khóa Brute-force (" + lockRemain + "s)", clientIp);
                            log(String.format("[CẢNH BÁO BRUTE-FORCE] Chặn đăng nhập '%s' từ %s (Đang khóa còn %ds)", username, clientIp, lockRemain));
                            responsePacket = UDPPacket.createLoginResponse(false,
                                    "BẢO VỆ CHỐNG BRUTE-FORCE: Tài khoản tạm khóa do nhập sai nhiều lần! Vui lòng thử lại sau " + lockRemain + " giây.",
                                    null, null);
                            break;
                        }

                        UserAccount acc = DatabaseManager.getInstance().authenticateUser(username, password);
                        if (acc != null) {
                            String token = SessionManager.getInstance().createSession(acc);
                            DatabaseManager.getInstance().logAudit(username, "LOGIN", "Đăng nhập thành công vai trò " + acc.getRole(), clientIp);
                            log(String.format("Xác thực THÀNH CÔNG cho '%s' (Quyền: %s, Token: %s...)", username, acc.getRole(), token.substring(0, 8)));
                            responsePacket = UDPPacket.createLoginResponse(true, "Đăng nhập thành công!", token, gson.toJson(acc));
                        } else {
                            int failCount = SessionManager.getInstance().recordLoginFailure(username);
                            DatabaseManager.getInstance().logAudit(username, "LOGIN_FAILED",
                                    "Sai mật khẩu lần " + failCount + "/5", clientIp);
                            log(String.format("Xác thực THẤT BẠI cho '%s' (Lần sai: %d/5)", username, failCount));
                            String msg = failCount >= 5
                                    ? "Sai mật khẩu 5 lần liên tiếp! Tài khoản đã bị khóa tạm thời 60 giây để bảo vệ an ninh."
                                    : String.format("Sai tên đăng nhập hoặc mật khẩu! (Cảnh báo an ninh: Lần thử sai %d/5)", failCount);
                            responsePacket = UDPPacket.createLoginResponse(false, msg, null, null);
                        }
                    } catch (Exception ex) {
                        log("Lỗi xử lý đăng nhập: " + ex.getMessage());
                        responsePacket = UDPPacket.createError("Lỗi đăng nhập: " + ex.getMessage());
                    }
                    break;

                case CONNECT_DB:
                    try {
                        SqlConfig config = gson.fromJson(packet.getPayload(), SqlConfig.class);
                        boolean ok = DatabaseManager.getInstance().connect(config);
                        if (ok) {
                            log("Client kết nối CSDL thành công (" + config.getDbType() + ")");
                            responsePacket = UDPPacket.createDbStatus(true, "Kết nối CSDL " + config.getDbType() + " thành công!");
                        } else {
                            responsePacket = UDPPacket.createDbStatus(false, "Không thể kết nối CSDL!");
                        }
                    } catch (Exception ex) {
                        log("Lỗi kết nối CSDL: " + ex.getMessage());
                        responsePacket = UDPPacket.createDbStatus(false, "Lỗi kết nối CSDL: " + ex.getMessage());
                    }
                    break;

                case TOGGLE_GRADE_LOCK:
                    try {
                        String token = packet.getToken();
                        SessionManager.UserSession session = SessionManager.getInstance().getSession(token);
                        if (session != null && session.getRole() == UserAccount.Role.HOCSINH) {
                            responsePacket = UDPPacket.createError("Chỉ Quản trị viên / Phòng Giáo vụ mới có quyền Khóa hoặc Mở khóa sổ điểm!");
                        } else {
                            boolean targetLock = Boolean.parseBoolean(packet.getPayload());
                            String operator = (session != null) ? session.getUsername() : "admin";
                            SessionManager.getInstance().setGradebookLocked(targetLock, operator);
                            String statusMsg = targetLock
                                    ? "ĐÃ KHÓA & CÔNG BỐ SỔ ĐIỂM HỌC KỲ (Chặn chỉnh sửa điểm)"
                                    : "ĐÃ MỞ KHÓA SỔ ĐIỂM HỌC KỲ (Cho phép Giảng viên cập nhật điểm)";
                            DatabaseManager.getInstance().logAudit(operator, targetLock ? "LOCK_GRADEBOOK" : "UNLOCK_GRADEBOOK", statusMsg, clientIp);
                            log("[GIÁO VỤ - " + operator + "] " + statusMsg);
                            responsePacket = UDPPacket.createGradeLockStatus(targetLock, statusMsg);
                            broadcastPushEvent("GRADE_LOCK", statusMsg, String.valueOf(targetLock));
                        }
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi đổi trạng thái khóa điểm: " + ex.getMessage());
                    }
                    break;

                case BROADCAST_ANNOUNCEMENT:
                    try {
                        String token = packet.getToken();
                        SessionManager.UserSession session = SessionManager.getInstance().getSession(token);
                        if (session != null && session.getRole() == UserAccount.Role.HOCSINH) {
                            responsePacket = UDPPacket.createError("Học sinh không có quyền phát thông báo đào tạo!");
                        } else {
                            String operator = (session != null) ? session.getFullName() : "Phòng Đào Tạo PTIT";
                            String text = packet.getPayload();
                            DatabaseManager.getInstance().logAudit(
                                    session != null ? session.getUsername() : "giangvien",
                                    "BROADCAST", "Phát thông báo: " + text, clientIp);
                            broadcastPushEvent("ANNOUNCEMENT", "Thông báo từ " + operator + ": " + text, text);
                            responsePacket = new UDPPacket(PacketType.BROADCAST_ANNOUNCEMENT, true, "Đã phát thông báo tới toàn bộ sinh viên trực tuyến!", text);
                        }
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi phát thông báo: " + ex.getMessage());
                    }
                    break;

                case GET_AUDIT_LOGS:
                    try {
                        int limit = 50;
                        try {
                            if (packet.getPayload() != null) limit = Integer.parseInt(packet.getPayload().trim());
                        } catch (Exception ignored) {}
                        List<AuditDAO.AuditEntry> logs = DatabaseManager.getInstance().getRecentAuditLogs(limit);
                        responsePacket = UDPPacket.createAuditLogsResult(gson.toJson(logs));
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi tải nhật ký thanh tra: " + ex.getMessage());
                    }
                    break;

                case ADD_STUDENT:
                case UPDATE_STUDENT:
                    try {
                        if (!DatabaseManager.getInstance().isConnected()) {
                            responsePacket = UDPPacket.createError("Server chưa được kết nối CSDL!");
                        } else {
                            String token = packet.getToken();
                            SessionManager.UserSession session = SessionManager.getInstance().getSession(token);

                            if (session != null && session.getRole() == UserAccount.Role.HOCSINH) {
                                responsePacket = UDPPacket.createError("BẢO VỆ PHÂN QUYỀN SERVER: Quyền Học sinh không được phép thêm hoặc cập nhật điểm số!");
                            } else if (SessionManager.getInstance().isGradebookLocked() &&
                                    (session == null || session.getRole() != UserAccount.Role.ADMIN)) {
                                responsePacket = UDPPacket.createError("[KHÓA SỔ ĐIỂM] Sổ điểm học kỳ đã khóa bởi Phòng Giáo Vụ! Giảng viên không thể sửa điểm khi sổ đã khóa (Yêu cầu quyền Admin để mở khóa).");
                            } else {
                                StudentData student = gson.fromJson(packet.getPayload(), StudentData.class);
                                String operator = (session != null) ? session.getUsername() : "giangvien";

                                // Lấy điểm cũ trước khi lưu để ghi Audit Trail bất biến (OldScore -> NewScore)
                                StudentResult oldRecord = null;
                                try {
                                    oldRecord = DatabaseManager.getInstance().getStudentDecrypted(student.getStudentId());
                                } catch (Exception ignored) {}

                                StudentResult result = DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(student);

                                String auditDetail;
                                if (oldRecord != null) {
                                    auditDetail = String.format("Sửa điểm %s (%s): ĐTB %.2f -> %.2f [CC: %.1f->%.1f, GK: %.1f->%.1f, CK: %.1f->%.1f | %s]",
                                            result.getStudentId(), result.getFullName(),
                                            oldRecord.getAverageScore(), result.getAverageScore(),
                                            oldRecord.getScoreCC(), result.getScoreCC(),
                                            oldRecord.getScoreGK(), result.getScoreGK(),
                                            oldRecord.getScoreCK(), result.getScoreCK(),
                                            result.getAcademicStatus());
                                } else {
                                    auditDetail = String.format("Thêm mới %s (%s): ĐTB %.2f [CC: %.1f, GK: %.1f, CK: %.1f | %s]",
                                            result.getStudentId(), result.getFullName(),
                                            result.getAverageScore(),
                                            result.getScoreCC(), result.getScoreGK(), result.getScoreCK(),
                                            result.getAcademicStatus());
                                }

                                DatabaseManager.getInstance().logAudit(operator, oldRecord != null ? "UPDATE_GRADE" : "INSERT_GRADE", auditDetail, clientIp);
                                log(String.format("[%s] %s", operator, auditDetail));

                                responsePacket = UDPPacket.createStudentResult(true, "Xử lý thành công", gson.toJson(result));

                                // Đẩy thông báo thời gian thực xuống Cổng Sinh Viên
                                broadcastPushEvent("GRADE_UPDATED",
                                        String.format("Điểm học phần của %s (%s) vừa được cập nhật: ĐTB %.2f (%s - %s)",
                                                result.getFullName(), result.getStudentId(),
                                                result.getAverageScore(), result.getLetterGrade(), result.getAcademicStatus()),
                                        gson.toJson(result));

                                if (logListener != null) {
                                    logListener.onDataUpdated();
                                }
                            }
                        }
                    } catch (Exception ex) {
                        log("Lỗi xử lý sinh viên: " + ex.getMessage());
                        responsePacket = UDPPacket.createError("Lỗi Server: " + ex.getMessage());
                    }
                    break;

                case DELETE_STUDENT:
                    try {
                        if (!DatabaseManager.getInstance().isConnected()) {
                            responsePacket = UDPPacket.createError("Server chưa được kết nối CSDL!");
                        } else {
                            String token = packet.getToken();
                            SessionManager.UserSession session = SessionManager.getInstance().getSession(token);

                            if (session != null && session.getRole() == UserAccount.Role.HOCSINH) {
                                responsePacket = UDPPacket.createError("BẢO VỆ PHÂN QUYỀN SERVER: Học sinh không được phép xóa dữ liệu sinh viên!");
                            } else if (SessionManager.getInstance().isGradebookLocked() &&
                                    (session == null || session.getRole() != UserAccount.Role.ADMIN)) {
                                responsePacket = UDPPacket.createError("[KHÓA SỔ ĐIỂM] Sổ điểm học kỳ đã khóa! Không thể xóa hồ sơ sinh viên khi sổ điểm đã khóa.");
                            } else {
                                String studentId = packet.getPayload();
                                String operator = (session != null) ? session.getUsername() : "giangvien";
                                boolean deleted = DatabaseManager.getInstance().deleteStudent(studentId);
                                if (deleted) {
                                    DatabaseManager.getInstance().logAudit(operator, "DELETE_STUDENT", "Xóa hồ sơ sinh viên " + studentId, clientIp);
                                    log("Đã xóa sinh viên có mã: " + studentId);
                                    responsePacket = new UDPPacket(PacketType.DELETE_STUDENT, true, "Đã xóa sinh viên " + studentId, studentId);
                                    broadcastPushEvent("STUDENT_DELETED", "Đã xóa hồ sơ sinh viên " + studentId, studentId);
                                    if (logListener != null) {
                                        logListener.onDataUpdated();
                                    }
                                } else {
                                    responsePacket = UDPPacket.createError("Không tìm thấy sinh viên có mã: " + studentId);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        log("Lỗi xóa sinh viên: " + ex.getMessage());
                        responsePacket = UDPPacket.createError("Lỗi xóa: " + ex.getMessage());
                    }
                    break;

                case SEARCH_STUDENTS:
                    try {
                        String query = packet.getPayload();
                        List<StudentResult> list = DatabaseManager.getInstance().searchStudents(query);
                        responsePacket = UDPPacket.createStudentsListResponse(true, "Tìm kiếm thành công", gson.toJson(list));
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi tìm kiếm: " + ex.getMessage());
                    }
                    break;

                case GET_STUDENTS_PAGE:
                    try {
                        com.qlsv.model.PageRequest pageReq = gson.fromJson(packet.getPayload(), com.qlsv.model.PageRequest.class);
                        if (pageReq == null) {
                            pageReq = new com.qlsv.model.PageRequest(1, 20);
                        }
                        com.qlsv.model.PageResult<StudentResult> pageResult;
                        if (pageReq.getQuery() != null && !pageReq.getQuery().trim().isEmpty()) {
                            pageResult = DatabaseManager.getInstance().searchStudentsPaged(pageReq.getQuery(), pageReq.getPage(), pageReq.getPageSize());
                        } else {
                            pageResult = DatabaseManager.getInstance().getStudentsPaged(pageReq.getPage(), pageReq.getPageSize());
                        }
                        responsePacket = UDPPacket.createPageResult(true, "Tải phân trang thành công", gson.toJson(pageResult));
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi phân trang: " + ex.getMessage());
                    }
                    break;

                case GET_ALL_STUDENTS:
                    try {
                        List<StudentResult> list = DatabaseManager.getInstance().getAllStudentsDecrypted();
                        responsePacket = UDPPacket.createStudentsListResponse(true, "Tải danh sách thành công", gson.toJson(list));
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi lấy danh sách: " + ex.getMessage());
                    }
                    break;

                case GET_TRANSCRIPT:
                    try {
                        String studentId = packet.getPayload();
                        StudentTranscript transcript = DatabaseManager.getInstance().getDetailedTranscript(studentId);
                        responsePacket = UDPPacket.createTranscriptResult(true, "Lấy bảng điểm thành công", gson.toJson(transcript));
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi tải bảng điểm: " + ex.getMessage());
                    }
                    break;

                case IMPORT_EXCEL:
                    try {
                        String token = packet.getToken();
                        SessionManager.UserSession session = SessionManager.getInstance().getSession(token);

                        if (session != null && session.getRole() == UserAccount.Role.HOCSINH) {
                            responsePacket = UDPPacket.createError("BẢO VỆ PHÂN QUYỀN SERVER: Học sinh không được phép nhập dữ liệu Excel!");
                        } else if (SessionManager.getInstance().isGradebookLocked() &&
                                (session == null || session.getRole() != UserAccount.Role.ADMIN)) {
                            responsePacket = UDPPacket.createError("[KHÓA SỔ ĐIỂM] Sổ điểm học kỳ đã khóa! Không thể import bảng điểm mới khi sổ đã khóa.");
                        } else {
                            Type listType = new TypeToken<List<StudentData>>() {}.getType();
                            List<StudentData> list = gson.fromJson(packet.getPayload(), listType);
                            int count = DatabaseManager.getInstance().saveStudentBatch(list);

                            String operator = (session != null) ? session.getUsername() : "giangvien";
                            DatabaseManager.getInstance().logAudit(operator, "IMPORT_EXCEL", "Nhập hàng loạt " + count + " sinh viên từ Excel", clientIp);
                            log(String.format("[%s] Đã nhập thành công %d sinh viên từ file Excel vào CSDL SQL!", operator, count));

                            responsePacket = UDPPacket.createImportResult(true, "Nhập thành công", count);
                            broadcastPushEvent("BATCH_IMPORT", "Giảng viên vừa cập nhật bảng điểm hàng loạt (" + count + " sinh viên)!", String.valueOf(count));
                            if (logListener != null) {
                                logListener.onDataUpdated();
                            }
                        }
                    } catch (Exception ex) {
                        log("Lỗi nhập dữ liệu Excel: " + ex.getMessage());
                        responsePacket = UDPPacket.createError("Lỗi nhập Excel: " + ex.getMessage());
                    }
                    break;

                default:
                    responsePacket = UDPPacket.createError("Loại gói tin không hợp lệ: " + packet.getType());
                    break;
            }

            sendPacket(responsePacket, seq, clientAddress, clientPort);

        } catch (Exception e) {
            log("Lỗi định dạng gói tin UDP: " + e.getMessage());
        }
    }

    private void sendPacket(UDPPacket responsePacket, int seq, InetAddress clientAddress, int clientPort) throws Exception {
        responsePacket.setAckNum(seq);
        byte[] respBytes = responsePacket.toBytes();
        DatagramPacket respPacket = new DatagramPacket(respBytes, respBytes.length, clientAddress, clientPort);
        socket.send(respPacket);
        log(String.format("Đã gửi phản hồi [Ack#%d | %s] tới Client %s:%d",
                seq, responsePacket.getType(), clientAddress.getHostAddress(), clientPort));
    }

    public synchronized void stop() {
        running = false;
        pushSubscribers.clear();
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        DatabaseManager.getInstance().closeConnection();
        log("Server RUDP đã dừng.");
    }

    public boolean isRunning() {
        return running;
    }

    private void log(String msg) {
        if (logListener != null) {
            logListener.onLog(msg);
        } else {
            System.out.println("[SERVER] " + msg);
        }
    }
}
