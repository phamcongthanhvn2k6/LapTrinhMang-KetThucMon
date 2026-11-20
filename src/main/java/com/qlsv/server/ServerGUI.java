package com.qlsv.server;

import com.formdev.flatlaf.FlatLightLaf;
import com.qlsv.database.DatabaseManager;
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
    private JPanel chartPanelContainer;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public ServerGUI() {
        setTitle("PTIT - TRUNG TÂM QUẢN TRỊ MÁY CHỦ ĐÀO TẠO");
        setSize(1160, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            FlatLightLaf.setup();
        } catch (Exception ignored) {}

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Header Panel (PTIT Red)
        JPanel headerPanel = new JPanel(new BorderLayout(0, 8));
        headerPanel.setBackground(PTIT_RED);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel titleLabel = new JLabel("HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG - TRUNG TÂM MÁY CHỦ");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Hệ Thống Quản Trị Cơ Sở Dữ Liệu & Dịch Vụ Đào Tạo Phân Tán");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(255, 230, 230));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 2, 2));
        titleBox.setOpaque(false);
        titleBox.add(titleLabel);
        titleBox.add(subtitleLabel);

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controlPanel.setOpaque(false);

        btnSettings = new JButton("⚙️ Thiết Lập Nâng Cao");
        btnSettings.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSettings.setBackground(new Color(240, 240, 240));
        btnSettings.setForeground(Color.DARK_GRAY);
        btnSettings.setFocusPainted(false);
        btnSettings.addActionListener(e -> openAdvancedSettingsDialog());

        btnStartStop = new JButton("KHỞI CHẠY DỊCH VỤ MÁY CHỦ");
        btnStartStop.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnStartStop.setBackground(Color.WHITE);
        btnStartStop.setForeground(PTIT_RED);
        btnStartStop.setFocusPainted(false);
        btnStartStop.addActionListener(e -> toggleServer());

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
                "Mã SV", "Họ Tên (Đã Bảo Mật)", "Đ.Toán (Bảo Mật)", "Đ.Văn (Bảo Mật)", "Đ.Anh (Bảo Mật)", "Họ Tên (Hiển Thị)", "Điểm TB"
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

        tabbedPane.addTab("Kiểm Tra An Toàn Dữ Liệu", tabData);
        tabbedPane.addTab("Biểu Đồ Thống Kê Học Lực", tabChart);
        tabbedPane.addTab("Nhật Ký Hoạt Động Hệ Thống", tabLogs);

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

        // Auto-connect to DB from db.properties if not connected
        if (!DatabaseManager.getInstance().isConnected()) {
            try {
                com.qlsv.model.SqlConfig config = com.qlsv.database.DatabaseConfigLoader.loadConfig();
                DatabaseManager.getInstance().connect(config);
            } catch (Exception ignored) {}
        }
        refreshDataInspection();
        updateChartPanel();
        updateDbStatusLabel();
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
        });
    }

    @Override
    public void onDataUpdated() {
        SwingUtilities.invokeLater(() -> {
            refreshDataInspection();
            updateChartPanel();
        });
    }

    private void refreshDataInspection() {
        tableModel.setRowCount(0);
        try {
            List<DatabaseManager.EncryptedRecord> list = DatabaseManager.getInstance().getAllRecordsForInspection();
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
            }
            updateDbStatusLabel();
        } catch (Exception e) {
            onLog("Lỗi tải bảng dữ liệu: " + e.getMessage());
        }
    }

    private void updateDbStatusLabel() {
        if (DatabaseManager.getInstance().isConnected()) {
            lblDbStatus.setText("Trạng thái CSDL: ĐÃ KẾT NỐI (Dữ liệu được bảo vệ an toàn)");
            lblDbStatus.setForeground(new Color(40, 167, 69));
        } else {
            lblDbStatus.setText("Trạng thái CSDL: CHƯA KẾT NỐI");
            lblDbStatus.setForeground(PTIT_RED);
        }
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
