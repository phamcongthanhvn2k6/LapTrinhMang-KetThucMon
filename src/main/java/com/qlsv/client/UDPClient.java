package com.qlsv.client;

import com.google.gson.Gson;
import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.network.PacketType;
import com.qlsv.network.UDPPacket;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

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

    /**
     * Sends PING packet over UDP to check Server connection.
     * Throws exception if timeout or connection fails.
     */
    public boolean pingServer() throws Exception {
        UDPPacket ping = UDPPacket.createPing();
        UDPPacket response = sendAndReceive(ping);
        return response != null && response.getType() == PacketType.PONG;
    }

    /**
     * Sends SQL credentials to Server over UDP to establish DB connection.
     */
    public UDPPacket connectDatabase(SqlConfig config) throws Exception {
        String jsonPayload = gson.toJson(config);
        UDPPacket request = UDPPacket.createConnectDb(jsonPayload);
        return sendAndReceive(request);
    }

    /**
     * Sends student data row to Server over UDP.
     * Returns StudentResult (FullName, StudentID, AverageScore) returned by Server.
     */
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
            throw new Exception("Nhận phản hồi không mong muốn: " + response.getType() + " - " + response.getMessage());
        }
    }

    private UDPPacket sendAndReceive(UDPPacket request) throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);

            InetAddress address = InetAddress.getByName(serverHost);
            byte[] sendData = request.toBytes();

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, address, serverPort);
            socket.send(sendPacket);

            byte[] receiveData = new byte[8192];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            return UDPPacket.fromBytes(receivePacket.getData(), receivePacket.getLength());
        } catch (SocketTimeoutException ste) {
            throw new Exception("Hết thời gian chờ phản hồi từ Server (" + serverHost + ":" + serverPort + "). Vui lòng kiểm tra lại địa chỉ và cổng!");
        } catch (Exception e) {
            throw new Exception("Lỗi kết nối tới Server (" + serverHost + ":" + serverPort + "): " + e.getMessage(), e);
        }
    }
}
