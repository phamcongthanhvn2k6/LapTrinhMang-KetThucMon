package com.qlsv.client;

import com.formdev.flatlaf.FlatLightLaf;
import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.network.UDPPacket;
import com.qlsv.util.ExcelExporter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ClientGUI extends JFrame {

    public static final Color PTIT_RED = new Color(200, 16, 46);       // #C8102E
    public static final Color PTIT_DARK_RED = new Color(140, 10, 30);  // #8C0A1E
    public static final Color PTIT_LIGHT_BG = new Color(248, 249, 250);

    private UDPClient client;
    private List<StudentResult> currentStudentList = new ArrayList<>();

    private JTabbedPane mainTabbedPane;

    // Header Badges
    private JLabel lblServerBadge;
    private JLabel lblDbBadge;

    // Connection Controls (Settings Tab)
    private JTextField txtServerHost;
    private JTextField txtServerPort;
    private JButton btnConnectServer;
    private JLabel lblConnStatus;

    private JComboBox<SqlConfig.DbType> cbDbType;
    private JTextField txtDbHost;
    private JTextField txtDbPort;
    private JTextField txtDbName;
    private JTextField txtDbUser;
    private JPasswordField txtDbPass;
    private JButton btnConnectDb;
    private JLabel lblDbStatusMsg;

    // CRUD Controls (Main Workspace Tab)
    private JTextField txtStudentName;
    private JTextField txtStudentId;
    private JTextField txtScoreMath;
    private JTextField txtScoreLit;
    private JTextField txtScoreEng;
    private JTextField txtSearchQuery;
    private JComboBox<String> cbFilterRank;

    private JButton btnSendStudent;
    private JButton btnClearForm;
    private JButton btnSearch;
    private JButton btnRefreshAll;
    private JButton btnDeleteSelected;
    private JButton btnExportExcel;

    private JTable tblResults;
    private DefaultTableModel tableModel;
    private JLabel lblTableSummary;

    public ClientGUI() {
        setTitle("PTIT CLIENT - HỆ THỐNG QUẢN LÝ SINH VIÊN");
        setSize(1180, 760);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        try {
            FlatLightLaf.setup();
        } catch (Exception ignored) {}

        initUI();
        autoConnectDefault();
    }

    private void initUI() {
        setLayout(new BorderLayout(0, 0));

        // Top Header Banner (PTIT Red)
        JPanel headerPanel = new JPanel(new BorderLayout(15, 0));
        headerPanel.setBackground(PTIT_RED);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel titleLabel = new JLabel("HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Hệ Thống Quản Lý Sinh Viên Truyền Bảng Mạng UDP & CSDL SQL");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(255, 230, 230));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 2, 2));
        titleBox.setOpaque(false);
        titleBox.add(titleLabel);
        titleBox.add(subtitleLabel);

        // Status Badges Panel
        JPanel statusBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        statusBox.setOpaque(false);

        lblServerBadge = new JLabel("● UDP Server: Chưa kết nối");
        lblServerBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblServerBadge.setForeground(Color.WHITE);
        lblServerBadge.setOpaque(true);
        lblServerBadge.setBackground(PTIT_DARK_RED);
        lblServerBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        lblDbBadge = new JLabel("● CSDL SQL: Chưa kết nối");
        lblDbBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblDbBadge.setForeground(Color.WHITE);
        lblDbBadge.setOpaque(true);
        lblDbBadge.setBackground(PTIT_DARK_RED);
        lblDbBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        statusBox.add(lblServerBadge);
        statusBox.add(lblDbBadge);

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(statusBox, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Main Tabbed Workspace
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JPanel studentWorkspace = createStudentWorkspaceTab();
        JPanel settingsWorkspace = createSettingsTab();

        mainTabbedPane.addTab("Quản Lý Sinh Viên", studentWorkspace);
        mainTabbedPane.addTab("Cấu Hình Kết Nối & CSDL", settingsWorkspace);

        add(mainTabbedPane, BorderLayout.CENTER);
    }

    private JPanel createStudentWorkspaceTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PTIT_LIGHT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Input Form Card (North)
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                " Form Nhập / Cập Nhật Thông Tin Sinh Viên ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), PTIT_RED
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Họ tên sinh viên:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        txtStudentName = new JTextField(16);
        txtStudentName.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(txtStudentName, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Mã sinh viên:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 1.0;
        txtStudentId = new JTextField(12);
        txtStudentId.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(txtStudentId, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Điểm Toán:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        txtScoreMath = new JTextField(10);
        txtScoreMath.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(txtScoreMath, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Điểm Văn:"), gbc);
        gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 1.0;
        txtScoreLit = new JTextField(10);
        txtScoreLit.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(txtScoreLit, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Điểm Anh:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        txtScoreEng = new JTextField(10);
        txtScoreEng.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(txtScoreEng, gbc);

        JPanel btnFormBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnFormBox.setOpaque(false);

        btnClearForm = new JButton("Xóa Form");
        btnClearForm.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClearForm.addActionListener(e -> clearForm());

        btnSendStudent = new JButton("LƯU SINH VIÊN VIA UDP");
        btnSendStudent.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSendStudent.setBackground(PTIT_RED);
        btnSendStudent.setForeground(Color.WHITE);
        btnSendStudent.setFocusPainted(false);
        btnSendStudent.addActionListener(e -> handleSendStudent());

        btnFormBox.add(btnClearForm);
        btnFormBox.add(btnSendStudent);

        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 2;
        formPanel.add(btnFormBox, gbc);

        panel.add(formPanel, BorderLayout.NORTH);

        // Center Table Card
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                " Danh Sách Sinh Viên Quản Lý ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), PTIT_RED
        ));

        // Toolbar
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        toolBar.setBackground(Color.WHITE);

        txtSearchQuery = new JTextField(15);
        txtSearchQuery.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        btnSearch = new JButton("Tìm Kiếm");
        btnSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSearch.addActionListener(e -> handleSearch());

        cbFilterRank = new JComboBox<>(new String[]{"Tất cả xếp loại", "Xuất sắc", "Giỏi", "Khá", "Trung bình", "Yếu"});
        cbFilterRank.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbFilterRank.addActionListener(e -> applyFilters());

        btnRefreshAll = new JButton("Tải Lại");
        btnRefreshAll.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnRefreshAll.addActionListener(e -> loadAllStudents());

        btnDeleteSelected = new JButton("Xóa Dòng Chọn");
        btnDeleteSelected.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDeleteSelected.setBackground(PTIT_DARK_RED);
        btnDeleteSelected.setForeground(Color.WHITE);
        btnDeleteSelected.addActionListener(e -> handleDeleteSelected());

        btnExportExcel = new JButton("Xuất File Excel (.xlsx)");
        btnExportExcel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExportExcel.setBackground(new Color(40, 167, 69));
        btnExportExcel.setForeground(Color.WHITE);
        btnExportExcel.addActionListener(e -> handleExportExcel());

        toolBar.add(new JLabel("Từ khóa:"));
        toolBar.add(txtSearchQuery);
        toolBar.add(btnSearch);
        toolBar.add(Box.createHorizontalStrut(5));
        toolBar.add(new JLabel("Lọc:"));
        toolBar.add(cbFilterRank);
        toolBar.add(btnRefreshAll);
        toolBar.add(btnDeleteSelected);
        toolBar.add(Box.createHorizontalStrut(10));
        toolBar.add(btnExportExcel);

        centerPanel.add(toolBar, BorderLayout.NORTH);

        // Table
        String[] columnNames = {"STT", "Mã Sinh Viên", "Họ Tên Sinh Viên", "Đ.Toán", "Đ.Văn", "Đ.Anh", "Điểm TB", "Xếp Loại"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblResults = new JTable(tableModel);
        tblResults.setRowHeight(28);
        tblResults.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblResults.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tblResults.getTableHeader().setBackground(new Color(240, 242, 245));
        tblResults.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Alignment
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tblResults.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblResults.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        tblResults.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        tblResults.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        tblResults.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        tblResults.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);
        tblResults.getColumnModel().getColumn(7).setCellRenderer(centerRenderer);

        JScrollPane scrollPane = new JScrollPane(tblResults);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // Footer Summary
        lblTableSummary = new JLabel("Tổng số: 0 sinh viên", SwingConstants.RIGHT);
        lblTableSummary.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTableSummary.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        centerPanel.add(lblTableSummary, BorderLayout.SOUTH);

        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createSettingsTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PTIT_LIGHT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints outerGbc = new GridBagConstraints();
        outerGbc.insets = new Insets(10, 10, 10, 10);
        outerGbc.fill = GridBagConstraints.BOTH;
        outerGbc.weightx = 1.0; outerGbc.weighty = 0.5;

        // Box 1: Server UDP Config
        JPanel serverBox = new JPanel(new GridBagLayout());
        serverBox.setBackground(Color.WHITE);
        serverBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                " 1. Cấu Hình Kết Nối UDP Server ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), PTIT_RED
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        serverBox.add(new JLabel("Địa chỉ IP Server:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        txtServerHost = new JTextField("127.0.0.1", 15);
        txtServerHost.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        serverBox.add(txtServerHost, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0;
        serverBox.add(new JLabel("Cổng UDP:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 1.0;
        txtServerPort = new JTextField("9876", 8);
        txtServerPort.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        serverBox.add(txtServerPort, gbc);

        btnConnectServer = new JButton("KẾT NỐI SERVER UDP");
        btnConnectServer.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnConnectServer.setBackground(PTIT_RED);
        btnConnectServer.setForeground(Color.WHITE);
        btnConnectServer.setFocusPainted(false);
        btnConnectServer.addActionListener(e -> handleConnectServer());

        gbc.gridx = 4; gbc.gridy = 0; gbc.weightx = 0.0;
        serverBox.add(btnConnectServer, gbc);

        lblConnStatus = new JLabel("Trạng thái: Chưa kiểm tra kết nối Server.");
        lblConnStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblConnStatus.setForeground(Color.GRAY);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 5;
        serverBox.add(lblConnStatus, gbc);

        outerGbc.gridx = 0; outerGbc.gridy = 0;
        panel.add(serverBox, outerGbc);

        // Box 2: Database Config
        JPanel dbBox = new JPanel(new GridBagLayout());
        dbBox.setBackground(Color.WHITE);
        dbBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                " 2. Cấu Hình CSDL SQL Gửi Lên Server ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), PTIT_RED
        ));

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 10, 6, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        dbBox.add(new JLabel("Loại CSDL SQL:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        cbDbType = new JComboBox<>(SqlConfig.DbType.values());
        cbDbType.setSelectedItem(SqlConfig.DbType.H2_EMBEDDED);
        cbDbType.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbDbType.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                SqlConfig.DbType type = (SqlConfig.DbType) cbDbType.getSelectedItem();
                if (type == SqlConfig.DbType.SQL_SERVER) {
                    txtDbPort.setText("1433"); txtDbUser.setText("sa"); txtDbName.setText("QLSV_DB");
                } else if (type == SqlConfig.DbType.MYSQL) {
                    txtDbPort.setText("3306"); txtDbUser.setText("root"); txtDbName.setText("QLSV_DB");
                } else if (type == SqlConfig.DbType.POSTGRESQL) {
                    txtDbPort.setText("5432"); txtDbUser.setText("postgres"); txtDbName.setText("postgres");
                } else {
                    txtDbPort.setText("0"); txtDbUser.setText("sa"); txtDbName.setText("qlsv_db");
                }
            }
        });
        dbBox.add(cbDbType, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0;
        dbBox.add(new JLabel("Địa chỉ Server CSDL:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 1.0;
        txtDbHost = new JTextField("localhost", 15);
        txtDbHost.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dbBox.add(txtDbHost, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        dbBox.add(new JLabel("Cổng CSDL:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        txtDbPort = new JTextField("0", 15);
        txtDbPort.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dbBox.add(txtDbPort, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.0;
        dbBox.add(new JLabel("Tên CSDL:"), gbc);
        gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 1.0;
        txtDbName = new JTextField("qlsv_db", 15);
        txtDbName.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dbBox.add(txtDbName, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        dbBox.add(new JLabel("Username SQL:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        txtDbUser = new JTextField("sa", 15);
        txtDbUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dbBox.add(txtDbUser, gbc);

        gbc.gridx = 2; gbc.gridy = 2; gbc.weightx = 0.0;
        dbBox.add(new JLabel("Password SQL:"), gbc);
        gbc.gridx = 3; gbc.gridy = 2; gbc.weightx = 1.0;
        txtDbPass = new JPasswordField("", 15);
        txtDbPass.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dbBox.add(txtDbPass, gbc);

        btnConnectDb = new JButton("GỬI CẤU HÌNH KẾT NỐI CSDL");
        btnConnectDb.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConnectDb.setBackground(PTIT_RED);
        btnConnectDb.setForeground(Color.WHITE);
        btnConnectDb.setFocusPainted(false);
        btnConnectDb.addActionListener(e -> handleConnectDb());

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
        dbBox.add(btnConnectDb, gbc);

        lblDbStatusMsg = new JLabel("Trạng thái CSDL: Chưa khởi tạo.");
        lblDbStatusMsg.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblDbStatusMsg.setForeground(Color.GRAY);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 4;
        dbBox.add(lblDbStatusMsg, gbc);

        outerGbc.gridx = 0; outerGbc.gridy = 1;
        panel.add(dbBox, outerGbc);

        return panel;
    }

    private void autoConnectDefault() {
        SwingUtilities.invokeLater(() -> {
            try {
                handleConnectServer();
                if (client != null) {
                    handleConnectDb();
                }
            } catch (Exception ignored) {}
        });
    }

    private void handleConnectServer() {
        String host = txtServerHost.getText().trim();
        String portStr = txtServerPort.getText().trim();

        if (host.isEmpty() || portStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ IP và Cổng Server!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            client = new UDPClient(host, port);

            boolean connected = client.pingServer();
            if (connected) {
                lblConnStatus.setText("ĐÃ KẾT NỐI SERVER UDP (" + host + ":" + port + ")");
                lblConnStatus.setForeground(new Color(40, 167, 69));

                lblServerBadge.setText("● UDP Server: " + host + ":" + port);
                lblServerBadge.setBackground(new Color(40, 167, 69));
            } else {
                throw new Exception("Server không phản hồi!");
            }
        } catch (Exception ex) {
            lblConnStatus.setText("Kết nối Server thất bại: " + ex.getMessage());
            lblConnStatus.setForeground(PTIT_RED);

            lblServerBadge.setText("● UDP Server: Chưa kết nối");
            lblServerBadge.setBackground(PTIT_DARK_RED);
        }
    }

    private void handleConnectDb() {
        if (client == null) {
            lblDbStatusMsg.setText("Chưa kết nối Server UDP!");
            lblDbStatusMsg.setForeground(PTIT_RED);
            return;
        }

        try {
            SqlConfig.DbType dbType = (SqlConfig.DbType) cbDbType.getSelectedItem();
            String host = txtDbHost.getText().trim();
            int port = Integer.parseInt(txtDbPort.getText().trim());
            String dbName = txtDbName.getText().trim();
            String user = txtDbUser.getText().trim();
            String pass = new String(txtDbPass.getPassword());

            SqlConfig config = new SqlConfig(dbType, host, port, dbName, user, pass);
            UDPPacket response = client.connectDatabase(config);

            if (response != null && response.isSuccess()) {
                lblDbStatusMsg.setText("ĐÃ KẾT NỐI CSDL THÀNH CÔNG ON SERVER (" + dbType + ")");
                lblDbStatusMsg.setForeground(new Color(40, 167, 69));

                lblDbBadge.setText("● CSDL: " + dbType);
                lblDbBadge.setBackground(new Color(40, 167, 69));

                loadAllStudents();
            } else {
                String errMsg = (response != null) ? response.getMessage() : "Không nhận được phản hồi";
                lblDbStatusMsg.setText("Lỗi kết nối CSDL: " + errMsg);
                lblDbStatusMsg.setForeground(PTIT_RED);

                lblDbBadge.setText("● CSDL: Kết nối thất bại");
                lblDbBadge.setBackground(PTIT_DARK_RED);
            }
        } catch (Exception ex) {
            lblDbStatusMsg.setText("Lỗi cấu hình CSDL: " + ex.getMessage());
            lblDbStatusMsg.setForeground(PTIT_RED);
        }
    }

    private void handleSendStudent() {
        if (client == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối Server UDP! Vui lòng mở Tab Cấu hình để kết nối.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String name = txtStudentName.getText().trim();
        String id = txtStudentId.getText().trim();
        String mathStr = txtScoreMath.getText().trim();
        String litStr = txtScoreLit.getText().trim();
        String engStr = txtScoreEng.getText().trim();

        if (name.isEmpty() || id.isEmpty() || mathStr.isEmpty() || litStr.isEmpty() || engStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            double math = Double.parseDouble(mathStr);
            double lit = Double.parseDouble(litStr);
            double eng = Double.parseDouble(engStr);

            if (math < 0 || math > 10 || lit < 0 || lit > 10 || eng < 0 || eng > 10) {
                JOptionPane.showMessageDialog(this, "Điểm thi phải từ 0.0 đến 10.0!", "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
                return;
            }

            StudentData student = new StudentData(id, name, math, lit, eng);
            StudentResult result = client.sendStudentData(student);

            JOptionPane.showMessageDialog(this,
                    String.format("Lưu thông tin Sinh viên thành công!\nHọ tên: %s (%s)\nĐiểm TB: %.2f - Xếp loại: %s",
                            result.getFullName(), result.getStudentId(), result.getAverageScore(), result.getAcademicRank()),
                    "Thông Báo Thành Công", JOptionPane.INFORMATION_MESSAGE);

            clearForm();
            loadAllStudents();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Điểm thi phải là số thực hợp lệ (ví dụ: 8.5)!", "Lỗi định dạng", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi gửi dữ liệu qua UDP: " + ex.getMessage(), "Lỗi UDP", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSearch() {
        String query = txtSearchQuery.getText().trim();
        if (client == null) return;
        try {
            currentStudentList = client.searchStudents(query);
            applyFilters();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tìm kiếm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleDeleteSelected() {
        int row = tblResults.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 sinh viên trong bảng để xóa!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String studentId = (String) tableModel.getValueAt(row, 1);
        String name = (String) tableModel.getValueAt(row, 2);

        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc chắn muốn xóa sinh viên " + name + " (" + studentId + ") không?",
                "Xác Nhận Xóa Sinh Viên", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean deleted = client.deleteStudent(studentId);
                if (deleted) {
                    JOptionPane.showMessageDialog(this, "Đã xóa sinh viên " + studentId + " thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    loadAllStudents();
                } else {
                    JOptionPane.showMessageDialog(this, "Xóa thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi xóa qua UDP: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadAllStudents() {
        if (client == null) return;
        try {
            currentStudentList = client.getAllStudents();
            applyFilters();
        } catch (Exception ex) {
            System.err.println("Lỗi tải danh sách: " + ex.getMessage());
        }
    }

    private void applyFilters() {
        String filterRank = (String) cbFilterRank.getSelectedItem();
        List<StudentResult> filtered = currentStudentList;

        if (filterRank != null && !filterRank.equalsIgnoreCase("Tất cả xếp loại")) {
            filtered = currentStudentList.stream()
                    .filter(s -> s.getAcademicRank() != null && s.getAcademicRank().equalsIgnoreCase(filterRank))
                    .collect(Collectors.toList());
        }

        tableModel.setRowCount(0);
        int stt = 1;
        for (StudentResult s : filtered) {
            tableModel.addRow(new Object[]{
                    stt++,
                    s.getStudentId(),
                    s.getFullName(),
                    String.format("%.1f", s.getScoreMath()),
                    String.format("%.1f", s.getScoreLiterature()),
                    String.format("%.1f", s.getScoreEnglish()),
                    String.format("%.2f", s.getAverageScore()),
                    s.getAcademicRank()
            });
        }
        lblTableSummary.setText("Tổng số: " + filtered.size() + " / " + currentStudentList.size() + " sinh viên");
    }

    private void handleExportExcel() {
        if (client == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối Server UDP!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (currentStudentList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Danh sách rỗng, không thể xuất!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Lưu Báo Cáo Excel Danh Sách Sinh Viên PTIT");
            chooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
            chooser.setSelectedFile(new File("PTIT_BaoCao_SinhVien.xlsx"));

            int userSelection = chooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = chooser.getSelectedFile();
                if (!fileToSave.getAbsolutePath().endsWith(".xlsx")) {
                    fileToSave = new File(fileToSave.getAbsolutePath() + ".xlsx");
                }

                ExcelExporter.exportStudentsToExcel(currentStudentList, fileToSave);
                JOptionPane.showMessageDialog(this,
                        "Xuất Báo Cáo Excel THÀNH CÔNG!\nĐã lưu tại: " + fileToSave.getAbsolutePath(),
                        "Thông Báo Excel", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi xuất Excel: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearForm() {
        txtStudentName.setText("");
        txtStudentId.setText("");
        txtScoreMath.setText("");
        txtScoreLit.setText("");
        txtScoreEng.setText("");
        txtStudentName.requestFocus();
    }

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            ClientGUI frame = new ClientGUI();
            frame.setVisible(true);
        });
    }
}
