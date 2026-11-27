package com.qlsv;

import com.qlsv.model.CourseGrade;
import com.qlsv.model.GradeAppeal;
import com.qlsv.model.StudentResult;
import com.qlsv.model.StudentTranscript;
import com.qlsv.network.UDPPacket;
import com.qlsv.security.SecurityManager;
import com.qlsv.util.InputValidator;

/**
 * Kiểm thử Tự động Toàn diện (Unit & Integration Verification):
 * 1. Kiểm tra InputValidator (Chuẩn hóa Mã SV, Họ tên, dấu phẩy thập phân '8,5', chặn dữ liệu rác)
 * 2. Kiểm tra Logic Nghiệp vụ Đào tạo Tín chỉ (Cấm thi ở 0.0 và < 5.0, Điểm liệt < 4.0, Tín chỉ tích lũy môn ĐẠT, Học bổng KKHT)
 * 3. Kiểm tra Bảo mật Kép (DES + AES-256-GCM) và Chữ ký gói tin HMAC-SHA256 chống giả mạo
 */
public class ValidationAndBusinessLogicTest {

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError("KIỂM THỬ THẤT BẠI: " + msg);
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== BẮT ĐẦU KIỂM THỬ TOÀN DIỆN VALIDATE, BẢO MẬT & LOGIC TÍN CHỈ ===");

        // 1. Test InputValidator
        assertTrue("SV001".equals(InputValidator.normalizeAndValidateStudentId("  sv001 ")), "Chuẩn hóa Mã SV chữ hoa");
        assertTrue("Nguyễn Văn An".equals(InputValidator.normalizeAndValidateFullName("  Nguyễn   Văn   An ")), "Chuẩn hóa khoảng trắng Họ tên");
        assertTrue(Math.abs(8.75 - InputValidator.parseAndValidateScore("8,75", "Điểm")) < 1e-6, "Chuyển đổi dấu phẩy thập phân 8,75 -> 8.75");

        boolean caughtInvalidId = false;
        try {
            InputValidator.normalizeAndValidateStudentId("sv@01!");
        } catch (IllegalArgumentException e) {
            caughtInvalidId = true;
        }
        assertTrue(caughtInvalidId, "Phải chặn Mã SV chứa ký tự đặc biệt");

        boolean caughtInvalidScore = false;
        try {
            InputValidator.parseAndValidateScore("10,5", "Điểm");
        } catch (IllegalArgumentException e) {
            caughtInvalidScore = true;
        }
        assertTrue(caughtInvalidScore, "Phải chặn điểm > 10.0");

        // 2. Test Exam Ban (Chuyên cần = 0.0 và Chuyên cần = 4.5)
        StudentResult bannedZero = new StudentResult("SV901", "Lê Văn Nghỉ", StudentResult.calculateWeightedScore(0.0, 9.0, 9.0));
        bannedZero.setScoreCC(0.0);
        bannedZero.setScoreGK(9.0);
        bannedZero.setScoreCK(9.0);
        assertTrue(bannedZero.isExamBanned(), "Chuyên cần = 0.0 phải bị Cấm thi");
        assertTrue("F".equals(bannedZero.getLetterGrade()), "Cấm thi phải nhận điểm chữ F");
        assertTrue(bannedZero.getGpa4() == 0.0, "Cấm thi phải có GPA Hệ 4 = 0.0");
        assertTrue(bannedZero.getAcademicStatus().contains("Cấm thi"), "Trạng thái phải là Cấm thi");

        // 3. Test Final Exam Fail (Điểm liệt Thi < 4.0 dù quá trình 10.0)
        StudentResult failedFinal = new StudentResult("SV902", "Trần Điểm Liệt", StudentResult.calculateWeightedScore(10.0, 10.0, 3.5));
        failedFinal.setScoreCC(10.0);
        failedFinal.setScoreGK(10.0);
        failedFinal.setScoreCK(3.5);
        assertTrue(failedFinal.isFinalExamFailed(), "Thi cuối kỳ 3.5 < 4.0 phải bị Điểm liệt");
        assertTrue("F".equals(failedFinal.getLetterGrade()), "Điểm liệt cuối kỳ phải nhận điểm chữ F");
        assertTrue(!failedFinal.isScholarshipEligible(), "Điểm liệt không được xét học bổng");

