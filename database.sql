-- ==============================================================================
-- ĐỀ TÀI 12: CHƯƠNG TRÌNH QUẢN LÝ SINH VIÊN CLIENT - SERVER UDP & MÃ HÓA DES
-- SCRIPT KHỞI TẠO CƠ SỞ DỮ LIỆU (DATABASE INITIALIZATION SCRIPT)
-- Hỗ trợ: PostgreSQL (Supabase Cloud), MySQL, MS SQL Server, H2, SQLite
-- ==============================================================================

-- 1. POSTGRESQL / SUPABASE CLOUD DATABASE SCRIPT
-- ------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "SinhVien" (
    "MaSV" VARCHAR(50) PRIMARY KEY,
    "HoTenEncrypted" VARCHAR(500) NOT NULL,
    "DiemToanEncrypted" VARCHAR(200) NOT NULL,
    "DiemVanEncrypted" VARCHAR(200) NOT NULL,
    "DiemAnhEncrypted" VARCHAR(200) NOT NULL,
    "DiemTB" DOUBLE PRECISION,
    "CreatedAt" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Xóa dữ liệu cũ nếu khởi tạo lại (tùy chọn)
-- TRUNCATE TABLE "SinhVien";

-- Thêm mẫu bản ghi đã mã hóa DES (với Key mã hóa mặc định: QLSV_KEY)
INSERT INTO "SinhVien" ("MaSV", "HoTenEncrypted", "DiemToanEncrypted", "DiemVanEncrypted", "DiemAnhEncrypted", "DiemTB")
VALUES 
('SV001', 'VK8QDpbp0t83SR+h65hZ0bIU4NydYppK', 'TA2jx3tVzc0=', 'v4nz+g/ryWk=', 'YqZzhy8qbY8=', 8.00),
('SV002', 'VrkToPZHYUqupfO96TYaLw==', 'mx69zGUILKs=', 'NMXwo+cn8nY=', '/OgtQmp2I5g=', 7.50),
('SV003', '2EOQNFpoZVEkU97HKG3/0BW5+tfYxvwu', 'ED3O5eRioUA=', 'TA2jx3tVzc0=', 'KAh0mrIi8c4=', 9.50)
ON CONFLICT ("MaSV") DO UPDATE SET 
    "HoTenEncrypted" = EXCLUDED."HoTenEncrypted",
    "DiemToanEncrypted" = EXCLUDED."DiemToanEncrypted",
    "DiemVanEncrypted" = EXCLUDED."DiemVanEncrypted",
    "DiemAnhEncrypted" = EXCLUDED."DiemAnhEncrypted",
    "DiemTB" = EXCLUDED."DiemTB";


-- 2. MYSQL / MARIADB SCRIPT
-- ------------------------------------------------------------------------------
/*
CREATE TABLE IF NOT EXISTS SinhVien (
    MaSV VARCHAR(50) PRIMARY KEY,
    HoTenEncrypted VARCHAR(500) NOT NULL,
    DiemToanEncrypted VARCHAR(200) NOT NULL,
    DiemVanEncrypted VARCHAR(200) NOT NULL,
    DiemAnhEncrypted VARCHAR(200) NOT NULL,
    DiemTB DOUBLE,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO SinhVien (MaSV, HoTenEncrypted, DiemToanEncrypted, DiemVanEncrypted, DiemAnhEncrypted, DiemTB)
VALUES 
('SV001', 'VK8QDpbp0t83SR+h65hZ0bIU4NydYppK', 'TA2jx3tVzc0=', 'v4nz+g/ryWk=', 'YqZzhy8qbY8=', 8.00),
('SV002', 'VrkToPZHYUqupfO96TYaLw==', 'mx69zGUILKs=', 'NMXwo+cn8nY=', '/OgtQmp2I5g=', 7.50),
('SV003', '2EOQNFpoZVEkU97HKG3/0BW5+tfYxvwu', 'ED3O5eRioUA=', 'TA2jx3tVzc0=', 'KAh0mrIi8c4=', 9.50)
ON DUPLICATE KEY UPDATE 
    HoTenEncrypted=VALUES(HoTenEncrypted), DiemTB=VALUES(DiemTB);
*/


-- 3. MICROSOFT SQL SERVER SCRIPT
-- ------------------------------------------------------------------------------
/*
IF NOT EXISTS (SELECT * FROM sys.objects WHERE object_id = OBJECT_ID(N'[dbo].[SinhVien]') AND type in (N'U'))
CREATE TABLE [dbo].[SinhVien] (
    [MaSV] VARCHAR(50) PRIMARY KEY,
    [HoTenEncrypted] NVARCHAR(500) NOT NULL,
    [DiemToanEncrypted] VARCHAR(200) NOT NULL,
    [DiemVanEncrypted] VARCHAR(200) NOT NULL,
    [DiemAnhEncrypted] VARCHAR(200) NOT NULL,
    [DiemTB] FLOAT,
    [CreatedAt] DATETIME DEFAULT GETDATE()
);
*/

-- ==============================================================================
-- TRUY VẤN KIỂM TRA DỮ LIỆU
-- ==============================================================================
SELECT "MaSV", "HoTenEncrypted", "DiemTB", "CreatedAt" FROM "SinhVien";
