# Đề Tài 12: Chương Trình Quản Lý Sinh Viên Client - Server bằng Java với Giao Thức UDP

Hệ thống quản lý sinh viên hoàn chỉnh viết bằng ngôn ngữ Java, truyền nhận dữ liệu thông qua giao thức UDP (`DatagramSocket`), tích hợp mã hóa bảo mật **DES**, lưu trữ cơ sở dữ liệu **SQL (SQL Server / MySQL / Embedded DB)** và tự động tính toán điểm trung bình trả về cho Client.

---

## 🌟 Tính Năng Nổi Bật

### 1. Phía Client
- **Bước 1: Kết nối Server UDP**:
  - Nhập IP Address & Port của Server.
  - Tự động thực hiện gói tin UDP Ping/Pong Handshake. Nếu không thành công, hệ thống sẽ thông báo lỗi trực quan và yêu cầu người dùng nhập lại.
- **Bước 2: Cấu hình CSDL SQL gửi lên Server**:
  - Giao diện nhập thông số SQL Server (Host, Port, Database Name, Username, Password).
  - Đóng gói gửi UDP packet lên Server để Server khởi tạo kết nối CSDL và tạo bảng `SinhVien`.
- **Bước 3: Nhập dữ liệu Sinh viên & Hiển thị kết quả**:
  - Giao diện cho phép nhập từng dòng dữ liệu: **Họ tên sinh viên, Mã sinh viên, Điểm Toán, Điểm Văn, Điểm Tiếng Anh**.
  - Gửi dữ liệu qua UDP lên Server.
  - Hiển thị bảng kết quả trả về từ Server gồm: **Họ tên sinh viên, Mã sinh viên, Điểm trung bình** (tính toán bởi Server).

### 2. Phía Server
- **Kết nối CSDL linh hoạt**: Nhận thông số SQL từ Client và thực hiện kết nối JDBC (Hỗ trợ **SQL Server**, **MySQL**, **H2 / SQLite Embedded**).
- **Mã hóa DES**: Khi nhận dữ liệu sinh viên từ Client, Server sử dụng thuật toán mã hóa **DES** (`DES/ECB/PKCS5Padding` với key 64-bit) để mã hóa dữ liệu trước khi lưu vào SQL.
- **Giải mã & Tính Điểm Trung Bình**:
  - Truy vấn đọc bản ghi từ CSDL SQL.
  - Giải mã DES lấy dữ liệu gốc.
  - Tính điểm trung bình theo công thức: $\text{Điểm TB} = \frac{\text{Điểm Toán} + \text{Điểm Văn} + \text{Điểm Tiếng Anh}}{3.0}$
  - Đóng gói dữ liệu kết quả gửi ngược về cho Client qua giao thức UDP.
- **Bảng Inspection CSDL**: Cho phép xem trực quan dữ liệu đã mã hóa DES Base64 trong SQL và đối chiếu dữ liệu đã giải mã.
- **Nhật ký live UDP**: Theo dõi toàn bộ luồng gói tin UDP theo thời gian thực.

---

## 📁 Cấu Trúc Thư Mục Dự Án

```
LapTrinhMang-KetThucMon/
├── pom.xml
├── run.bat
├── README.md
└── src/
    ├── main/
    │   └── java/
    │       └── com/
    │           └── qlsv/
    │               ├── AppLauncher.java        # Main App Launcher (Chạy Server & Client GUI)
    │               ├── model/
    │               │   ├── StudentData.java    # Model sinh viên đầu vào
    │               │   ├── StudentResult.java  # Model kết quả (Họ tên, Mã SV, DTB)
    │               │   └── SqlConfig.java      # Cấu hình CSDL SQL
    │               ├── security/
    │               │   └── DESEncryption.java  # Thuật toán mã hóa & giải mã DES
    │               ├── database/
    │               │   └── DatabaseManager.java# Quản lý CSDL SQL & Mã hóa DES
    │               ├── network/
    │               │   ├── UDPPacket.java      # Gói tin truyền nhận UDP
    │               │   └── PacketType.java     # Enum phân loại gói tin UDP
    │               ├── server/
    │               │   ├── UDPServer.java      # Lắng nghe & Xử lý gói tin UDP
    │               │   └── ServerGUI.java      # Giao diện Server Dashboard
    │               └── client/
    │                   ├── UDPClient.java      # Engine UDP Client với Timeout
    │                   └── ClientGUI.java      # Giao diện Client 3 bước
    └── test/
        └── java/
            └── com/
                └── qlsv/
                    ├── security/DESEncryptionTest.java
                    └── network/FullSystemIntegrationTest.java
```

---

## 🚀 Hướng Dẫn Khởi Chạy

### Cách 1: Sử dụng File Chạy Nhanh (`run.bat`)
Nhấp kép vào file `run.bat` trong thư mục gốc dự án. Hệ thống sẽ bật giao diện Launcher cho phép chọn:
1. **Khởi chạy CẢ HAI**: Bật cả Server GUI và Client GUI trên cùng một màn hình.
2. **Chỉ chạy Server GUI**: Dành cho máy đóng vai trò Server.
3. **Chỉ chạy Client GUI**: Dành cho máy đóng vai trò Client.

### Cách 2: Sử dụng Dòng Lệnh (Maven)
Chạy bằng lệnh Maven:
```bash
# Khởi chạy Giao diện Launcher chung
mvn exec:java -Dexec.mainClass="com.qlsv.AppLauncher"

# Hoặc khởi chạy riêng Server GUI
mvn exec:java -Dexec.mainClass="com.qlsv.server.ServerGUI"

# Hoặc khởi chạy riêng Client GUI
mvn exec:java -Dexec.mainClass="com.qlsv.client.ClientGUI"
```

---

## 🧪 Kết Quả Kiểm Thử (Unit & Integration Tests)

Hệ thống đã trải qua kiểm thử đơn vị và kiểm thử tích hợp 100% tự động:
- `DESEncryptionTest`: Kiểm tra mã hóa chuỗi tiếng Việt có dấu qua DES, giải mã khớp 100%, kiểm tra công thức tính điểm trung bình chính xác.
- `FullSystemIntegrationTest`: Giả lập quy trình đầy đủ: **UDP Handshake Ping/Pong -> Truyền cấu hình SQL -> Gửi 3 sinh viên -> Mã hóa DES -> Lưu CSDL SQL -> Đọc CSDL SQL -> Giải mã DES -> Tính Điểm Trung Bình -> Phản hồi Client**.
