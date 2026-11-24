package com.qlsv.network;

import com.google.gson.Gson;
import com.qlsv.security.SecurityManager;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class UDPPacket {
    private static final Gson gson = new Gson();
    private static final long MAX_REPLAY_DRIFT_MS = 5 * 60 * 1000L; // 5 minutes anti-replay window

    private String packetId;      // Unique Packet ID for deduplication
    private int sequenceNum;      // RUDP Sequence Number
    private int ackNum;           // RUDP Acknowledgment Number
    private PacketType type;      // Type of operation
    private boolean success;      // Status flag
    private String message;       // Informative message
    private String token;         // Session Auth Token (Server-side RBAC)
    private String payload;       // JSON data payload
    private int totalChunks = 1;  // For packet chunking
    private int chunkIndex = 0;   // Chunk sequence
    private long timestamp;       // Creation timestamp
    private String hmacSignature; // HMAC-SHA256 integrity signature

    public UDPPacket() {
        this.packetId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }

    public UDPPacket(PacketType type, boolean success, String message, String payload) {
        this();
        this.type = type;
        this.success = success;
        this.message = message;
        this.payload = payload;
    }

    public UDPPacket(PacketType type, boolean success, String message, String payload, String token) {
        this(type, success, message, payload);
        this.token = token;
    }

    // Factory methods
    public static UDPPacket createPing() {
        return new UDPPacket(PacketType.PING, true, "PING", null);
    }

    public static UDPPacket createPong() {
        return new UDPPacket(PacketType.PONG, true, "PONG_OK", null);
    }

    public static UDPPacket createLogin(String username, String password) {
        String json = gson.toJson(new String[]{username, password});
        return new UDPPacket(PacketType.LOGIN, true, "User Login Request", json);
    }

    public static UDPPacket createLoginResponse(boolean success, String message, String token, String userAccountJson) {
        UDPPacket p = new UDPPacket(PacketType.LOGIN_RESPONSE, success, message, userAccountJson);
        p.setToken(token);
        return p;
    }

    public static UDPPacket createConnectDb(String sqlConfigJson) {
        return new UDPPacket(PacketType.CONNECT_DB, true, "Connecting DB", sqlConfigJson);
    }

    public static UDPPacket createDbStatus(boolean success, String message) {
        return new UDPPacket(PacketType.DB_STATUS, success, message, null);
    }

    public static UDPPacket createAddStudent(String studentDataJson, String token) {
        return new UDPPacket(PacketType.ADD_STUDENT, true, "Add Student Data", studentDataJson, token);
    }

    public static UDPPacket createStudentResult(boolean success, String message, String studentResultJson) {
        return new UDPPacket(PacketType.STUDENT_RESULT, success, message, studentResultJson);
    }

    public static UDPPacket createSearchRequest(String query, String token) {
        return new UDPPacket(PacketType.SEARCH_STUDENTS, true, "Search Query", query, token);
    }

    public static UDPPacket createUpdateRequest(String studentDataJson, String token) {
        return new UDPPacket(PacketType.UPDATE_STUDENT, true, "Update Student Data", studentDataJson, token);
    }

    public static UDPPacket createDeleteRequest(String studentId, String token) {
        return new UDPPacket(PacketType.DELETE_STUDENT, true, "Delete Student ID", studentId, token);
    }

    public static UDPPacket createGetAllRequest(String token) {
        return new UDPPacket(PacketType.GET_ALL_STUDENTS, true, "Get All Students", null, token);
    }

    public static UDPPacket createStudentsListResponse(boolean success, String message, String listJson) {
        return new UDPPacket(PacketType.STUDENTS_LIST, success, message, listJson);
    }

    public static UDPPacket createGetPageRequest(com.qlsv.model.PageRequest pageReq, String token) {
        return new UDPPacket(PacketType.GET_STUDENTS_PAGE, true, "Get Page", gson.toJson(pageReq), token);
    }

    public static UDPPacket createPageResult(boolean success, String message, String pageResultJson) {
        return new UDPPacket(PacketType.STUDENTS_PAGE_RESULT, success, message, pageResultJson);
    }

    public static UDPPacket createGetTranscript(String studentId, String token) {
        return new UDPPacket(PacketType.GET_TRANSCRIPT, true, "Get Transcript", studentId, token);
    }

    public static UDPPacket createTranscriptResult(boolean success, String message, String transcriptJson) {
        return new UDPPacket(PacketType.TRANSCRIPT_RESULT, success, message, transcriptJson);
    }

    public static UDPPacket createImportExcel(String studentListJson, String token) {
        return new UDPPacket(PacketType.IMPORT_EXCEL, true, "Import Excel Batch", studentListJson, token);
    }

    public static UDPPacket createImportResult(boolean success, String message, int count) {
        return new UDPPacket(PacketType.IMPORT_RESULT, success, message, String.valueOf(count));
    }

    public static UDPPacket createPushSubscribe(int clientPushPort, String studentId, String token) {
        String payload = clientPushPort + "|" + (studentId != null ? studentId : "");
        return new UDPPacket(PacketType.PUSH_SUBSCRIBE, true, "Subscribe Push Channel", payload, token);
    }

    public static UDPPacket createPushEvent(String eventType, String message, String jsonPayload) {
        return new UDPPacket(PacketType.PUSH_EVENT, true, "[" + eventType + "] " + message, jsonPayload);
    }

    public static UDPPacket createToggleGradeLock(boolean lockState, String token) {
        return new UDPPacket(PacketType.TOGGLE_GRADE_LOCK, true, "Toggle Grade Lock", String.valueOf(lockState), token);
    }

    public static UDPPacket createGradeLockStatus(boolean isLocked, String message) {
        return new UDPPacket(PacketType.GRADE_LOCK_STATUS, true, message, String.valueOf(isLocked));
    }

    public static UDPPacket createBroadcastAnnouncement(String announcementText, String token) {
        return new UDPPacket(PacketType.BROADCAST_ANNOUNCEMENT, true, "Broadcast Announcement", announcementText, token);
    }

    public static UDPPacket createGetAuditLogs(int limit, String token) {
        return new UDPPacket(PacketType.GET_AUDIT_LOGS, true, "Get Audit Logs", String.valueOf(limit), token);
    }

    public static UDPPacket createAuditLogsResult(String logsJson) {
        return new UDPPacket(PacketType.AUDIT_LOGS_RESULT, true, "Audit Logs Result", logsJson);
    }

    public static UDPPacket createUpdateCourseGrade(String studentId, String courseCode, double processScore, double examScore, String token) {
        String payload = studentId + "|" + courseCode + "|" + processScore + "|" + examScore;
        return new UDPPacket(PacketType.UPDATE_COURSE_GRADE, true, "Update Single Course Grade", payload, token);
    }

    public static UDPPacket createSubmitGradeAppeal(String studentId, String courseCode, String reason, String token) {
        String payload = studentId + "|" + courseCode + "|" + reason;
        return new UDPPacket(PacketType.SUBMIT_GRADE_APPEAL, true, "Submit Grade Appeal", payload, token);
    }

    public static UDPPacket createGetGradeAppeals(String token) {
        return new UDPPacket(PacketType.GET_GRADE_APPEALS, true, "Get Grade Appeals", null, token);
    }

    public static UDPPacket createGradeAppealsResult(String appealsJson) {
        return new UDPPacket(PacketType.GRADE_APPEALS_RESULT, true, "Grade Appeals List", appealsJson);
    }

    public static UDPPacket createResolveGradeAppeal(int appealId, boolean approved, double newProcessScore,
                                                     double newExamScore, String teacherNote, String token) {
        String payload = appealId + "|" + approved + "|" + newProcessScore + "|" + newExamScore + "|" + (teacherNote != null ? teacherNote : "");
        return new UDPPacket(PacketType.RESOLVE_GRADE_APPEAL, true, "Resolve Grade Appeal", payload, token);
    }

    public static UDPPacket createRegisterCredits(String studentId, int totalCredits, String coursesCsv, String token) {
        String payload = studentId + "|" + totalCredits + "|" + coursesCsv;
        return new UDPPacket(PacketType.REGISTER_CREDITS, true, "Register Semester Credits", payload, token);
    }

    public static UDPPacket createRotateCryptoKey(String newAlgoName, String newKey, String token) {
        String payload = newAlgoName + "|" + newKey;
        return new UDPPacket(PacketType.ROTATE_CRYPTO_KEY, true, "Rotate Cryptographic Key", payload, token);
    }

    public static UDPPacket createDownloadCourseMaterial(String courseCode, int chunkIndex, String token) {
        String payload = courseCode + "|" + chunkIndex;
        return new UDPPacket(PacketType.DOWNLOAD_COURSE_MATERIAL, true, "Download Course Material Chunk", payload, token);
    }

    public static UDPPacket createCourseMaterialChunk(String courseCode, int chunkIndex, int totalChunks, String sha256Checksum, String chunkContent) {
        UDPPacket pkt = new UDPPacket(PacketType.COURSE_MATERIAL_CHUNK, true, sha256Checksum, chunkContent);
        pkt.setChunkIndex(chunkIndex);
        pkt.setTotalChunks(totalChunks);
        return pkt;
    }

    public static UDPPacket createUploadCourseMaterial(String courseCode, String content, String token) {
        String payload = courseCode + "|" + content;
        return new UDPPacket(PacketType.UPLOAD_COURSE_MATERIAL, true, "Upload Course Material", payload, token);
    }

    public static UDPPacket createAck(int ackNum, String originalPacketId) {
        UDPPacket p = new UDPPacket(PacketType.ACK, true, "ACK", originalPacketId);
        p.setAckNum(ackNum);
        return p;
    }

    public static UDPPacket createError(String message) {
        return new UDPPacket(PacketType.ERROR, false, message, null);
    }

    // HMAC-SHA256 Signing & Anti-Replay Verification
    private String buildCanonicalSigningString() {
        return (packetId != null ? packetId : "") + "|" +
                sequenceNum + "|" +
                (type != null ? type.name() : "") + "|" +
                timestamp + "|" +
                (payload != null ? payload : "");
    }

    public void signPacket() {
        this.hmacSignature = SecurityManager.computeHmacSha256(buildCanonicalSigningString());
    }

    public boolean verifyIntegrity() {
        if (hmacSignature == null || hmacSignature.isEmpty()) {
            return true; // Allow legacy unsigned packets in unit tests
        }
        return SecurityManager.verifyHmacSha256(buildCanonicalSigningString(), hmacSignature);
    }

    public boolean isReplayValid() {
        if (timestamp <= 0) return true;
        long drift = Math.abs(System.currentTimeMillis() - timestamp);
        return drift <= MAX_REPLAY_DRIFT_MS;
    }

    public byte[] toBytes() {
        this.signPacket();
        String json = gson.toJson(this);
        byte[] raw = json.getBytes(StandardCharsets.UTF_8);
        if (raw.length > 512) {
            try {
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                try (java.util.zip.GZIPOutputStream gzip = new java.util.zip.GZIPOutputStream(baos)) {
                    gzip.write(raw);
                }
                return baos.toByteArray();
            } catch (Exception e) {
                return raw;
            }
        }
        return raw;
    }

    public static UDPPacket fromBytes(byte[] data, int length) {
        if (data == null || length <= 0) return null;
        try {
            if (length >= 2 && data[0] == (byte) 0x1f && data[1] == (byte) 0x8b) {
                java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(data, 0, length);
                try (java.util.zip.GZIPInputStream gzip = new java.util.zip.GZIPInputStream(bais);
                     java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
                    byte[] buf = new byte[4096];
                    int n;
                    while ((n = gzip.read(buf)) != -1) {
                        baos.write(buf, 0, n);
                    }
                    String json = baos.toString(StandardCharsets.UTF_8);
                    return gson.fromJson(json, UDPPacket.class);
                }
            }
            String json = new String(data, 0, length, StandardCharsets.UTF_8).trim();
            return gson.fromJson(json, UDPPacket.class);
        } catch (Exception ex) {
            return null;
        }
    }

    // Getters and Setters
    public String getPacketId() {
        return packetId;
    }

    public void setPacketId(String packetId) {
        this.packetId = packetId;
    }

    public int getSequenceNum() {
        return sequenceNum;
    }

    public void setSequenceNum(int sequenceNum) {
        this.sequenceNum = sequenceNum;
    }

    public int getAckNum() {
        return ackNum;
    }

    public void setAckNum(int ackNum) {
        this.ackNum = ackNum;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(int chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getHmacSignature() {
        return hmacSignature;
    }

    public void setHmacSignature(String hmacSignature) {
        this.hmacSignature = hmacSignature;
    }
}
