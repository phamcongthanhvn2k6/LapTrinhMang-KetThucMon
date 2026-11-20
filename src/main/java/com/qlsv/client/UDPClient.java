package com.qlsv.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.model.StudentTranscript;
import com.qlsv.model.UserAccount;
import com.qlsv.network.PacketType;
import com.qlsv.network.UDPPacket;

import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class UDPClient implements AutoCloseable {

    private static final Gson gson = new Gson();
    private static final int TIMEOUT_MS = 2500; // 2.5s timeout per attempt
    private static final int MAX_RETRIES = 3;   // RUDP 3 retries with backoff

    private final String serverHost;
    private final int serverPort;
    private DatagramSocket socket;
    private final AtomicInteger sequenceCounter = new AtomicInteger(100);

    private String sessionToken = null;
    private UserAccount currentUser = null;

    public UDPClient(String serverHost, int serverPort) throws Exception {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(TIMEOUT_MS);
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public UserAccount getCurrentUser() {
        return currentUser;
    }

    public void logout() {
        this.sessionToken = null;
        this.currentUser = null;
    }

    public boolean pingServer() throws Exception {
        UDPPacket ping = UDPPacket.createPing();
        UDPPacket response = sendAndReceiveWithRetry(ping, 2);
        return response != null && response.getType() == PacketType.PONG;
    }

    public UserAccount login(String username, String password) throws Exception {
        UDPPacket req = UDPPacket.createLogin(username, password);
        UDPPacket res = sendAndReceiveWithRetry(req, MAX_RETRIES);

        if (res == null) {
            throw new Exception("Không nhận được phản hồi đăng nhập từ Server!");
        }

        if (!res.isSuccess()) {
            throw new Exception(res.getMessage());
        }

        this.sessionToken = res.getToken();
        this.currentUser = gson.fromJson(res.getPayload(), UserAccount.class);
        return this.currentUser;
    }

    public UDPPacket connectDatabase(SqlConfig config) throws Exception {
        String jsonPayload = gson.toJson(config);
        UDPPacket request = UDPPacket.createConnectDb(jsonPayload);
        return sendAndReceiveWithRetry(request, MAX_RETRIES);
    }

    public StudentResult sendStudentData(StudentData student) throws Exception {
        String jsonPayload = gson.toJson(student);
        UDPPacket request = UDPPacket.createAddStudent(jsonPayload, sessionToken);
        UDPPacket response = sendAndReceiveWithRetry(request, MAX_RETRIES);

        if (response == null) {
            throw new Exception("Không nhận được phản hồi từ Server!");
        }

        if (!response.isSuccess()) {
            throw new Exception(response.getMessage());
        }

        if (response.getType() == PacketType.STUDENT_RESULT) {
            return gson.fromJson(response.getPayload(), StudentResult.class);
        } else {
            throw new Exception("Nhận phản hồi không mong muốn: " + response.getType());
        }
    }

    public List<StudentResult> searchStudents(String query) throws Exception {
        UDPPacket request = UDPPacket.createSearchRequest(query, sessionToken);
        UDPPacket response = sendAndReceiveWithRetry(request, MAX_RETRIES);
        if (response != null && response.isSuccess()) {
            Type listType = new TypeToken<List<StudentResult>>() {}.getType();
            return gson.fromJson(response.getPayload(), listType);
        }
        throw new Exception(response != null ? response.getMessage() : "Lỗi tìm kiếm");
    }

    public boolean deleteStudent(String studentId) throws Exception {
        UDPPacket request = UDPPacket.createDeleteRequest(studentId, sessionToken);
        UDPPacket response = sendAndReceiveWithRetry(request, MAX_RETRIES);
        return response != null && response.isSuccess();
    }

    public List<StudentResult> getAllStudents() throws Exception {
        UDPPacket request = UDPPacket.createGetAllRequest(sessionToken);
        UDPPacket response = sendAndReceiveWithRetry(request, MAX_RETRIES);
        if (response != null && response.isSuccess()) {
            Type listType = new TypeToken<List<StudentResult>>() {}.getType();
            return gson.fromJson(response.getPayload(), listType);
        }
        throw new Exception(response != null ? response.getMessage() : "Lỗi lấy danh sách");
    }

    public StudentTranscript getDetailedTranscript(String studentId) throws Exception {
        UDPPacket request = UDPPacket.createGetTranscript(studentId, sessionToken);
        UDPPacket response = sendAndReceiveWithRetry(request, MAX_RETRIES);
        if (response != null && response.isSuccess()) {
            return gson.fromJson(response.getPayload(), StudentTranscript.class);
        }
        throw new Exception(response != null ? response.getMessage() : "Lỗi tải bảng điểm chi tiết");
    }

    public int importStudentsFromExcel(List<StudentData> list) throws Exception {
        String jsonPayload = gson.toJson(list);
        UDPPacket request = UDPPacket.createImportExcel(jsonPayload, sessionToken);
        UDPPacket response = sendAndReceiveWithRetry(request, MAX_RETRIES);
        if (response != null && response.isSuccess()) {
            try {
                return Integer.parseInt(response.getPayload());
            } catch (Exception e) {
                return list.size();
            }
        }
        throw new Exception(response != null ? response.getMessage() : "Lỗi nhập danh sách từ Excel");
    }

    private synchronized UDPPacket sendAndReceiveWithRetry(UDPPacket request, int maxAttempts) throws Exception {
        if (socket == null || socket.isClosed()) {
            socket = new DatagramSocket();
            socket.setSoTimeout(TIMEOUT_MS);
        }

        request.setSequenceNum(sequenceCounter.getAndIncrement());
        if (sessionToken != null && request.getToken() == null) {
            request.setToken(sessionToken);
        }

        InetAddress address = InetAddress.getByName(serverHost);
        byte[] sendData = request.toBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, serverPort);

        byte[] receiveData = new byte[65507]; // Max UDP Datagram capacity
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                socket.send(sendPacket);
                socket.receive(receivePacket);

                UDPPacket resp = UDPPacket.fromBytes(receivePacket.getData(), receivePacket.getLength());
                return resp;
            } catch (SocketTimeoutException ste) {
                lastException = ste;
                System.out.println(String.format("[RUDP Client] Gói #%d Timeout lần %d/%d, đang truyền lại (Retransmitting)...",
                        request.getSequenceNum(), attempt, maxAttempts));
                try {
                    Thread.sleep(150L * attempt); // Backoff before retransmission
                } catch (InterruptedException ignored) {}
            } catch (Exception ex) {
                lastException = ex;
                break;
            }
        }

        throw new Exception("Hết thời gian chờ phản hồi từ Server RUDP (" + serverHost + ":" + serverPort + 
                ") sau " + maxAttempts + " lần truyền lại: " + (lastException != null ? lastException.getMessage() : "Timeout"));
    }

    @Override
    public void close() {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
