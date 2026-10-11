# 🗄️ SƠ ĐỒ ERD & MÔ HÌNH CƠ SỞ DỮ LIỆU SQL (BỔ SUNG MỞ RỘNG)
## 🎓 HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG (PTIT)
**Dự án**: Hệ Thống Quản Lý Sinh Viên Truyền Bảng Mạng UDP & Bảo Mật Mã Hóa CSDL SQL

---

## 1. 📊 SƠ ĐỒ THỰC THỂ MỐI QUAN HỆ ERD MỞ RỘNG (EXTENDED ERD)

Sơ đồ thể hiện toàn bộ các thực thể cốt lõi và **bổ sung thêm thực thể TaiKhoan (Xác thực & Phân quyền) và LichSuAudit (Nhật ký hệ thống)**:

```mermaid
erDiagram
    KHOA ||--o{ LOPHOC : "thuộc"
    LOPHOC ||--o{ SINHVIEN : "chứa"
    SINHVIEN ||--o{ BANGDIEM : "có"
    MONHOC ||--o{ BANGDIEM : "gồm"
    TAIKHOAN ||--o| SINHVIEN : "xác thực"
    TAIKHOAN ||--o{ LICHSUAUDIT : "thực hiện"

    KHOA {
        VARCHAR_20 MaKhoa PK "Mã Khoa (Khóa chính)"
        VARCHAR_100 TenKhoa "Tên Khoa"
        INT NamThanhlap "Năm thành lập"
    }

    LOPHOC {
        VARCHAR_30 MaLop PK "Mã Lớp (Khóa chính)"
        VARCHAR_100 TenLop "Tên Lớp"
        VARCHAR_20 MaKhoa FK "Mã Khoa (Khóa ngoại -> Khoa)"
        VARCHAR_20 NienKhoa "Niên khóa (VD: 2021-2026)"
    }

    SINHVIEN {
        VARCHAR_50 MaSV PK "Mã Sinh viên (Khóa chính)"
        VARCHAR_500 HoTenEncrypted "Họ tên (MÃ HÓA AES-256 / DES)"
        DATE NgaySinh "Ngày sinh"
        VARCHAR_10 GioiTinh "Giới tính (Nam/Nữ)"
        VARCHAR_30 MaLop FK "Mã Lớp (Khóa ngoại -> LopHoc)"
        VARCHAR_100 Email "Địa chỉ Email"
        VARCHAR_200 SoDienThoaiEncrypted "Số điện thoại (MÃ HÓA DES/AES)"
        DOUBLE DiemTB "Điểm trung bình tích lũy"
        VARCHAR_20 XepLoai "Xếp loại học lực (Xuất sắc, Giỏi, Khá...)"
        VARCHAR_30 TrangThai "Trạng thái (Đang học, Bảo lưu...)"
        TIMESTAMP CreatedAt "Thời gian tạo bản ghi"
    }

    MONHOC {
        VARCHAR_20 MaMon PK "Mã Môn học (Khóa chính)"
        VARCHAR_150 TenMon "Tên Môn học"
        INT SoTinChi "Số tín chỉ"
    }

    BANGDIEM {
        SERIAL Id PK "ID tự tăng (Khóa chính)"
        VARCHAR_50 MaSV FK "Mã Sinh viên (Khóa ngoại -> SinhVien)"
        VARCHAR_20 MaMon FK "Mã Môn học (Khóa ngoại -> MonHoc)"
        VARCHAR_20 HocKy "Học kỳ (VD: HK1 2024-2025)"
        VARCHAR_200 DiemQuaTrinhEncrypted "Điểm quá trình (MÃ HÓA DES/AES)"
        VARCHAR_200 DiemThiEncrypted "Điểm thi kết thúc môn (MÃ HÓA DES/AES)"
        DOUBLE DiemTongKet "Điểm tổng kết hệ 10"
        VARCHAR_5 DiemChu "Điểm quy đổi chữ (A+, A, B+...)"
        TIMESTAMP CreatedAt "Thời gian ghi nhận"
    }

    TAIKHOAN {
        VARCHAR_50 TenDangNhap PK "Tên đăng nhập / Username"
        VARCHAR_250 MatKhauHash "Mật khẩu (Hashed SHA-256)"
        VARCHAR_20 VaiTro "Vai trò (ADMIN, GIANGVIEN, HOCSINH)"
        VARCHAR_50 MaSV FK "Mã Sinh viên (Nếu vai trò HOCSINH)"
        TIMESTAMP LanDangNhapCuoi "Lần đăng nhập gần nhất"
    }

    LICHSUAUDIT {
        SERIAL Id PK "Mã định danh nhật ký"
        VARCHAR_50 TenDangNhap FK "Tên tài khoản thao tác"
        VARCHAR_50 HanhDong "Hành động (ADD, UPDATE, DELETE, LOGIN)"
        VARCHAR_500 ChiTiet "Chi tiết thao tác"
        VARCHAR_50 DiaChiIP "Địa chỉ IP nguồn"
        TIMESTAMP ThoiGian "Thời điểm thực hiện"
    }
```

