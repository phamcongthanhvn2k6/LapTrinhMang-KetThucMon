package com.qlsv.network;

import com.qlsv.database.DatabaseManager;
import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;

public class SupabaseTest {
    public static void main(String[] args) {
        System.out.println("========== KIỂM TRA KẾT NỐI CSDL SUPABASE CLOUD ==========");

        String[][] testCases = {
            {"aws-0-ap-southeast-1.pooler.supabase.com", "6543", "postgres.udkrzbihnlxbynfovtfy"},
            {"aws-0-ap-southeast-1.pooler.supabase.com", "5432", "postgres.udkrzbihnlxbynfovtfy"},
            {"db.udkrzbihnlxbynfovtfy.supabase.co", "5432", "postgres"},
            {"aws-0-ap-southeast-1.pooler.supabase.com", "6543", "postgres"},
            {"aws-0-ap-southeast-1.pooler.supabase.com", "5432", "postgres"}
        };

        boolean connected = false;
        for (String[] tc : testCases) {
            String host = tc[0];
            int port = Integer.parseInt(tc[1]);
            String user = tc[2];

            System.out.printf("Thử kết nối: Host=%s | Port=%d | User=%s ...\n", host, port, user);
            try {
                SqlConfig config = new SqlConfig(
                        SqlConfig.DbType.POSTGRESQL,
                        host, port, "postgres", user, "thanhtyou123@"
                );
                if (DatabaseManager.getInstance().connect(config)) {
                    System.out.printf("===> KẾT NỐI THÀNH CÔNG VỚI CONFIG: Host=%s | Port=%d | User=%s\n", host, port, user);

                    StudentData student = new StudentData("SV888", "Nguyễn Văn Supabase", 9.0, 9.5, 10.0);
                    StudentResult result = DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(student);
                    System.out.printf("Đã lưu CSDL Supabase thành công: Mã SV=%s | ĐTB=%.2f\n", result.getStudentId(), result.getAverageScore());

                    java.util.List<StudentResult> all = DatabaseManager.getInstance().getAllStudentsDecrypted();
                    System.out.println("===> SỐ LƯỢNG SINH VIÊN TRUY VẤN ĐƯỢC: " + all.size());
                    
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    com.qlsv.network.UDPPacket respPacket = com.qlsv.network.UDPPacket.createStudentsListResponse(true, "Tải danh sách thành công", gson.toJson(all));
                    byte[] bytes = respPacket.toBytes();
                    System.out.println("===> KÍCH THƯỚC GÓI TIN UDP (BYTES): " + bytes.length);

                    com.qlsv.network.UDPPacket decodedPacket = com.qlsv.network.UDPPacket.fromBytes(bytes, bytes.length);
                    java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<StudentResult>>() {}.getType();
                    java.util.List<StudentResult> decodedList = gson.fromJson(decodedPacket.getPayload(), listType);
                    System.out.println("===> GIẢI NÉN VÀ GIẢI MÃ THÀNH CÔNG: " + decodedList.size() + " SINH VIÊN!");
                    connected = true;
                    break;
                }
            } catch (Exception e) {
                System.out.println("   --> Thất bại: " + e.getMessage());
            } finally {
                DatabaseManager.getInstance().closeConnection();
            }
        }

        if (!connected) {
            System.err.println("!!! Chưa thể kết nối trực tiếp Supabase Cloud (Vui lòng kiểm tra trạng thái khởi tạo trên web Supabase).");
        }
    }
}
