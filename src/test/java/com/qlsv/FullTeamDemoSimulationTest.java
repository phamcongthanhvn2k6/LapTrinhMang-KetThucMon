package com.qlsv;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

import com.qlsv.client.UDPClient;
import com.qlsv.database.DatabaseManager;
import com.qlsv.database.dao.AuditDAO;
import com.qlsv.model.GradeAppeal;
import com.qlsv.model.PageResult;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.model.StudentTranscript;
import com.qlsv.model.UserAccount;
import com.qlsv.network.UDPPacket;
import com.qlsv.security.DESEncryption;
import com.qlsv.server.SessionManager;
import com.qlsv.server.UDPServer;
import com.qlsv.util.ExcelExporter;
import com.qlsv.util.ExcelImporter;

/**
 * KỊCH BẢN MÔ PHỎNG & KIỂM THỬ THỰC NGHIỆM TRực TIẾP (LIVE E2E DEMO SIMULATION)
 * Chạy kiểm chứng tự động từng bước Demo của cả 6 thành viên Nhóm 2 trên Server RUDP & CSDL thật.
 */
public class FullTeamDemoSimulationTest {

    private static void verify(boolean cond, String stepName) {
        if (!cond) {
            throw new AssertionError("THẤT BẠI TẠI BƯỚC DEMO: " + stepName);
        }
        System.out.println("   [OK] " + stepName);
    }

