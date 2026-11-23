# CẤU TRÚC DỰ ÁN & KIẾN TRÚC HỆ THỐNG PHÂN TÁN RUDP 2.0
## HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG (PTIT)
**Dự án**: Hệ Thống Quản Lý Đào Tạo Tín Chỉ & Sinh Viên Qua Mạng RUDP 2.0 & Bảo Mật Đa Lớp (DES / AES-256-GCM + HMAC-SHA256 + Supabase Cloud)

---

## 1. CẤU TRÚC THƯ MỤC MÃ NGUỒN CHUẨN KIẾN TRÚC 4 TẦNG (4-TIER CLEAN ARCHITECTURE)

```
LapTrinhMang-KetThucMon/
├── pom.xml                                  # Cấu hình Maven (FlatLaf, JFreeChart, HikariCP, PostgreSQL/H2, Apache POI)
├── run.bat                                  # Script tự động giải phóng cổng 9876 & khởi chạy nhanh trên Windows
├── rebuild_and_run.bat                      # Script biên dịch sạch (clean package) và khởi chạy bản mới nhất
├── database.sql                             # Script SQL khởi tạo CSDL Supabase PostgreSQL & 251 sinh viên mẫu
├── db.properties                            # Cấu hình kết nối Cloud Database Pooler (Supabase PostgreSQL / H2)
├── docs/                                    # Tài liệu đặc tả kiến trúc & hướng dẫn bảo vệ đồ án
│   ├── 01_CAU_TRUC_DU_AN.md                 # Cấu trúc dự án & Giao thức RUDP 2.0
│   ├── 02_SO_DO_ERD.md                      # Sơ đồ thực thể ERD & Bảo mật CSDL
│   ├── 03_CHUC_NANG_HOC_SINH.md             # Phân hệ Cổng Thông Tin Sinh Viên (Student Portal)
│   ├── 04_CHUC_NANG_GIANG_VIEN_USER.md      # Phân hệ Giảng Viên & Quản Lý Đào Tạo Tín Chỉ
│   └── 05_CHUC_NANG_ADMIN.md                # Phân hệ Trung Tâm Máy Chủ RUDP, Thanh Tra Audit & Chaos Lab
└── src/main/java/com/qlsv/
    ├── AppLauncher.java                     # Trung tâm điều hành Demo Hub (Chế độ 3 cửa sổ song song 1-Click)
    ├── model/                               # Tầng Thực thể & Quy chế Học vụ (Domain Models)
    │   ├── StudentData.java                 # Dữ liệu điểm thành phần đầu vào (10% CC, 20% GK, 70% CK)
    │   ├── StudentResult.java               # Động cơ tính điểm tín chỉ, xét Cấm thi, Điểm liệt, Học bổng KKHT
    │   ├── CourseGrade.java                 # Điểm chi tiết từng học phần tín chỉ
    │   ├── StudentTranscript.java           # Hồ sơ bảng điểm tích lũy cá nhân
    │   ├── PageRequest.java & PageResult.java # Phân trang dữ liệu qua mạng UDP (20/50/100 SV/gói)
    │   ├── SqlConfig.java                   # Cấu hình đa hệ quản trị CSDL
    │   └── UserAccount.java                 # Tài khoản người dùng & Phân quyền RBAC
    ├── security/                            # Tầng Bảo mật Đa lớp (Defense-in-Depth Security)
    │   ├── SecurityManager.java             # Dual-Cipher Engine (DES + AES-256-GCM) & Ký số HMAC-SHA256
    │   ├── AESEncryption.java               # Mã hóa xác thực hiện đại AES-256-GCM (12-byte IV, 128-bit Auth Tag)
    │   ├── DESEncryption.java               # Mã hóa đối xứng DES chuẩn đề bài
    │   └── PasswordHasher.java              # Băm mật khẩu một chiều SHA-256 + Salt ngẫu nhiên
    ├── database/                            # Tầng Truy cập Dữ liệu & Connection Pool (Persistence Layer)
    │   ├── ConnectionPool.java              # Quản lý kết nối HikariCP hiệu năng cao tới Supabase Cloud
    │   ├── DatabaseConfigLoader.java        # Trình nạp cấu hình CSDL tự động kèm cơ chế Fallback H2
    │   ├── DatabaseManager.java             # Facade điều phối giao dịch CSDL
    │   └── dao/                             # Các lớp Data Access Object chuyên biệt
    │       ├── StudentDAO.java              # Truy vấn phân trang, mã hóa/giải mã điểm sinh viên
    │       ├── TranscriptDAO.java           # Quản lý bảng điểm môn học chi tiết
    │       ├── SubjectDAO.java              # Danh mục học phần tín chỉ PTIT
    │       ├── AccountDAO.java              # Xác thực tài khoản Giảng viên, Admin, Sinh viên
    │       └── AuditDAO.java                # Lưu vết Thanh tra biến động điểm số bất biến (OldScore -> NewScore)
    ├── network/                             # Tầng Giao thức Mạng Tin cậy RUDP 2.0
    │   ├── UDPPacket.java                   # Gói tin RUDP tích hợp nén GZIP, chữ ký HMAC-SHA256 & Anti-Replay
    │   └── PacketType.java                  # Tập lệnh giao thức RUDP 2.0 (Request/Response & Server-Push)
    ├── server/                              # Phân hệ Máy chủ Trung tâm
    │   ├── UDPServer.java                   # Máy chủ RUDP đa luồng (20 Workers), Chaos Simulator & Push Engine
    │   ├── SessionManager.java              # Quản lý phiên Token, Chống Brute-force (Khóa 60s) & Khóa sổ điểm
    │   └── ServerGUI.java                   # Giao diện Giám sát Mã hóa, Thanh tra Điểm & Phòng Thí Nghiệm Mạng
    └── client/                              # Phân hệ Máy trạm Đào tạo & Sinh viên
        ├── UDPClient.java                   # RUDP Client với ARQ Retransmit, Đo RTT (ms) & Async Push Listener
        └── ClientGUI.java                   # Giao diện Cổng Sinh Viên & Quản Lý Đào Tạo Tín Chỉ
```

