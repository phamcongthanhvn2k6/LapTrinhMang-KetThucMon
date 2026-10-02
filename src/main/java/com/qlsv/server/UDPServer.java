package com.qlsv.server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qlsv.database.DatabaseManager;
import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.network.PacketType;
import com.qlsv.network.UDPPacket;

import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
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
        threadPool = Executors.newFixedThreadPool(15);

        log("Server UDP bắt đầu khởi chạy với ThreadPool đa luồng trên cổng: " + port);

        serverThread = new Thread(() -> {
            byte[] buffer = new byte[16384];
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
            log(String.format("Nhận [%s] từ Client %s:%d", packet.getType(), clientAddress.getHostAddress(), clientPort));

            UDPPacket responsePacket;

            switch (packet.getType()) {
                case PING:
                    responsePacket = UDPPacket.createPong();
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
                            StudentData student = gson.fromJson(packet.getPayload(), StudentData.class);
                            log("Đang lưu/cập nhật sinh viên: " + student.getStudentId() + " - " + student.getFullName());

                            StudentResult result = DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(student);
                            log(String.format("Đã lưu CSDL & tính ĐTB cho %s: %.2f", result.getFullName(), result.getAverageScore()));

                            responsePacket = UDPPacket.createStudentResult(true, "Xử lý thành công", gson.toJson(result));

                            if (logListener != null) {
                                logListener.onDataUpdated();
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
                            String studentId = packet.getPayload();
                            boolean deleted = DatabaseManager.getInstance().deleteStudent(studentId);
                            if (deleted) {
                                log("Đã xóa sinh viên có mã: " + studentId);
                                responsePacket = new UDPPacket(PacketType.DELETE_STUDENT, true, "Đã xóa sinh viên " + studentId, studentId);
                                if (logListener != null) {
                                    logListener.onDataUpdated();
                                }
                            } else {
                                responsePacket = UDPPacket.createError("Không tìm thấy sinh viên có mã: " + studentId);
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

                default:
                    responsePacket = UDPPacket.createError("Loại gói tin không hợp lệ: " + packet.getType());
                    break;
            }

            byte[] respBytes = responsePacket.toBytes();
            DatagramPacket respPacket = new DatagramPacket(respBytes, respBytes.length, clientAddress, clientPort);
            socket.send(respPacket);
            log(String.format("Đã gửi [%s] tới Client %s:%d", responsePacket.getType(), clientAddress.getHostAddress(), clientPort));

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
        log("Server UDP đã dừng.");
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
