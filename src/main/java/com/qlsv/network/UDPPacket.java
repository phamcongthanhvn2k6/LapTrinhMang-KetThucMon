package com.qlsv.network;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;

public class UDPPacket {
    private static final Gson gson = new Gson();

    private PacketType type;
    private boolean success;
    private String message;
    private String payload; // JSON representation of data payload

    public UDPPacket() {
    }

    public UDPPacket(PacketType type, boolean success, String message, String payload) {
        this.type = type;
        this.success = success;
        this.message = message;
        this.payload = payload;
    }

    public static UDPPacket createPing() {
        return new UDPPacket(PacketType.PING, true, "PING", null);
    }

    public static UDPPacket createPong() {
        return new UDPPacket(PacketType.PONG, true, "PONG_OK", null);
    }

    public static UDPPacket createConnectDb(String sqlConfigJson) {
        return new UDPPacket(PacketType.CONNECT_DB, true, "Connecting DB", sqlConfigJson);
    }

    public static UDPPacket createDbStatus(boolean success, String message) {
        return new UDPPacket(PacketType.DB_STATUS, success, message, null);
    }

    public static UDPPacket createAddStudent(String studentDataJson) {
        return new UDPPacket(PacketType.ADD_STUDENT, true, "Add Student Data", studentDataJson);
    }

    public static UDPPacket createStudentResult(boolean success, String message, String studentResultJson) {
        return new UDPPacket(PacketType.STUDENT_RESULT, success, message, studentResultJson);
    }

    public static UDPPacket createError(String message) {
        return new UDPPacket(PacketType.ERROR, false, message, null);
    }

    public byte[] toBytes() {
        String json = gson.toJson(this);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    public static UDPPacket fromBytes(byte[] data, int length) {
        String json = new String(data, 0, length, StandardCharsets.UTF_8).trim();
        return gson.fromJson(json, UDPPacket.class);
    }

    public PacketType getType() {
        return type;
    }

    public void setType(PacketType type) {
        this.type = type;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
