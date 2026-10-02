package com.qlsv.server;

import com.google.gson.Gson;
import com.qlsv.database.DatabaseManager;
import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.network.PacketType;
import com.qlsv.network.UDPPacket;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

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

        log("Server UDP bắt đầu khởi chạy trên cổng: " + port);

        serverThread = new Thread(() -> {
            byte[] buffer = new byte[8192];
            while (running && !socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    // Process in async task to prevent blocking UDP receive loop
                    byte[] data = packet.getData();
                    int length = packet.getLength();
                    InetAddress clientAddress = packet.getAddress();
                    int clientPort = packet.getPort();

                    new Thread(() -> processPacket(data, length, clientAddress, clientPort)).start();
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
                    try {
                        if (!DatabaseManager.getInstance().isConnected()) {
                            responsePacket = UDPPacket.createError("Server chưa được kết nối với CSDL! Hãy thực hiện kết nối CSDL trước.");
                        } else {
                            StudentData student = gson.fromJson(packet.getPayload(), StudentData.class);
                            log("Đang xử lý sinh viên: " + student.getStudentId() + " - " + student.getFullName());

                            // Encrypt DES -> Save SQL -> Decrypt DES -> Calculate Average Score
                            StudentResult result = DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(student);
                            log(String.format("Đã lưu CSDL (Mã hóa DES) & tính DTB thành công cho %s: %.2f", result.getFullName(), result.getAverageScore()));

                            String resultJson = gson.toJson(result);
                            responsePacket = UDPPacket.createStudentResult(true, "Xử lý thành công", resultJson);

                            if (logListener != null) {
                                logListener.onDataUpdated();
                            }
                        }
                    } catch (Exception ex) {
                        log("Lỗi xử lý dữ liệu sinh viên: " + ex.getMessage());
                        responsePacket = UDPPacket.createError("Lỗi Server: " + ex.getMessage());
                    }
                    break;

                default:
                    responsePacket = UDPPacket.createError("Loại gói tin không hợp lệ: " + packet.getType());
                    break;
            }

            // Send UDP response back to client
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
