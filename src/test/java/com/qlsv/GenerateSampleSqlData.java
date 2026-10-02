package com.qlsv;

import com.qlsv.security.DESEncryption;

public class GenerateSampleSqlData {
    public static void main(String[] args) throws Exception {
        String key = "QLSV_KEY";

        Object[][] students = {
            {"SV001", "Nguyễn Văn An", 9.0, 8.5, 9.5},
            {"SV002", "Lê Thị Bình", 7.5, 8.0, 8.5},
            {"SV003", "Phạm Minh Cường", 9.5, 10.0, 9.0},
            {"SV004", "Hoàng Anh Dũng", 6.0, 7.0, 6.5},
            {"SV005", "Đỗ Thị Giang", 8.5, 9.0, 8.0},
            {"SV006", "Vũ Quốc Hải", 9.0, 9.0, 9.5},
            {"SV007", "Bùi Thanh Hương", 7.0, 7.5, 8.0},
            {"SV008", "Đặng Văn Khánh", 8.0, 8.5, 8.5},
            {"SV009", "Nghiêm Thu Linh", 9.5, 9.0, 10.0},
            {"SV010", "Trần Đức Nam", 6.5, 6.0, 7.0},
            {"SV011", "Ngô Ngọc Oanh", 8.0, 8.0, 8.5},
            {"SV012", "Dương Minh Phúc", 9.0, 8.5, 8.5},
            {"SV013", "Lý Thanh Quang", 7.5, 7.0, 7.5},
            {"SV014", "Phan Thị Sơn", 8.5, 9.5, 9.0},
            {"SV015", "Tạ Hoàng Tú", 10.0, 9.5, 9.5}
        };

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO \"SinhVien\" (\"MaSV\", \"HoTenEncrypted\", \"DiemToanEncrypted\", \"DiemVanEncrypted\", \"DiemAnhEncrypted\", \"DiemTB\")\nVALUES\n");

        for (int i = 0; i < students.length; i++) {
            String id = (String) students[i][0];
            String name = (String) students[i][1];
            double m = (Double) students[i][2];
            double v = (Double) students[i][3];
            double a = (Double) students[i][4];

            double avg = Math.round(((m + v + a) / 3.0) * 100.0) / 100.0;

            String encName = DESEncryption.encrypt(name, key);
            String encM = DESEncryption.encrypt(String.valueOf(m), key);
            String encV = DESEncryption.encrypt(String.valueOf(v), key);
            String encA = DESEncryption.encrypt(String.valueOf(a), key);

            sb.append(String.format("('%s', '%s', '%s', '%s', '%s', %.2f)",
                    id, encName, encM, encV, encA, avg));

            if (i < students.length - 1) {
                sb.append(",\n");
            } else {
                sb.append("\n");
            }
        }

        sb.append("ON CONFLICT (\"MaSV\") DO UPDATE SET\n")
          .append("    \"HoTenEncrypted\" = EXCLUDED.\"HoTenEncrypted\",\n")
          .append("    \"DiemToanEncrypted\" = EXCLUDED.\"DiemToanEncrypted\",\n")
          .append("    \"DiemVanEncrypted\" = EXCLUDED.\"DiemVanEncrypted\",\n")
          .append("    \"DiemAnhEncrypted\" = EXCLUDED.\"DiemAnhEncrypted\",\n")
          .append("    \"DiemTB\" = EXCLUDED.\"DiemTB\";\n");

        System.out.println(sb.toString());
    }
}
