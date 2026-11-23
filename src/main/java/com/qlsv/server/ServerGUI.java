package com.qlsv.server;

import com.formdev.flatlaf.FlatLightLaf;
import com.qlsv.AppLauncher;
import com.qlsv.database.DatabaseManager;
import com.qlsv.database.dao.AuditDAO;
import com.qlsv.security.SecurityManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ServerGUI extends JFrame implements UDPServer.ServerLogListener {

    public static final Color PTIT_RED = new Color(200, 16, 46);       // #C8102E
    public static final Color PTIT_DARK_RED = new Color(140, 10, 30);  // #8C0A1E
    public static final Color PTIT_LIGHT_BG = new Color(245, 247, 250);

    private UDPServer udpServer;
    private int serverPort = 9876;
    private JButton btnStartStop;
    private JButton btnSettings;
    private JLabel lblServerStatus;
    private JLabel lblDbStatus;

    private JTextArea txtLog;
    private JTable tblDataInspection;
    private DefaultTableModel tableModel;
    private DefaultTableModel auditTableModel;
    private JLabel lblChaosTelemetry;
    private JButton btnLockGradebook;
    private JPanel chartPanelContainer;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public ServerGUI() {
        setTitle("PTIT - TRUNG TÂM QUẢN TRỊ MÁY CHỦ ĐÀO TẠO");
        setSize(1160, 720);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            FlatLightLaf.setup();
        } catch (Exception ignored) {}

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setJMenuBar(createServerDemoMenuBar());

        // Header Panel (PTIT Red) - Compact non-overlapping layout
        JPanel headerPanel = new JPanel(new BorderLayout(10, 4));
        headerPanel.setBackground(PTIT_RED);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        JLabel titleLabel = new JLabel("PTIT - TRUNG TÂM QUẢN TRỊ MÁY CHỦ RUDP");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Hệ Thống Quản Trị Cơ Sở Dữ Liệu & Dịch Vụ Đào Tạo Phân Tán");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitleLabel.setForeground(new Color(255, 230, 230));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 1, 1));
        titleBox.setOpaque(false);
        titleBox.add(titleLabel);
        titleBox.add(subtitleLabel);

        // Control Panel - Only core server buttons on header (other tools in MenuBar above)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        controlPanel.setOpaque(false);

        JButton btnDemoParallel = new JButton("Xếp Demo 3 Cửa Sổ");
        btnDemoParallel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnDemoParallel.setBackground(new Color(255, 193, 7));
        btnDemoParallel.setForeground(Color.BLACK);
        btnDemoParallel.setFocusPainted(false);
        btnDemoParallel.addActionListener(e -> AppLauncher.launchDemoParallelLayout());

        btnSettings = new JButton("Cấu Hình");
        btnSettings.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnSettings.setBackground(new Color(240, 240, 240));
        btnSettings.setForeground(Color.DARK_GRAY);
        btnSettings.setFocusPainted(false);
        btnSettings.addActionListener(e -> openAdvancedSettingsDialog());

        btnStartStop = new JButton("KHỞI CHẠY MÁY CHỦ");
        btnStartStop.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnStartStop.setBackground(Color.WHITE);
        btnStartStop.setForeground(PTIT_RED);
        btnStartStop.setFocusPainted(false);
        btnStartStop.addActionListener(e -> toggleServer());

        controlPanel.add(btnDemoParallel);
        controlPanel.add(btnSettings);
        controlPanel.add(btnStartStop);

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(controlPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Main Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab 1: Database Inspection
        JPanel tabData = new JPanel(new BorderLayout(5, 5));
        tabData.setBackground(Color.WHITE);
        tabData.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columnNames = {
                "Mã SV", "Họ Tên (Mã Hóa DES)", "Chuyên Cần (10% Enc)", "Giữa Kỳ (20% Enc)", "Thi Kết Thúc (70% Enc)", "Họ Tên (Giải Mã)", "Điểm HP (10)"
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblDataInspection = new JTable(tableModel);
        tblDataInspection.setRowHeight(26);
        tblDataInspection.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblDataInspection.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblDataInspection.getTableHeader().setBackground(new Color(240, 242, 245));

        JScrollPane scrollTable = new JScrollPane(tblDataInspection);

        JPanel tableActionBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tableActionBar.setBackground(Color.WHITE);

        JButton btnRefreshData = new JButton("Làm Mới Dữ Liệu");
        btnRefreshData.setBackground(PTIT_RED);
        btnRefreshData.setForeground(Color.WHITE);
        btnRefreshData.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRefreshData.addActionListener(e -> refreshDataInspection());
        tableActionBar.add(btnRefreshData);

        lblDbStatus = new JLabel("Trạng thái CSDL: Chưa kết nối");
        lblDbStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        tableActionBar.add(Box.createHorizontalStrut(20));
        tableActionBar.add(lblDbStatus);

        tabData.add(tableActionBar, BorderLayout.NORTH);
        tabData.add(scrollTable, BorderLayout.CENTER);

        // Tab 2: Chart
        JPanel tabChart = new JPanel(new BorderLayout(5, 5));
        tabChart.setBackground(Color.WHITE);
        chartPanelContainer = new JPanel(new BorderLayout());
        chartPanelContainer.setBackground(Color.WHITE);
        tabChart.add(chartPanelContainer, BorderLayout.CENTER);
        updateChartPanel();

        // Tab 3: Logs
        JPanel tabLogs = new JPanel(new BorderLayout(5, 5));
        tabLogs.setBackground(Color.WHITE);
        tabLogs.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtLog.setBackground(new Color(250, 250, 250));
        txtLog.setForeground(new Color(20, 80, 20));

        JScrollPane scrollLog = new JScrollPane(txtLog);

        JPanel logActionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        logActionBar.setBackground(Color.WHITE);

        JButton btnExportLog = new JButton("Xuất File Log (.log)");
        btnExportLog.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnExportLog.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Lưu Nhật Ký Hoạt Động");
            chooser.setSelectedFile(new File("nhat_ky_he_thong.log"));
            if (chooser.showSaveDialog(ServerGUI.this) == JFileChooser.APPROVE_OPTION) {
                try (java.io.PrintWriter pw = new java.io.PrintWriter(chooser.getSelectedFile())) {
                    pw.write(txtLog.getText());
                    JOptionPane.showMessageDialog(ServerGUI.this, "Đã xuất file log thành công tại:\n" + chooser.getSelectedFile().getAbsolutePath(), "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ServerGUI.this, "Lỗi xuất log: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton btnClearLog = new JButton("Xóa Nhật Ký");
        btnClearLog.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClearLog.addActionListener(e -> txtLog.setText(""));

        logActionBar.add(btnExportLog);
        logActionBar.add(btnClearLog);

        tabLogs.add(scrollLog, BorderLayout.CENTER);
        tabLogs.add(logActionBar, BorderLayout.SOUTH);

        tabbedPane.addTab("1. Kiểm Tra Mã Hóa CSDL (DES / AES-256)", tabData);
        tabbedPane.addTab("2. Thanh Tra Điểm & Giả Lập Mạng RUDP", createAuditAndChaosTab());
        tabbedPane.addTab("3. Biểu Đồ Thống Kê Học Lực", tabChart);
        tabbedPane.addTab("4. Nhật Ký Hoạt Động Hệ Thống", tabLogs);

        add(tabbedPane, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        statusBar.setBackground(Color.WHITE);
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));
        lblServerStatus = new JLabel("Trạng thái: Dịch vụ máy chủ đang tạm dừng");
        lblServerStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblServerStatus.setForeground(PTIT_RED);
        statusBar.add(lblServerStatus);

        add(statusBar, BorderLayout.SOUTH);

        // 1. Khởi chạy ngay lập tức cổng RUDP 9876 (< 2ms) để các Client kết nối không bị trễ
        toggleServer();

        // 2. Kết nối CSDL Supabase & tải bảng dữ liệu/biểu đồ bất đồng bộ trên nền (Không gây đơ giao diện)
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                if (!DatabaseManager.getInstance().isConnected()) {
                    try {
                        com.qlsv.model.SqlConfig config = com.qlsv.database.DatabaseConfigLoader.loadConfig();
                        DatabaseManager.getInstance().connect(config);
                    } catch (Exception ignored) {}
                }
                return null;
            }

            @Override
            protected void done() {
                refreshDataInspection();
                updateDbStatusLabel();
            }
        }.execute();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (udpServer != null && udpServer.isRunning()) {
                    udpServer.stop();
                }
            }
        });
    }

    private void openAdvancedSettingsDialog() {
        JDialog d = new JDialog(this, "Thiết Lập Tham Số Nâng Cao", true);
        d.setSize(420, 260);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(10, 10));

        JPanel p = new JPanel(new GridLayout(3, 2, 10, 12));
        p.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));

        JTextField tfPort = new JTextField(String.valueOf(serverPort));
        JComboBox<SecurityManager.EncryptionAlgo> cbAlgo = new JComboBox<>(SecurityManager.EncryptionAlgo.values());
        cbAlgo.setSelectedItem(SecurityManager.getCurrentAlgo());
        JTextField tfKey = new JTextField(DatabaseManager.getInstance().getDesKey());

        p.add(new JLabel("Cổng kết nối nội bộ:"));
        p.add(tfPort);
        p.add(new JLabel("Cơ chế bảo vệ dữ liệu:"));
        p.add(cbAlgo);
        p.add(new JLabel("Khóa bảo mật:"));
        p.add(tfKey);

        d.add(p, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnSave = new JButton("Lưu Thiết Lập");
        btnSave.setBackground(PTIT_RED);
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(e -> {
            try {
                this.serverPort = Integer.parseInt(tfPort.getText().trim());
                SecurityManager.setCurrentAlgo((SecurityManager.EncryptionAlgo) cbAlgo.getSelectedItem());
                DatabaseManager.getInstance().setDesKey(tfKey.getText().trim());
                JOptionPane.showMessageDialog(d, "Đã lưu thiết lập tham số thành công!");
                d.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, "Lỗi nhập tham số: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton btnCancel = new JButton("Hủy");
        btnCancel.addActionListener(e -> d.dispose());

        btnPanel.add(btnSave);
        btnPanel.add(btnCancel);
        d.add(btnPanel, BorderLayout.SOUTH);

        d.setVisible(true);
    }

    private void updateChartPanel() {
        chartPanelContainer.removeAll();
        DefaultPieDataset dataset = new DefaultPieDataset();
        try {
            Map<String, Integer> stats = DatabaseManager.getInstance().getRankStatistics();
            for (Map.Entry<String, Integer> entry : stats.entrySet()) {
                dataset.setValue(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue());
            }
        } catch (Exception ignored) {
            dataset.setValue("Chưa có dữ liệu", 1);
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "THỐNG KÊ PHÂN BỔ HỌC LỰC SINH VIÊN - PTIT ACADEMIC REPORT",
                dataset, true, true, false
        );
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setPaint(PTIT_RED);
        chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 16));

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(new Color(250, 250, 252));
        plot.setSectionPaint("Xuất sắc", PTIT_RED);
        plot.setSectionPaint("Giỏi", new Color(40, 167, 69));
        plot.setSectionPaint("Khá", new Color(0, 122, 255));
        plot.setSectionPaint("Trung bình", new Color(255, 153, 0));
        plot.setSectionPaint("Yếu", new Color(108, 117, 125));

        ChartPanel cp = new ChartPanel(chart);
        chartPanelContainer.add(cp, BorderLayout.CENTER);
        chartPanelContainer.revalidate();
        chartPanelContainer.repaint();
    }

    private void toggleServer() {
        if (udpServer != null && udpServer.isRunning()) {
            udpServer.stop();
            btnStartStop.setText("KHỞI CHẠY DỊCH VỤ MÁY CHỦ");
            btnStartStop.setBackground(Color.WHITE);
            btnStartStop.setForeground(PTIT_RED);
            lblServerStatus.setText("Trạng thái: Dịch vụ máy chủ đang tạm dừng");
            lblServerStatus.setForeground(PTIT_RED);
            btnSettings.setEnabled(true);
        } else {
            try {
                udpServer = new UDPServer(serverPort);
                udpServer.setLogListener(this);
                udpServer.start();

                btnStartStop.setText("TẠM DỪNG DỊCH VỤ MÁY CHỦ");
                btnStartStop.setBackground(PTIT_DARK_RED);
                btnStartStop.setForeground(Color.WHITE);
                lblServerStatus.setText("Trạng thái: Máy chủ đang hoạt động và sẵn sàng phục vụ kết nối");
                lblServerStatus.setForeground(new Color(40, 167, 69));
                btnSettings.setEnabled(false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể khởi động máy chủ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void onLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String time = dateFormat.format(new Date());
            txtLog.append(String.format("[%s] %s\n", time, message));
            txtLog.setCaretPosition(txtLog.getDocument().getLength());
            updateDbStatusLabel();
            updateChaosTelemetryLabel();
        });
    }

    @Override
    public void onDataUpdated() {
        SwingUtilities.invokeLater(this::refreshDataInspection);
    }

    private JPanel createAuditAndChaosTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        // 1. Top Box: Network Chaos Simulator & Gradebook Governance
        JPanel topBox = new JPanel(new GridLayout(2, 1, 6, 6));
        topBox.setBackground(new Color(248, 249, 252));
        topBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(PTIT_RED, 1),
                " PHÒNG THÍ NGHIỆM MẠNG RUDP 2.0 (CHAOS SIMULATOR) & QUẢN TRỊ HỌC VỤ ",
                javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), PTIT_RED
        ));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        row1.setOpaque(false);

        JComboBox<String> cbDropRate = new JComboBox<>(new String[]{
                "0% Rớt Gói (Mạng Lý Tưởng)",
                "20% Rớt Gói (Test ARQ Retransmit)",
                "40% Rớt Gói (Mạng Nghẽn Cao)",
                "60% Rớt Gói (Kiểm Thử Chịu Lỗi Cực Hạn)"
        });
        cbDropRate.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cbDropRate.addActionListener(e -> {
            int idx = cbDropRate.getSelectedIndex();
            int rate = idx == 1 ? 20 : (idx == 2 ? 40 : (idx == 3 ? 60 : 0));
            if (udpServer != null) {
                udpServer.setSimulatedDropRatePercent(rate);
            }
            updateChaosTelemetryLabel();
        });

        JComboBox<String> cbLatency = new JComboBox<>(new String[]{
                "0 ms (Độ trễ LAN)",
                "120 ms (Độ trễ WAN / 4G)",
                "300 ms (Mạng Nghẽn Cao)"
        });
        cbLatency.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbLatency.addActionListener(e -> {
            int idx = cbLatency.getSelectedIndex();
            int lat = idx == 1 ? 120 : (idx == 2 ? 300 : 0);
            if (udpServer != null) {
                udpServer.setSimulatedLatencyMs(lat);
            }
            updateChaosTelemetryLabel();
        });

        JComboBox<SecurityManager.EncryptionAlgo> cbCipher = new JComboBox<>(SecurityManager.EncryptionAlgo.values());
        cbCipher.setSelectedItem(SecurityManager.getCurrentAlgo());
        cbCipher.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cbCipher.addActionListener(e -> {
            SecurityManager.EncryptionAlgo selected = (SecurityManager.EncryptionAlgo) cbCipher.getSelectedItem();
            SecurityManager.setCurrentAlgo(selected);
            onLog("[BẢO MẬT CSDL] Đã chuyển thuật toán mã hóa sang: " + selected.getDisplayName());
            updateDbStatusLabel();
        });

        row1.add(new JLabel("Giả lập mất gói UDP:"));
        row1.add(cbDropRate);
        row1.add(new JLabel("Độ trễ:"));
        row1.add(cbLatency);
        row1.add(new JLabel("Mã hóa CSDL:"));
        row1.add(cbCipher);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        row2.setOpaque(false);

        btnLockGradebook = new JButton("Khóa Sổ Điểm Học Kỳ (Đang Mở)");
        btnLockGradebook.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnLockGradebook.setBackground(new Color(40, 167, 69));
        btnLockGradebook.setForeground(Color.WHITE);
        btnLockGradebook.setFocusPainted(false);
        btnLockGradebook.addActionListener(e -> {
            boolean newLock = !SessionManager.getInstance().isGradebookLocked();
            SessionManager.getInstance().setGradebookLocked(newLock, "Phòng Giáo Vụ PTIT");
            if (newLock) {
                btnLockGradebook.setText("Mở Khóa Sổ Điểm (Đang Khóa)");
                btnLockGradebook.setBackground(PTIT_RED);
            } else {
                btnLockGradebook.setText("Khóa Sổ Điểm Học Kỳ (Đang Mở)");
                btnLockGradebook.setBackground(new Color(40, 167, 69));
            }
            String msg = newLock
                    ? "ĐÃ KHÓA SỔ ĐIỂM HỌC KỲ (Chặn Giảng viên sửa/xóa điểm)"
                    : "ĐÃ MỞ KHÓA SỔ ĐIỂM HỌC KỲ (Cho phép Giảng viên chấm điểm)";
            DatabaseManager.getInstance().logAudit("admin", newLock ? "LOCK_GRADEBOOK" : "UNLOCK_GRADEBOOK", msg, "127.0.0.1");
            onLog("[GIÁO VỤ] " + msg);
            if (udpServer != null) {
                udpServer.broadcastPushEvent("GRADE_LOCK", msg, String.valueOf(newLock));
            }
            refreshAuditLogs();
        });

        JButton btnBroadcast = new JButton("Phát Thông Báo Toàn Trường (UDP Push)");
        btnBroadcast.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnBroadcast.setBackground(new Color(0, 123, 255));
        btnBroadcast.setForeground(Color.WHITE);
        btnBroadcast.setFocusPainted(false);
        btnBroadcast.addActionListener(e -> {
            String msg = JOptionPane.showInputDialog(this,
                    "Nhập nội dung thông báo phát trực tiếp (UDP Server-Push) xuống mọi Cổng Sinh Viên:",
                    "Phòng Đào Tạo PTIT: Đã công bố điểm thi kết thúc học phần Học kỳ 1 năm học 2024-2025!");
            if (msg != null && !msg.trim().isEmpty()) {
                DatabaseManager.getInstance().logAudit("admin", "BROADCAST_PUSH", "Phát thông báo: " + msg.trim(), "127.0.0.1");
                if (udpServer != null) {
                    udpServer.broadcastPushEvent("ANNOUNCEMENT", "Phòng Đào Tạo PTIT: " + msg.trim(), msg.trim());
                }
                refreshAuditLogs();
            }
        });

        JButton btnRefreshAudit = new JButton("Tải Lại Nhật Ký Thanh Tra");
        btnRefreshAudit.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefreshAudit.addActionListener(e -> refreshAuditLogs());

        lblChaosTelemetry = new JLabel("● Gói nhận: 0 | Rớt giả lập: 0 | Push Clients: 0 | Chữ ký: HMAC-SHA256");
        lblChaosTelemetry.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblChaosTelemetry.setForeground(new Color(30, 90, 160));

        row2.add(btnLockGradebook);
        row2.add(btnBroadcast);
        row2.add(btnRefreshAudit);
        row2.add(Box.createHorizontalStrut(10));
        row2.add(lblChaosTelemetry);

        topBox.add(row1);
        topBox.add(row2);
        panel.add(topBox, BorderLayout.NORTH);

        // 2. Center Box: Immutable Audit Log Table
        String[] auditCols = {
                "ID", "Tài Khoản", "Loại Thao Tác", "Chi Tiết Thanh Tra (Điểm Cũ -> Điểm Mới / Nội Dung)", "Địa Chỉ IP", "Thời Gian Ghi Nhận"
        };
        auditTableModel = new DefaultTableModel(auditCols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        JTable tblAudit = new JTable(auditTableModel);
        tblAudit.setRowHeight(26);
        tblAudit.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblAudit.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblAudit.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblAudit.getColumnModel().getColumn(1).setPreferredWidth(95);
        tblAudit.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblAudit.getColumnModel().getColumn(3).setPreferredWidth(460);
        tblAudit.getColumnModel().getColumn(4).setPreferredWidth(100);
        tblAudit.getColumnModel().getColumn(5).setPreferredWidth(155);

        panel.add(new JScrollPane(tblAudit), BorderLayout.CENTER);
        return panel;
    }

    private void refreshAuditLogs() {
        if (auditTableModel == null || !DatabaseManager.getInstance().isConnected()) return;
        new SwingWorker<List<AuditDAO.AuditEntry>, Void>() {
            @Override
            protected List<AuditDAO.AuditEntry> doInBackground() {
                return DatabaseManager.getInstance().getRecentAuditLogs(80);
            }

            @Override
            protected void done() {
                try {
                    List<AuditDAO.AuditEntry> logs = get();
                    auditTableModel.setRowCount(0);
                    for (AuditDAO.AuditEntry entry : logs) {
                        auditTableModel.addRow(new Object[]{
                                entry.id,
                                entry.username,
                                entry.action,
                                entry.details,
                                entry.ipAddress,
                                entry.timestamp
                        });
                    }
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void updateChaosTelemetryLabel() {
        if (lblChaosTelemetry != null && udpServer != null) {
            lblChaosTelemetry.setText(String.format("● Gói UDP nhận: %d  |  Rớt giả lập (ARQ): %d  |  Kênh Push Online: %d  |  HMAC-SHA256: BẬT",
                    udpServer.getTotalPacketsReceived(),
                    udpServer.getTotalPacketsDroppedByChaos(),
                    udpServer.getActivePushSubscriberCount()));
        }
    }

    private void refreshDataInspection() {
        if (!DatabaseManager.getInstance().isConnected()) {
            updateDbStatusLabel();
            return;
        }
        new SwingWorker<List<DatabaseManager.EncryptedRecord>, Void>() {
            @Override
            protected List<DatabaseManager.EncryptedRecord> doInBackground() throws Exception {
                return DatabaseManager.getInstance().getAllRecordsForInspection();
            }

            @Override
            protected void done() {
                try {
                    List<DatabaseManager.EncryptedRecord> list = get();
                    tableModel.setRowCount(0);
                    int xs = 0, gioi = 0, kha = 0, tb = 0, yeu = 0;
                    for (DatabaseManager.EncryptedRecord rec : list) {
                        tableModel.addRow(new Object[]{
                                rec.maSV,
                                rec.hoTenEncrypted,
                                rec.diemToanEncrypted,
                                rec.diemVanEncrypted,
                                rec.diemAnhEncrypted,
                                rec.hoTenDecrypted,
                                String.format("%.2f", rec.diemTB)
                        });
                        if (rec.diemTB >= 9.0) xs++;
                        else if (rec.diemTB >= 8.0) gioi++;
                        else if (rec.diemTB >= 6.5) kha++;
                        else if (rec.diemTB >= 5.0) tb++;
                        else yeu++;
                    }
                    updateChartFromCounts(xs, gioi, kha, tb, yeu);
                    updateDbStatusLabel();
                    refreshAuditLogs();
                    updateChaosTelemetryLabel();
                } catch (Exception e) {
                    onLog("Lỗi tải bảng dữ liệu: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void updateChartFromCounts(int xs, int gioi, int kha, int tb, int yeu) {
        if (chartPanelContainer == null) return;
        chartPanelContainer.removeAll();
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Xuất sắc (" + xs + ")", xs);
        dataset.setValue("Giỏi (" + gioi + ")", gioi);
        dataset.setValue("Khá (" + kha + ")", kha);
        dataset.setValue("Trung bình (" + tb + ")", tb);
        dataset.setValue("Yếu (" + yeu + ")", yeu);

        JFreeChart chart = ChartFactory.createPieChart(
                "THỐNG KÊ PHÂN BỔ HỌC LỰC SINH VIÊN - PTIT ACADEMIC REPORT",
                dataset, true, true, false
        );
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setPaint(PTIT_RED);
        chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 15));

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(new Color(250, 250, 252));
        ChartPanel cp = new ChartPanel(chart);
        chartPanelContainer.add(cp, BorderLayout.CENTER);
        chartPanelContainer.revalidate();
        chartPanelContainer.repaint();
    }

    private void updateDbStatusLabel() {
        if (DatabaseManager.getInstance().isConnected()) {
            lblDbStatus.setText("Trạng thái CSDL: ĐÃ KẾT NỐI | Mã hóa: " + SecurityManager.getCurrentAlgo().name() + " | Toàn vẹn gói: HMAC-SHA256");
            lblDbStatus.setForeground(new Color(40, 167, 69));
        } else {
            lblDbStatus.setText("Trạng thái CSDL: CHƯA KẾT NỐI");
            lblDbStatus.setForeground(PTIT_RED);
        }
    }

    private JMenuBar createServerDemoMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu menuDemo = new JMenu("Bảng Điều Khiển Demo");
        menuDemo.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JMenuItem miParallel = new JMenuItem("▶ Chế độ Demo song song (Xếp Server + Giảng viên + Sinh viên)");
        miParallel.addActionListener(e -> AppLauncher.launchDemoParallelLayout());

        JMenuItem miTile = new JMenuItem("▶ Tự động sắp xếp lại cửa sổ trên màn hình");
        miTile.addActionListener(e -> AppLauncher.tileAllWindows());

        JMenuItem miOpenStudent = new JMenuItem("▶ Mở Cổng Thông Tin Sinh Viên");
        miOpenStudent.addActionListener(e -> AppLauncher.launchStudentClient());

        JMenuItem miOpenTeacher = new JMenuItem("▶ Mở Phân Hệ Giảng Viên");
        miOpenTeacher.addActionListener(e -> AppLauncher.launchTeacherClient());

        JMenuItem miOpenLauncher = new JMenuItem("▶ Mở Bảng Điều Khiển Launcher PTIT");
        miOpenLauncher.addActionListener(e -> AppLauncher.showLauncher());

        menuDemo.add(miParallel);
        menuDemo.add(miTile);
        menuDemo.addSeparator();
        menuDemo.add(miOpenStudent);
        menuDemo.add(miOpenTeacher);
        menuDemo.addSeparator();
        menuDemo.add(miOpenLauncher);

        JMenu menuData = new JMenu("Dữ Liệu Hệ Thống");
        menuData.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JMenuItem miRefresh = new JMenuItem("▶ Nạp lại dữ liệu mã hóa CSDL & Thanh tra");
        miRefresh.addActionListener(e -> {
            refreshDataInspection();
            updateChartPanel();
        });
        menuData.add(miRefresh);

        mb.add(menuDemo);
        mb.add(menuData);
        return mb;
    }

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            ServerGUI frame = new ServerGUI();
            frame.setVisible(true);
        });
    }
}
