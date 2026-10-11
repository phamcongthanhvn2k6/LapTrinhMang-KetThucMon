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

    public enum UserRole {
        STUDENT("🔒 Quyền: Học sinh (Chỉ xem)"),
        TEACHER("🔓 Quyền: Giảng viên (Toàn quyền)");

        private final String label;
        UserRole(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private UserRole currentRole = UserRole.STUDENT; // Default to Student mode for security

    private UDPClient client;
    private List<StudentResult> currentStudentList = new ArrayList<>();

    private JTabbedPane mainTabbedPane;

    // Header Badges & Role Controls
    private JLabel lblServerBadge;
    private JLabel lblDbBadge;
    private JButton btnRoleAuth;

    // Workspace panels
    private JPanel formPanel;
    private JPanel settingsWorkspace;

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

        // Status Badges & Role Switcher Panel
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

        btnRoleAuth = new JButton("🔒 Quyền: Học sinh (Bấm để Đăng nhập Giảng viên)");
        btnRoleAuth.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRoleAuth.setBackground(new Color(220, 53, 69));
        btnRoleAuth.setForeground(Color.WHITE);
        btnRoleAuth.setFocusPainted(false);
        btnRoleAuth.addActionListener(e -> toggleRoleAuth());

        statusBox.add(lblServerBadge);
        statusBox.add(lblDbBadge);
        statusBox.add(btnRoleAuth);

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(statusBox, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Main Tabbed Workspace
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JPanel studentWorkspace = createStudentWorkspaceTab();
        settingsWorkspace = createSettingsTab();

        mainTabbedPane.addTab("Tra Cứu / Quản Lý Sinh Viên", studentWorkspace);
        mainTabbedPane.addTab("Cấu Hình Kết Nối & CSDL", settingsWorkspace);

        add(mainTabbedPane, BorderLayout.CENTER);
        updateRoleUI();
    }

    private JPanel createStudentWorkspaceTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PTIT_LIGHT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Input Form Card (North)
        formPanel = new JPanel(new GridBagLayout());
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

        JButton btnViewTranscript = new JButton("Bảng Điểm Môn Học");
        btnViewTranscript.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnViewTranscript.setBackground(new Color(0, 123, 255));
        btnViewTranscript.setForeground(Color.WHITE);
        btnViewTranscript.addActionListener(e -> openTranscriptDialog());

        JButton btnGpaSimulator = new JButton("Giả Lập GPA");
        btnGpaSimulator.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnGpaSimulator.setBackground(new Color(111, 66, 193));
        btnGpaSimulator.setForeground(Color.WHITE);
        btnGpaSimulator.addActionListener(e -> openGpaSimulatorDialog());

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
        toolBar.add(btnViewTranscript);
        toolBar.add(btnGpaSimulator);
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

        // Click row: In Teacher Mode -> Auto-fill form. Double-click -> Open detailed transcript dialog
        tblResults.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = tblResults.getSelectedRow();
                if (row >= 0) {
                    if (currentRole == UserRole.TEACHER) {
                        txtStudentId.setText((String) tableModel.getValueAt(row, 1));
                        txtStudentName.setText((String) tableModel.getValueAt(row, 2));
                        txtScoreMath.setText((String) tableModel.getValueAt(row, 3));
                        txtScoreLit.setText((String) tableModel.getValueAt(row, 4));
                        txtScoreEng.setText((String) tableModel.getValueAt(row, 5));
                    }
                    if (e.getClickCount() == 2) {
                        openTranscriptDialog();
                    }
                }
            }
        });

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

    private void toggleRoleAuth() {
        if (currentRole == UserRole.STUDENT) {
            JPasswordField pf = new JPasswordField();
            int option = JOptionPane.showConfirmDialog(this, pf,
                    "Nhập Mật Khẩu Giảng Viên (Mặc định: ptit123):", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option == JOptionPane.OK_OPTION) {
                String pass = new String(pf.getPassword()).trim();
                if ("ptit123".equals(pass) || "admin123".equals(pass) || "123456".equals(pass)) {
                    currentRole = UserRole.TEACHER;
                    updateRoleUI();
                    JOptionPane.showMessageDialog(this,
                            "Xác thực THÀNH CÔNG!\nQuyền Giảng viên đã được mở khóa. Bạn có đầy đủ quyền Thêm, Sửa, Xóa và Cấu hình CSDL.",
                            "Bảo Mật Giảng Viên", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Mật khẩu Giảng viên KHÔNG CHÍNH XÁC!\nTruy cập bị từ chối. Chế độ Học sinh tiếp tục được bảo vệ.",
                            "Từ Chối Truy Cập", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có muốn thoát quyền Giảng viên và quay lại Chế độ Học sinh (Chỉ xem) không?",
                    "Đăng Xuất Quyền Giảng Viên", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                currentRole = UserRole.STUDENT;
                updateRoleUI();
                JOptionPane.showMessageDialog(this,
                        "Đã khóa tính năng Giảng viên. Chuyển sang Chế độ Học sinh (Chỉ xem).",
                        "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void updateRoleUI() {
        boolean isTeacher = (currentRole == UserRole.TEACHER);

        if (btnRoleAuth != null) {
            btnRoleAuth.setText(currentRole.getLabel());
            if (isTeacher) {
                btnRoleAuth.setBackground(new Color(40, 167, 69)); // Green for Teacher
            } else {
                btnRoleAuth.setBackground(new Color(220, 53, 69)); // Red for Student
            }
        }

        if (formPanel != null) {
            formPanel.setVisible(isTeacher); // Hide/Show student data entry form
        }

        if (btnDeleteSelected != null) {
            btnDeleteSelected.setEnabled(isTeacher);
            btnDeleteSelected.setToolTipText(isTeacher ? "Xóa dòng chọn" : "Tính năng yêu cầu Quyền Giảng viên");
        }

        if (mainTabbedPane != null && settingsWorkspace != null) {
            if (isTeacher) {
                if (mainTabbedPane.getTabCount() < 2) {
                    mainTabbedPane.addTab("Cấu Hình Kết Nối & CSDL", settingsWorkspace);
                }
            } else {
                if (mainTabbedPane.getTabCount() >= 2) {
                    mainTabbedPane.remove(1); // Remove Settings tab for Student mode
                }
            }
        }

        revalidate();
        repaint();
    }

    private void handleSendStudent() {
        if (currentRole != UserRole.TEACHER) {
            JOptionPane.showMessageDialog(this,
                    "Tính năng THÊM / SỬA SINH VIÊN bị khóa đối với Học sinh!\nVui lòng bấm '🔒 Quyền: Học sinh' góc trên để đăng nhập Quyền Giảng viên.",
                    "Bảo Mật Truy Cập", JOptionPane.WARNING_MESSAGE);
            return;
        }

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
        if (currentRole != UserRole.TEACHER) {
            JOptionPane.showMessageDialog(this,
                    "Tính năng XÓA SINH VIÊN bị khóa đối với Học sinh!\nVui lòng đăng nhập Quyền Giảng viên.",
                    "Bảo Mật Truy Cập", JOptionPane.WARNING_MESSAGE);
            return;
        }
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
        double maxGpa = 0.0, minGpa = 10.0, sumGpa = 0.0;
        if (!filtered.isEmpty()) {
            for (StudentResult s : filtered) {
                double avg = s.getAverageScore();
                if (avg > maxGpa) maxGpa = avg;
                if (avg < minGpa) minGpa = avg;
                sumGpa += avg;
            }
            double classAvg = sumGpa / filtered.size();
            lblTableSummary.setText(String.format("Tổng số: %d / %d sinh viên  |  ĐTB Cao nhất: %.2f  |  ĐTB Thấp nhất: %.2f  |  ĐTB Trung bình lớp: %.2f",
                    filtered.size(), currentStudentList.size(), maxGpa, minGpa, classAvg));
        } else {
            lblTableSummary.setText("Tổng số: 0 sinh viên");
        }
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

    private void openTranscriptDialog() {
        int row = tblResults.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 sinh viên trong bảng để xem phiếu điểm chi tiết!", "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String maSv = (String) tableModel.getValueAt(row, 1);
        String hoTen = (String) tableModel.getValueAt(row, 2);
        double math = 8.0, lit = 8.0, eng = 8.0, gpa = 8.0;
        try {
            math = Double.parseDouble((String) tableModel.getValueAt(row, 3));
            lit = Double.parseDouble((String) tableModel.getValueAt(row, 4));
            eng = Double.parseDouble((String) tableModel.getValueAt(row, 5));
            gpa = Double.parseDouble((String) tableModel.getValueAt(row, 6));
        } catch (Exception ignored) {}
        String xepLoai = (String) tableModel.getValueAt(row, 7);

        JDialog dialog = new JDialog(this, "PHIẾU BÁO ĐIỂM CHI TIẾT - " + hoTen + " (" + maSv + ")", true);
        dialog.setSize(760, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        // Header Dialog
        JPanel headPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        headPanel.setBackground(PTIT_RED);
        headPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JLabel title = new JLabel("HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG (PTIT)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel("PHIẾU BÁO KẾT QUẢ HỌC TẬP & ĐIỂM THI CHI TIẾT THEO HỌC KỲ");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(255, 230, 230));
        headPanel.add(title);
        headPanel.add(subtitle);
        dialog.add(headPanel, BorderLayout.NORTH);

        // Body Info & Table
        JPanel bodyPanel = new JPanel(new BorderLayout(8, 8));
        bodyPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        JPanel infoCard = new JPanel(new GridLayout(2, 3, 10, 5));
        infoCard.setBorder(BorderFactory.createTitledBorder("Thông Tin Sinh Viên"));
        infoCard.add(new JLabel("Mã SV: " + maSv));
        infoCard.add(new JLabel("Họ và Tên: " + hoTen));
        infoCard.add(new JLabel("Lớp: D21CQCN01-N"));
        infoCard.add(new JLabel("Điểm TB Tích Lũy: " + String.format("%.2f", gpa)));
        infoCard.add(new JLabel("Xếp Loại: " + xepLoai));
        infoCard.add(new JLabel("Trạng Thái: Đang học"));
        bodyPanel.add(infoCard, BorderLayout.NORTH);

        String[] subCols = {"STT", "Mã Môn", "Tên Môn Học", "Số TC", "Đ.Quá Trình (30%)", "Đ.Thi (70%)", "Đ.Tổng Kết", "Điểm Chữ"};
        DefaultTableModel subModel = new DefaultTableModel(subCols, 0);

        Object[][] subjects = {
                {1, "INT1339", "Lập Trình Mạng", 3, String.format("%.1f", math * 0.95), String.format("%.1f", math), String.format("%.2f", math), getGradeChar(math)},
                {2, "INT1340", "Mã Hóa & An Toàn Thông Tin", 3, String.format("%.1f", lit * 0.9), String.format("%.1f", lit), String.format("%.2f", lit), getGradeChar(lit)},
                {3, "INT1310", "Cơ Sở Dữ Liệu SQL", 4, String.format("%.1f", eng * 0.9), String.format("%.1f", eng), String.format("%.2f", eng), getGradeChar(eng)},
                {4, "INT1306", "Cấu Trúc Dữ Liệu & Giải Thuật", 3, String.format("%.1f", gpa * 0.92), String.format("%.1f", gpa), String.format("%.2f", gpa), getGradeChar(gpa)},
                {5, "SKD1102", "Kỹ Năng Giao Tiếp", 2, "9.0", "9.5", "9.35", "A+"}
        };
        for (Object[] rowData : subjects) {
            subModel.addRow(rowData);
        }

        JTable subTable = new JTable(subModel);
        subTable.setRowHeight(26);
        subTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        bodyPanel.add(new JScrollPane(subTable), BorderLayout.CENTER);

        dialog.add(bodyPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton btnPrint = new JButton("In / Xuất Báo Cáo Phiếu Điểm");
        btnPrint.setBackground(new Color(40, 167, 69));
        btnPrint.setForeground(Color.WHITE);
        btnPrint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPrint.addActionListener(e -> JOptionPane.showMessageDialog(dialog, "Đã gửi lệnh in phiếu điểm sinh viên " + hoTen + " (" + maSv + ")!", "Thông Báo", JOptionPane.INFORMATION_MESSAGE));

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.addActionListener(e -> dialog.dispose());

        btnBar.add(btnPrint);
        btnBar.add(btnClose);
        dialog.add(btnBar, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private String getGradeChar(double score) {
        if (score >= 9.0) return "A+";
        if (score >= 8.5) return "A";
        if (score >= 8.0) return "B+";
        if (score >= 7.0) return "B";
        if (score >= 6.5) return "C+";
        if (score >= 5.5) return "C";
        if (score >= 5.0) return "D+";
        if (score >= 4.0) return "D";
        return "F";
    }

    private void openGpaSimulatorDialog() {
        int row = tblResults.getSelectedRow();
        String name = (row >= 0) ? (String) tableModel.getValueAt(row, 2) : "Sinh viên";
        double currentGpa = 7.5;
        if (row >= 0) {
            try {
                currentGpa = Double.parseDouble((String) tableModel.getValueAt(row, 6));
            } catch (Exception ignored) {}
        }

        final double baseGpa = currentGpa;

        JDialog simDialog = new JDialog(this, "BỘ GIẢ LẬP ĐIỂM GPA SIMULATOR - PTIT", true);
        simDialog.setSize(620, 420);
        simDialog.setLocationRelativeTo(this);
        simDialog.setLayout(new BorderLayout(10, 10));

        JPanel head = new JPanel(new GridLayout(2, 1));
        head.setBackground(PTIT_RED);
        head.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JLabel lblT = new JLabel("BỘ GIẢ LẬP ĐIỂM GPA & DỰ ĐOÁN HỌC LỰC MỤC TIÊU");
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblT.setForeground(Color.WHITE);
        JLabel lblSub = new JLabel("Tính toán số điểm cần đạt để nâng hạng học lực (Xuất sắc, Giỏi, Khá)");
        lblSub.setForeground(new Color(255, 230, 230));
        head.add(lblT);
        head.add(lblSub);
        simDialog.add(head, BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(8, 10, 8, 10);
        g.fill = GridBagConstraints.HORIZONTAL;

        g.gridx = 0; g.gridy = 0;
        body.add(new JLabel("Sinh viên:"), g);
        g.gridx = 1; g.gridy = 0;
        JLabel lblStudent = new JLabel(name + " (ĐTB Hiện tại: " + String.format("%.2f", baseGpa) + ")");
        lblStudent.setFont(new Font("Segoe UI", Font.BOLD, 13));
        body.add(lblStudent, g);

        g.gridx = 0; g.gridy = 1;
        body.add(new JLabel("Mục tiêu xếp loại:"), g);
        g.gridx = 1; g.gridy = 1;
        JComboBox<String> cbTarget = new JComboBox<>(new String[]{"Xuất sắc (>= 9.0)", "Giỏi (>= 8.0)", "Khá (>= 6.5)"});
        body.add(cbTarget, g);

        g.gridx = 0; g.gridy = 2;
        body.add(new JLabel("Số tín chỉ môn sắp tới:"), g);
        g.gridx = 1; g.gridy = 2;
        JTextField txtCredits = new JTextField("6", 10);
        body.add(txtCredits, g);

        JTextArea txtAdvice = new JTextArea(4, 30);
        txtAdvice.setEditable(false);
        txtAdvice.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtAdvice.setBackground(new Color(245, 248, 255));
        txtAdvice.setBorder(BorderFactory.createTitledBorder("Kết Quả Dự Đoán Lộ Trình Học Tập"));
        txtAdvice.setText("Nhấn nút 'Tính Toán Lộ Trình' bên dưới để nhận kết quả phân tích.");

        g.gridx = 0; g.gridy = 3; g.gridwidth = 2;
        body.add(txtAdvice, g);

        simDialog.add(body, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton btnCalc = new JButton("Tính Toán Lộ Trình");
        btnCalc.setBackground(PTIT_RED);
        btnCalc.setForeground(Color.WHITE);
        btnCalc.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCalc.addActionListener(e -> {
            String target = (String) cbTarget.getSelectedItem();
            double targetScore = target.contains("9.0") ? 9.0 : (target.contains("8.0") ? 8.0 : 6.5);
            if (baseGpa >= targetScore) {
                txtAdvice.setText("Chúc mừng! Điểm hiện tại (" + String.format("%.2f", baseGpa) + ") đã đạt xếp loại mục tiêu.\nHãy duy trì phong độ tối thiểu " + String.format("%.2f", targetScore) + " điểm ở các môn tiếp theo!");
            } else {
                double diff = targetScore - baseGpa;
                double needed = baseGpa + (diff * 2.0);
                if (needed > 10.0) needed = 9.8;
                txtAdvice.setText(String.format("Để nâng hạng học lực lên %s:\n• Bạn cần đạt trung bình tối thiểu: %.2f điểm ở các môn học kỳ tiếp theo.\n• Lời khuyên: Tập trung cao vào môn Lập Trình Mạng & Mã Hóa CSDL!", target, needed));
            }
        });

        JButton btnClose = new JButton("Đóng");
        btnClose.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnClose.addActionListener(e -> simDialog.dispose());

        bottom.add(btnCalc);
        bottom.add(btnClose);
        simDialog.add(bottom, BorderLayout.SOUTH);

        simDialog.setVisible(true);
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
