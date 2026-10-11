# 🛡️ TÍNH NĂNG PHÂN HỆ QUẢN TRỊ VIÊN / ADMIN (SERVER GUI)
## 🎓 HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG (PTIT)
**Dự án**: Hệ Thống Quản Lý Sinh Viên Truyền Bảng Mạng UDP & Bảo Mật Mã Hóa CSDL SQL

---

## 1. 📌 TỔNG QUAN VÀ QUYỀN HẠN CỦA QUẢN TRỊ VIÊN (ADMIN)

Phân hệ **Quản trị viên / Admin** được vận hành trên ứng dụng Server GUI (`ServerGUI.java`), đóng vai trò điều khiển cổng lắng nghe UDP, cấu hình mã hóa CSDL và phân tích biểu đồ thống kê:

![Server GUI Admin Dashboard](../screenshots/06_server_admin_dashboard.png)

```mermaid
graph TD
    ADM[🛡️ Admin / System Administrator] --> BASIC[Tính Năng Cơ Bản]
    ADM --> ADV[Tính Năng Nâng Cao Bổ Sung]

    BASIC --> B1[Điều khiển UDP Server Port 9876]
    BASIC --> B2[Chuyển đổi mã hóa AES-256 / DES]
    BASIC --> B3[Giám sát CSDL thô Base64 Ciphertext]
    BASIC --> B4[Theo dõi Live UDP Traffic Logs]
    BASIC --> B5[Biểu đồ thống kê JFreeChart]

    ADV --> A1[Phân quyền tài khoản RBAC Admin/Giảng viên/Học sinh]
    ADV --> A2[Sao lưu & Khôi phục CSDL SQL - .sql Backup]
    ADV --> A3[Quản lý & Xoay khóa mã hóa - Key Rotation]
    ADV --> A4[Xuất Nhật ký Traffic UDP ra tệp .log / .csv]
```

---

## 2. 🎯 CÁC CHỨC NĂNG CHI TIẾT DÀNH CHO ADMIN / SERVER GUI

### 2.1. Giám Sát CSDL Mã Hóa (Encrypted Data Inspection)
- Bảng đối chiếu hai chiều giữa **Dữ liệu thô đã mã hóa trong CSDL** (Base64 Ciphertext) và **Dữ liệu sau khi giải mã** (Decrypted Plaintext).
- Chứng minh dữ liệu cá nhân (Họ tên, Điểm thi) được bảo mật tuyệt đối an toàn trong CSDL SQL.

---

### 2.2. Biểu Đồ Thống Kê Học Lực Trực Quan (JFreeChart)
- Biểu đồ hình tròn (Pie Chart) tự động cập nhật phân bổ xếp loại học lực toàn trường:
  - 🥇 **Xuất sắc**: Điểm TB $\ge 9.0$ (Đỏ PTIT `#C8102E`)
  - 🥈 **Giỏi**: $8.0 \le$ Điểm TB $< 9.0$ (Xanh lá `#28A745`)
  - 📙 **Trung bình**: $5.0 \le$ Điểm TB $< 6.5$ (Cam `#FF9900`)

![Biểu Đồ JFreeChart](../screenshots/07_server_jfreechart_stats.png)

---

### 2.3. Toàn Cảnh Kết Nối Client - Server UDP Thời Gian Thực
- Khởi động đồng thời Server GUI và Client GUI qua mạng UDP đa luồng:

![Toàn Cảnh Hệ Thống PTIT](../screenshots/08_ptit_full_system_overview.png)
