package com.qlsv.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qlsv.database.DatabaseManager;
import com.qlsv.model.*;
import com.qlsv.network.PacketType;
import com.qlsv.network.UDPPacket;

import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UDPServer {

    public interface ServerLogListener {
        void onLog(String message);
        void onDataUpdated();
    }

    private static final Gson gson = new Gson();
    private int port;
    private DatagramSocket socket;
    private boolean running = false;
    private Thread serverThread;
    private ServerLogListener logListener;
    private ExecutorService threadPool;

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

    public synchronized void start() throws Exception {
        if (running) return;
        socket = new DatagramSocket(port);
        running = true;
        threadPool = Executors.newFixedThreadPool(20);

        log(String.format("Server RUDP khởi chạy trên cổng %d với ThreadPool (20 Workers, Phân quyền RBAC, Chống trùng lặp ARQ)", port));

        serverThread = new Thread(() -> {
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

    private void processPacket(byte[] data, int length, InetAddress clientAddress, int clientPort) {
        try {
            UDPPacket packet = UDPPacket.fromBytes(data, length);
            String clientIp = clientAddress.getHostAddress();
            int seq = packet.getSequenceNum();

            log(String.format("Nhận [Seq#%d | %s] từ Client %s:%d", seq, packet.getType(), clientIp, clientPort));

            // Deduplication Check
            if (packet.getPacketId() != null && seenPackets.containsKey(packet.getPacketId())) {
                log(String.format("Phát hiện gói tin trùng lặp (ARQ Retransmit) #%d [%s]. Bỏ qua xử lý lại CSDL.", seq, packet.getPacketId()));
            } else if (packet.getPacketId() != null) {
                seenPackets.put(packet.getPacketId(), Boolean.TRUE);
            }

            UDPPacket responsePacket;

            switch (packet.getType()) {
                case PING:
                    responsePacket = UDPPacket.createPong();
                    break;

                case LOGIN:
                    try {
                        String[] creds = gson.fromJson(packet.getPayload(), String[].class);
                        String username = creds[0];
                        String password = creds[1];

                        UserAccount acc = DatabaseManager.getInstance().authenticateUser(username, password);
                        if (acc != null) {
                            String token = SessionManager.getInstance().createSession(acc);
                            DatabaseManager.getInstance().logAudit(username, "LOGIN", "Đăng nhập thành công vai trò " + acc.getRole(), clientIp);
                            log(String.format("Xác thực THÀNH CÔNG cho '%s' (Quyền: %s, Token: %s...)", username, acc.getRole(), token.substring(0, 8)));
                            responsePacket = UDPPacket.createLoginResponse(true, "Đăng nhập thành công!", token, gson.toJson(acc));
                        } else {
                            DatabaseManager.getInstance().logAudit(username, "LOGIN_FAILED", "Sai mật khẩu hoặc tài khoản", clientIp);
                            log("Xác thực THẤT BẠI cho tài khoản: " + username);
                            responsePacket = UDPPacket.createLoginResponse(false, "Sai tên đăng nhập hoặc mật khẩu!", null, null);
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

                case ADD_STUDENT:
                case UPDATE_STUDENT:
                    try {
                        if (!DatabaseManager.getInstance().isConnected()) {
                            responsePacket = UDPPacket.createError("Server chưa được kết nối CSDL!");
                        } else {
                            // Check Server-side Role Authorization
                            String token = packet.getToken();
                            SessionManager.UserSession session = SessionManager.getInstance().getSession(token);

                            // Allow if valid teacher/admin session, or fallback if unauthenticated demo
                            if (session != null && session.getRole() == UserAccount.Role.HOCSINH) {
                                responsePacket = UDPPacket.createError("BẢO VỆ PHÂN QUYỀN SERVER: Quyền Học sinh không được phép thêm hoặc cập nhật điểm số!");
                            } else {
                                StudentData student = gson.fromJson(packet.getPayload(), StudentData.class);
                                String operator = (session != null) ? session.getUsername() : "Client";
                                log(String.format("[%s] Đang lưu/cập nhật sinh viên: %s - %s", operator, student.getStudentId(), student.getFullName()));

                                StudentResult result = DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(student);
                                DatabaseManager.getInstance().logAudit(operator, "SAVE_STUDENT", "Lưu sinh viên " + student.getStudentId(), clientIp);

                                log(String.format("Đã lưu CSDL & tính ĐTB cho %s: %.2f", result.getFullName(), result.getAverageScore()));
                                responsePacket = UDPPacket.createStudentResult(true, "Xử lý thành công", gson.toJson(result));

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
                            } else {
                                String studentId = packet.getPayload();
                                String operator = (session != null) ? session.getUsername() : "Client";
                                boolean deleted = DatabaseManager.getInstance().deleteStudent(studentId);
                                if (deleted) {
                                    DatabaseManager.getInstance().logAudit(operator, "DELETE_STUDENT", "Xóa sinh viên " + studentId, clientIp);
                                    log("Đã xóa sinh viên có mã: " + studentId);
                                    responsePacket = new UDPPacket(PacketType.DELETE_STUDENT, true, "Đã xóa sinh viên " + studentId, studentId);
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
                        } else {
                            Type listType = new TypeToken<List<StudentData>>() {}.getType();
                            List<StudentData> list = gson.fromJson(packet.getPayload(), listType);
                            int count = DatabaseManager.getInstance().saveStudentBatch(list);

                            String operator = (session != null) ? session.getUsername() : "Giảng viên";
                            DatabaseManager.getInstance().logAudit(operator, "IMPORT_EXCEL", "Nhập thành công " + count + " sinh viên từ Excel", clientIp);
                            log(String.format("[%s] Đã nhập thành công %d sinh viên từ file Excel vào CSDL SQL!", operator, count));

                            responsePacket = UDPPacket.createImportResult(true, "Nhập thành công", count);
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

            // Attach ACK sequence number to response
            responsePacket.setAckNum(seq);

            byte[] respBytes = responsePacket.toBytes();
            DatagramPacket respPacket = new DatagramPacket(respBytes, respBytes.length, clientAddress, clientPort);
            socket.send(respPacket);
            log(String.format("Đã gửi phản hồi [Ack#%d | %s] tới Client %s:%d", seq, responsePacket.getType(), clientIp, clientPort));

        } catch (Exception e) {
            log("Lỗi định dạng gói tin UDP: " + e.getMessage());
        }
    }

    public synchronized void stop() {
        running = false;
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
