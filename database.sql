-- ==============================================================================
-- CHƯƠNG TRÌNH QUẢN LÝ SINH VIÊN CHUYÊN SÂU (ADVANCED STUDENT MANAGEMENT SYSTEM)
-- MÔ HÌNH CLIENT-SERVER UDP | MÃ HÓA DES | CSDL RELATIONAL SUPABASE POSTGRESQL
-- ==============================================================================

-- 1. BẢNG KHOA (FACULTIES / DEPARTMENTS)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "Khoa" (
    "MaKhoa" VARCHAR(20) PRIMARY KEY,
    "TenKhoa" VARCHAR(100) NOT NULL,
    "NamThanhlap" INT DEFAULT 2000
);

-- 2. BẢNG LỚP HỌC (CLASSES)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "LopHoc" (
    "MaLop" VARCHAR(30) PRIMARY KEY,
    "TenLop" VARCHAR(100) NOT NULL,
    "MaKhoa" VARCHAR(20) REFERENCES "Khoa"("MaKhoa") ON DELETE CASCADE,
    "NienKhoa" VARCHAR(20) NOT NULL
);

-- 3. BẢNG SINH VIÊN (STUDENTS) - MÃ HÓA THÔNG TIN CÁ NHÂN VỚI DES
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "SinhVien" (
    "MaSV" VARCHAR(50) PRIMARY KEY,
    "HoTenEncrypted" VARCHAR(500) NOT NULL,
    "NgaySinh" DATE,
    "GioiTinh" VARCHAR(10),
    "MaLop" VARCHAR(30) REFERENCES "LopHoc"("MaLop") ON DELETE SET NULL,
    "Email" VARCHAR(100),
    "SoDienThoaiEncrypted" VARCHAR(200),
    "DiemTB" DOUBLE PRECISION DEFAULT 0.0,
    "XepLoai" VARCHAR(20) DEFAULT 'Chưa xếp loại',
    "TrangThai" VARCHAR(30) DEFAULT 'Đang học',
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. BẢNG MÔN HỌC (COURSES / SUBJECTS)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "MonHoc" (
    "MaMon" VARCHAR(20) PRIMARY KEY,
    "TenMon" VARCHAR(150) NOT NULL,
    "SoTinChi" INT DEFAULT 3
);

-- 5. BẢNG BẢNG ĐIỂM CHI TIẾT (STUDENT GRADES - ENCRYPTED WITH DES)
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "BangDiem" (
    "Id" SERIAL PRIMARY KEY,
    "MaSV" VARCHAR(50) REFERENCES "SinhVien"("MaSV") ON DELETE CASCADE,
    "MaMon" VARCHAR(20) REFERENCES "MonHoc"("MaMon") ON DELETE CASCADE,
    "HocKy" VARCHAR(20) DEFAULT 'HK1 2024-2025',
    "DiemQuaTrinhEncrypted" VARCHAR(200) NOT NULL,
    "DiemThiEncrypted" VARCHAR(200) NOT NULL,
    "DiemTongKet" DOUBLE PRECISION,
    "DiemChu" VARCHAR(5),
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE ("MaSV", "MaMon", "HocKy")
);

-- ==============================================================================
-- DỮ LIỆU MẪU BAN ĐẦU (INITIAL SAMPLE DATA FOR SUPABASE)
-- ==============================================================================

-- 1. Nạp danh mục Khoa
INSERT INTO "Khoa" ("MaKhoa", "TenKhoa", "NamThanhlap") VALUES
('CNTT', 'Khoa Công Nghệ Thông Tin', 1997),
('ATTT', 'Khoa An toàn Thông tin', 2013),
('DTVT', 'Khoa Điện tử Viễn thông', 1997),
('KTKD', 'Khoa Quản trị Kinh doanh', 2005)
ON CONFLICT ("MaKhoa") DO NOTHING;

-- 2. Nạp danh mục Lớp Học
INSERT INTO "LopHoc" ("MaLop", "TenLop", "MaKhoa", "NienKhoa") VALUES
('D21CQCN01-N', 'Lớp Công nghệ thông tin 01', 'CNTT', '2021-2026'),
('D21CQCN02-N', 'Lớp Công nghệ thông tin 02', 'CNTT', '2021-2026'),
('D21CQAT01-N', 'Lớp An toàn thông tin 01', 'ATTT', '2021-2026'),
('D21CQVT01-N', 'Lớp Điện tử Viễn thông 01', 'DTVT', '2021-2026')
ON CONFLICT ("MaLop") DO NOTHING;

-- 3. Nạp danh mục Môn Học
INSERT INTO "MonHoc" ("MaMon", "TenMon", "SoTinChi") VALUES
('INT1339', 'Lập Trình Mạng', 3),
('INT1340', 'Mã Hóa & An Toàn Thông Tin', 3),
('INT1310', 'Cơ Sở Dữ Liệu', 4),
('INT1306', 'Cấu Trúc Dữ Liệu & Giải Thuật', 3)
ON CONFLICT ("MaMon") DO NOTHING;

-- 4. Nạp Sinh Viên (HoTenEncrypted được mã hóa DES với khóa: QLSV_KEY)
INSERT INTO "SinhVien" ("MaSV", "HoTenEncrypted", "NgaySinh", "GioiTinh", "MaLop", "Email", "DiemTB", "XepLoai", "TrangThai")
VALUES
('SV001', 'VK8QDpbp0t83SR+h65hZ0bIU4NydYppK', '2003-05-15', 'Nam', 'D21CQCN01-N', 'sv001@ptit.edu.vn', 9.00, 'Xuất sắc', 'Đang học'),
('SV002', 'VrkToPZHYUqupfO96TYaLw==', '2003-08-20', 'Nữ', 'D21CQCN01-N', 'sv002@ptit.edu.vn', 8.00, 'Giỏi', 'Đang học'),
('SV003', '2EOQNFpoZVEkU97HKG3/0BW5+tfYxvwu', '2003-12-10', 'Nam', 'D21CQAT01-N', 'sv003@ptit.edu.vn', 9.50, 'Xuất sắc', 'Đang học'),
('SV004', 'Z14YuggNRaQFknud19aXzbIU4NydYppK', '2003-02-28', 'Nam', 'D21CQCN02-N', 'sv004@ptit.edu.vn', 6.50, 'Khá', 'Đang học'),
('SV005', '08Nw1YaUKSeZXRi2ZtktygOt7NlCUtXR', '2003-09-12', 'Nữ', 'D21CQAT01-N', 'sv005@ptit.edu.vn', 8.50, 'Giỏi', 'Đang học')
ON CONFLICT ("MaSV") DO UPDATE SET
    "HoTenEncrypted" = EXCLUDED."HoTenEncrypted",
    "DiemTB" = EXCLUDED."DiemTB",
    "XepLoai" = EXCLUDED."XepLoai";

-- 5. Nạp Bảng Điểm Chi Tiết (DiemQuaTrinhEncrypted & DiemThiEncrypted mã hóa DES)
INSERT INTO "BangDiem" ("MaSV", "MaMon", "HocKy", "DiemQuaTrinhEncrypted", "DiemThiEncrypted", "DiemTongKet", "DiemChu")
VALUES
('SV001', 'INT1339', 'HK1 2024-2025', 'TA2jx3tVzc0=', 'KAh0mrIi8c4=', 9.35, 'A+'),
('SV001', 'INT1340', 'HK1 2024-2025', '/OgtQmp2I5g=', 'TA2jx3tVzc0=', 8.70, 'A'),
('SV002', 'INT1339', 'HK1 2024-2025', 'NMXwo+cn8nY=', '/OgtQmp2I5g=', 8.00, 'B+'),
('SV003', 'INT1339', 'HK1 2024-2025', 'KAh0mrIi8c4=', 'ED3O5eRioUA=', 9.60, 'A+')
ON CONFLICT ("MaSV", "MaMon", "HocKy") DO NOTHING;

-- ==============================================================================
-- TRUY VẤN VIEW TỔNG HỢP CHO HỆ THỐNG QUẢN LÝ
-- ==============================================================================
CREATE OR REPLACE VIEW "v_ChiTietSinhVien" AS
SELECT 
    s."MaSV",
    s."HoTenEncrypted",
    s."NgaySinh",
    s."GioiTinh",
    l."TenLop",
    k."TenKhoa",
    s."Email",
    s."DiemTB",
    s."XepLoai",
    s."TrangThai"
FROM "SinhVien" s
LEFT JOIN "LopHoc" l ON s."MaLop" = l."MaLop"
LEFT JOIN "Khoa" k ON l."MaKhoa" = k."MaKhoa";

-- TRUY VẤN KIỂM TRA VIEW TỔNG HỢP
SELECT * FROM "v_ChiTietSinhVien" ORDER BY "MaSV" ASC;
