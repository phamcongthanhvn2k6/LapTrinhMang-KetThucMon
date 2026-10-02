package com.qlsv.server;

import com.formdev.flatlaf.FlatDarkLaf;
import com.qlsv.database.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ServerGUI extends JFrame implements UDPServer.ServerLogListener {

    private UDPServer udpServer;
    private JTextField txtPort;
    private JTextField txtDesKey;
    private JButton btnStartStop;
    private JLabel lblServerStatus;
    private JLabel lblDbStatus;

    private JTextArea txtLog;
    private JTable tblDataInspection;
    private DefaultTableModel tableModel;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    public ServerGUI() {
        setTitle("SERVER - Quản Lý Sinh Viên (UDP & Mã Hóa DES)");
        setSize(1000, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(28, 35, 49));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("HỆ THỐNG SERVER QUẢN LÝ SINH VIÊN (UDP)");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Lưu trữ CSDL + Mã hóa DES + Tính điểm trung bình");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(180, 190, 200));

        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);
        titleBox.add(titleLabel);
        titleBox.add(subtitleLabel);

        headerPanel.add(titleBox, BorderLayout.WEST);

        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        controlPanel.setOpaque(false);

        JLabel lblPort = new JLabel("Cổng UDP:");
        lblPort.setForeground(Color.WHITE);
        txtPort = new JTextField("9876", 5);

        JLabel lblKey = new JLabel("Khóa DES (8-char):");
        lblKey.setForeground(Color.WHITE);
        txtDesKey = new JTextField(DatabaseManager.getInstance().getDesKey(), 8);

        btnStartStop = new JButton("Khởi Động Server");
        btnStartStop.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnStartStop.setBackground(new Color(40, 167, 69));
        btnStartStop.setForeground(Color.WHITE);
        btnStartStop.setFocusPainted(false);
        btnStartStop.addActionListener(e -> toggleServer());

        controlPanel.add(lblPort);
        controlPanel.add(txtPort);
        controlPanel.add(lblKey);
        controlPanel.add(txtDesKey);
        controlPanel.add(btnStartStop);

        headerPanel.add(controlPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Main Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Tab 1: Database Inspection (DES Encrypted Storage)
        JPanel tabData = new JPanel(new BorderLayout(5, 5));
        tabData.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] columnNames = {
                "Mã SV", "Họ Tên (Mã Hóa DES Base64)", "Đ.Toán (DES)", "Đ.Văn (DES)", "Đ.Anh (DES)", "Họ Tên (Giải Mã)", "ĐTB"
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblDataInspection = new JTable(tableModel);
        tblDataInspection.setRowHeight(25);
        tblDataInspection.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblDataInspection.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        JScrollPane scrollTable = new JScrollPane(tblDataInspection);

        JPanel tableActionBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefreshData = new JButton("Làm mới CSDL Inspection");
        btnRefreshData.addActionListener(e -> refreshDataInspection());
        tableActionBar.add(btnRefreshData);

        lblDbStatus = new JLabel("Trạng thái CSDL: Chưa kết nối");
        lblDbStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        tableActionBar.add(Box.createHorizontalStrut(20));
        tableActionBar.add(lblDbStatus);

        tabData.add(tableActionBar, BorderLayout.NORTH);
        tabData.add(scrollTable, BorderLayout.CENTER);

        // Tab 2: Logs
        JPanel tabLogs = new JPanel(new BorderLayout(5, 5));
        tabLogs.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        txtLog.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtLog.setBackground(new Color(20, 24, 33));
        txtLog.setForeground(new Color(130, 220, 140));

        JScrollPane scrollLog = new JScrollPane(txtLog);

        JPanel logActionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnClearLog = new JButton("Xóa nhật ký");
        btnClearLog.addActionListener(e -> txtLog.setText(""));
        logActionBar.add(btnClearLog);

        tabLogs.add(scrollLog, BorderLayout.CENTER);
        tabLogs.add(logActionBar, BorderLayout.SOUTH);

        tabbedPane.addTab("Dữ liệu Mã hóa DES trong CSDL", tabData);
        tabbedPane.addTab("Nhật ký gói tin UDP (Live Logs)", tabLogs);

        add(tabbedPane, BorderLayout.CENTER);

        // Status bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        statusBar.setBorder(BorderFactory.createEtchedBorder());
        lblServerStatus = new JLabel("Trạng thái Server: Đã dừng");
        lblServerStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblServerStatus.setForeground(Color.RED);
        statusBar.add(lblServerStatus);

        add(statusBar, BorderLayout.SOUTH);
    }

    private void toggleServer() {
        if (udpServer != null && udpServer.isRunning()) {
            udpServer.stop();
            btnStartStop.setText("Khởi Động Server");
            btnStartStop.setBackground(new Color(40, 167, 69));
            lblServerStatus.setText("Trạng thái Server: Đã dừng");
            lblServerStatus.setForeground(Color.RED);
            txtPort.setEnabled(true);
            txtDesKey.setEnabled(true);
        } else {
            try {
                int port = Integer.parseInt(txtPort.getText().trim());
                String desKey = txtDesKey.getText().trim();
                DatabaseManager.getInstance().setDesKey(desKey);

                udpServer = new UDPServer(port);
                udpServer.setLogListener(this);
                udpServer.start();

                btnStartStop.setText("Dừng Server");
                btnStartStop.setBackground(new Color(220, 53, 69));
                lblServerStatus.setText("Trạng thái Server: Đang lắng nghe cổng UDP " + port);
                lblServerStatus.setForeground(new Color(40, 167, 69));
                txtPort.setEnabled(false);
                txtDesKey.setEnabled(false);
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(this, "Cổng kết nối không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
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
        SwingUtilities.invokeLater(this::refreshDataInspection);
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
            lblDbStatus.setText("Trạng thái CSDL: ĐÃ KẾT NỐI (Sẵn sàng lưu & mã hóa DES)");
            lblDbStatus.setForeground(new Color(40, 167, 69));
        } else {
            lblDbStatus.setText("Trạng thái CSDL: CHƯA KẾT NỐI");
            lblDbStatus.setForeground(Color.RED);
        }
    }

    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            ServerGUI frame = new ServerGUI();
            frame.setVisible(true);
        });
    }
}
