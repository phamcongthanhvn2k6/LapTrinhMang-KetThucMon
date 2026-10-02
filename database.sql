-- ==============================================================================
-- ĐỀ TÀI 12: CHƯƠNG TRÌNH QUẢN LÝ SINH VIÊN CLIENT - SERVER UDP & MÃ HÓA DES
-- SCRIPT KHỞI TẠO CƠ SỞ DỮ LIỆU (DATABASE INITIALIZATION SCRIPT)
-- Hỗ trợ: PostgreSQL (Supabase Cloud), MySQL, MS SQL Server, H2, SQLite
-- ==============================================================================

-- 1. POSTGRESQL / SUPABASE CLOUD DATABASE SCRIPT (15 SINH VIÊN MẪU)
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

-- Thêm 15 bản ghi sinh viên đã mã hóa DES (Key mã hóa mặc định: QLSV_KEY)
INSERT INTO "SinhVien" ("MaSV", "HoTenEncrypted", "DiemToanEncrypted", "DiemVanEncrypted", "DiemAnhEncrypted", "DiemTB")
VALUES
('SV001', 'VK8QDpbp0t83SR+h65hZ0bIU4NydYppK', 'TA2jx3tVzc0=', '/OgtQmp2I5g=', 'KAh0mrIi8c4=', 9.00),
('SV002', 'VrkToPZHYUqupfO96TYaLw==', 'NMXwo+cn8nY=', 'v4nz+g/ryWk=', '/OgtQmp2I5g=', 8.00),
('SV003', '2EOQNFpoZVEkU97HKG3/0BW5+tfYxvwu', 'KAh0mrIi8c4=', 'ED3O5eRioUA=', 'TA2jx3tVzc0=', 9.50),
('SV004', 'Z14YuggNRaQFknud19aXzbIU4NydYppK', 't7Yh4V0fQ5U=', 'YqZzhy8qbY8=', 'mx69zGUILKs=', 6.50),
('SV005', '08Nw1YaUKSeZXRi2ZtktygOt7NlCUtXR', '/OgtQmp2I5g=', 'TA2jx3tVzc0=', 'v4nz+g/ryWk=', 8.50),
('SV006', '56P5+wEvJx61uwNszqtKdrIU4NydYppK', 'TA2jx3tVzc0=', 'TA2jx3tVzc0=', 'KAh0mrIi8c4=', 9.17),
('SV007', 'jdswGPUDqSu9fGRbhXlNvYGizivoSUVy', 'YqZzhy8qbY8=', 'NMXwo+cn8nY=', 'v4nz+g/ryWk=', 7.50),
('SV008', 'G9lBLvwP2VVkjK/e0I+XylpNfdp0Xhr9', 'v4nz+g/ryWk=', '/OgtQmp2I5g=', '/OgtQmp2I5g=', 8.33),
('SV009', 'ynNnTnGqwAr7uKSkwi5vpbIU4NydYppK', 'KAh0mrIi8c4=', 'TA2jx3tVzc0=', 'ED3O5eRioUA=', 9.50),
('SV010', 'Y2gxE6SNtDUJ9lWk2GEK5ATdRvS935ig', 'mx69zGUILKs=', 't7Yh4V0fQ5U=', 'YqZzhy8qbY8=', 6.50),
('SV011', 'EuNnJWSr1gLNXFMhtzNcpLIU4NydYppK', 'v4nz+g/ryWk=', 'v4nz+g/ryWk=', '/OgtQmp2I5g=', 8.17),
('SV012', 'X2+biJFulWx+nrsjsl0TsdgTsGRgrMzB', 'TA2jx3tVzc0=', '/OgtQmp2I5g=', '/OgtQmp2I5g=', 8.67),
('SV013', 'HRsvneniH/+IFv8ttN3spg==', 'NMXwo+cn8nY=', 'YqZzhy8qbY8=', 'NMXwo+cn8nY=', 7.33),
('SV014', 'gFmFxJ5ALlx2KpYGms+8jA==', '/OgtQmp2I5g=', 'KAh0mrIi8c4=', 'TA2jx3tVzc0=', 9.00),
('SV015', 'U+GJWVA1WhBhgYiBtioIIQ==', 'ED3O5eRioUA=', 'KAh0mrIi8c4=', 'KAh0mrIi8c4=', 9.67)
ON CONFLICT ("MaSV") DO UPDATE SET
    "HoTenEncrypted" = EXCLUDED."HoTenEncrypted",
    "DiemToanEncrypted" = EXCLUDED."DiemToanEncrypted",
    "DiemVanEncrypted" = EXCLUDED."DiemVanEncrypted",
    "DiemAnhEncrypted" = EXCLUDED."DiemAnhEncrypted",
    "DiemTB" = EXCLUDED."DiemTB";

-- 2. TRUY VẤN KIỂM TRA TOÀN BỘ DANH SÁCH SINH VIÊN
-- ------------------------------------------------------------------------------
SELECT "MaSV", "HoTenEncrypted", "DiemTB", "CreatedAt" FROM "SinhVien" ORDER BY "MaSV" ASC;
