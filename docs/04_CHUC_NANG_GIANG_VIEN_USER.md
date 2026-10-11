# 👩‍🏫 TÍNH NĂNG & BẢO VỆ PHÂN QUYỀN GIẢNG VIÊN (TEACHER ROLE SECURITY)
## 🎓 HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG (PTIT)
**Dự án**: Hệ Thống Quản Lý Sinh Viên Truyền Bảng Mạng UDP & Bảo Mật Mã Hóa CSDL SQL

---

## 1. 🛡️ CƠ CHẾ BẢO VỆ TÍNH NĂNG CỦA GIẢNG VIÊN / GIÁO VIÊN

Hệ thống thiết lập cơ chế **Phân quyền và Bảo vệ Đăng nhập (Role-Based Authentication & Access Control)** nhằm đảm bảo Học sinh / Sinh viên không thể tự ý sửa đổi hoặc xóa điểm số thi:

```mermaid
flowchart TD
    Start([Khởi chạy Client GUI]) --> StudentMode["🔒 Chế Độ Học Sinh (Mặc định - Chỉ xem)"]
    StudentMode --> ReadOnly["• Form Nhập Điểm: BỊ ẨN / KHÓA\n• Nút Xóa Sinh Viên: VÔ HIỆU HÓA\n• Tab Cấu hình CSDL: BỊ ẨN\n• Chỉ tra cứu & xem điểm"]

    StudentMode -->|Click nút '🔒 Quyền: Học sinh'| AuthDialog["Hiển thị Dialog Đăng nhập Quyền Giảng viên"]
    AuthDialog --> InputPass{"Nhập Mật Khẩu Giảng Viên\n(Mặc định: ptit123 / admin123)"}

    InputPass -->|Mật khẩu Sai| AccessDenied["🚫 Truy cập bị từ chối!\nTiếp tục bảo vệ Chế độ Học sinh"]
    InputPass -->|Mật khẩu Đúng| TeacherMode["🔓 Mở Khóa Quyền Giảng Viên (Toàn quyền)"]

    TeacherMode --> FullAccess["• Mở Form Nhập / Sửa Điểm\n• Kích hoạt Nút Xóa Dòng Chọn\n• Mở Tab Cấu Hình Kết Nối & CSDL\n• Mở tính năng Sửa nhanh Inline JTable"]
```

---

## 2. 🎯 CÁC CHỨC NĂNG ĐƯỢC BẢO VỆ DÀNH RIÊNG CHO GIẢNG VIÊN

### 2.1. Xác Thực Mật Khẩu Bảo Mật Giảng Viên (Password Authentication)
- **Nút bảo vệ Role Badge**: Nút chuyển đổi vai trò `btnRoleAuth` được đặt ở vị trí nổi bật trên thanh Header màu đỏ PTIT.
- **Xác thực mật khẩu**: Khi người dùng muốn bật quyền Giảng viên, hệ thống yêu cầu nhập mật khẩu bảo mật (Mặc định: `ptit123` hoặc `admin123`).

![Xác Thực Mật Khẩu Giảng Viên](../screenshots/02_teacher_auth_dialog.png)

- **Tự động Khóa bảo vệ**: Khi đăng xuất hoặc mở lại ứng dụng, hệ thống mặc định trở về Chế độ Học sinh (Chỉ xem) để đảm bảo an toàn dữ liệu.

---

### 2.2. Nhập / Sửa Hồ Sơ & Điểm Thi Sinh Viên (CRUD Workspace)
- **Form Nhập dữ liệu (`formPanel`)**: Chỉ xuất hiện và cho phép thao tác khi người dùng đã được xác thực Quyền Giảng viên thành công.
- **Badge chuyển sang màu xanh**: `🔓 Quyền: Giảng viên (Toàn quyền)`.
- **Tính năng Sửa nhanh Inline**: Click vào dòng sinh viên trên JTable sẽ tự động đổ thông tin lên Form để chỉnh sửa nhanh.

![Giao Diện Chế Độ Giảng Viên](../screenshots/03_client_teacher_mode.png)

---

### 2.3. Quản Lý Xóa Dữ Liệu Sinh Viên (Delete Protected)
- **Nút Xóa dòng chọn (`btnDeleteSelected`)**: Nút màu đỏ sẫm PTIT kích hoạt cho phép xóa bản ghi sau khi xác nhận.
- **Kiểm soát thao tác**: Khi chưa đăng nhập Giảng viên, nút xóa bị vô hiệu hóa hoàn toàn.

---

### 2.4. Quản Lý Cấu Hình Kết Nối & CSDL (Settings Tab Protected)
- **Khóa Tab Cấu hình**: Tab *"Cấu hình Kết nối & CSDL"* bị ẩn hoàn toàn đối với Học sinh để ngăn chặn việc chỉnh sửa địa chỉ Server, tài khoản CSDL Supabase PostgreSQL hoặc H2 Embedded.
- **Mở khóa dành riêng cho Giảng viên**: Tab Cấu hình chỉ hiển thị lại sau khi Giảng viên xác thực mật khẩu thành công.
