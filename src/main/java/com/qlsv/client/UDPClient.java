package com.qlsv.client;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.qlsv.database.dao.AuditDAO;
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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RUDP 2.0 Client với:
 * - Chữ ký số gói tin tự động HMAC-SHA256 & GZIP Compression
 * - Cơ chế truyền lại tự động ARQ (Stop-and-Wait with Exponential Backoff)
 * - Luồng lắng nghe bất đồng bộ Full-Duplex (UDP Server-Push Listener)
 * - Đo đạc chỉ số mạng thời gian thực (RTT ms, Packets Sent, Retransmissions)
 */
public class UDPClient implements AutoCloseable {

    public interface PushNotificationListener {
        void onPushReceived(String message, String payload);
    }

    private static final Gson gson = new Gson();
    private static final int TIMEOUT_MS = 2200; // 2.2s timeout per attempt
    private static final int MAX_RETRIES = 3;   // RUDP 3 retries with backoff

    private final String serverHost;
    private final int serverPort;
    private DatagramSocket socket;
    private DatagramSocket pushSocket;
    private Thread pushListenerThread;
    private volatile boolean runningPush = false;
    private PushNotificationListener pushListener;

    private final AtomicInteger sequenceCounter = new AtomicInteger(100);
    private final AtomicLong lastRttMs = new AtomicLong(0);
    private final AtomicLong totalPacketsSent = new AtomicLong(0);
    private final AtomicLong totalRetransmissions = new AtomicLong(0);

    private String sessionToken = null;
    private UserAccount currentUser = null;
    private volatile boolean gradebookLocked = false;

    public UDPClient(String serverHost, int serverPort) throws Exception {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.socket = new DatagramSocket();
        this.socket.setSoTimeout(TIMEOUT_MS);
        startPushListener();
    }

