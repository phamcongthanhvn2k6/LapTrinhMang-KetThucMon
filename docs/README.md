# 📚 TÀI LIỆU DỰ ÁN QUẢN LÝ SINH VIÊN (UDP & BẢO MẬT MÃ HÓA CSDL SQL)
## 🎓 HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG (PTIT)
**Đề Tài 12**: Hệ Thống Quản Lý Sinh Viên Truyền Bảng Mạng UDP & Bảo Mật Mã Hóa CSDL SQL

---

## 📌 MỤC LỤC TÀI LIỆU SYSTEM DOCUMENTATION

Tài liệu thiết kế chi tiết hệ thống và **cơ chế bảo vệ giao diện/logic phân quyền Giảng viên vs Học sinh** được lưu trữ tại thư mục `docs/`:

| Tệp tài liệu | Nội dung chính |
| :--- | :--- |
| 🏗️ [**01_CAU_TRUC_DU_AN.md**](./01_CAU_TRUC_DU_AN.md) | Cấu trúc thư mục nguồn, kiến trúc Client-Server UDP, luồng truyền nhận gói tin, lớp mã hóa AES/DES và các gói tin phân quyền. |
| 🗄️ [**02_SO_DO_ERD.md**](./02_SO_DO_ERD.md) | Sơ đồ ERD mở rộng (Khoa, LopHoc, SinhVien, MonHoc, BangDiem, TaiKhoan Phân Quyền, LichSuAudit), thuộc tính, khóa chính/ngoại và các trường mã hóa. |
| 👨‍🎓 [**03_CHUC_NANG_HOC_SINH.md**](./03_CHUC_NANG_HOC_SINH.md) | Giao diện Chế độ Học sinh an toàn (Chỉ xem/Tra cứu), khóa toàn bộ các nút sửa/xóa/cấu hình CSDL để đảm bảo an toàn điểm số. |
| 👩‍🏫 [**04_CHUC_NANG_GIANG_VIEN_USER.md**](./04_CHUC_NANG_GIANG_VIEN_USER.md) | **Giao diện & Logic bảo vệ phân quyền Giảng viên**: Nút xác thực mật khẩu Role Badge, mở khóa Form nhập điểm, kích hoạt nút Xóa, mở Tab Cấu hình CSDL. |
| 🛡️ [**05_CHUC_NANG_ADMIN.md**](./05_CHUC_NANG_ADMIN.md) | Chi tiết phân hệ Server GUI Admin: Phân quyền người dùng RBAC, Sao lưu & Khôi phục CSDL SQL (.sql backup), Xoay khóa mã hóa (Key Rotation), Xuất Log Traffic UDP và Biểu đồ JFreeChart. |

---

## 🔒 BẢNG MA TRẬN BẢO VỆ PHÂN QUYỀN GIAO DIỆN (ROLE-BASED ACCESS CONTROL)

| Chức năng giao diện / Thao tác | 👨‍🎓 Chế độ Học sinh (Student Mode) | 👩‍🏫 Quyền Giảng viên (Teacher Mode) |
| :--- | :---: | :---: |
| **Xác thực Mật khẩu Đăng nhập** | Không yêu cầu (Truy cập an toàn chỉ xem) | **Bắt buộc nhập mật khẩu (Default: `ptit123`)** |
| **Tra cứu & Tìm kiếm từ khóa** | ✅ ĐƯỢC PHÉP | ✅ ĐƯỢC PHÉP |
| **Lọc danh sách theo Học lực** | ✅ ĐƯỢC PHÉP | ✅ ĐƯỢC PHÉP |
| **Xem thanh Thống kê Min/Max/Avg**| ✅ ĐƯỢC PHÉP | ✅ ĐƯỢC PHÉP |
| **Form Nhập / Cập nhật Điểm thi** | 🚫 **BỊ ẨN HOÀN TOÀN** | ✅ **MỞ KHÓA THAO TÁC** |
| **Double-click dòng JTable để sửa** | 🚫 **BỊ KHÓA** | ✅ **MỞ KHÓA TỰ ĐỘNG ĐỔ FORM** |
| **Nút Xóa dòng Sinh viên chọn** | 🚫 **VÔ HIỆU HÓA (DISABLED)** | ✅ **KÍCH HOẠT THỰC THI** |
| **Tab Cấu hình Kết nối & CSDL** | 🚫 **BỊ ẨN HOÀN TOÀN** | ✅ **MỞ KHÓA TAB CẤU HÌNH** |

---
*Tài liệu được biên soạn tự động và đồng bộ trực tiếp với mã nguồn dự án `LapTrinhMang-KetThucMon`.*
