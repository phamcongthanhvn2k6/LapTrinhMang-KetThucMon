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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ServerGUI extends JFrame implements UDPServer.ServerLogListener {

    public static final Color PTIT_RED = new Color(200, 16, 46);       // #C8102E
    public static final Color PTIT_DARK_RED = new Color(140, 10, 30);  // #8C0A1E
    public static final Color PTIT_LIGHT_BG = new Color(245, 247, 250);

    private UDPServer udpServer;
    private JTextField txtPort;
    private JTextField txtDesKey;
    private JComboBox<SecurityManager.EncryptionAlgo> cbAlgo;
    private JButton btnStartStop;
    private JLabel lblServerStatus;
    private JLabel lblDbStatus;

    private JTextArea txtLog;
    private JTable tblDataInspection;
    private DefaultTableModel tableModel;
    private JPanel chartPanelContainer;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public ServerGUI() {
        setTitle("PTIT SERVER - Chương Trình Quản Lý Sinh Viên UDP");
        setSize(1150, 720);
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

        JLabel titleLabel = new JLabel("HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG - SERVER UDP");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Đề tài 12: Quản lý sinh viên | Bảo mật AES-256 / DES | Kết nối CSDL SQL");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(255, 230, 230));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 2, 2));
        titleBox.setOpaque(false);
        titleBox.add(titleLabel);
        titleBox.add(subtitleLabel);

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        controlPanel.setOpaque(false);

        JLabel lblAlgo = new JLabel("Thuật toán:");
        lblAlgo.setForeground(Color.WHITE);
        lblAlgo.setFont(new Font("Segoe UI", Font.BOLD, 12));

        cbAlgo = new JComboBox<>(SecurityManager.EncryptionAlgo.values());
        cbAlgo.setSelectedItem(SecurityManager.getCurrentAlgo());
        cbAlgo.addActionListener(e -> SecurityManager.setCurrentAlgo((SecurityManager.EncryptionAlgo) cbAlgo.getSelectedItem()));

        JLabel lblPort = new JLabel("Cổng UDP:");
        lblPort.setForeground(Color.WHITE);
        lblPort.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtPort = new JTextField("9876", 4);

        JLabel lblKey = new JLabel("Khóa:");
        lblKey.setForeground(Color.WHITE);
        lblKey.setFont(new Font("Segoe UI", Font.BOLD, 12));
        txtDesKey = new JTextField(DatabaseManager.getInstance().getDesKey(), 8);

        btnStartStop = new JButton("KÍCH HOẠT SERVER");
        btnStartStop.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnStartStop.setBackground(Color.WHITE);
        btnStartStop.setForeground(PTIT_RED);
        btnStartStop.setFocusPainted(false);
        btnStartStop.addActionListener(e -> toggleServer());

        controlPanel.add(lblAlgo);
        controlPanel.add(cbAlgo);
        controlPanel.add(Box.createHorizontalStrut(10));
        controlPanel.add(lblPort);
        controlPanel.add(txtPort);
        controlPanel.add(Box.createHorizontalStrut(10));
        controlPanel.add(lblKey);
        controlPanel.add(txtDesKey);
        controlPanel.add(Box.createHorizontalStrut(15));
        controlPanel.add(btnStartStop);

        headerPanel.add(titleBox, BorderLayout.NORTH);
        headerPanel.add(controlPanel, BorderLayout.SOUTH);
        add(headerPanel, BorderLayout.NORTH);

        // Main Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab 1: Database Inspection
        JPanel tabData = new JPanel(new BorderLayout(5, 5));
        tabData.setBackground(Color.WHITE);
        tabData.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columnNames = {
                "Mã SV", "Họ Tên (Mã Hóa Encrypted)", "Đ.Toán (Encrypted)", "Đ.Văn (Encrypted)", "Đ.Anh (Encrypted)", "Họ Tên (Giải Mã)", "ĐTB"
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

        JButton btnRefreshData = new JButton("Làm Mới CSDL Inspection");
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
        JButton btnClearLog = new JButton("Xóa Nhật Ký");
        btnClearLog.addActionListener(e -> txtLog.setText(""));
        logActionBar.add(btnClearLog);

        tabLogs.add(scrollLog, BorderLayout.CENTER);
        tabLogs.add(logActionBar, BorderLayout.SOUTH);

        tabbedPane.addTab("Dữ Liệu Mã Hóa CSDL Inspection", tabData);
        tabbedPane.addTab("Biểu Đồ Thống Kê Học Lực (PTIT JFreeChart)", tabChart);
        tabbedPane.addTab("Nhật Ký Gói Tin UDP (Live Logs)", tabLogs);

        add(tabbedPane, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        statusBar.setBackground(Color.WHITE);
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)));
        lblServerStatus = new JLabel("Trạng thái Server: Đã dừng");
        lblServerStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblServerStatus.setForeground(PTIT_RED);
        statusBar.add(lblServerStatus);

        add(statusBar, BorderLayout.SOUTH);
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
            btnStartStop.setText("KÍCH HOẠT SERVER");
            btnStartStop.setBackground(Color.WHITE);
            btnStartStop.setForeground(PTIT_RED);
            lblServerStatus.setText("Trạng thái Server: Đã dừng");
            lblServerStatus.setForeground(PTIT_RED);
            txtPort.setEnabled(true);
            txtDesKey.setEnabled(true);
            cbAlgo.setEnabled(true);
        } else {
            try {
                int port = Integer.parseInt(txtPort.getText().trim());
                String desKey = txtDesKey.getText().trim();
                DatabaseManager.getInstance().setDesKey(desKey);

                udpServer = new UDPServer(port);
                udpServer.setLogListener(this);
                udpServer.start();

                btnStartStop.setText("DỪNG SERVER");
                btnStartStop.setBackground(PTIT_DARK_RED);
                btnStartStop.setForeground(Color.WHITE);
                lblServerStatus.setText("Trạng thái Server: Đang lắng nghe cổng UDP " + port + " (Multi-threaded ThreadPool)");
                lblServerStatus.setForeground(new Color(40, 167, 69));
                txtPort.setEnabled(false);
                txtDesKey.setEnabled(false);
                cbAlgo.setEnabled(false);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Không thể khởi động Server: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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
            onLog("Lỗi tải bảng dữ liệu inspection: " + e.getMessage());
        }
    }

    private void updateDbStatusLabel() {
        if (DatabaseManager.getInstance().isConnected()) {
            lblDbStatus.setText("Trạng thái CSDL: ĐÃ KẾT NỐI (Mã hóa " + SecurityManager.getCurrentAlgo() + ")");
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