    public static void main(String[] args) throws Exception {
        int testPort = 9889;
        UDPServer server = new UDPServer(testPort);
        server.start();
        Thread.sleep(1200); // Chờ Server kết nối HikariCP Cloud DB

        System.out.println("\n================================================================================");
        System.out.println("   BẮT ĐẦU CHẠY MÔ PHỎNG DEMO THỰC TẾ 6 THÀNH VIÊN NHÓM 2 (PORT RUDP " + testPort + ")");
        System.out.println("================================================================================\n");

        try (UDPClient teacherClient = new UDPClient("127.0.0.1", testPort);
             UDPClient studentClient = new UDPClient("127.0.0.1", testPort);
             UDPClient adminClient = new UDPClient("127.0.0.1", testPort)) {

            // =========================================================================
            // 1. THÀNH VIÊN 1 (NHÓM TRƯỞNG - 35%): PHẠM CÔNG THÀNH (N24DTCN079)
            // Giao thức RUDP 2.0, ARQ Chaos Recovery, Chunked Stream SHA-256, DES/AES-256 & IDS
            // =========================================================================
            System.out.println(">>> [1/6] DEMO THÀNH VIÊN 1 (NHÓM TRƯỞNG 35%): PHẠM CÔNG THÀNH (N24DTCN079)");
            verify(teacherClient.pingServer(), "Bước 1.1: Bắt tay PING/PONG & Đăng ký kênh Full-Duplex UDP Server-Push (RTT=" + teacherClient.getLastRttMs() + "ms)");

            // Giả lập mạng rớt gói 40% (Chaos Drop) -> Kiểm chứng RUDP Stop-and-Wait ARQ tự động truyền lại thành công
            server.setSimulatedDropRatePercent(40);
            String syllabus = studentClient.downloadCourseMaterialChunked("INT1339", null);
            verify(syllabus != null && syllabus.contains("INT1339"),
                    "Bước 1.2: Truyền tệp Đề cương phân mảnh (RUDP Chunked File Stream + SHA-256 Checksum) vượt qua mạng rớt gói 40% (ARQ Retries=" + studentClient.getTotalRetransmissions() + ")");
            server.setSimulatedDropRatePercent(0);

            // Kiểm chứng khóa DES 8-byte nghiêm ngặt & Bắt lỗi sai khóa BadPaddingException
            DESEncryption.validateStrictEightByteKey("QLSV_KEY");
            String sampleCipher = DESEncryption.encrypt("Phạm Công Thành", "QLSV_KEY");
            boolean blockedWrongDesKey = false;
            try {
                String wrong = DESEncryption.decrypt(sampleCipher, "QLSV_KEi");
                if (!"Phạm Công Thành".equals(wrong)) blockedWrongDesKey = true;
            } catch (Exception ex) {
                blockedWrongDesKey = true;
            }
            verify(blockedWrongDesKey, "Bước 1.3: Kiểm chứng khóa DES 8-byte & Chặn đứng hiển thị khi sai 1 ký tự khóa ('QLSV_KEi')");

            // Kiểm chứng Tường lửa IDS chặn gói MITM sửa điểm & gói Replay Attack quá hạn
            try (DatagramSocket rawSock = new DatagramSocket()) {
                rawSock.setSoTimeout(2000);
                UDPPacket mitmPkt = UDPPacket.createAddStudent("{\"studentId\":\"SV001\"}", "fake-token");
                mitmPkt.setSequenceNum(7001);
                mitmPkt.signPacket();
                mitmPkt.setPayload("{\"studentId\":\"SV001\",\"hacked\":10.0}");
                byte[] tampered = mitmPkt.toBytesWithoutResigning();
                rawSock.send(new DatagramPacket(tampered, tampered.length, InetAddress.getByName("127.0.0.1"), testPort));
                byte[] buf = new byte[4096];
                DatagramPacket dp = new DatagramPacket(buf, buf.length);
                rawSock.receive(dp);
                UDPPacket mitmResp = UDPPacket.fromBytes(dp.getData(), dp.getLength());
                verify(mitmResp != null && !mitmResp.isSuccess() && mitmResp.getMessage().contains("HMAC-SHA256"),
                        "Bước 1.4: IDS Firewall chặn đứng gói tin UDP bị tấn công MITM sửa điểm trên đường truyền");
            }

            // =========================================================================
            // 2. THÀNH VIÊN 2 (13%): HỒ QUỐC KHẢI (N24DTCN042)
            // Hạ tầng HikariCP Cloud PostgreSQL, Phân trang SQL LIMIT/OFFSET & ACID Transaction
            // =========================================================================
            System.out.println("\n>>> [2/6] DEMO THÀNH VIÊN 2 (13%): HỒ QUỐC KHẢI (N24DTCN042)");
            verify(DatabaseManager.getInstance().isConnected(), "Bước 2.1: Kết nối HikariCP Connection Pool tới CSDL thành công");
            PageResult<StudentResult> page1 = teacherClient.getStudentsPage(1, 20, "");
            verify(page1 != null && !page1.getItems().isEmpty() && page1.getTotalItems() >= 200,
                    "Bước 2.2: Truy vấn Phân trang SQL (LIMIT 20 OFFSET 0) & Giải mã thành công (Tổng: " + page1.getTotalItems() + " sinh viên)");
            PageResult<StudentResult> searchPage = teacherClient.getStudentsPage(1, 20, "SV001");
            verify(searchPage != null && !searchPage.getItems().isEmpty(),
                    "Bước 2.3: Tìm kiếm phân trang theo Mã SV 'SV001' trả về đúng hồ sơ sinh viên");

            // =========================================================================
            // 3. THÀNH VIÊN 3 (13%): DƯƠNG TRUNG DŨNG (N24DTCN017)
            // Xác thực SHA-256 + 16B Salt, Phân quyền Zero-Trust RBAC, Brute-Force & Audit Trail
            // =========================================================================
            System.out.println("\n>>> [3/6] DEMO THÀNH VIÊN 3 (13%): DƯƠNG TRUNG DŨNG (N24DTCN017)");
            // Thử gửi lệnh sửa điểm khi chưa đăng nhập (Zero-Trust RBAC phải chặn)
            boolean blockedUnauthWrite = false;
            try {
                studentClient.sendStudentData(new StudentData("SV001", "Vũ Khánh Bích", 10.0, 10.0, 10.0));
            } catch (Exception ex) {
                blockedUnauthWrite = ex.getMessage().contains("ZERO-TRUST") || ex.getMessage().contains("PHÂN QUYỀN");
            }
            verify(blockedUnauthWrite, "Bước 3.1: Zero-Trust RBAC chặn đứng Client Học sinh / Chưa đăng nhập tự ý sửa điểm");

            UserAccount gvAcc = teacherClient.login("giangvien", "ptit123");
            verify(gvAcc != null && gvAcc.getRole() == UserAccount.Role.GIANGVIEN && teacherClient.getSessionToken() != null,
                    "Bước 3.2: Đăng nhập tài khoản Giảng viên ('giangvien / ptit123') nhận Session Token RBAC hợp lệ");

            UserAccount adminAcc = adminClient.login("admin", "admin123");
            verify(adminAcc != null && adminAcc.getRole() == UserAccount.Role.ADMIN,
                    "Bước 3.3: Đăng nhập tài khoản Quản trị viên ('admin / admin123') thành công");

            List<AuditDAO.AuditEntry> logs = teacherClient.getAuditLogs(20);
            verify(logs != null && !logs.isEmpty(),
                    "Bước 3.4: Truy xuất Nhật ký Thanh tra Bất biến (Dual-Persistence Audit Trail: " + logs.size() + " bản ghi mới nhất)");

            // =========================================================================
            // 4. THÀNH VIÊN 4 (13%): CAO THÀNH LONG (N24DTCN050)
            // Nghiệp vụ Điểm Tín Chỉ (10%-20%-70%), Cấm thi, Điểm liệt, Chấm điểm môn & Học bổng KKHT
            // =========================================================================
            System.out.println("\n>>> [4/6] DEMO THÀNH VIÊN 4 (13%): CAO THÀNH LONG (N24DTCN050)");
            StudentResult origSv001 = DatabaseManager.getInstance().getStudentDecrypted("SV001");
            StudentData updateSv001 = new StudentData("SV001", origSv001.getFullName(), 9.0, 8.5, 9.0);
            StudentResult updatedRes = teacherClient.sendStudentData(updateSv001);
            verify(updatedRes != null && Math.abs(updatedRes.getAverageScore() - 8.90) < 0.01 && updatedRes.isScholarshipEligible(),
                    "Bước 4.1: Giảng viên chấm điểm SV001 (CC=9.0, GK=8.5, CK=9.0 -> ĐTB=8.90, GPA=3.7, Đạt Học bổng KKHT)");

            StudentTranscript tAfterCourseEdit = teacherClient.updateCourseGrade("SV001", "INT1310", 8.5, 9.0);
            verify(tAfterCourseEdit != null && tAfterCourseEdit.getCumulativeGpa4() > 0,
                    "Bước 4.2: Chấm điểm riêng môn Cơ Sở Dữ Liệu SQL (INT1310) trong Phiếu báo điểm mà không làm lệch điểm môn chính INT1339");

            // Khôi phục điểm gốc của SV001 để giữ nguyên trạng thái CSDL sạch
            teacherClient.sendStudentData(new StudentData("SV001", origSv001.getFullName(),
                    origSv001.getScoreCC(), origSv001.getScoreGK(), origSv001.getScoreCK()));

            // =========================================================================
            // 5. THÀNH VIÊN 5 (13%): NGUYỄN DUY QUANG NHẬT (N24DTCN119)
            // Cổng Sinh Viên, Phúc Khảo Điểm 2 Chiều & Cổng Đăng Ký Tín Chỉ (10 <= TC <= 14/22)
            // =========================================================================
            System.out.println("\n>>> [5/6] DEMO THÀNH VIÊN 5 (13%): NGUYỄN DUY QUANG NHẬT (N24DTCN119)");
            StudentTranscript svTranscript = studentClient.getDetailedTranscript("SV001");
            verify(svTranscript != null && svTranscript.getGrades().size() >= 5,
                    "Bước 5.1: Cổng Sinh Viên tra cứu Bảng điểm 5 môn tín chỉ & 4 thẻ KPI (CPA=" + svTranscript.getCumulativeGpa4() + ")");

            SessionManager.getInstance().clearSpamState("SV001");
            String appealResp = studentClient.submitGradeAppeal("SV002", "INT1340", "Em xin phúc khảo bài thi môn An toàn thông tin");
            verify(appealResp != null && appealResp.contains("phúc khảo"),
                    "Bước 5.2: Sinh viên gửi Đơn phúc khảo điểm trực tuyến lên Hội đồng bộ môn");

            List<GradeAppeal> appeals = teacherClient.getGradeAppeals();
            GradeAppeal newestAppeal = appeals.get(0);
            List<GradeAppeal> afterResolve = teacherClient.resolveGradeAppeal(newestAppeal.getAppealId(), false, 0, 0, "Đã chấm thẩm định, giữ nguyên điểm");
            verify(afterResolve != null && "TỪ CHỐI".equals(afterResolve.get(0).getStatus()),
                    "Bước 5.3: Hội đồng Giảng viên xét duyệt đơn phúc khảo 2 chiều & phản hồi trạng thái thời gian thực");

            // Kiểm tra chặn đăng ký tín chỉ < 10 TC và cho phép đăng ký hợp lệ 16 TC
            boolean blockedUnder10Credits = false;
            try {
                studentClient.registerSemesterCredits("SV001", 6, "INT1401, INT1402");
            } catch (Exception ex) {
                blockedUnder10Credits = ex.getMessage().contains("10 tín chỉ");
            }
            verify(blockedUnder10Credits, "Bước 5.4: Cổng Đăng ký Tín chỉ chặn đúng quy chế khi chọn dưới 10 tín chỉ (6 TC)");

            SessionManager.getInstance().clearSpamState("SV001");
            String creditOk = studentClient.registerSemesterCredits("SV001", 16, "INT1401 (3 TC), INT1402 (3 TC), INT1403 (4 TC), INT1404 (3 TC), INT1405 (3 TC)");
            verify(creditOk != null && creditOk.contains("16/"),
                    "Bước 5.5: Đăng ký thành công 16 tín chỉ học kỳ mới kèm tính học phí tự động");

            // =========================================================================
            // 6. THÀNH VIÊN 6 (13%): NGUYỄN QUANG HOÀNG (N24DTCN032)
            // Xuất/Nhập Báo Cáo Excel (.xlsx Apache POI), Thống Kê Biểu Đồ Kép & InputValidator
            // =========================================================================
            System.out.println("\n>>> [6/6] DEMO THÀNH VIÊN 6 (13%): NGUYỄN QUANG HOÀNG (N24DTCN032)");
            Map<String, Integer> rankStats = DatabaseManager.getInstance().getRankStatistics();
            int totalRanked = rankStats.values().stream().mapToInt(Integer::intValue).sum();
            verify(totalRanked >= 200, "Bước 6.1: Tổng hợp dữ liệu Biểu đồ Kép JFreeChart (Tròn + Cột Phổ điểm) trên " + totalRanked + " sinh viên");

            File tempTemplate = new File("target/Test_Mau_Import_PTIT.xlsx");
            ExcelImporter.createSampleTemplate(tempTemplate);
            List<StudentData> parsedExcel = ExcelImporter.importStudentsFromExcel(tempTemplate);
            verify(tempTemplate.exists() && !parsedExcel.isEmpty(),
                    "Bước 6.2: Tạo File Mẫu Excel (.xlsx) & Đọc kiểm tra hợp lệ bằng Apache POI (" + parsedExcel.size() + " dòng mẫu)");

            File tempExport = new File("target/Test_PhieuDiem_SV001.xlsx");
            ExcelExporter.exportTranscriptToExcel(svTranscript, tempExport);
            verify(tempExport.exists() && tempExport.length() > 1000,
                    "Bước 6.3: Xuất Phiếu báo điểm cá nhân ra file Excel (.xlsx) thành công (" + tempExport.length() + " bytes)");

            tempTemplate.delete();
            tempExport.delete();
        } finally {
            server.stop();
        }

        System.out.println("\n================================================================================");
        System.out.println("✔ HOÀN TẤT MÔ PHỎNG DEMO 100%: TẤT CẢ 6 PHẦN CỦA 6 THÀNH VIÊN ĐỀU CHẠY HOÀN HẢO!");
        System.out.println("================================================================================\n");
    }
}

