package com.qlsv;

import com.formdev.flatlaf.FlatLightLaf;
import com.qlsv.client.ClientGUI;
import com.qlsv.server.ServerGUI;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Trung Tâm Điều Hành & Trình Diễn Hệ Thống Phân Tán (PTIT Demo Hub)
 * Quản lý vòng đời tất cả các cửa sổ Server & Client, hỗ trợ chế độ Demo song song đa vai trò
 * không cần tắt ứng dụng hay khởi động lại mã nguồn.
 */
public class AppLauncher {

    public static final Color PTIT_RED = new Color(200, 16, 46);       // #C8102E
    public static final Color PTIT_DARK_RED = new Color(150, 10, 30);  // #960A1E
    public static final Color PTIT_GOLD = new Color(255, 193, 7);
    public static final Color PTIT_LIGHT_BG = new Color(248, 249, 250);

    private static JFrame launcherFrame = null;
    private static ServerGUI serverInstance = null;
    private static ClientGUI teacherClientInstance = null;
    private static ClientGUI studentClientInstance = null;
    private static final List<ClientGUI> activeClients = new ArrayList<>();
    private static JLabel lblStatusInfo;

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(AppLauncher::showLauncher);
    }

    public static synchronized void showLauncher() {
        if (launcherFrame != null && launcherFrame.isDisplayable()) {
            launcherFrame.setState(Frame.NORMAL);
            launcherFrame.setVisible(true);
            launcherFrame.toFront();
            launcherFrame.requestFocus();
            updateStatusUI();
            return;
        }

        launcherFrame = new JFrame("HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG - TRUNG TÂM DEMO");
        launcherFrame.setSize(650, 520);
        launcherFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        launcherFrame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Header Banner PTIT Red
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PTIT_RED);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel lblTitle = new JLabel("HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        lblTitle.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("TRUNG TÂM ĐIỀU HÀNH & TRÌNH DIỄN HỆ THỐNG PHÂN TÁN (DEMO HUB)", SwingConstants.CENTER);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(255, 230, 230));

        JPanel headerBox = new JPanel(new GridLayout(2, 1, 4, 4));
        headerBox.setOpaque(false);
        headerBox.add(lblTitle);
        headerBox.add(lblSub);
        headerPanel.add(headerBox, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Content Body
        JPanel contentPanel = new JPanel(new BorderLayout(12, 12));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        contentPanel.setBackground(Color.WHITE);

        JLabel lblPrompt = new JLabel("Chọn phân hệ muốn mở (Cửa sổ Launcher luôn duy trì sẵn sàng, không cần khởi động lại):", SwingConstants.CENTER);
        lblPrompt.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPrompt.setForeground(new Color(50, 50, 50));
        contentPanel.add(lblPrompt, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new GridLayout(4, 1, 10, 10));
        btnPanel.setOpaque(false);

        // Button 1: DEMO Song Song
        JButton btnParallel = new JButton("▶ 1. KHỞI CHẠY CHẾ ĐỘ DEMO BÁO CÁO (Song Song Server + Giảng Viên + Sinh Viên)");
        btnParallel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnParallel.setBackground(PTIT_RED);
        btnParallel.setForeground(Color.WHITE);
        btnParallel.setFocusPainted(false);
        btnParallel.setToolTipText("Tự động mở và chia đều màn hình 3 cửa sổ: Server, Giảng viên và Sinh viên");
        btnParallel.addActionListener(e -> launchDemoParallelLayout());

        // Button 2: Cổng Sinh Viên
        JButton btnStudent = new JButton("▶ 2. Mở Cổng Thông Tin Sinh Viên (Tra Cứu Học Tập, Bảng Điểm & GPA)");
        btnStudent.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnStudent.setBackground(new Color(0, 123, 255));
        btnStudent.setForeground(Color.WHITE);
        btnStudent.setFocusPainted(false);
        btnStudent.addActionListener(e -> launchStudentClient());

        // Button 3: Phân Hệ Giảng Viên
        JButton btnTeacher = new JButton("▶ 3. Mở Phân Hệ Giảng Viên (Quản Lý Đào Tạo, Nhập Điểm & Excel)");
        btnTeacher.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnTeacher.setBackground(new Color(40, 167, 69));
        btnTeacher.setForeground(Color.WHITE);
        btnTeacher.setFocusPainted(false);
        btnTeacher.addActionListener(e -> launchTeacherClient());

        // Button 4: Máy Chủ Server
        JButton btnServer = new JButton("▶ 4. Mở Trung Tâm Quản Trị Máy Chủ Server (RUDP & Database)");
        btnServer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnServer.setBackground(new Color(52, 58, 64));
        btnServer.setForeground(Color.WHITE);
        btnServer.setFocusPainted(false);
        btnServer.addActionListener(e -> launchOrFocusServer());

        btnPanel.add(btnParallel);
        btnPanel.add(btnStudent);
        btnPanel.add(btnTeacher);
        btnPanel.add(btnServer);

        contentPanel.add(btnPanel, BorderLayout.CENTER);

        // Tiện ích phụ & Trạng thái
        JPanel bottomContainer = new JPanel(new BorderLayout(8, 8));
        bottomContainer.setOpaque(false);

        JPanel utilityBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        utilityBar.setOpaque(false);

        JButton btnTile = new JButton("Sắp Xếp Lại Cửa Sổ (Tile)");
        btnTile.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnTile.addActionListener(e -> tileAllWindows());

        JButton btnCloseAll = new JButton("Đóng Toàn Bộ Cửa Sổ Con");
        btnCloseAll.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnCloseAll.addActionListener(e -> closeAllDemoWindows());

        utilityBar.add(btnTile);
        utilityBar.add(btnCloseAll);

        lblStatusInfo = new JLabel("● Máy chủ: Sẵn sàng  |  CSDL: Supabase Cloud (251 SV)  |  RUDP: Port 9876", SwingConstants.CENTER);
        lblStatusInfo.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblStatusInfo.setForeground(new Color(100, 100, 100));

        bottomContainer.add(utilityBar, BorderLayout.NORTH);
        bottomContainer.add(lblStatusInfo, BorderLayout.SOUTH);
        contentPanel.add(bottomContainer, BorderLayout.SOUTH);

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        launcherFrame.add(mainPanel);
        launcherFrame.setVisible(true);
    }

    /**
     * Khởi chạy chế độ Demo song song: Server (trái) + Giảng viên (phải trên) + Sinh viên (phải dưới)
     */
    public static void launchDemoParallelLayout() {
        SwingUtilities.invokeLater(() -> {
            launchOrFocusServer();

            Timer t1 = new Timer(300, evt -> {
                if (teacherClientInstance == null || !teacherClientInstance.isDisplayable()) {
                    teacherClientInstance = new ClientGUI();
                    activeClients.add(teacherClientInstance);
                }
                teacherClientInstance.switchDemoRole(ClientGUI.UserRole.TEACHER, null);
                teacherClientInstance.setVisible(true);

                Timer t2 = new Timer(200, evt2 -> {
                    if (studentClientInstance == null || !studentClientInstance.isDisplayable()) {
                        studentClientInstance = new ClientGUI();
                        activeClients.add(studentClientInstance);
                    }
                    studentClientInstance.switchDemoRole(ClientGUI.UserRole.STUDENT, "SV001");
                    studentClientInstance.setVisible(true);

                    tileAllWindows();
                    updateStatusUI();
                });
                t2.setRepeats(false);
                t2.start();
            });
            t1.setRepeats(false);
            t1.start();
        });
    }

    public static synchronized void launchOrFocusServer() {
        SwingUtilities.invokeLater(() -> {
            if (serverInstance != null && serverInstance.isDisplayable()) {
                serverInstance.setState(Frame.NORMAL);
                serverInstance.setVisible(true);
                serverInstance.toFront();
                serverInstance.requestFocus();
            } else {
                serverInstance = new ServerGUI();
                serverInstance.setLocation(50, 50);
                serverInstance.setVisible(true);
            }
            updateStatusUI();
        });
    }

    public static synchronized ClientGUI launchStudentClient() {
        if (studentClientInstance == null || !studentClientInstance.isDisplayable()) {
            studentClientInstance = new ClientGUI();
            activeClients.add(studentClientInstance);
        }
        studentClientInstance.switchDemoRole(ClientGUI.UserRole.STUDENT, "SV001");
        studentClientInstance.setState(Frame.NORMAL);
        studentClientInstance.setVisible(true);
        studentClientInstance.toFront();
        updateStatusUI();
        return studentClientInstance;
    }

    public static synchronized ClientGUI launchTeacherClient() {
        if (teacherClientInstance == null || !teacherClientInstance.isDisplayable()) {
            teacherClientInstance = new ClientGUI();
            activeClients.add(teacherClientInstance);
        }
        teacherClientInstance.switchDemoRole(ClientGUI.UserRole.TEACHER, null);
        teacherClientInstance.setState(Frame.NORMAL);
        teacherClientInstance.setVisible(true);
        teacherClientInstance.toFront();
        updateStatusUI();
        return teacherClientInstance;
    }

    public static synchronized ClientGUI launchNewClient(int tabIndex) {
        ClientGUI c = new ClientGUI();
        if (c.getMainTabbedPane() != null && tabIndex >= 0 && tabIndex < c.getMainTabbedPane().getTabCount()) {
            c.getMainTabbedPane().setSelectedIndex(tabIndex);
        }
        activeClients.add(c);
        c.setVisible(true);
        updateStatusUI();
        return c;
    }

    public static void tileAllWindows() {
        SwingUtilities.invokeLater(() -> {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle bounds = ge.getMaximumWindowBounds();
            int sx = bounds.x;
            int sy = bounds.y;
            int sw = bounds.width;
            int sh = bounds.height;

            int leftW = (int) (sw * 0.42);
            int rightW = sw - leftW;
            int halfH = sh / 2;

            if (serverInstance != null && serverInstance.isDisplayable()) {
                serverInstance.setBounds(sx, sy, leftW, sh);
                serverInstance.toFront();
            }

            if (teacherClientInstance != null && teacherClientInstance.isDisplayable()) {
                teacherClientInstance.setBounds(sx + leftW, sy, rightW, halfH);
                teacherClientInstance.toFront();
            }

            if (studentClientInstance != null && studentClientInstance.isDisplayable()) {
                studentClientInstance.setBounds(sx + leftW, sy + halfH, rightW, sh - halfH);
                studentClientInstance.toFront();
            }
        });
    }

    public static synchronized void closeAllDemoWindows() {
        SwingUtilities.invokeLater(() -> {
            if (serverInstance != null && serverInstance.isDisplayable()) {
                serverInstance.dispose();
                serverInstance = null;
            }
            if (teacherClientInstance != null && teacherClientInstance.isDisplayable()) {
                teacherClientInstance.dispose();
                teacherClientInstance = null;
            }
            if (studentClientInstance != null && studentClientInstance.isDisplayable()) {
                studentClientInstance.dispose();
                studentClientInstance = null;
            }
            for (ClientGUI c : activeClients) {
                if (c != null && c.isDisplayable()) {
                    c.dispose();
                }
            }
            activeClients.clear();
            updateStatusUI();
            JOptionPane.showMessageDialog(launcherFrame,
                    "Đã đóng toàn bộ cửa sổ demo. Bạn có thể chọn mở lại bất cứ lúc nào!",
                    "Dọn Dẹp Cửa Sổ", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private static void updateStatusUI() {
        if (lblStatusInfo != null) {
            int openCount = (serverInstance != null && serverInstance.isDisplayable() ? 1 : 0)
                    + (teacherClientInstance != null && teacherClientInstance.isDisplayable() ? 1 : 0)
                    + (studentClientInstance != null && studentClientInstance.isDisplayable() ? 1 : 0);
            lblStatusInfo.setText(String.format("● Máy chủ RUDP: Port 9876  |  CSDL: Supabase Cloud  |  Đang mở: %d cửa sổ", openCount));
        }
    }
}