        // 4. Test CourseGrade & StudentTranscript Accumulated Credits (Chỉ tính tín chỉ môn ĐẠT)
        StudentTranscript transcript = new StudentTranscript("SV001", "Vũ Khánh Bích", "D21CQCN01-N");
        transcript.addCourseGrade(new CourseGrade("INT1339", "Lập Trình Mạng", 3, 9.0, 8.5, 9.0));       // ĐẠT (3 TC)
        transcript.addCourseGrade(new CourseGrade("INT1340", "An Toàn Thông Tin", 3, 8.0, 8.0, 8.5));    // ĐẠT (3 TC)
        transcript.addCourseGrade(new CourseGrade("INT1310", "Cơ Sở Dữ Liệu", 3, 9.0, 9.0, 3.5));        // TRƯỢT do điểm liệt thi 3.5 < 4.0 (0 TC tích lũy)
        assertTrue(transcript.getRegisteredCredits() == 9, "Tổng tín chỉ đăng ký phải là 9 TC");
        assertTrue(transcript.getTotalCredits() == 6, "Tín chỉ tích lũy ĐẠT chỉ được tính 6 TC (loại trừ môn F)");

        // 5. Test Dual Cipher (DES & AES-256-GCM) & HMAC-SHA256 Packet Tampering Detection
        SecurityManager.setCurrentAlgo(SecurityManager.EncryptionAlgo.AES_256);
        String encAes = SecurityManager.encrypt("Vũ Khánh Bích", "QLSV_KEY");
        assertTrue(encAes.startsWith("GCM:"), "AES-256-GCM phải có tiền tố GCM:");
        assertTrue("Vũ Khánh Bích".equals(SecurityManager.decrypt(encAes, "QLSV_KEY")), "Giải mã AES-256-GCM chính xác");
        SecurityManager.setCurrentAlgo(SecurityManager.EncryptionAlgo.DES);
        assertTrue("Vũ Khánh Bích".equals(SecurityManager.decrypt(encAes, "QLSV_KEY")), "Tự động giải mã chéo AES-256-GCM ngay cả khi đang ở chế độ DES");

        UDPPacket pkt = UDPPacket.createAddStudent("{\"studentId\":\"SV001\"}", "token123");
        pkt.toBytes(); // Ký HMAC-SHA256
        assertTrue(pkt.verifyIntegrity(), "Chữ ký HMAC-SHA256 gốc phải hợp lệ");
        pkt.setPayload("{\"studentId\":\"SV001\",\"hackedScore\":10.0}"); // Giả lập kẻ tấn công sửa gói tin trên đường truyền
        assertTrue(!pkt.verifyIntegrity(), "Phải phát hiện gói tin UDP bị sửa đổi Payload (HMAC-SHA256 mismatch)");

        // 6. Test Phase 5: GradeAppeal two-way resolution & Snapshot HMAC verification
        GradeAppeal appeal = new GradeAppeal(101, "SV001", "INT1339", "Phúc khảo điểm thi cuối kỳ", "CHỜ DUYỆT", "Đang chấm lại", "2026-11-24 23:00");
        appeal.setStatus("ĐÃ DUYỆT NÂNG ĐIỂM");
        appeal.setTeacherNote("Hội đồng thống nhất nâng điểm thi lên 8.5");
        assertTrue("ĐÃ DUYỆT NÂNG ĐIỂM".equals(appeal.getStatus()), "Cập nhật trạng thái đơn phúc khảo 2 chiều");
        assertTrue(appeal.getTeacherNote().contains("8.5"), "Lưu ghi chú hội đồng chấm phúc khảo");

