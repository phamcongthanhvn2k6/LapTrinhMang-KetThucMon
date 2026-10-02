package com.qlsv.network;

import com.qlsv.database.DatabaseManager;
import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;

public class SupabaseTest {
    public static void main(String[] args) {
        System.out.println("========== KIỂM TRA KẾT NỐI CSDL SUPABASE CLOUD ==========");
        try {
            SqlConfig config = new SqlConfig(
                    SqlConfig.DbType.POSTGRESQL,
                    "aws-0-ap-southeast-1.pooler.supabase.com",
                    5432,
                    "postgres",
                    "postgres.udkrzbihnixbynfovtfy",
                    "thanhtyou123@"
            );

            System.out.println("Đang kết nối tới Supabase Cloud...");
            boolean success = DatabaseManager.getInstance().connect(config);

            if (success) {
                System.out.println("===> KẾT NỐI THÀNH CÔNG TỚI SUPABASE POSTGRESQL CLOUD!");

                // Insert a test student
                StudentData student = new StudentData("SV999", "Trần Văn Supabase", 9.0, 9.5, 10.0);
                StudentResult result = DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(student);

                System.out.printf("Đã lưu bản ghi mã hóa DES vào Supabase! Mã SV: %s | Họ tên: %s | ĐTB: %.2f\n",
                        result.getStudentId(), result.getFullName(), result.getAverageScore());
                System.out.println("========== THỬ NGHIỆM SUPABASE HOÀN HẢO! ==========");
            }
        } catch (Exception e) {
            System.err.println("!!! Lỗi kết nối Supabase: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DatabaseManager.getInstance().closeConnection();
        }
    }
}
