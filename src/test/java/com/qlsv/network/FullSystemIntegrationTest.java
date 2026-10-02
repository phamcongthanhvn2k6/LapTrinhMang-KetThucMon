package com.qlsv.network;

import com.google.gson.Gson;
import com.qlsv.client.UDPClient;
import com.qlsv.database.DatabaseManager;
import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.server.UDPServer;

import java.util.List;

public class FullSystemIntegrationTest {

    private static final Gson gson = new Gson();

    public static void main(String[] args) {
        System.out.println("========== BẮT ĐẦU KIỂM THỬ TÍCH HỢP TOÀN BỘ HỆ THỐNG (UDP & DES & SQL) ==========");
        int testPort = 9877;
        UDPServer server = null;

        try {
            // 1. Start UDP Server
            server = new UDPServer(testPort);
            server.setLogListener(new UDPServer.ServerLogListener() {
                @Override
                public void onLog(String message) {
                    System.out.println("[TEST LOG] " + message);
                }

                @Override
                public void onDataUpdated() {
                    System.out.println("[TEST LOG] Dữ liệu CSDL đã được cập nhật!");
                }
            });
            server.start();
            Thread.sleep(500);

            // 2. Initialize UDP Client
            UDPClient client = new UDPClient("127.0.0.1", testPort);

            // 3. Test UDP Ping
            System.out.println("\n--- BƯỚC 1: KIỂM TRA UDP PING HANDSHAKE ---");
            boolean pingSuccess = client.pingServer();
            if (pingSuccess) {
                System.out.println("===> BƯỚC 1 PHẢN HỒI PING PONG THÀNH CÔNG!");
            } else {
                throw new Exception("Lỗi: Ping Server thất bại!");
            }

            // 4. Test DB Connection configuration via UDP
            System.out.println("\n--- BƯỚC 2: GỬI THÔNG SỐ CSDL LÊN SERVER VIA UDP ---");
            SqlConfig h2Config = new SqlConfig(
                    SqlConfig.DbType.H2_EMBEDDED,
                    "localhost", 0, "test_qlsv_db", "sa", ""
            );
            UDPPacket dbResp = client.connectDatabase(h2Config);
            if (dbResp != null && dbResp.isSuccess()) {
                System.out.println("===> BƯỚC 2 KẾT NỐI CSDL THÀNH CÔNG: " + dbResp.getMessage());
            } else {
                throw new Exception("Lỗi: Server kết nối CSDL thất bại! " + (dbResp != null ? dbResp.getMessage() : ""));
            }

            // 5. Test Sending Student Records & Average Calculation
            System.out.println("\n--- BƯỚC 3: GỬI DỮ LIỆU SINH VIÊN, MÃ HÓA DES, LƯU SQL & TÍNH DTB ---");

            StudentData s1 = new StudentData("SV001", "Nguyễn Văn An", 9.0, 8.0, 7.0);
            StudentData s2 = new StudentData("SV002", "Lê Thị Bình", 6.5, 7.5, 8.5);
            StudentData s3 = new StudentData("SV003", "Phạm Minh Cường", 10.0, 9.0, 9.5);

            StudentResult r1 = client.sendStudentData(s1);
            System.out.printf("Kết quả SV1 (%s): %s | Điểm TB = %.2f (Kỳ vọng: 8.00)\n", r1.getStudentId(), r1.getFullName(), r1.getAverageScore());
            assertAlmostEquals(8.00, r1.getAverageScore(), "SV1 DTB");

            StudentResult r2 = client.sendStudentData(s2);
            System.out.printf("Kết quả SV2 (%s): %s | Điểm TB = %.2f (Kỳ vọng: 7.50)\n", r2.getStudentId(), r2.getFullName(), r2.getAverageScore());
            assertAlmostEquals(7.50, r2.getAverageScore(), "SV2 DTB");

            StudentResult r3 = client.sendStudentData(s3);
            System.out.printf("Kết quả SV3 (%s): %s | Điểm TB = %.2f (Kỳ vọng: 9.50)\n", r3.getStudentId(), r3.getFullName(), r3.getAverageScore());
            assertAlmostEquals(9.50, r3.getAverageScore(), "SV3 DTB");

            // 6. Verify DES Encrypted Storage in SQL Database Inspection
            System.out.println("\n--- BƯỚC 4: KIỂM TRA DỮ LIỆU ĐÃ MÃ HÓA DES TRONG CSDL ---");
            List<DatabaseManager.EncryptedRecord> records = DatabaseManager.getInstance().getAllRecordsForInspection();
            System.out.println("Tổng số bản ghi trong CSDL: " + records.size());

            for (DatabaseManager.EncryptedRecord rec : records) {
                System.out.println("--------------------------------------------------");
                System.out.println("Mã SV               : " + rec.maSV);
                System.out.println("Họ tên (Mã hóa DES) : " + rec.hoTenEncrypted);
                System.out.println("Điểm Toán (Mã hóa)  : " + rec.diemToanEncrypted);
                System.out.println("Điểm Văn (Mã hóa)   : " + rec.diemVanEncrypted);
                System.out.println("Điểm Anh (Mã hóa)   : " + rec.diemAnhEncrypted);
                System.out.println("Họ tên (Giải mã DES): " + rec.hoTenDecrypted);
                System.out.println("Điểm TB Lưu CSDL   : " + rec.diemTB);

                if (rec.hoTenEncrypted.equals(rec.hoTenDecrypted)) {
                    throw new Exception("Lỗi: Dữ liệu chưa được mã hóa DES!");
                }
            }

            System.out.println("\n==================================================================");
            System.out.println("===> TOÀN BỘ KIỂM THỬ TÍCH HỢP HỆ THỐNG ĐÃ ĐẠT KẾT QUẢ 100% SUÔN SẺ!");
            System.out.println("==================================================================");

        } catch (Exception e) {
            System.err.println("!!! LỖI KIỂM THỬ TÍCH HỢP: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

    private static void assertAlmostEquals(double expected, double actual, String label) throws Exception {
        if (Math.abs(expected - actual) > 0.01) {
            throw new Exception("Lỗi sai số cho " + label + ": kỳ vọng " + expected + " nhưng nhận được " + actual);
        }
    }
}
