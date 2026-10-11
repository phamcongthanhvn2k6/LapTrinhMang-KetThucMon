package com.qlsv;

import com.formdev.flatlaf.FlatLightLaf;
import com.qlsv.client.ClientGUI;
import com.qlsv.database.DatabaseManager;
import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.security.SecurityManager;
import com.qlsv.server.ServerGUI;
import com.qlsv.server.UDPServer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CaptureAllScreenshots {

    public static void main(String[] args) {
        System.out.println("========== BẮT ĐẦU CHỤP BỘ ẢNH GIAO DIỆN CHUẨN THỰC TẾ PTIT ==========");

        try {
            FlatLightLaf.setup();
        } catch (Exception ignored) {}

        File screenshotDir = new File("screenshots");
        if (!screenshotDir.exists()) {
            screenshotDir.mkdirs();
        }

        SwingUtilities.invokeLater(() -> {
            try {
                // 1. Khởi tạo Database H2
                SqlConfig h2Config = new SqlConfig(
                        SqlConfig.DbType.H2_EMBEDDED,
                        "localhost", 0, "test_qlsv_db", "sa", ""
                );
                DatabaseManager.getInstance().connect(h2Config);

                DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(
                        new StudentData("SV001", "Nguyễn Văn An", 9.2, 8.8, 9.5));
                DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(
                        new StudentData("SV002", "Lê Thị Bình", 7.5, 8.0, 8.5));
                DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(
                        new StudentData("SV003", "Phạm Minh Cường", 9.6, 9.8, 9.5));
                DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(
                        new StudentData("SV004", "Trần Hoài Nam", 6.0, 6.5, 6.2));
                DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(
                        new StudentData("SV005", "Đỗ Quỳnh Mai", 8.4, 8.6, 8.2));
                DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(
                        new StudentData("SV006", "Hoàng Anh Dũng", 5.2, 5.0, 5.5));

                // 2. Khởi tạo Server GUI
                ServerGUI serverGUI = new ServerGUI();
                serverGUI.setSize(1150, 720);
                serverGUI.setLocationRelativeTo(null);
                serverGUI.setVisible(true);

                // Kích hoạt Server UDP
                UDPServer udpServer = new UDPServer(9876);
                udpServer.setLogListener(serverGUI);
                udpServer.start();

                // Nạp dữ liệu vào bảng Server
                Method refreshServerMethod = ServerGUI.class.getDeclaredMethod("refreshDataInspection");
                refreshServerMethod.setAccessible(true);
                refreshServerMethod.invoke(serverGUI);

                // 3. Khởi tạo Client GUI
                ClientGUI clientGUI = new ClientGUI();
                clientGUI.setSize(1180, 760);
                clientGUI.setLocationRelativeTo(null);
                clientGUI.setVisible(true);

                // Nạp danh sách sinh viên vào ClientGUI
                List<StudentResult> mockStudents = new ArrayList<>();
                mockStudents.add(createResult("SV003", "Phạm Minh Cường", 9.6, 9.8, 9.5));
                mockStudents.add(createResult("SV001", "Nguyễn Văn An", 9.2, 8.8, 9.5));
                mockStudents.add(createResult("SV005", "Đỗ Quỳnh Mai", 8.4, 8.6, 8.2));
                mockStudents.add(createResult("SV002", "Lê Thị Bình", 7.5, 8.0, 8.5));
                mockStudents.add(createResult("SV004", "Trần Hoài Nam", 6.0, 6.5, 6.2));
                mockStudents.add(createResult("SV006", "Hoàng Anh Dũng", 5.2, 5.0, 5.5));

                Field listField = ClientGUI.class.getDeclaredField("currentStudentList");
                listField.setAccessible(true);
                listField.set(clientGUI, mockStudents);

                Method filterMethod = ClientGUI.class.getDeclaredMethod("applyFilters");
                filterMethod.setAccessible(true);
                filterMethod.invoke(clientGUI);

                // Tạo các cửa sổ mẫu đẹp mắt
                JFrame frameAuth = createTeacherAuthFrame();
                frameAuth.setLocation(200, 200);
                frameAuth.setVisible(true);

                JFrame frameTranscript = createTranscriptFrame();
                frameTranscript.setLocation(220, 150);
                frameTranscript.setVisible(true);

                JFrame frameGpa = createGpaSimulatorFrame();
                frameGpa.setLocation(240, 180);
                frameGpa.setVisible(true);

                Timer timer = new Timer(1800, e -> {
                    try {
                        // Nạp dữ liệu sinh viên vào ClientGUI ngay trước khi chụp
                        listField.set(clientGUI, mockStudents);
                        filterMethod.invoke(clientGUI);

                        // 1. Chụp Client GUI Chế độ Học sinh (Safe Mode)
                        saveFrame(clientGUI, new File("screenshots/01_client_student_mode.png"));
                        System.out.println("✅ Đã chụp: screenshots/01_client_student_mode.png");

                        // 2. Chụp Teacher Auth Dialog
                        saveFrame(frameAuth, new File("screenshots/02_teacher_auth_dialog.png"));
                        frameAuth.dispose();
                        System.out.println("✅ Đã chụp: screenshots/02_teacher_auth_dialog.png");

                        // 3. Mở khóa Giảng viên và chụp Client GUI Chế độ Giảng viên
                        Field roleField = ClientGUI.class.getDeclaredField("currentRole");
                        roleField.setAccessible(true);
                        roleField.set(clientGUI, ClientGUI.UserRole.TEACHER);

                        Method updateMethod = ClientGUI.class.getDeclaredMethod("updateRoleUI");
                        updateMethod.setAccessible(true);
                        updateMethod.invoke(clientGUI);

                        listField.set(clientGUI, mockStudents);
                        filterMethod.invoke(clientGUI);

                        saveFrame(clientGUI, new File("screenshots/03_client_teacher_mode.png"));
                        System.out.println("✅ Đã chụp: screenshots/03_client_teacher_mode.png");

                        // 4. Chụp Phiếu Điểm Chi Tiết
                        saveFrame(frameTranscript, new File("screenshots/04_student_transcript_modal.png"));
                        frameTranscript.dispose();
                        System.out.println("✅ Đã chụp: screenshots/04_student_transcript_modal.png");

                        // 5. Chụp GPA Simulator
                        saveFrame(frameGpa, new File("screenshots/05_gpa_simulator_modal.png"));
                        frameGpa.dispose();
                        System.out.println("✅ Đã chụp: screenshots/05_gpa_simulator_modal.png");

                        // 6. Chụp Server GUI Admin Dashboard
                        saveFrame(serverGUI, new File("screenshots/06_server_admin_dashboard.png"));
                        System.out.println("✅ Đã chụp: screenshots/06_server_admin_dashboard.png");

                        // 7. Chụp Biểu đồ Thống kê JFreeChart độc lập độ nét cao
                        saveChartScreenshot(new File("screenshots/07_server_jfreechart_stats.png"));
                        System.out.println("✅ Đã chụp: screenshots/07_server_jfreechart_stats.png");

                        // 8. Chụp Toàn cảnh Hệ thống (System Overview)
                        BufferedImage combined = new BufferedImage(1920, 800, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = combined.createGraphics();
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setColor(new Color(245, 247, 250));
                        g.fillRect(0, 0, 1920, 800);

                        Graphics2D gServer = (Graphics2D) g.create(15, 15, 940, 770);
                        serverGUI.paint(gServer);
                        gServer.dispose();

                        Graphics2D gClient = (Graphics2D) g.create(965, 15, 940, 770);
                        clientGUI.paint(gClient);
                        gClient.dispose();

                        g.dispose();
                        ImageIO.write(combined, "png", new File("screenshots/08_ptit_full_system_overview.png"));
                        System.out.println("✅ Đã chụp: screenshots/08_ptit_full_system_overview.png");

                        System.out.println("🎉 HOÀN TẤT CHỤP TẤT CẢ 8 ẢNH CHUẨN ĐỘ NÉT CAO VÀO THƯ MỤC screenshots/!");

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        System.exit(0);
                    }
                });
                timer.setRepeats(false);
                timer.start();

            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        });
    }

    private static void saveFrame(JFrame frame, File file) throws Exception {
        BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        frame.paint(g2d);
        g2d.dispose();
        ImageIO.write(image, "png", file);
    }

    private static void saveChartScreenshot(File file) throws Exception {
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Xuất sắc (33.3%)", 2);
        dataset.setValue("Giỏi (33.3%)", 2);
        dataset.setValue("Trung bình (33.3%)", 2);

        JFreeChart chart = ChartFactory.createPieChart(
                "THỐNG KÊ PHÂN BỔ HỌC LỰC SINH VIÊN - PTIT ACADEMIC REPORT",
                dataset, true, true, false
        );
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setPaint(ClientGUI.PTIT_RED);
        chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 16));

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(new Color(250, 250, 252));
        plot.setSectionPaint("Xuất sắc (33.3%)", ClientGUI.PTIT_RED);
        plot.setSectionPaint("Giỏi (33.3%)", new Color(40, 167, 69));
        plot.setSectionPaint("Trung bình (33.3%)", new Color(255, 153, 0));

        BufferedImage img = chart.createBufferedImage(850, 520);
        ImageIO.write(img, "png", file);
    }

    private static JFrame createTeacherAuthFrame() {
        JFrame frame = new JFrame("XÁC THỰC QUYỀN GIẢNG VIÊN - PTIT SECURITY");
        frame.setSize(520, 260);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel head = new JPanel(new GridLayout(2, 1));
        head.setBackground(ClientGUI.PTIT_RED);
        head.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JLabel t = new JLabel("HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG");
        t.setFont(new Font("Segoe UI", Font.BOLD, 15));
        t.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Xác Thực Quyền Quản Lý & Chỉnh Sửa Dữ Liệu Giảng Viên");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(new Color(255, 230, 230));
        head.add(t);
        head.add(sub);
        frame.add(head, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 10, 8, 10);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0;
        body.add(new JLabel("Mật khẩu Giảng viên:"), g);
        g.gridx = 1; g.gridy = 0; g.weightx = 1.0;
        JPasswordField pf = new JPasswordField("ptit123", 15);
        pf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        body.add(pf, g);

        JLabel tip = new JLabel("(*) Mật khẩu mặc định: ptit123. Học sinh không được cấp quyền.");
        tip.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        tip.setForeground(Color.GRAY);
        g.gridx = 0; g.gridy = 1; g.gridwidth = 2;
        body.add(tip, g);

        frame.add(body, BorderLayout.CENTER);

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        JButton btnOk = new JButton("Xác Thực & Mở Khóa");
        btnOk.setBackground(ClientGUI.PTIT_RED);
        btnOk.setForeground(Color.WHITE);
        btnOk.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JButton btnCancel = new JButton("Hủy");
        btnCancel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bot.add(btnOk);
        bot.add(btnCancel);
        frame.add(bot, BorderLayout.SOUTH);

        return frame;
    }

    private static JFrame createTranscriptFrame() {
        JFrame frame = new JFrame("PHIẾU BÁO ĐIỂM CHI TIẾT - PHẠM MINH CƯỜNG (SV003)");
        frame.setSize(780, 520);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel headPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        headPanel.setBackground(ClientGUI.PTIT_RED);
        headPanel.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        JLabel title = new JLabel("HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG (PTIT)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("PHIẾU BÁO KẾT QUẢ HỌC TẬP & ĐIỂM THI CHI TIẾT THEO HỌC KỲ");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(255, 230, 230));
        headPanel.add(title);
        headPanel.add(subtitle);
        frame.add(headPanel, BorderLayout.NORTH);

        JPanel bodyPanel = new JPanel(new BorderLayout(8, 8));
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JPanel infoCard = new JPanel(new GridLayout(2, 3, 10, 5));
        infoCard.setBorder(BorderFactory.createTitledBorder("Thông Tin Sinh Viên"));
        infoCard.add(new JLabel("Mã SV: SV003"));
        infoCard.add(new JLabel("Họ và Tên: Phạm Minh Cường"));
        infoCard.add(new JLabel("Lớp: D21CQCN01-N"));
        infoCard.add(new JLabel("Điểm TB Tích Lũy: 9.63"));
        infoCard.add(new JLabel("Xếp Loại: Xuất sắc"));
        infoCard.add(new JLabel("Trạng Thái: Đang học"));
        bodyPanel.add(infoCard, BorderLayout.NORTH);

        String[] subCols = {"STT", "Mã Môn", "Tên Môn Học", "Số TC", "Đ.Quá Trình (30%)", "Đ.Thi (70%)", "Đ.Tổng Kết", "Điểm Chữ"};
        DefaultTableModel subModel = new DefaultTableModel(subCols, 0);

        Object[][] subjects = {
                {1, "INT1339", "Lập Trình Mạng", 3, "9.5", "9.6", "9.57", "A+"},
                {2, "INT1340", "Mã Hóa & An Toàn Thông Tin", 3, "9.8", "9.8", "9.80", "A+"},
                {3, "INT1310", "Cơ Sở Dữ Liệu SQL", 4, "9.2", "9.5", "9.41", "A+"},
                {4, "INT1306", "Cấu Trúc Dữ Liệu & Giải Thuật", 3, "9.0", "9.6", "9.42", "A+"},
                {5, "SKD1102", "Kỹ Năng Giao Tiếp", 2, "9.5", "10.0", "9.85", "A+"}
        };
        for (Object[] rowData : subjects) {
            subModel.addRow(rowData);
        }

        JTable subTable = new JTable(subModel);
        subTable.setRowHeight(28);
        subTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        subTable.getTableHeader().setBackground(new Color(240, 242, 245));
        bodyPanel.add(new JScrollPane(subTable), BorderLayout.CENTER);

        frame.add(bodyPanel, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton btnPrint = new JButton("In / Xuất Báo Cáo Phiếu Điểm");
        btnPrint.setBackground(new Color(40, 167, 69));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        btnBar.add(btnPrint);
        btnBar.add(btnClose);
        frame.add(btnBar, BorderLayout.SOUTH);

        return frame;
    }

    private static JFrame createGpaSimulatorFrame() {
        JFrame simFrame = new JFrame("BỘ GIẢ LẬP ĐIỂM GPA SIMULATOR - PTIT");
        simFrame.setSize(640, 440);
        simFrame.setLayout(new BorderLayout(10, 10));

        JPanel head = new JPanel(new GridLayout(2, 1));
        head.setBackground(ClientGUI.PTIT_RED);
        head.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JLabel lblT = new JLabel("BỘ GIẢ LẬP ĐIỂM GPA & DỰ ĐOÁN HỌC LỰC MỤC TIÊU");
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblT.setForeground(Color.WHITE);
        JLabel lblSub = new JLabel("Tính toán số điểm cần đạt để nâng hạng học lực (Xuất sắc, Giỏi, Khá)");
        lblSub.setForeground(new Color(255, 230, 230));
        head.add(lblT);
        head.add(lblSub);
        simFrame.add(head, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 10, 8, 10);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0;
        body.add(new JLabel("Sinh viên:"), g);
        g.gridx = 1; g.gridy = 0;
        JLabel lblStudent = new JLabel("Lê Thị Bình (ĐTB Hiện tại: 8.00)");
        lblStudent.setFont(new Font("Segoe UI", Font.BOLD, 13));
        body.add(lblStudent, g);

        g.gridx = 0; g.gridy = 1;
        body.add(new JLabel("Mục tiêu xếp loại:"), g);
        g.gridx = 1; g.gridy = 1;
        JComboBox<String> cbTarget = new JComboBox<>(new String[]{"Xuất sắc (>= 9.0)", "Giỏi (>= 8.0)", "Khá (>= 6.5)"});
        cbTarget.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        body.add(cbTarget, g);

        g.gridx = 0; g.gridy = 2;
        body.add(new JLabel("Số tín chỉ môn sắp tới:"), g);
        g.gridx = 1; g.gridy = 2;
        JTextField txtCredits = new JTextField("6", 10);
        txtCredits.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        body.add(txtCredits, g);

        JTextArea txtAdvice = new JTextArea(4, 30);
        txtAdvice.setEditable(false);
        txtAdvice.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtAdvice.setBackground(new Color(245, 248, 255));
        txtAdvice.setBorder(BorderFactory.createTitledBorder("Kết Quả Dự Đoán Lộ Trình Học Tập"));
        txtAdvice.setText("Để nâng hạng học lực lên Xuất sắc (>= 9.0):\n• Bạn cần đạt trung bình tối thiểu: 9.60 điểm ở các môn học kỳ tiếp theo.\n• Lời khuyên: Tập trung cao vào môn Lập Trình Mạng & Mã Hóa CSDL!");

        g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
        body.add(txtAdvice, g);

        simFrame.add(body, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton btnCalc = new JButton("Tính Toán Lộ Trình");
        btnCalc.setBackground(ClientGUI.PTIT_RED);
        btnCalc.setForeground(Color.WHITE);
        btnCalc.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        bottom.add(btnCalc);
        bottom.add(btnClose);
        simFrame.add(bottom, BorderLayout.SOUTH);

        return simFrame;
    }

    private static StudentResult createResult(String id, String name, double math, double lit, double eng) {
        double avg = (math + lit + eng) / 3.0;
        StudentResult r = new StudentResult(id, name, avg);
        r.setScoreMath(math);
        r.setScoreLiterature(lit);
        r.setScoreEnglish(eng);
        return r;
    }
}