---

## 2. TẬP LỆNH GIAO THỨC MẠNG RUDP 2.0 (`PacketType`)

| PacketType | Hướng truyền | Cơ chế Bảo mật & Chức năng chi tiết |
| :--- | :--- | :--- |
| `PING` / `PONG` | Hai chiều | Kiểm tra kết nối (Heartbeat) và đo độ trễ khứ hồi `RTT (ms)`. |
| `LOGIN` / `LOGIN_RESPONSE` | Hai chiều | Xác thực tài khoản kèm cơ chế **Rate-Limiting** (Khóa 60s nếu sai 5 lần). |
| `GET_STUDENTS_PAGE` | Client ➔ Server | Tải danh sách sinh viên phân trang (`LIMIT / OFFSET`) nén GZIP qua UDP. |
| `ADD_STUDENT` / `UPDATE_STUDENT` | Client ➔ Server | Thêm/Sửa điểm học phần (Kiểm tra quyền RBAC, trạng thái Khóa sổ điểm & ghi vết `OldScore -> NewScore`). |
| `DELETE_STUDENT` | Client ➔ Server | Xóa hồ sơ sinh viên (Chỉ mở cho Giảng viên/Admin khi sổ điểm chưa khóa). |
| `GET_TRANSCRIPT` | Client ➔ Server | Truy xuất phiếu báo điểm tín chỉ cá nhân cho Cổng Thông Tin Sinh Viên. |
| `IMPORT_EXCEL` | Client ➔ Server | Nhập điểm hàng loạt từ tệp Excel `.xlsx` theo giao dịch Batch SQL. |
| `PUSH_SUBSCRIBE` | Client ➔ Server | Đăng ký cổng lắng nghe bất đồng bộ nhận thông báo đẩy từ Server. |
| `PUSH_EVENT` | Server ➔ Client | **Full-Duplex Server-Push**: Tự động đẩy điểm mới & thông báo xuống mọi Client đang mở theo thời gian thực. |
| `TOGGLE_GRADE_LOCK` | Client ➔ Server | Phòng Giáo vụ / Admin khóa hoặc mở khóa sổ điểm học kỳ. |
| `BROADCAST_ANNOUNCEMENT` | Client ➔ Server | Phát thông báo đào tạo toàn trường qua kênh UDP Push. |
| `GET_AUDIT_LOGS` | Client ➔ Server | Truy xuất nhật ký thanh tra an ninh và lịch sử sửa điểm. |