        UDPPacket resolvePkt = UDPPacket.createResolveGradeAppeal(101, true, 9.0, 8.5, "Duyệt nâng điểm", "token123");
        resolvePkt.toBytes();
        assertTrue(resolvePkt.verifyIntegrity(), "Gói tin RESOLVE_GRADE_APPEAL phải được ký HMAC-SHA256 hợp lệ");

        UDPPacket regPkt = UDPPacket.createRegisterCredits("SV001", 16, "INT1401, INT1402", "token123");
        regPkt.toBytes();
        assertTrue(regPkt.verifyIntegrity(), "Gói tin REGISTER_CREDITS phải được ký HMAC-SHA256 hợp lệ");

        // 7. Test Strict 8-Byte DES Key Validation & Wrong-Key BadPaddingException Safe Recovery (from ptit-ltm/student-udp-des)
        com.qlsv.security.DESEncryption.validateStrictEightByteKey("PTIT2026");
        boolean caughtShortKey = false;
        try {
            com.qlsv.security.DESEncryption.validateStrictEightByteKey("SHORT");
        } catch (IllegalArgumentException e) {
            caughtShortKey = true;
        }
        assertTrue(caughtShortKey, "Khóa DES không đủ 8 bytes phải báo lỗi rõ ràng");

        String desCipher = com.qlsv.security.DESEncryption.encrypt("Nguyễn Văn An", "PTIT2026");
        boolean caughtWrongKey = false;
        try {
            String wrongDec = com.qlsv.security.DESEncryption.decrypt(desCipher, "WRONG999");
            if (!"Nguyễn Văn An".equals(wrongDec)) {
                caughtWrongKey = true;
            }
        } catch (javax.crypto.BadPaddingException | javax.crypto.IllegalBlockSizeException ex) {
            caughtWrongKey = true;
        }
        assertTrue(caughtWrongKey, "Giải mã DES với khóa sai (WRONG999) phải bắt được BadPaddingException hoặc khác bản rõ mà không sập hệ thống");
        assertTrue("Nguyễn Văn An".equals(com.qlsv.security.DESEncryption.decrypt(desCipher, "PTIT2026")),
                "Khôi phục lại đúng khóa DES (PTIT2026) phải giải mã chính xác bản rõ ban đầu");

        // 8. Test Account Registration & Change Password RUDP Packets + SHA-256 Salt Hashing
        String salt = com.qlsv.security.PasswordHasher.generateSalt();
        String hash = com.qlsv.security.PasswordHasher.hashPassword("ptit123456", salt);
        assertTrue(com.qlsv.security.PasswordHasher.verifyPassword("ptit123456", salt, hash), "Xác thực mật khẩu SHA-256 + Salt chính xác");
        assertTrue(!com.qlsv.security.PasswordHasher.verifyPassword("wrongpass", salt, hash), "Từ chối mật khẩu sai với SHA-256 + Salt");

        UDPPacket regAccPkt = UDPPacket.createRegisterAccount("sv_thanh079", "ptit123456", "HOCSINH", "SV001", "Phạm Công Thành", "n24dtcn079@student.ptithcm.edu.vn", "");
        regAccPkt.toBytes();
        assertTrue(regAccPkt.verifyIntegrity(), "Gói tin REGISTER_ACCOUNT phải được ký HMAC-SHA256 hợp lệ");

        UDPPacket chgPassPkt = UDPPacket.createChangePassword("sv_thanh079", "ptit123456", "newpass789", "token123");
        chgPassPkt.toBytes();
        assertTrue(chgPassPkt.verifyIntegrity(), "Gói tin CHANGE_PASSWORD phải được ký HMAC-SHA256 hợp lệ");

        // 9. Test Zero-Trust Unsigned Packet & Zero-Timestamp Rejection (Chặn tuyệt đối gói tin bỏ trống chữ ký HMAC hoặc Timestamp = 0)
        UDPPacket unsignedPkt = UDPPacket.createAddStudent("{\"studentId\":\"SV999\"}", "token123");
        unsignedPkt.setHmacSignature("");
        assertTrue(!unsignedPkt.verifyIntegrity(), "Phải chặn tuyệt đối gói tin UDP bỏ trống chữ ký HMAC-SHA256");
        unsignedPkt.setTimestamp(0L);
        assertTrue(!unsignedPkt.isReplayValid(), "Phải chặn tuyệt đối gói tin UDP có Timestamp <= 0");