    private void startPushListener() {
        try {
            this.pushSocket = new DatagramSocket(0); // Ephemeral local push port
            this.runningPush = true;
            this.pushListenerThread = new Thread(() -> {
                byte[] buf = new byte[65507];
                while (runningPush && pushSocket != null && !pushSocket.isClosed()) {
                    try {
                        DatagramPacket dp = new DatagramPacket(buf, buf.length);
                        pushSocket.receive(dp);
                        UDPPacket pkt = UDPPacket.fromBytes(dp.getData(), dp.getLength());
                        if (pkt != null && pkt.getType() == PacketType.PUSH_EVENT) {
                            String msg = pkt.getMessage() != null ? pkt.getMessage() : "";
                            if (msg.contains("[GRADE_LOCK]")) {
                                gradebookLocked = Boolean.parseBoolean(pkt.getPayload());
                            }
                            if (pushListener != null) {
                                pushListener.onPushReceived(msg, pkt.getPayload());
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }, "RUDP-Client-Push-Listener");
            this.pushListenerThread.setDaemon(true);
            this.pushListenerThread.start();
        } catch (Exception ignored) {}
    }

    public void setPushNotificationListener(PushNotificationListener listener) {
        this.pushListener = listener;
    }

    public void subscribePushNotifications(String studentId) {
        if (pushSocket == null || pushSocket.isClosed()) return;
        try {
            UDPPacket sub = UDPPacket.createPushSubscribe(pushSocket.getLocalPort(), studentId, sessionToken);
            UDPPacket resp = sendAndReceiveWithRetry(sub, 1);
            if (resp != null && resp.getType() == PacketType.GRADE_LOCK_STATUS && resp.getPayload() != null) {
                this.gradebookLocked = Boolean.parseBoolean(resp.getPayload());
            }
        } catch (Exception ignored) {}
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

    public long getLastRttMs() {
        return lastRttMs.get();
    }

    public long getTotalPacketsSent() {
        return totalPacketsSent.get();
    }

    public long getTotalRetransmissions() {
        return totalRetransmissions.get();
    }

    public boolean isGradebookLocked() {
        return gradebookLocked;
    }

    public void logout() {
        this.sessionToken = null;
        this.currentUser = null;
    }

    public boolean pingServer() throws Exception {
        UDPPacket ping = UDPPacket.createPing();
        UDPPacket response = sendAndReceiveWithRetry(ping, 2);
        if (response != null && response.getType() == PacketType.PONG) {
            subscribePushNotifications("SV001");
            return true;
        }
        return false;
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

    public boolean toggleGradeLock(boolean lockState) throws Exception {
        UDPPacket req = UDPPacket.createToggleGradeLock(lockState, sessionToken);
        UDPPacket res = sendAndReceiveWithRetry(req, MAX_RETRIES);
        if (res != null && res.isSuccess()) {
            this.gradebookLocked = Boolean.parseBoolean(res.getPayload());
            return this.gradebookLocked;
        }
        throw new Exception(res != null ? res.getMessage() : "Không thể thay đổi trạng thái khóa sổ điểm!");
    }

    public void broadcastAnnouncement(String announcementText) throws Exception {
        UDPPacket req = UDPPacket.createBroadcastAnnouncement(announcementText, sessionToken);
        UDPPacket res = sendAndReceiveWithRetry(req, MAX_RETRIES);
        if (res == null || !res.isSuccess()) {
            throw new Exception(res != null ? res.getMessage() : "Không thể phát thông báo!");
        }
    }

    public List<AuditDAO.AuditEntry> getAuditLogs(int limit) throws Exception {
        UDPPacket req = UDPPacket.createGetAuditLogs(limit, sessionToken);
        UDPPacket res = sendAndReceiveWithRetry(req, MAX_RETRIES);
        if (res != null && res.isSuccess()) {
            Type listType = new TypeToken<List<AuditDAO.AuditEntry>>() {}.getType();
            return gson.fromJson(res.getPayload(), listType);
        }
        return Collections.emptyList();
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
        if (response != null && !response.isSuccess()) {
            throw new Exception(response.getMessage());
        }
        return response != null && response.isSuccess();
    }

    public com.qlsv.model.PageResult<StudentResult> getStudentsPage(int page, int pageSize, String query) throws Exception {
        com.qlsv.model.PageRequest req = new com.qlsv.model.PageRequest(page, pageSize, query);
        UDPPacket request = UDPPacket.createGetPageRequest(req, sessionToken);
        UDPPacket response = sendAndReceiveWithRetry(request, MAX_RETRIES);
        if (response != null && response.isSuccess()) {
            Type pageType = new TypeToken<com.qlsv.model.PageResult<StudentResult>>() {}.getType();
            return gson.fromJson(response.getPayload(), pageType);
        }
        return new com.qlsv.model.PageResult<>(Collections.emptyList(), page, pageSize, 0);
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

    public StudentTranscript updateCourseGrade(String studentId, String courseCode, double processScore, double examScore) throws Exception {
        UDPPacket request = UDPPacket.createUpdateCourseGrade(studentId, courseCode, processScore, examScore, sessionToken);
        UDPPacket response = sendAndReceiveWithRetry(request, MAX_RETRIES);
        if (response != null && response.isSuccess()) {
            return gson.fromJson(response.getPayload(), StudentTranscript.class);
        }
        throw new Exception(response != null ? response.getMessage() : "Lỗi cập nhật điểm môn học");
    }

    public String submitGradeAppeal(String studentId, String courseCode, String reason) throws Exception {
        UDPPacket request = UDPPacket.createSubmitGradeAppeal(studentId, courseCode, reason, sessionToken);
        UDPPacket response = sendAndReceiveWithRetry(request, MAX_RETRIES);
        if (response != null && response.isSuccess()) {
            return response.getMessage();
        }
        throw new Exception(response != null ? response.getMessage() : "Lỗi gửi đơn phúc khảo");
    }

    public interface ChunkProgressListener {
        void onChunkReceived(int chunkIndex, int totalChunks, String sha256Checksum);
    }

    /**
     * Tải tệp đề cương học phần phân mảnh qua giao thức RUDP (Chunked Datagram Streaming)
     * kèm kiểm tra mã băm toàn vẹn SHA-256 Checksum.
     */
    public String downloadCourseMaterialChunked(String courseCode, ChunkProgressListener progressListener) throws Exception {
        UDPPacket firstReq = UDPPacket.createDownloadCourseMaterial(courseCode, 0, sessionToken);
        UDPPacket firstResp = sendAndReceiveWithRetry(firstReq, MAX_RETRIES);
        if (firstResp == null || !firstResp.isSuccess()) {
            throw new Exception(firstResp != null ? firstResp.getMessage() : "Không thể tải mảnh đầu tiên của đề cương!");
        }

        int totalChunks = Math.max(1, firstResp.getTotalChunks());
        String expectedSha256 = firstResp.getMessage() != null ? firstResp.getMessage().trim() : "";
        StringBuilder reassembled = new StringBuilder();
        reassembled.append(firstResp.getPayload() != null ? firstResp.getPayload() : "");

        if (progressListener != null) {
            progressListener.onChunkReceived(1, totalChunks, expectedSha256);
        }

        for (int idx = 1; idx < totalChunks; idx++) {
            UDPPacket req = UDPPacket.createDownloadCourseMaterial(courseCode, idx, sessionToken);
            UDPPacket resp = sendAndReceiveWithRetry(req, MAX_RETRIES);
            if (resp == null || !resp.isSuccess()) {
                throw new Exception("Lỗi mất mảnh RUDP #" + (idx + 1) + "/" + totalChunks);
            }
            reassembled.append(resp.getPayload() != null ? resp.getPayload() : "");
            if (progressListener != null) {
                progressListener.onChunkReceived(idx + 1, totalChunks, expectedSha256);
            }
        }

        String finalContent = reassembled.toString();
        // Kiểm tra toàn vẹn SHA-256 Checksum sau khi ghép đủ các mảnh UDP
        if (!expectedSha256.isEmpty()) {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(finalContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            if (!expectedSha256.equalsIgnoreCase(sb.toString())) {
                throw new Exception("CẢNH BÁO TOÀN VẸN FILE RUDP: Mã băm SHA-256 sau khi ghép mảnh không khớp với Server!");
            }
        }
        return finalContent;
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

        byte[] receiveData = new byte[65507];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        Exception lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            long startNs = System.nanoTime();
            try {
                totalPacketsSent.incrementAndGet();
                socket.send(sendPacket);

                // Lọc bỏ các gói tin UDP phản hồi đến trễ của SequenceNum cũ
                while (true) {
                    socket.receive(receivePacket);
                    UDPPacket resp = UDPPacket.fromBytes(receivePacket.getData(), receivePacket.getLength());
                    if (resp == null) {
                        continue;
                    }
                    if (resp.getAckNum() > 0 && resp.getAckNum() != request.getSequenceNum()) {
                        System.out.println(String.format("[RUDP Client] Bỏ qua gói phản hồi trễ Ack#%d (Đang chờ Seq#%d)",
                                resp.getAckNum(), request.getSequenceNum()));
                        continue;
                    }
                    long elapsedMs = Math.max(1, (System.nanoTime() - startNs) / 1_000_000L);
                    lastRttMs.set(elapsedMs);
                    return resp;
                }
            } catch (SocketTimeoutException ste) {
                lastException = ste;
                totalRetransmissions.incrementAndGet();
                System.out.println(String.format("[RUDP Client] Gói #%d Timeout lần %d/%d, đang truyền lại (ARQ Retransmitting)...",
                        request.getSequenceNum(), attempt, maxAttempts));
                try {
                    Thread.sleep(150L * attempt);
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
        runningPush = false;
        if (pushSocket != null && !pushSocket.isClosed()) {
            pushSocket.close();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
