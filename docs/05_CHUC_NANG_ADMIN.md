# PHÂN HỆ TRUNG TÂM MÁY CHỦ RUDP, THANH TRA AUDIT & PHÒNG THÍ NGHIỆM MẠNG
## HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG (PTIT)
**Dự án**: Hệ Thống Quản Lý Đào Tạo Tín Chỉ & Sinh Viên Qua Mạng RUDP 2.0

---

## 1. TRUNG TÂM QUẢN TRỊ MÁY CHỦ ĐÀO TẠO (`ServerGUI`)

Giao diện Máy chủ trung tâm gồm **4 Tab chuyên sâu** giúp giám sát toàn diện an ninh dữ liệu, luồng gói tin RUDP và quản trị học vụ:

![Giao Diện Máy Chủ Server](../screenshots/06_server_data_inspection.png)

### 1.1. Tab 1 — Kiểm Tra Mã Hóa CSDL (`DES` / `AES-256-GCM`)
- Hiển thị trực tiếp các trường bản mã (`Ciphertext Base64`) lưu trong bảng `SinhVien` trên Supabase PostgreSQL (`Họ Tên Mã Hóa`, `Chuyên Cần Enc`, `Giữa Kỳ Enc`, `Thi Kết Thúc Enc`) đối chiếu với cột Họ tên đã giải mã.
- Hỗ trợ chuyển đổi trực tiếp giữa thuật toán **DES (56-bit chuẩn đề bài)** và **AES-256-GCM (chuẩn mã hóa xác thực hiện đại có 12-byte IV & 128-bit Auth Tag)** mà vẫn tự động giải mã tương thích ngược 100% với dữ liệu cũ.

### 1.2. Tab 2 — Thanh Tra Điểm & Giả Lập Mạng RUDP (`Audit & Chaos Lab`)
- **Bộ Giả Lập Mạng Chập Chờn (`Network Chaos Simulator`):**
  - Cho phép điều chỉnh trực tiếp **Tỷ lệ rớt gói UDP (`0% - 20% - 40% - 60%`)** và **Độ trễ mạng (`0ms - 120ms - 300ms`)**.
  - Khi bật rớt gói giả lập, Server cố ý làm rớt lượt truyền đầu tiên của gói tin để hội đồng chấm thi quan sát trực tiếp cơ chế **RUDP ARQ Timeout & Retransmission** tự động truyền lại và khôi phục dữ liệu thành công 100%.
- **Quản Trị Học Vụ Phòng Giáo Vụ:**
  - Nút **`Khóa Sổ Điểm Học Kỳ`**: Khi khóa sổ điểm, toàn bộ thao tác sửa/xóa điểm của Giảng viên bị chặn ngay tại Server và thông báo khóa sổ được đẩy tức thời xuống mọi Client.
  - Nút **`Phát Thông Báo Toàn Trường (UDP Push)`**: Đẩy thông báo trực tiếp xuống tất cả các cửa sổ Cổng Sinh Viên đang trực tuyến.
- **Nhật Ký Thanh Tra Biến Động Điểm Số Bất Biến (`LichSuAudit`):**
  - Ghi nhận chi tiết từng thao tác sửa điểm theo định dạng: `Sửa điểm SV001 (Vũ Khánh Bích): ĐTB 6.90 -> 8.65 [CC: 9.7->9.5, GK: 9.7->8.5, CK: 5.7->8.6]` kèm tài khoản thực hiện, địa chỉ IP và thời gian.

### 1.3. Tab 3 & Tab 4 — Biểu Đồ Thống Kê Học Lực & Nhật Ký Gói Tin RUDP
- Biểu đồ tròn `JFreeChart` thống kê tỷ lệ học lực toàn trường theo thời gian thực.
- Nhật ký gói tin mạng ghi nhận từng gói `[Seq# | PacketType | HMAC:OK]` và hỗ trợ kết xuất ra tệp `.log`.