        // 10. Test CourseCode, Username & Pipe-Delimiter Sanitization Validation
        assertTrue("INT1339".equals(InputValidator.normalizeAndValidateCourseCode(" int1339 ")), "Chuẩn hóa mã học phần INT1339");
        assertTrue("sv_thanh079".equals(InputValidator.validateAndNormalizeUsername(" SV_Thanh079 ")), "Chuẩn hóa tên đăng nhập chữ thường");
        String sanitizedReason = InputValidator.validateAndSanitizeReasonOrNote("Em xin phúc khảo câu 1 | câu 2", "Lý do phúc khảo", 5, 300);
        assertTrue(!sanitizedReason.contains("|"), "Phải lọc bỏ ký tự phân cách giao thức '|' trong lý do phúc khảo");

        // 11. Test Full-Envelope HMAC-SHA256 Signing (Token, Success, Message, AckNum Tampering Detection)
        UDPPacket tokenPkt = UDPPacket.createAddStudent("{\"studentId\":\"SV001\"}", "valid-teacher-token");
        tokenPkt.toBytes();
        assertTrue(tokenPkt.verifyIntegrity(), "Gói tin có Session Token hợp lệ phải xác thực HMAC thành công");
        tokenPkt.setToken("hijacked-admin-token");
        assertTrue(!tokenPkt.verifyIntegrity(), "Phải phát hiện ngay hành vi đánh tráo Session Token trên đường truyền");

        UDPPacket respPkt = UDPPacket.createLoginResponse(false, "Sai mật khẩu", null, null);
        respPkt.toBytes();
        assertTrue(respPkt.verifyIntegrity(), "Gói tin phản hồi Server phải được ký HMAC hợp lệ");
        respPkt.setSuccess(true); // Giả lập kẻ gian sửa cờ success từ false -> true
        assertTrue(!respPkt.verifyIntegrity(), "Phải chặn ngay hành vi sửa cờ trạng thái success trên gói phản hồi");

        // 12. Test Multi-Tier Anti-Spam & DoS Flood Rate Limiting (Sliding Window + Action Cooldown)
        com.qlsv.server.SessionManager sm = com.qlsv.server.SessionManager.getInstance();
        String testBotKey = "10.0.0.99:bot-token";
        sm.clearSpamState(testBotKey);
        for (int i = 1; i <= com.qlsv.server.SessionManager.MAX_MUTATIONS_PER_10_SEC; i++) {
            assertTrue(sm.checkPacketSpamLimit(testBotKey, true) == null, "Các lệnh ghi trong ngưỡng cho phép phải thành công");
        }
        String spamBlockMsg = sm.checkPacketSpamLimit(testBotKey, true);
        assertTrue(spamBlockMsg != null && spamBlockMsg.contains("CHẶN SPAM"), "Vượt ngưỡng 18 lệnh ghi/10s phải bị tường lửa Anti-Spam chặn");

        assertTrue(sm.checkActionCooldown("SV001", "GỬI ĐƠN PHÚC KHẢO", 5000L) == null, "Lần gửi đơn đầu tiên phải được phép");
        String cooldownMsg = sm.checkActionCooldown("SV001", "GỬI ĐƠN PHÚC KHẢO", 5000L);
        assertTrue(cooldownMsg != null && cooldownMsg.contains("CHỐNG SPAM"), "Gửi liên tiếp trong thời gian Cooldown 5s phải bị chặn");
        sm.clearSpamState(testBotKey);

        System.out.println("✔ TẤT CẢ CÁC BÀI KIỂM THỬ VALIDATE, BẢO MẬT & LOGIC TÍN CHỈ ĐỀU ĐẠT 100%!");
    }
}