---

## 2. 📋 CHI TIẾT CÁC BẢNG CSDL SQL BỔ SUNG

### 2.1. Bảng `TaiKhoan` (Xác thực & Phân quyền Nguời dùng - RBAC) [MỚI]
Bảng lưu trữ thông tin đăng nhập và phân định quyền hạn sử dụng ứng dụng.

| Tên trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `TenDangNhap` | `VARCHAR(50)` | `PRIMARY KEY` | Tên đăng nhập duy nhất (VD: `admin`, `giangvien01`, `SV001`). |
| `MatKhauHash` | `VARCHAR(250)`| `NOT NULL` | Mật khẩu đã băm an toàn bằng thuật toán SHA-256 + Salt. |
| `VaiTro` | `VARCHAR(20)` | `NOT NULL` | Phân quyền vai trò (`ADMIN`, `GIANGVIEN`, `HOCSINH`). |
| `MaSV` | `VARCHAR(50)` | `FOREIGN KEY` | Liên kết tới `SinhVien(MaSV)` nếu tài khoản thuộc vai trò HOCSINH. |
| `LanDangNhapCuoi`|`TIMESTAMP` | `NULLABLE` | Thời gian lần truy cập gần nhất. |

---

### 2.2. Bảng `LichSuAudit` (Nhật ký Kiểm toán & An toàn Thông tin) [MỚI]
Bảng ghi nhận toàn bộ nhật ký thao tác dữ liệu nhạy cảm của người dùng.

| Tên trường | Kiểu dữ liệu | Ràng buộc | Mô tả |
| :--- | :--- | :--- | :--- |
| `Id` | `SERIAL` | `PRIMARY KEY` | Mã định danh tự tăng. |
| `TenDangNhap` | `VARCHAR(50)` | `FOREIGN KEY` | Tài khoản thực hiện hành động. |
| `HanhDong` | `VARCHAR(50)` | `NOT NULL` | Loại thao tác (`LOGIN`, `ADD_STUDENT`, `UPDATE_STUDENT`, `DELETE_STUDENT`, `IMPORT_EXCEL`). |
| `ChiTiet` | `VARCHAR(500)`| `NULLABLE` | Thông tin chi tiết bản ghi bị tác động. |
| `DiaChiIP` | `VARCHAR(50)` | `NULLABLE` | Địa chỉ IP của Client gửi yêu cầu. |
| `ThoiGian` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` | Thời điểm ghi nhận log. |

---

## 3. 🛡️ QUY TRÌNH MÃ HÓA DỮ LIỆU & BẢO MẬT MẬT KHẨU

1. **Mã hóa Dữ liệu Học sinh & Điểm thi**:
   - Sử dụng **AES-256 CBC** hoặc **DES ECB** chuyển đổi sang **Base64 String**.
2. **Băm Mật khẩu Tài khoản (Password Hashing)**:
   - Mật khẩu đăng nhập của Admin/Giảng viên/Học sinh không lưu dưới dạng plaintext mà được băm 1 chiều SHA-256 trước khi lưu vào bảng `TaiKhoan`.
