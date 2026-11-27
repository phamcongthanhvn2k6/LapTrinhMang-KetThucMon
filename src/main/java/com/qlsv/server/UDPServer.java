package com.qlsv.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qlsv.database.DatabaseManager;
import com.qlsv.database.dao.AuditDAO;
import com.qlsv.model.*;
import com.qlsv.network.PacketType;
import com.qlsv.network.UDPPacket;
import com.qlsv.util.InputValidator;

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
    private final AtomicLong totalSecurityThreatsBlocked = new AtomicLong(0);

    // Active Client Push Subscribers (Key: "ip:pushPort" -> InetSocketAddress)
    private final Map<String, InetSocketAddress> pushSubscribers = new ConcurrentHashMap<>();

    // Kho đề cương & tài liệu học phần phục vụ truyền tệp phân mảnh qua RUDP (Chunked File Stream)
    private final Map<String, String> courseMaterials = new ConcurrentHashMap<>();

    // Danh sách Đơn Phúc Khảo Điểm Hai Chiều (Student <-> Teacher/Board)
    private final List<GradeAppeal> gradeAppeals = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.concurrent.atomic.AtomicInteger appealIdCounter = new java.util.concurrent.atomic.AtomicInteger(1001);

    // RUDP Idempotent Response Cache (LRU cache of 500 recent packetIds -> UDPPacket response)
    private final Map<String, UDPPacket> responseCache = Collections.synchronizedMap(
            new LinkedHashMap<String, UDPPacket>(100, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, UDPPacket> eldest) {
                    return size() > 500;
                }
            }
    );

    public UDPServer(int port) {
        this.port = port;
        initDefaultCourseMaterials();
        initDefaultGradeAppeals();
    }

    private void initDefaultGradeAppeals() {
        gradeAppeals.add(new GradeAppeal(
                appealIdCounter.getAndIncrement(),
                "SV001", "INT1339",
                "Em xin phúc khảo bài thi kết thúc học phần môn Lập Trình Mạng (Điểm hiện tại 5.70).",
                "CHỜ DUYỆT", "Đang chờ Hội đồng chuyên môn chấm lại", "2025-11-24 08:30:00"
        ));
        gradeAppeals.add(new GradeAppeal(
                appealIdCounter.getAndIncrement(),
                "SV006", "INT1310",
                "Em xin kiểm tra lại điểm cộng thực hành môn Cơ Sở Dữ Liệu SQL.",
                "CHỜ DUYỆT", "Đang chờ Giảng viên bộ môn xác nhận", "2025-11-24 09:15:00"
        ));
    }

    private void initDefaultCourseMaterials() {
        courseMaterials.put("INT1339",
                "======================================================================\n" +
                "   HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG (PTIT) - KHOA CNTT 1\n" +
                "   ĐỀ CƯƠNG CHI TIẾT & TÀI LIỆU ÔN TẬP HỌC PHẦN: LẬP TRÌNH MẠNG\n" +
                "   Mã học phần: INT1339  |  Số tín chỉ: 03 (2 LT + 1 TH)\n" +
                "======================================================================\n\n" +
                "1. MỤC TIÊU HỌC PHẦN:\n" +
                "   - Nắm vững kiến trúc phân tầng TCP/IP, lập trình Socket TCP & UDP.\n" +
                "   - Thiết kế giao thức truyền thông tin cậy RUDP (Sequence, ACK, ARQ, GZIP).\n" +
                "   - Bảo mật dữ liệu đường truyền với HMAC-SHA256, DES và AES-256-GCM.\n" +
                "   - Xây dựng hệ thống phân tán đa luồng kết nối Cloud Database Pooler.\n\n" +
                "2. CẤU TRÚC ĐÁNH GIÁ ĐIỂM HỌC PHẦN (CHUẨN TÍN CHỈ PTIT):\n" +
                "   - Điểm Chuyên cần (10%): Yêu cầu >= 5.0 (Dưới 5.0 bị CẤM THI - Điểm F).\n" +
                "   - Điểm Kiểm tra Giữa kỳ & Thực hành (20%): Kiểm tra lập trình Socket.\n" +
                "   - Điểm Thi Kết thúc học phần (70%): Bảo vệ đồ án & Vấn đáp (Điểm liệt < 4.0).\n\n" +
                "3. NỘI DUNG ÔN TẬP TRỌNG TÂM:\n" +
                "   - Chương 1: Quản lý luồng I/O và Đa luồng (ThreadPoolExecutor, SwingWorker).\n" +
                "   - Chương 2: Lập trình DatagramSocket & DatagramPacket (UDP Unicast/Push).\n" +
                "   - Chương 3: Cơ chế chống mất gói Stop-and-Wait ARQ & Idempotent Cache.\n" +
                "   - Chương 4: Tích hợp JDBC HikariCP & An toàn dữ liệu CSDL phân tán.\n");
        courseMaterials.put("INT1340",
                "======================================================================\n" +
                "   ĐỀ CƯƠNG HỌC PHẦN: MÃ HÓA & AN TOÀN THÔNG TIN (INT1340 - 3 TÍN CHỈ)\n" +
                "======================================================================\n" +
                "- Trọng tâm 1: Mã hóa khối đối xứng DES (56-bit) & AES-256-GCM (128-bit Tag).\n" +
                "- Trọng tâm 2: Hàm băm mật mã SHA-256, PBKDF2 & Mã xác thực HMAC-SHA256.\n" +
                "- Trọng tâm 3: Phòng chống Replay Attack, Brute-Force & Lưu vết Audit Trail.\n");
        courseMaterials.put("INT1310",
                "======================================================================\n" +
                "   ĐỀ CƯƠNG HỌC PHẦN: CƠ SỞ DỮ LIỆU SQL (INT1310 - 3 TÍN CHỈ)\n" +
                "======================================================================\n" +
                "- Thiết kế lược đồ ERD chuẩn 3NF, Giao dịch ACID (Commit / Rollback).\n" +
                "- Tối ưu truy vấn phân trang LIMIT/OFFSET và quản lý HikariCP Connection Pool.\n");
    }

    private static String computeSha256Hex(String text) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
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

    public long getTotalSecurityThreatsBlocked() {
        return totalSecurityThreatsBlocked.get();
    }

    public synchronized void start() throws Exception {
        if (running) return;
        socket = new DatagramSocket(port);
        running = true;
        threadPool = Executors.newFixedThreadPool(20);

        log(String.format("Server RUDP 2.0 khởi chạy trên cổng %d (20 Workers, HMAC-SHA256, Anti-Replay, Server-Push)", port));
        DatabaseManager.getInstance().logAudit("system", "SERVER_START",
                "Khởi chạy Máy chủ RUDP 2.0 trên cổng " + port + " (20 Workers, HMAC-SHA256, Anti-Replay, Server-Push)", "127.0.0.1");

        serverThread = new Thread(() -> {
            if (!DatabaseManager.getInstance().isConnected()) {
                try {
                    SqlConfig config = com.qlsv.database.DatabaseConfigLoader.loadConfig();
                    DatabaseManager.getInstance().connect(config);
                    log("Tự động kết nối CSDL (" + config.getDbType() + ") từ db.properties qua HikariCP thành công.");
                    DatabaseManager.getInstance().logAudit("system", "DB_CONNECTED",
                            "Kết nối CSDL đám mây " + config.getDbType() + " qua HikariCP thành công (Chuẩn mã hóa: " + com.qlsv.security.SecurityManager.getCurrentAlgo().name() + ")", "127.0.0.1");
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
                totalSecurityThreatsBlocked.incrementAndGet();
                String alert = String.format("Chặn đứng gói tin [Seq#%d | %s] bị sửa đổi Payload trên đường truyền (Sai chữ ký HMAC-SHA256)!",
                        seq, packet.getType());
                log("[IDS CẢNH BÁO AN NINH - MITM TAMPERING] " + alert);
                DatabaseManager.getInstance().logAudit("IDS-FIREWALL", "BLOCK_MITM_HMAC", alert, clientIp);
                broadcastPushEvent("SECURITY_ALERT", "[IDS CẢNH BÁO AN NINH] " + alert, clientIp);
                if (logListener != null) {
                    logListener.onDataUpdated();
                }
                sendPacket(UDPPacket.createError("BẢO MẬT RUDP (IDS): Chữ ký số HMAC-SHA256 không hợp lệ (Gói tin có dấu hiệu bị can thiệp MITM)!"),
                        seq, clientAddress, clientPort);
                return;
            }

            if (!packet.isReplayValid()) {
                totalSecurityThreatsBlocked.incrementAndGet();
                String alert = String.format("Chặn đứng gói tin phát lại quá hạn Timestamp (Replay Attack > 5 phút) [Seq#%d | %s]!",
                        seq, packet.getType());
                log("[IDS CẢNH BÁO AN NINH - REPLAY ATTACK] " + alert);
                DatabaseManager.getInstance().logAudit("IDS-FIREWALL", "BLOCK_REPLAY_ATTACK", alert, clientIp);
                broadcastPushEvent("SECURITY_ALERT", "[IDS CẢNH BÁO AN NINH] " + alert, clientIp);
                if (logListener != null) {
                    logListener.onDataUpdated();
                }
                sendPacket(UDPPacket.createError("BẢO MẬT RUDP (IDS): Gói tin đã hết hạn Timestamp (Chống tấn công phát lại - Anti-Replay)!"),
                        seq, clientAddress, clientPort);
                return;
            }

            log(String.format("Nhận [Seq#%d | %s | HMAC:OK] từ Client %s:%d", seq, packet.getType(), clientIp, clientPort));

            // 3. Idempotent Deduplication Check (Chỉ cache cho các lệnh ghi/sửa để bảo vệ ACID, không cache các lệnh truy vấn đọc)
            boolean isMutatingRequest = (packet.getType() == PacketType.ADD_STUDENT ||
                    packet.getType() == PacketType.UPDATE_STUDENT ||
                    packet.getType() == PacketType.DELETE_STUDENT ||
                    packet.getType() == PacketType.UPDATE_COURSE_GRADE ||
                    packet.getType() == PacketType.SUBMIT_GRADE_APPEAL ||
                    packet.getType() == PacketType.RESOLVE_GRADE_APPEAL ||
                    packet.getType() == PacketType.REGISTER_CREDITS ||
                    packet.getType() == PacketType.REGISTER_ACCOUNT ||
                    packet.getType() == PacketType.CHANGE_PASSWORD ||
                    packet.getType() == PacketType.IMPORT_EXCEL);
            if (isMutatingRequest && packet.getPacketId() != null && responseCache.containsKey(packet.getPacketId())) {
                UDPPacket cachedResp = responseCache.get(packet.getPacketId());
                if (cachedResp != null) {
                    log(String.format("[ARQ IDEMPOTENT CACHE] Phát hiện gói ghi truyền lại #%d [%s] -> Gửi lại phản hồi đã cache, bảo vệ toàn vẹn CSDL.",
                            seq, packet.getPacketId().substring(0, 8)));
                    sendPacket(cachedResp, seq, clientAddress, clientPort);
                    return;
                }
            }

            UDPPacket responsePacket = null;

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
                            totalSecurityThreatsBlocked.incrementAndGet();
                            DatabaseManager.getInstance().logAudit(username, "LOGIN_LOCKED",
                                    "Tài khoản đang bị khóa Brute-force (" + lockRemain + "s)", clientIp);
                            log(String.format("[CẢNH BÁO BRUTE-FORCE] Chặn đăng nhập '%s' từ %s (Đang khóa còn %ds)", username, clientIp, lockRemain));
                            if (logListener != null) {
                                logListener.onDataUpdated();
                            }
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
                            if (failCount >= 5) {
                                totalSecurityThreatsBlocked.incrementAndGet();
                                broadcastPushEvent("SECURITY_ALERT",
                                        "[IDS BRUTE-FORCE LOCKOUT] Đã khóa tài khoản '" + username + "' trong 60s do dò mật khẩu 5 lần liên tiếp!", clientIp);
                            }
                            DatabaseManager.getInstance().logAudit(username, "LOGIN_FAILED",
                                    "Sai mật khẩu lần " + failCount + "/5", clientIp);
                            log(String.format("Xác thực THẤT BẠI cho '%s' (Lần sai: %d/5)", username, failCount));
                            if (logListener != null) {
                                logListener.onDataUpdated();
                            }
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

                case REGISTER_ACCOUNT:
                    try {
                        String[] params = gson.fromJson(packet.getPayload(), String[].class);
                        String regUser = params.length > 0 ? params[0].trim() : "";
                        String regPass = params.length > 1 ? params[1].trim() : "";
                        String regRoleStr = params.length > 2 ? params[2].trim() : "HOCSINH";
                        String regSvId = params.length > 3 ? params[3].trim() : "SV001";
                        String regName = params.length > 4 ? params[4].trim() : regUser;
                        String regEmail = params.length > 5 ? params[5].trim() : "";
                        String inviteCode = params.length > 6 ? params[6].trim() : "";

                        UserAccount.Role targetRole = UserAccount.Role.HOCSINH;
                        if ("GIANGVIEN".equalsIgnoreCase(regRoleStr)) {
                            if (!"PTIT-GV-2025".equalsIgnoreCase(inviteCode) && !"ptit123".equalsIgnoreCase(inviteCode)) {
                                totalSecurityThreatsBlocked.incrementAndGet();
                                DatabaseManager.getInstance().logAudit(regUser, "REGISTER_GV_REJECTED",
                                        "Từ chối đăng ký quyền Giảng viên do nhập sai Mã cấp quyền Bộ môn", clientIp);
                                responsePacket = UDPPacket.createError("BẢO MẬT PHÂN QUYỀN: Mã xác thực cấp quyền Giảng viên không chính xác! Vui lòng liên hệ Phòng Đào tạo.");
                                break;
                            }
                            targetRole = UserAccount.Role.GIANGVIEN;
                        }

                        UserAccount newAcc = DatabaseManager.getInstance().registerNewAccount(
                                regUser, regPass, targetRole, regSvId, regName, regEmail);
                        String token = SessionManager.getInstance().createSession(newAcc);
                        String detail = String.format("Đăng ký tài khoản mới '%s' (Vai trò: %s | Mã SV: %s | Băm: SHA-256 + Salt 16B)",
                                newAcc.getUsername(), newAcc.getRole(), newAcc.getStudentId());
                        DatabaseManager.getInstance().logAudit(newAcc.getUsername(), "REGISTER_ACCOUNT", detail, clientIp);
                        log("[ĐĂNG KÝ TÀI KHOẢN MỚI] " + detail);
                        if (logListener != null) {
                            logListener.onDataUpdated();
                        }
                        responsePacket = UDPPacket.createLoginResponse(true,
                                "Đăng ký tài khoản thành công (Đã băm mật khẩu SHA-256 + Salt)!", token, gson.toJson(newAcc));
                    } catch (Exception ex) {
                        log("Từ chối đăng ký tài khoản: " + ex.getMessage());
                        responsePacket = UDPPacket.createError(ex.getMessage());
                    }
                    break;

                case CHANGE_PASSWORD:
                    try {
                        String[] params = gson.fromJson(packet.getPayload(), String[].class);
                        String cpUser = params.length > 0 ? params[0].trim() : "";
                        String oldPass = params.length > 1 ? params[1].trim() : "";
                        String newPass = params.length > 2 ? params[2].trim() : "";

                        long lockRemain = SessionManager.getInstance().getRemainingLockoutSeconds(cpUser);
                        if (lockRemain > 0) {
                            totalSecurityThreatsBlocked.incrementAndGet();
                            responsePacket = UDPPacket.createError("BẢO VỆ CHỐNG BRUTE-FORCE: Tài khoản đang tạm khóa (" + lockRemain + " giây)!");
                            break;
                        }
                        try {
                            DatabaseManager.getInstance().changePassword(cpUser, oldPass, newPass);
                            SessionManager.getInstance().clearLoginFailures(cpUser);
                            String detail = String.format("Đổi mật khẩu tài khoản '%s' thành công (Đã sinh Salt mới & tái băm SHA-256)", cpUser);
                            DatabaseManager.getInstance().logAudit(cpUser, "CHANGE_PASSWORD", detail, clientIp);
                            log("[ĐỔI MẬT KHẨU] " + detail);
                            if (logListener != null) {
                                logListener.onDataUpdated();
                            }
                            responsePacket = new UDPPacket(PacketType.CHANGE_PASSWORD, true, detail, cpUser);
                        } catch (IllegalArgumentException authEx) {
                            if (authEx.getMessage() != null && authEx.getMessage().contains("không chính xác")) {
                                int failCount = SessionManager.getInstance().recordLoginFailure(cpUser);
                                if (failCount >= 5) {
                                    totalSecurityThreatsBlocked.incrementAndGet();
                                }
                            }
                            throw authEx;
                        }
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError(ex.getMessage());
                    }
                    break;

                case CONNECT_DB:
                    try {
                        String token = packet.getToken();
                        SessionManager.UserSession session = SessionManager.getInstance().getSession(token);
                        if (session == null || session.getRole() == UserAccount.Role.HOCSINH) {
                            totalSecurityThreatsBlocked.incrementAndGet();
                            DatabaseManager.getInstance().logAudit(session != null ? session.getUsername() : "anonymous", "RBAC_VIOLATION_DB",
                                    "Chặn kết nối trái phép thay đổi cấu hình CSDL (Thiếu Token hoặc quyền Học sinh)", clientIp);
                            responsePacket = UDPPacket.createDbStatus(false, "BẢO VỆ PHÂN QUYỀN SERVER (ZERO-TRUST): Yêu cầu đăng nhập quyền Quản trị viên hoặc Giảng viên để thay đổi cấu hình CSDL!");
                            break;
                        }
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
                        if (session == null || session.getRole() == UserAccount.Role.HOCSINH) {
                            totalSecurityThreatsBlocked.incrementAndGet();
                            responsePacket = UDPPacket.createError("BẢO VỆ PHÂN QUYỀN SERVER (ZERO-TRUST): Chỉ Quản trị viên / Phòng Giáo vụ đã xác thực mới có quyền Khóa hoặc Mở khóa sổ điểm!");
                        } else {
                            boolean targetLock = Boolean.parseBoolean(packet.getPayload());
                            String operator = session.getUsername();
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
                        if (session == null || session.getRole() == UserAccount.Role.HOCSINH) {
                            totalSecurityThreatsBlocked.incrementAndGet();
                            responsePacket = UDPPacket.createError("BẢO VỆ PHÂN QUYỀN SERVER (ZERO-TRUST): Yêu cầu đăng nhập quyền Giảng viên/Admin để phát thông báo đào tạo!");
                        } else {
                            String operator = session.getFullName();
                            String text = InputValidator.validateAndSanitizeReasonOrNote(packet.getPayload(), "Nội dung thông báo", 3, 400);
                            DatabaseManager.getInstance().logAudit(
                                    session.getUsername(),
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
                        String token = packet.getToken();
                        SessionManager.UserSession session = SessionManager.getInstance().getSession(token);
                        if (session == null || session.getRole() == UserAccount.Role.HOCSINH) {
                            totalSecurityThreatsBlocked.incrementAndGet();
                            responsePacket = UDPPacket.createError("BẢO VỆ PHÂN QUYỀN SERVER (ZERO-TRUST): Chỉ Giảng viên hoặc Quản trị viên mới có quyền truy xuất Nhật ký Thanh tra!");
                            break;
                        }
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

                            if (session == null || session.getRole() == UserAccount.Role.HOCSINH) {
                                totalSecurityThreatsBlocked.incrementAndGet();
                                DatabaseManager.getInstance().logAudit(session != null ? session.getUsername() : "anonymous", "RBAC_VIOLATION_GRADE",
                                        "Chặn gói tin thêm/sửa điểm trái phép (Không có Session Token hợp lệ hoặc quyền Học sinh)", clientIp);
                                responsePacket = UDPPacket.createError("BẢO VỆ PHÂN QUYỀN SERVER (ZERO-TRUST): Yêu cầu đăng nhập phiên Giảng viên/Admin hợp lệ để thêm hoặc cập nhật điểm số!");
                            } else if (SessionManager.getInstance().isGradebookLocked() &&
                                    session.getRole() != UserAccount.Role.ADMIN) {
                                responsePacket = UDPPacket.createError("[KHÓA SỔ ĐIỂM] Sổ điểm học kỳ đã khóa bởi Phòng Giáo Vụ! Giảng viên không thể sửa điểm khi sổ đã khóa (Yêu cầu quyền Admin để mở khóa).");
                            } else {
                                StudentData student = gson.fromJson(packet.getPayload(), StudentData.class);
                                InputValidator.validateAndNormalizeStudentData(student);
                                String operator = session.getUsername();

                                // Lấy điểm cũ trước khi lưu để ghi Audit Trail bất biến (OldScore -> NewScore)
                                StudentResult oldRecord = null;
                                try {
                                    oldRecord = DatabaseManager.getInstance().getStudentDecrypted(student.getStudentId());
                                } catch (Exception ignored) {}

                                StudentResult result = DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(student);

                                try {
                                    String k = DatabaseManager.getInstance().getDesKey();
                                    String encName = com.qlsv.security.SecurityManager.encrypt(result.getFullName(), k);
                                    String encCC = com.qlsv.security.SecurityManager.encrypt(String.valueOf(result.getScoreCC()), k);
                                    String encGK = com.qlsv.security.SecurityManager.encrypt(String.valueOf(result.getScoreGK()), k);
                                    String encCK = com.qlsv.security.SecurityManager.encrypt(String.valueOf(result.getScoreCK()), k);
                                    log(String.format("[CIPHERTEXT BASE64 LƯU DB] ma_sv=%s | ho_ten=%s | cc=%s | gk=%s | ck=%s",
                                            result.getStudentId(), encName, encCC, encGK, encCK));
                                } catch (Exception ignored) {}

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

                            if (session == null || session.getRole() == UserAccount.Role.HOCSINH) {
                                totalSecurityThreatsBlocked.incrementAndGet();
                                DatabaseManager.getInstance().logAudit(session != null ? session.getUsername() : "anonymous", "RBAC_VIOLATION_DELETE",
                                        "Chặn gói tin xóa sinh viên trái phép (Thiếu Token hoặc quyền Học sinh)", clientIp);
                                responsePacket = UDPPacket.createError("BẢO VỆ PHÂN QUYỀN SERVER (ZERO-TRUST): Yêu cầu đăng nhập phiên Giảng viên/Admin hợp lệ để xóa dữ liệu sinh viên!");
                            } else if (SessionManager.getInstance().isGradebookLocked() &&
                                    session.getRole() != UserAccount.Role.ADMIN) {
                                responsePacket = UDPPacket.createError("[KHÓA SỔ ĐIỂM] Sổ điểm học kỳ đã khóa! Không thể xóa hồ sơ sinh viên khi sổ điểm đã khóa.");
                            } else {
                                String studentId = InputValidator.normalizeAndValidateStudentId(packet.getPayload());
                                String operator = session.getUsername();
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
                        if (query != null && !query.trim().isEmpty()) {
                            DatabaseManager.getInstance().logAudit("client", "SEARCH_STUDENTS",
                                    "Tra cứu danh sách sinh viên với từ khóa: '" + query.trim() + "' (Kết quả: " + list.size() + " SV)", clientIp);
                        }
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
                            DatabaseManager.getInstance().logAudit("client", "SEARCH_STUDENTS",
                                    "Tìm kiếm phân trang từ khóa: '" + pageReq.getQuery().trim() + "' (Trang " + pageReq.getPage() + ")", clientIp);
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
                        DatabaseManager.getInstance().logAudit(studentId != null ? studentId.trim().toUpperCase() : "student",
                                "VIEW_TRANSCRIPT",
                                String.format("Tra cứu bảng điểm học phần của %s (%s) - CPA: %.2f (%s)",
                                        transcript.getFullName(), transcript.getStudentId(),
                                        transcript.getCumulativeGpa4(), transcript.getAcademicRank()),
                                clientIp);
                        responsePacket = UDPPacket.createTranscriptResult(true, "Lấy bảng điểm thành công", gson.toJson(transcript));
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi tải bảng điểm: " + ex.getMessage());
                    }
                    break;

                case UPDATE_COURSE_GRADE:
                    try {
                        String token = packet.getToken();
                        SessionManager.UserSession session = SessionManager.getInstance().getSession(token);
                        if (session == null || session.getRole() == UserAccount.Role.HOCSINH) {
                            totalSecurityThreatsBlocked.incrementAndGet();
                            DatabaseManager.getInstance().logAudit(session != null ? session.getUsername() : "anonymous", "RBAC_VIOLATION_COURSE_GRADE",
                                    "Chặn chấm điểm học phần trái phép (Thiếu Token hoặc quyền Học sinh)", clientIp);
                            responsePacket = UDPPacket.createError("BẢO VỆ PHÂN QUYỀN SERVER (ZERO-TRUST): Yêu cầu đăng nhập phiên Giảng viên/Admin hợp lệ để chấm điểm học phần!");
                        } else if (SessionManager.getInstance().isGradebookLocked() &&
                                session.getRole() != UserAccount.Role.ADMIN) {
                            responsePacket = UDPPacket.createError("[KHÓA SỔ ĐIỂM] Sổ điểm học kỳ đang khóa! Không thể sửa điểm học phần.");
                        } else {
                            String[] parts = packet.getPayload().split("\\|");
                            String stId = InputValidator.normalizeAndValidateStudentId(parts[0]);
                            String cCode = InputValidator.normalizeAndValidateCourseCode(parts[1]);
                            double procScore = InputValidator.validateScoreValue(Double.parseDouble(parts[2]), "Điểm Quá trình");
                            double exScore = InputValidator.validateScoreValue(Double.parseDouble(parts[3]), "Điểm Thi kết thúc");

                            StudentTranscript updatedT = DatabaseManager.getInstance().updateCourseGrade(stId, cCode, procScore, exScore);
                            String operator = session.getUsername();
                            String detail = String.format("Chấm điểm môn %s của %s: Quá trình=%.2f, Thi=%.2f -> GPA Tích lũy mới=%.2f (%s)",
                                    cCode, stId, procScore, exScore, updatedT.getCumulativeGpa4(), updatedT.getAcademicRank());
                            DatabaseManager.getInstance().logAudit(operator, "UPDATE_COURSE_GRADE", detail, clientIp);
                            log("[" + operator + "] " + detail);

                            responsePacket = UDPPacket.createTranscriptResult(true, "Đã cập nhật điểm môn " + cCode, gson.toJson(updatedT));
                            broadcastPushEvent("COURSE_GRADE_UPDATED", detail, stId);
                            if (logListener != null) {
                                logListener.onDataUpdated();
                            }
                        }
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi cập nhật điểm môn học: " + ex.getMessage());
                    }
                    break;

                case SUBMIT_GRADE_APPEAL:
                    try {
                        String token = packet.getToken();
                        SessionManager.UserSession session = SessionManager.getInstance().getSession(token);
                        String[] parts = packet.getPayload().split("\\|", 3);
                        String stId = InputValidator.normalizeAndValidateStudentId(parts[0]);
                        String cCode = InputValidator.normalizeAndValidateCourseCode(parts.length > 1 ? parts[1] : "INT1339");
                        String reason = InputValidator.validateAndSanitizeReasonOrNote(
                                parts.length > 2 ? parts[2] : "Xin phúc khảo điểm bài thi cuối kỳ",
                                "Lý do phúc khảo", 5, 300);

                        if (session != null && session.getRole() == UserAccount.Role.HOCSINH
                                && session.getStudentId() != null && !session.getStudentId().isEmpty()
                                && !session.getStudentId().equalsIgnoreCase(stId)) {
                            totalSecurityThreatsBlocked.incrementAndGet();
                            DatabaseManager.getInstance().logAudit(session.getUsername(), "IDOR_APPEAL_BLOCKED",
                                    "Chặn SV " + session.getStudentId() + " gửi đơn phúc khảo mạo danh Mã SV " + stId, clientIp);
                            responsePacket = UDPPacket.createError("BẢO MẬT ĐỊNH DANH (ANTI-IDOR): Tài khoản của bạn liên kết với Mã SV " + session.getStudentId() + ", không được phép gửi đơn phúc khảo mạo danh Mã SV " + stId + "!");
                            break;
                        }
                        // Kiểm tra sinh viên có tồn tại thực sự trong CSDL hay không trước khi tạo đơn
                        DatabaseManager.getInstance().getStudentDecrypted(stId);

                        boolean hasPendingAppeal = false;
                        for (GradeAppeal existing : gradeAppeals) {
                            if (existing.getStudentId().equalsIgnoreCase(stId)
                                    && existing.getCourseCode().equalsIgnoreCase(cCode)
                                    && "CHỜ DUYỆT".equalsIgnoreCase(existing.getStatus())) {
                                responsePacket = UDPPacket.createError("Sinh viên " + stId + " đã có 1 đơn phúc khảo môn " + cCode + " đang ở trạng thái CHỜ DUYỆT (#" + existing.getAppealId() + ")! Vui lòng chờ Hội đồng xét duyệt.");
                                hasPendingAppeal = true;
                                break;
                            }
                        }
                        if (hasPendingAppeal) {
                            break;
                        }

                        int newAppealId = appealIdCounter.getAndIncrement();
                        String nowStr = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                        GradeAppeal appeal = new GradeAppeal(newAppealId, stId, cCode, reason,
                                "CHỜ DUYỆT", "Chờ Giảng viên xét duyệt", nowStr);
                        gradeAppeals.add(0, appeal);

                        String appealMsg = String.format("Đơn phúc khảo #%d từ SV %s (Môn %s): \"%s\"", newAppealId, stId, cCode, reason);
                        DatabaseManager.getInstance().logAudit(stId, "GRADE_APPEAL", appealMsg, clientIp);
                        log("[PHÚC KHẢO ĐIỂM] " + appealMsg);

                        broadcastPushEvent("GRADE_APPEAL", appealMsg, stId + "|" + cCode);
                        if (logListener != null) {
                            logListener.onDataUpdated();
                        }
                        responsePacket = new UDPPacket(PacketType.SUBMIT_GRADE_APPEAL, true,
                                "Đã gửi đơn phúc khảo #" + newAppealId + " (Môn " + cCode + ") tới Hội đồng & Giảng viên bộ môn!", appealMsg);
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi gửi đơn phúc khảo: " + ex.getMessage());
                    }
                    break;

                case GET_GRADE_APPEALS:
                    responsePacket = UDPPacket.createGradeAppealsResult(gson.toJson(gradeAppeals));
                    break;

                case RESOLVE_GRADE_APPEAL:
                    try {
                        String token = packet.getToken();
                        SessionManager.UserSession session = SessionManager.getInstance().getSession(token);
                        if (session == null || session.getRole() == UserAccount.Role.HOCSINH) {
                            totalSecurityThreatsBlocked.incrementAndGet();
                            DatabaseManager.getInstance().logAudit(session != null ? session.getUsername() : "anonymous", "RBAC_VIOLATION_APPEAL",
                                    "Chặn xét duyệt phúc khảo trái phép (Thiếu Token hoặc quyền Học sinh)", clientIp);
                            responsePacket = UDPPacket.createError("BẢO VỆ PHÂN QUYỀN SERVER (ZERO-TRUST): Yêu cầu đăng nhập phiên Giảng viên/Admin hợp lệ để xét duyệt đơn phúc khảo!");
                        } else if (SessionManager.getInstance().isGradebookLocked() &&
                                session.getRole() != UserAccount.Role.ADMIN) {
                            responsePacket = UDPPacket.createError("[KHÓA SỔ ĐIỂM] Sổ điểm học kỳ đã khóa bởi Phòng Giáo Vụ! Không thể duyệt sửa điểm phúc khảo khi sổ đang khóa.");
                        } else {
                            String[] parts = packet.getPayload().split("\\|", 5);
                            int apId = Integer.parseInt(parts[0].trim());
                            boolean approved = Boolean.parseBoolean(parts[1].trim());
                            double newProc = Double.parseDouble(parts[2].trim());
                            double newExam = Double.parseDouble(parts[3].trim());
                            String rawNote = parts.length > 4 ? parts[4].trim() : "";
                            String note = rawNote.isEmpty() ? "" : InputValidator.validateAndSanitizeReasonOrNote(rawNote, "Kết luận phúc khảo", 3, 300);

                            GradeAppeal target = null;
                            for (GradeAppeal ga : gradeAppeals) {
                                if (ga.getAppealId() == apId) {
                                    target = ga;
                                    break;
                                }
                            }
                            if (target == null) {
                                responsePacket = UDPPacket.createError("Không tìm thấy đơn phúc khảo #" + apId);
                            } else if (!"CHỜ DUYỆT".equalsIgnoreCase(target.getStatus())) {
                                responsePacket = UDPPacket.createError("Đơn phúc khảo #" + apId + " đã được xử lý trước đó (Trạng thái: " + target.getStatus() + "), không thể xét duyệt lại!");
                            } else {
                                String operator = session.getUsername();
                                if (approved) {
                                    InputValidator.validateScoreValue(newProc, "Điểm Quá trình mới");
                                    InputValidator.validateScoreValue(newExam, "Điểm Thi mới");
                                    StudentTranscript updatedT = DatabaseManager.getInstance().updateCourseGrade(
                                            target.getStudentId(), target.getCourseCode(), newProc, newExam);
                                    target.setStatus("ĐÃ DUYỆT NÂNG ĐIỂM");
                                    target.setTeacherNote(String.format("Đạt QT=%.2f, Thi=%.2f (GPA mới: %.2f) - %s",
                                            newProc, newExam, updatedT.getCumulativeGpa4(), note));
                                    String msg = String.format("Đơn phúc khảo #%d (%s - Môn %s) ĐÃ ĐƯỢC DUYỆT: Điểm QT=%.2f, Thi=%.2f!",
                                            apId, target.getStudentId(), target.getCourseCode(), newProc, newExam);
                                    DatabaseManager.getInstance().logAudit(operator, "APPROVE_APPEAL", msg, clientIp);
                                    log("[DUYỆT PHÚC KHẢO] " + msg);
                                    broadcastPushEvent("APPEAL_RESOLVED", msg, target.getStudentId());
                                } else {
                                    target.setStatus("TỪ CHỐI");
                                    target.setTeacherNote(note.isEmpty() ? "Đã chấm thẩm định lại, giữ nguyên điểm gốc" : note);
                                    String msg = String.format("Đơn phúc khảo #%d (%s - Môn %s) TỪ CHỐI: %s",
                                            apId, target.getStudentId(), target.getCourseCode(), target.getTeacherNote());
                                    DatabaseManager.getInstance().logAudit(operator, "REJECT_APPEAL", msg, clientIp);
                                    log("[TỪ CHỐI PHÚC KHẢO] " + msg);
                                    broadcastPushEvent("APPEAL_REJECTED", msg, target.getStudentId());
                                }
                                if (logListener != null) {
                                    logListener.onDataUpdated();
                                }
                                responsePacket = UDPPacket.createGradeAppealsResult(gson.toJson(gradeAppeals));
                            }
                        }
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi xét duyệt phúc khảo: " + ex.getMessage());
                    }
                    break;

                case REGISTER_CREDITS:
                    try {
                        String token = packet.getToken();
                        SessionManager.UserSession session = SessionManager.getInstance().getSession(token);
                        String[] parts = packet.getPayload().split("\\|", 3);
                        String stId = InputValidator.normalizeAndValidateStudentId(parts[0]);
                        int reqCredits = Integer.parseInt(parts[1].trim());
                        String coursesCsv = parts.length > 2 ? parts[2].trim() : "";

                        if (session != null && session.getRole() == UserAccount.Role.HOCSINH
                                && session.getStudentId() != null && !session.getStudentId().isEmpty()
                                && !session.getStudentId().equalsIgnoreCase(stId)) {
                            totalSecurityThreatsBlocked.incrementAndGet();
                            DatabaseManager.getInstance().logAudit(session.getUsername(), "IDOR_CREDITS_BLOCKED",
                                    "Chặn SV " + session.getStudentId() + " đăng ký tín chỉ mạo danh Mã SV " + stId, clientIp);
                            responsePacket = UDPPacket.createError("BẢO MẬT ĐỊNH DANH (ANTI-IDOR): Tài khoản của bạn liên kết với Mã SV " + session.getStudentId() + ", không được phép đăng ký tín chỉ thay cho Mã SV " + stId + "!");
                            break;
                        }

                        StudentResult student = DatabaseManager.getInstance().getStudentDecrypted(stId);
                        StudentTranscript transcript = DatabaseManager.getInstance().getDetailedTranscript(stId);
                        double cumulativeGpa4 = transcript.getCumulativeGpa4();
                        boolean isWarning = cumulativeGpa4 < 2.0 || student.getGpa4() < 2.0 || student.isExamBanned() || student.isFinalExamFailed();
                        int maxCredits = isWarning ? 14 : 22;

                        if (reqCredits < 10) {
                            responsePacket = UDPPacket.createError(
                                    "QUY CHẾ ĐÀO TẠO TÍN CHỈ: Số tín chỉ đăng ký tối thiểu trong 1 học kỳ chính là 10 tín chỉ (Bạn đang chọn " + reqCredits + " TC).\nVui lòng chọn thêm học phần!");
                        } else if (reqCredits > maxCredits) {
                            String err = String.format(
                                    "QUY CHẾ ĐÀO TẠO TÍN CHỈ: Sinh viên %s (%s) hiện có CPA Tích lũy = %.2f (GPA môn chính = %.2f - %s),\n" +
                                    "định mức đăng ký tối đa là %d tín chỉ/học kỳ (Bạn đang chọn %d TC).\n" +
                                    "Vui lòng bỏ bớt học phần tự chọn để đảm bảo chất lượng học tập!",
                                    student.getFullName(), stId, cumulativeGpa4, student.getGpa4(), student.getAcademicStatus(), maxCredits, reqCredits);
                            responsePacket = UDPPacket.createError(err);
                        } else {
                            long tuitionVnd = reqCredits * 650_000L;
                            String msg = String.format("Đăng ký học phần HK2 thành công cho %s (%s): %d/%d TC [%s] - Học phí dự kiến: %,d VNĐ",
                                    student.getFullName(), stId, reqCredits, maxCredits, coursesCsv, tuitionVnd);
                            DatabaseManager.getInstance().logAudit(stId, "REGISTER_CREDITS", msg, clientIp);
                            log("[ĐĂNG KÝ TÍN CHỈ] " + msg);
                            broadcastPushEvent("CREDIT_REGISTERED", msg, stId);
                            if (logListener != null) {
                                logListener.onDataUpdated();
                            }
                            responsePacket = new UDPPacket(PacketType.REGISTER_CREDITS, true, msg, String.valueOf(reqCredits));
                        }
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi đăng ký tín chỉ: " + ex.getMessage());
                    }
                    break;

                case ROTATE_CRYPTO_KEY:
                    try {
                        String token = packet.getToken();
                        SessionManager.UserSession session = SessionManager.getInstance().getSession(token);
                        if (session == null || session.getRole() != UserAccount.Role.ADMIN) {
                            totalSecurityThreatsBlocked.incrementAndGet();
                            DatabaseManager.getInstance().logAudit(session != null ? session.getUsername() : "anonymous", "RBAC_VIOLATION_KEY",
                                    "Chặn xoay khóa mật mã CSDL trái phép (Yêu cầu quyền ADMIN)", clientIp);
                            responsePacket = UDPPacket.createError("BẢO VỆ PHÂN QUYỀN SERVER (ZERO-TRUST): Chỉ Quản trị viên (ADMIN) đã xác thực mới có quyền Xoay khóa & Tái mã hóa CSDL!");
                            break;
                        }
                        String[] parts = packet.getPayload().split("\\|", 2);
                        com.qlsv.security.SecurityManager.EncryptionAlgo newAlgo =
                                com.qlsv.security.SecurityManager.EncryptionAlgo.valueOf(parts[0].trim());
                        String newKey = parts.length > 1 ? parts[1].trim() : DatabaseManager.getInstance().getDesKey();

                        int count = DatabaseManager.getInstance().rotateEncryptionForAllRecords(newKey, newAlgo);
                        String msg = String.format("Đã xoay khóa & tái mã hóa thành công %d hồ sơ sinh viên sang chuẩn %s!",
                                count, newAlgo.getDisplayName());
                        DatabaseManager.getInstance().logAudit(session.getUsername(), "KEY_ROTATION", msg, clientIp);
                        log("[XOAY KHÓA MẬT MÃ CSDL] " + msg);
                        if (logListener != null) {
                            logListener.onDataUpdated();
                        }
                        broadcastPushEvent("KEY_ROTATION", msg, newAlgo.name());
                        responsePacket = new UDPPacket(PacketType.ROTATE_CRYPTO_KEY, true, msg, String.valueOf(count));
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi xoay khóa mã hóa CSDL: " + ex.getMessage());
                    }
                    break;

                case DOWNLOAD_COURSE_MATERIAL:
                    try {
                        String[] parts = packet.getPayload().split("\\|");
                        String cCode = parts[0].trim().toUpperCase();
                        int reqChunk = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;

                        String fullMaterial = courseMaterials.getOrDefault(cCode, courseMaterials.get("INT1339"));
                        String sha256 = computeSha256Hex(fullMaterial);

                        int chunkSize = 240; // Phân mảnh 240 ký tự/gói để trình diễn trực quan cơ chế RUDP Chunk Streaming
                        int totalChunks = Math.max(1, (int) Math.ceil((double) fullMaterial.length() / chunkSize));
                        if (reqChunk < 0) reqChunk = 0;
                        if (reqChunk >= totalChunks) reqChunk = totalChunks - 1;

                        int startIdx = reqChunk * chunkSize;
                        int endIdx = Math.min(fullMaterial.length(), startIdx + chunkSize);
                        String slice = fullMaterial.substring(startIdx, endIdx);

                        log(String.format("[RUDP FILE STREAM] Truyền mảnh %d/%d đề cương %s (%d bytes | SHA-256: %s...)",
                                reqChunk + 1, totalChunks, cCode, slice.length(), sha256.substring(0, 12)));
                        if (reqChunk == totalChunks - 1) {
                            DatabaseManager.getInstance().logAudit("student", "DOWNLOAD_SYLLABUS",
                                    String.format("Tải hoàn tất đề cương học phần %s (%d mảnh RUDP | SHA-256: %s...)",
                                            cCode, totalChunks, sha256.substring(0, 12)), clientIp);
                        }
                        responsePacket = UDPPacket.createCourseMaterialChunk(cCode, reqChunk, totalChunks, sha256, slice);
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi tải tài liệu học phần: " + ex.getMessage());
                    }
                    break;

                case UPLOAD_COURSE_MATERIAL:
                    try {
                        String token = packet.getToken();
                        SessionManager.UserSession session = SessionManager.getInstance().getSession(token);
                        if (session == null || session.getRole() == UserAccount.Role.HOCSINH) {
                            totalSecurityThreatsBlocked.incrementAndGet();
                            DatabaseManager.getInstance().logAudit(session != null ? session.getUsername() : "anonymous", "RBAC_VIOLATION_UPLOAD",
                                    "Chặn phát hành đề cương học phần trái phép (Thiếu Token hoặc quyền Học sinh)", clientIp);
                            responsePacket = UDPPacket.createError("BẢO VỆ PHÂN QUYỀN SERVER (ZERO-TRUST): Yêu cầu đăng nhập phiên Giảng viên/Admin hợp lệ để phát hành đề cương học phần!");
                            break;
                        }
                        String[] parts = packet.getPayload().split("\\|", 2);
                        String cCode = InputValidator.normalizeAndValidateCourseCode(parts[0]);
                        String content = parts.length > 1 ? parts[1].trim() : "";
                        if (content.length() < 10) {
                            throw new IllegalArgumentException("Nội dung đề cương học phần phải có tối thiểu 10 ký tự!");
                        }
                        courseMaterials.put(cCode, content);
                        String sha256 = computeSha256Hex(content);
                        String msg = String.format("Giảng viên %s vừa cập nhật Đề cương học phần %s (SHA-256: %s...)",
                                session.getUsername(), cCode, sha256.substring(0, 12));
                        DatabaseManager.getInstance().logAudit(session.getUsername(), "UPLOAD_MATERIAL", msg, clientIp);
                        log("[CẬP NHẬT ĐỀ CƯƠNG] " + msg);
                        broadcastPushEvent("MATERIAL_UPDATED", msg, cCode);
                        responsePacket = new UDPPacket(PacketType.UPLOAD_COURSE_MATERIAL, true, msg, sha256);
                    } catch (Exception ex) {
                        responsePacket = UDPPacket.createError("Lỗi tải lên đề cương: " + ex.getMessage());
                    }
                    break;

                case IMPORT_EXCEL:
                    try {
                        String token = packet.getToken();
                        SessionManager.UserSession session = SessionManager.getInstance().getSession(token);

                        if (session == null || session.getRole() == UserAccount.Role.HOCSINH) {
                            totalSecurityThreatsBlocked.incrementAndGet();
                            DatabaseManager.getInstance().logAudit(session != null ? session.getUsername() : "anonymous", "RBAC_VIOLATION_IMPORT",
                                    "Chặn import bảng điểm Excel trái phép (Thiếu Token hoặc quyền Học sinh)", clientIp);
                            responsePacket = UDPPacket.createError("BẢO VỆ PHÂN QUYỀN SERVER (ZERO-TRUST): Yêu cầu đăng nhập phiên Giảng viên/Admin hợp lệ để nhập dữ liệu Excel!");
                        } else if (SessionManager.getInstance().isGradebookLocked() &&
                                session.getRole() != UserAccount.Role.ADMIN) {
                            responsePacket = UDPPacket.createError("[KHÓA SỔ ĐIỂM] Sổ điểm học kỳ đã khóa! Không thể import bảng điểm mới khi sổ đã khóa.");
                        } else {
                            Type listType = new TypeToken<List<StudentData>>() {}.getType();
                            List<StudentData> list = gson.fromJson(packet.getPayload(), listType);
                            int count = DatabaseManager.getInstance().saveStudentBatch(list);

                            String operator = session.getUsername();
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

            if (isMutatingRequest && packet.getPacketId() != null && responsePacket != null) {
                responseCache.put(packet.getPacketId(), responsePacket);
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
        log("Server RUDP đã dừng lắng nghe cổng mạng (Giữ nguyên kết nối CSDL HikariCP cho bảng quản trị).");
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
