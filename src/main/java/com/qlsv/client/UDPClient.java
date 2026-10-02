package com.qlsv.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.network.PacketType;
import com.qlsv.network.UDPPacket;

import java.lang.reflect.Type;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.List;

public class UDPClient {

    private static final Gson gson = new Gson();
    private static final int TIMEOUT_MS = 3500; // 3.5 seconds timeout

    private String serverHost;
    private int serverPort;

    public UDPClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public String getServerHost() {
        return serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public boolean pingServer() throws Exception {
        UDPPacket ping = UDPPacket.createPing();
        UDPPacket response = sendAndReceive(ping);
        return response != null && response.getType() == PacketType.PONG;
    }

    public UDPPacket connectDatabase(SqlConfig config) throws Exception {
        String jsonPayload = gson.toJson(config);
        UDPPacket request = UDPPacket.createConnectDb(jsonPayload);
        return sendAndReceive(request);
    }

    public StudentResult sendStudentData(StudentData student) throws Exception {
        String jsonPayload = gson.toJson(student);
        UDPPacket request = UDPPacket.createAddStudent(jsonPayload);
        UDPPacket response = sendAndReceive(request);

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
        UDPPacket request = UDPPacket.createSearchRequest(query);
        UDPPacket response = sendAndReceive(request);
        if (response != null && response.isSuccess()) {
            Type listType = new TypeToken<List<StudentResult>>() {}.getType();
            return gson.fromJson(response.getPayload(), listType);
        }
        throw new Exception(response != null ? response.getMessage() : "Lỗi tìm kiếm");
    }

    public boolean deleteStudent(String studentId) throws Exception {
        UDPPacket request = UDPPacket.createDeleteRequest(studentId);
        UDPPacket response = sendAndReceive(request);
        return response != null && response.isSuccess();
    }

    public List<StudentResult> getAllStudents() throws Exception {
        UDPPacket request = UDPPacket.createGetAllRequest();
        UDPPacket response = sendAndReceive(request);
        if (response != null && response.isSuccess()) {
            Type listType = new TypeToken<List<StudentResult>>() {}.getType();
            return gson.fromJson(response.getPayload(), listType);
        }
        throw new Exception(response != null ? response.getMessage() : "Lỗi lấy danh sách");
    }

    private UDPPacket sendAndReceive(UDPPacket request) throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);

            InetAddress address = InetAddress.getByName(serverHost);
            byte[] sendData = request.toBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, serverPort);
            socket.send(sendPacket);

            byte[] receiveData = new byte[16384];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            return UDPPacket.fromBytes(receivePacket.getData(), receivePacket.getLength());
        } catch (SocketTimeoutException ste) {
            throw new Exception("Hết thời gian chờ phản hồi từ Server (" + serverHost + ":" + serverPort + ").");
        } catch (Exception e) {
            throw new Exception("Lỗi kết nối tới Server (" + serverHost + ":" + serverPort + "): " + e.getMessage(), e);
        }
    }
}
