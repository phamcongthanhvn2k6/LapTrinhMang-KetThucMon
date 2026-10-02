package com.qlsv;

import com.qlsv.security.DESEncryption;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;

public class Generate80PlusStudents {
    public static void main(String[] args) {
        String key = "QLSV_KEY";

        String[] ho = {"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngo", "Dương", "Lý"};
        String[] demNam = {"Văn", "Minh", "Quốc", "Thành", "Đức", "Hoàng", "Anh", "Hữu", "Đình", "Tuấn", "Thanh", "Công"};
        String[] demNu = {"Thị", "Ngọc", "Thu", "Phương", "Thảo", "Hồng", "Khánh", "Minh", "Thanh", "Bích", "Tú"};
        String[] tenNam = {"An", "Bình", "Cường", "Dũng", "Em", "Hải", "Hùng", "Huy", "Khang", "Khánh", "Lâm", "Long", "Nam", "Nghĩa", "Phúc", "Quân", "Quang", "Sơn", "Tâm", "Thắng", "Thịnh", "Tiến", "Tú", "Tuấn", "Vinh", "Vũ"};
        String[] tenNu = {"Anh", "Bích", "Châu", "Dung", "Giang", "Hà", "Hằng", "Hoa", "Hương", "Linh", "Mai", "Nga", "Ngân", "Nhi", "Oanh", "Phương", "Quỳnh", "Thảo", "Trang", "Trinh", "Vân", "Vy", "Yến"};

        String[] dschLop = {
            "D21CQCN01-N", "D21CQCN02-N", 
            "D21CQAT01-N", "D21CQAT02-N", 
            "D21CQVT01-N", "D21CQKD01-N"
        };

        Random rand = new Random(12345); // Fixed seed for reproducible generation

        StringBuilder sql = new StringBuilder();

        sql.append("-- ==============================================================================\n")
           .append("-- CHƯƠNG TRÌNH QUẢN LÝ SINH VIÊN CHUYÊN SÂU (ADVANCED STUDENT MANAGEMENT SYSTEM)\n")
           .append("-- DỮ LIỆU MẪU > 250 SINH VIÊN & > 1,000 BẢNG ĐIỂM | MÃ HÓA DES | SUPABASE POSTGRESQL\n")
           .append("-- ==============================================================================\n\n");

        // Drop old tables first to upgrade schema cleanly
        sql.append("-- 0. XÓA CẤU TRÚC BẢNG CŨ (NẾU CÓ) ĐỂ TẠO MỚI HOÀN TOÀN CẤU TRÚC CHUYÊN SÂU\n")
           .append("DROP VIEW IF EXISTS \"v_ChiTietSinhVien\" CASCADE;\n")
           .append("DROP TABLE IF EXISTS \"BangDiem\" CASCADE;\n")
           .append("DROP TABLE IF EXISTS \"SinhVien\" CASCADE;\n")
           .append("DROP TABLE IF EXISTS \"MonHoc\" CASCADE;\n")
           .append("DROP TABLE IF EXISTS \"LopHoc\" CASCADE;\n")
           .append("DROP TABLE IF EXISTS \"Khoa\" CASCADE;\n\n");

        // Table creation statements
        sql.append("-- 1. TẠO BẢNG CSDL MỚI\n")
           .append("CREATE TABLE IF NOT EXISTS \"Khoa\" (\n")
           .append("    \"MaKhoa\" VARCHAR(20) PRIMARY KEY,\n")
           .append("    \"TenKhoa\" VARCHAR(100) NOT NULL,\n")
           .append("    \"NamThanhlap\" INT DEFAULT 2000\n")
           .append(");\n\n")
           .append("CREATE TABLE IF NOT EXISTS \"LopHoc\" (\n")
           .append("    \"MaLop\" VARCHAR(30) PRIMARY KEY,\n")
           .append("    \"TenLop\" VARCHAR(100) NOT NULL,\n")
           .append("    \"MaKhoa\" VARCHAR(20) REFERENCES \"Khoa\"(\"MaKhoa\") ON DELETE CASCADE,\n")
           .append("    \"NienKhoa\" VARCHAR(20) NOT NULL\n")
           .append(");\n\n")
           .append("CREATE TABLE IF NOT EXISTS \"SinhVien\" (\n")
           .append("    \"MaSV\" VARCHAR(50) PRIMARY KEY,\n")
           .append("    \"HoTenEncrypted\" VARCHAR(500) NOT NULL,\n")
           .append("    \"NgaySinh\" DATE,\n")
           .append("    \"GioiTinh\" VARCHAR(10),\n")
           .append("    \"MaLop\" VARCHAR(30) REFERENCES \"LopHoc\"(\"MaLop\") ON DELETE SET NULL,\n")
           .append("    \"Email\" VARCHAR(100),\n")
           .append("    \"SoDienThoaiEncrypted\" VARCHAR(200),\n")
           .append("    \"DiemTB\" DOUBLE PRECISION DEFAULT 0.0,\n")
           .append("    \"XepLoai\" VARCHAR(20) DEFAULT 'Chưa xếp loại',\n")
           .append("    \"TrangThai\" VARCHAR(30) DEFAULT 'Đang học',\n")
           .append("    \"CreatedAt\" TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n")
           .append(");\n\n")
           .append("CREATE TABLE IF NOT EXISTS \"MonHoc\" (\n")
           .append("    \"MaMon\" VARCHAR(20) PRIMARY KEY,\n")
           .append("    \"TenMon\" VARCHAR(150) NOT NULL,\n")
           .append("    \"SoTinChi\" INT DEFAULT 3\n")
           .append(");\n\n")
           .append("CREATE TABLE IF NOT EXISTS \"BangDiem\" (\n")
           .append("    \"Id\" SERIAL PRIMARY KEY,\n")
           .append("    \"MaSV\" VARCHAR(50) REFERENCES \"SinhVien\"(\"MaSV\") ON DELETE CASCADE,\n")
           .append("    \"MaMon\" VARCHAR(20) REFERENCES \"MonHoc\"(\"MaMon\") ON DELETE CASCADE,\n")
           .append("    \"HocKy\" VARCHAR(20) DEFAULT 'HK1 2024-2025',\n")
           .append("    \"DiemQuaTrinhEncrypted\" VARCHAR(200) NOT NULL,\n")
           .append("    \"DiemThiEncrypted\" VARCHAR(200) NOT NULL,\n")
           .append("    \"DiemTongKet\" DOUBLE PRECISION,\n")
           .append("    \"DiemChu\" VARCHAR(5),\n")
           .append("    \"CreatedAt\" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n")
           .append("    UNIQUE (\"MaSV\", \"MaMon\", \"HocKy\")\n")
           .append(");\n\n");

        // Insert Khoa, Lop, MonHoc
        sql.append("-- 2. NẠP DANH MỤC KHOA, LỚP, MÔN HỌC\n")
           .append("INSERT INTO \"Khoa\" (\"MaKhoa\", \"TenKhoa\", \"NamThanhlap\") VALUES\n")
           .append("('CNTT', 'Khoa Công Nghệ Thông Tin', 1997),\n")
           .append("('ATTT', 'Khoa An toàn Thông tin', 2013),\n")
           .append("('DTVT', 'Khoa Điện tử Viễn thông', 1997),\n")
           .append("('KTKD', 'Khoa Quản trị Kinh doanh', 2005)\n")
           .append("ON CONFLICT (\"MaKhoa\") DO NOTHING;\n\n")
           .append("INSERT INTO \"LopHoc\" (\"MaLop\", \"TenLop\", \"MaKhoa\", \"NienKhoa\") VALUES\n")
           .append("('D21CQCN01-N', 'Lớp Công nghệ thông tin 01', 'CNTT', '2021-2026'),\n")
           .append("('D21CQCN02-N', 'Lớp Công nghệ thông tin 02', 'CNTT', '2021-2026'),\n")
           .append("('D21CQAT01-N', 'Lớp An toàn thông tin 01', 'ATTT', '2021-2026'),\n")
           .append("('D21CQAT02-N', 'Lớp An toàn thông tin 02', 'ATTT', '2021-2026'),\n")
           .append("('D21CQVT01-N', 'Lớp Điện tử Viễn thông 01', 'DTVT', '2021-2026'),\n")
           .append("('D21CQKD01-N', 'Lớp Quản trị kinh doanh 01', 'KTKD', '2021-2026')\n")
           .append("ON CONFLICT (\"MaLop\") DO NOTHING;\n\n")
           .append("INSERT INTO \"MonHoc\" (\"MaMon\", \"TenMon\", \"SoTinChi\") VALUES\n")
           .append("('INT1339', 'Lập Trình Mạng', 3),\n")
           .append("('INT1340', 'Mã Hóa & An Toàn Thông Tin', 3),\n")
           .append("('INT1310', 'Cơ Sở Dữ Liệu', 4),\n")
           .append("('INT1306', 'Cấu Trúc Dữ Liệu & Giải Thuật', 3),\n")
           .append("('INT1401', 'Phát Triển Ứng Dụng Di Động', 3),\n")
           .append("('INT1402', 'Phân Tích & Thiết Kế Hệ Thống', 3),\n")
           .append("('INT1403', 'Hệ Quản Trị CSDL Nâng Cao', 3),\n")
           .append("('SKD1102', 'Kỹ Năng Giao Tiếp', 2)\n")
           .append("ON CONFLICT (\"MaMon\") DO NOTHING;\n\n");

        // Insert 250 SinhVien
        sql.append("-- 3. NẠP DANH SÁCH 250 SINH VIÊN (MÃ HÓA DES HỌ TÊN & SĐT)\n")
           .append("INSERT INTO \"SinhVien\" (\"MaSV\", \"HoTenEncrypted\", \"NgaySinh\", \"GioiTinh\", \"MaLop\", \"Email\", \"SoDienThoaiEncrypted\", \"DiemTB\", \"XepLoai\", \"TrangThai\") VALUES\n");

        int totalStudents = 250;
        try {
            for (int i = 1; i <= totalStudents; i++) {
                String maSV = String.format("SV%03d", i);

                boolean isNam = rand.nextBoolean();
                String gioiTinh = isNam ? "Nam" : "Nữ";

                String h = ho[rand.nextInt(ho.length)];
                String d = isNam ? demNam[rand.nextInt(demNam.length)] : demNu[rand.nextInt(demNu.length)];
                String t = isNam ? tenNam[rand.nextInt(tenNam.length)] : tenNu[rand.nextInt(tenNu.length)];

                String hoTen = h + " " + d + " " + t;
                String hoTenEnc = DESEncryption.encrypt(hoTen, key);

                int year = 2002 + rand.nextInt(3);
                int month = 1 + rand.nextInt(12);
                int day = 1 + rand.nextInt(28);
                String ngaySinh = String.format("%04d-%02d-%02d", year, month, day);

                String maLop = dschLop[rand.nextInt(dschLop.length)];
                String email = String.format("sv%03d@ptit.edu.vn", i);

                String sdtRaw = String.format("09%08d", rand.nextInt(100000000));
                String sdtEnc = DESEncryption.encrypt(sdtRaw, key);

                double rawScore = 5.0 + (rand.nextDouble() * 5.0);
                double dtb = Math.round(rawScore * 100.0) / 100.0;

                String xepLoai;
                if (dtb >= 9.0) xepLoai = "Xuất sắc";
                else if (dtb >= 8.0) xepLoai = "Giỏi";
                else if (dtb >= 6.5) xepLoai = "Khá";
                else if (dtb >= 5.0) xepLoai = "Trung bình";
                else xepLoai = "Yếu";

                sql.append(String.format("('%s', '%s', '%s', '%s', '%s', '%s', '%s', %.2f, '%s', 'Đang học')",
                        maSV, hoTenEnc, ngaySinh, gioiTinh, maLop, email, sdtEnc, dtb, xepLoai));

                if (i < totalStudents) {
                    sql.append(",\n");
                } else {
                    sql.append("\n");
                }
            }
            sql.append("ON CONFLICT (\"MaSV\") DO UPDATE SET\n")
               .append("    \"HoTenEncrypted\" = EXCLUDED.\"HoTenEncrypted\",\n")
               .append("    \"DiemTB\" = EXCLUDED.\"DiemTB\",\n")
               .append("    \"XepLoai\" = EXCLUDED.\"XepLoai\";\n\n");

            // Insert BangDiem
            sql.append("-- 4. NẠP ĐIỂM THI CHI TIẾT MÔN HỌC DÀNH CHO BẢNG ĐIỂM (MÃ HÓA DES)\n")
               .append("INSERT INTO \"BangDiem\" (\"MaSV\", \"MaMon\", \"HocKy\", \"DiemQuaTrinhEncrypted\", \"DiemThiEncrypted\", \"DiemTongKet\", \"DiemChu\") VALUES\n");

            String[] monHocArr = {
                "INT1339", "INT1340", "INT1310", "INT1306", 
                "INT1401", "INT1402", "INT1403", "SKD1102"
            };
            int countGrade = 0;
            StringBuilder gradeSb = new StringBuilder();

            for (int i = 1; i <= totalStudents; i++) {
                String maSV = String.format("SV%03d", i);

                for (String mon : monHocArr) {
                    double dqt = Math.round((5.0 + rand.nextDouble() * 5.0) * 10.0) / 10.0;
                    double dthi = Math.round((5.0 + rand.nextDouble() * 5.0) * 10.0) / 10.0;
                    double dtong = Math.round((dqt * 0.3 + dthi * 0.7) * 100.0) / 100.0;

                    String dqtEnc = DESEncryption.encrypt(String.valueOf(dqt), key);
                    String dthiEnc = DESEncryption.encrypt(String.valueOf(dthi), key);

                    String diemChu;
                    if (dtong >= 9.0) diemChu = "A+";
                    else if (dtong >= 8.5) diemChu = "A";
                    else if (dtong >= 8.0) diemChu = "B+";
                    else if (dtong >= 7.0) diemChu = "B";
                    else if (dtong >= 6.5) diemChu = "C+";
                    else if (dtong >= 5.5) diemChu = "C";
                    else if (dtong >= 5.0) diemChu = "D+";
                    else if (dtong >= 4.0) diemChu = "D";
                    else diemChu = "F";

                    if (countGrade > 0) {
                        gradeSb.append(",\n");
                    }
                    gradeSb.append(String.format("('%s', '%s', 'HK1 2024-2025', '%s', '%s', %.2f, '%s')",
                            maSV, mon, dqtEnc, dthiEnc, dtong, diemChu));
                    countGrade++;
                }
            }

            sql.append(gradeSb.toString())
               .append("\nON CONFLICT (\"MaSV\", \"MaMon\", \"HocKy\") DO NOTHING;\n\n");

            // View creation
            sql.append("-- 5. TẠO VIEW TRUY VẤN TỔNG HỢP\n")
               .append("CREATE OR REPLACE VIEW \"v_ChiTietSinhVien\" AS\n")
               .append("SELECT \n")
               .append("    s.\"MaSV\", s.\"HoTenEncrypted\", s.\"NgaySinh\", s.\"GioiTinh\",\n")
               .append("    l.\"TenLop\", k.\"TenKhoa\", s.\"Email\", s.\"DiemTB\", s.\"XepLoai\", s.\"TrangThai\"\n")
               .append("FROM \"SinhVien\" s\n")
               .append("LEFT JOIN \"LopHoc\" l ON s.\"MaLop\" = l.\"MaLop\"\n")
               .append("LEFT JOIN \"Khoa\" k ON l.\"MaKhoa\" = k.\"MaKhoa\";\n\n")
               .append("-- TRUY VẤN KIỂM TRA DỮ LIỆU TỔNG CỘNG 250 SINH VIÊN\n")
               .append("SELECT * FROM \"v_ChiTietSinhVien\" ORDER BY \"MaSV\" ASC;\n");

            // Write out to database.sql
            try (PrintWriter out = new PrintWriter(new FileWriter("database.sql"))) {
                out.print(sql.toString());
            }

            System.out.println("===> ĐÃ TẠO THÀNH CÔNG FILE DATABASE.SQL MỚI CÓ DROP TABLE CASCADE!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
