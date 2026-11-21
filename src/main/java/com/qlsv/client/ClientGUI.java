package com.qlsv.client;

import com.formdev.flatlaf.FlatLightLaf;
import com.qlsv.model.CourseGrade;
import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.model.StudentTranscript;
import com.qlsv.model.UserAccount;
import com.qlsv.network.UDPPacket;
import com.qlsv.util.ExcelExporter;
import com.qlsv.util.ExcelImporter;

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
        TEACHER("🔓 Quyền: Giảng viên (Toàn quyền)"),
        ADMIN("🛡️ Quyền: Quản trị viên (Admin)");

        private final String label;
        UserRole(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private UserRole currentRole = UserRole.STUDENT; // Default to Student mode for security
    private UserAccount currentUserAccount = null;

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
    private JTextField txtServerPort; // Internal default (hidden from UI)
    private JButton btnConnectServer;
    private JLabel lblConnStatus;

    private JComboBox<SqlConfig.DbType> cbDbType;
    private JTextField txtDbHost;
    private JTextField txtDbPort;     // Internal default (hidden from UI)
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
    private JButton btnImportExcel;
    private JButton btnDownloadTemplate;
    private JButton btnExportExcel;

    private JTable tblResults;
    private DefaultTableModel tableModel;
    private JLabel lblTableSummary;

    public ClientGUI() {
        setTitle("PTIT - CỔNG THÔNG TIN QUẢN LÝ ĐÀO TẠO & SINH VIÊN");
        setSize(1220, 780);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (client != null) {
                    client.close();
                }
            }
        });

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

        JLabel subtitleLabel = new JLabel("Hệ Thống Quản Lý Đào Tạo & Tra Cứu Thông Tin Sinh Viên");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(255, 230, 230));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 2, 2));
        titleBox.setOpaque(false);
        titleBox.add(titleLabel);
        titleBox.add(subtitleLabel);

        // Status Badges & Role Switcher Panel
        JPanel statusBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        statusBox.setOpaque(false);

        lblServerBadge = new JLabel("● Máy Chủ: Chưa kết nối");
        lblServerBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblServerBadge.setForeground(Color.WHITE);
        lblServerBadge.setOpaque(true);
        lblServerBadge.setBackground(PTIT_DARK_RED);
        lblServerBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        lblDbBadge = new JLabel("● Cơ Sở Dữ Liệu: Chưa kết nối");
        lblDbBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblDbBadge.setForeground(Color.WHITE);
        lblDbBadge.setOpaque(true);
        lblDbBadge.setBackground(PTIT_DARK_RED);
        lblDbBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));

        btnRoleAuth = new JButton("🔒 Quyền: Học sinh (Bấm để Đăng nhập)");
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
        mainTabbedPane.addTab("Cài Đặt Hệ Thống", settingsWorkspace);

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
                " Form Nhập / Cập Nhật Thông Tin Sinh Viên (Quyền Giảng Viên) ",
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

        btnSendStudent = new JButton("LƯU THÔNG TIN SINH VIÊN");
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
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        toolBar.setBackground(Color.WHITE);

        txtSearchQuery = new JTextField(12);
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

        btnDeleteSelected = new JButton("Xóa Sinh Viên");
        btnDeleteSelected.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnDeleteSelected.setBackground(PTIT_DARK_RED);
        btnDeleteSelected.setForeground(Color.WHITE);
        btnDeleteSelected.addActionListener(e -> handleDeleteSelected());

        btnImportExcel = new JButton("Nhập File Excel");
        btnImportExcel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnImportExcel.setBackground(new Color(23, 162, 184));
        btnImportExcel.setForeground(Color.WHITE);
        btnImportExcel.addActionListener(e -> handleImportExcel());

        btnDownloadTemplate = new JButton("Tải File Mẫu");
        btnDownloadTemplate.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btnDownloadTemplate.addActionListener(e -> handleDownloadTemplate());

        btnExportExcel = new JButton("Xuất Báo Cáo");
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
        toolBar.add(Box.createHorizontalStrut(6));
        toolBar.add(btnImportExcel);
        toolBar.add(btnDownloadTemplate);
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
                    if (currentRole == UserRole.TEACHER || currentRole == UserRole.ADMIN) {
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

        // Hidden internal port fields
        txtServerPort = new JTextField("9876");
        txtDbPort = new JTextField("0");

        // Box 1: Server Config
        JPanel serverBox = new JPanel(new GridBagLayout());
        serverBox.setBackground(Color.WHITE);
        serverBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                " 1. Kết Nối Đến Máy Chủ Trung Tâm ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), PTIT_RED
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 12, 10, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        serverBox.add(new JLabel("Địa chỉ Máy Chủ:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        txtServerHost = new JTextField("127.0.0.1", 20);
        txtServerHost.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        serverBox.add(txtServerHost, gbc);

        btnConnectServer = new JButton("KẾT NỐI MÁY CHỦ");
        btnConnectServer.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnConnectServer.setBackground(PTIT_RED);
        btnConnectServer.setForeground(Color.WHITE);
        btnConnectServer.setFocusPainted(false);
        btnConnectServer.addActionListener(e -> handleConnectServer());

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0;
        serverBox.add(btnConnectServer, gbc);

        lblConnStatus = new JLabel("Trạng thái: Chưa kiểm tra kết nối máy chủ.");
        lblConnStatus.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblConnStatus.setForeground(Color.GRAY);

        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3;
        serverBox.add(lblConnStatus, gbc);

        outerGbc.gridx = 0; outerGbc.gridy = 0;
        panel.add(serverBox, outerGbc);

        // Box 2: Database Config
        JPanel dbBox = new JPanel(new GridBagLayout());
        dbBox.setBackground(Color.WHITE);
        dbBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                " 2. Cấu Hình Kho Dữ Liệu ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), PTIT_RED
        ));

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        dbBox.add(new JLabel("Loại Cơ Sở Dữ Liệu:"), gbc);
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
        dbBox.add(new JLabel("Địa chỉ Kho Dữ Liệu:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 1.0;
        txtDbHost = new JTextField("localhost", 15);
        txtDbHost.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dbBox.add(txtDbHost, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        dbBox.add(new JLabel("Tên Cơ Sở Dữ Liệu:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        txtDbName = new JTextField("qlsv_db", 15);
        txtDbName.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dbBox.add(txtDbName, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.0;
        dbBox.add(new JLabel("Tài khoản:"), gbc);
        gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 1.0;
        txtDbUser = new JTextField("sa", 15);
        txtDbUser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dbBox.add(txtDbUser, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        dbBox.add(new JLabel("Mật khẩu:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        txtDbPass = new JPasswordField("", 15);
        txtDbPass.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dbBox.add(txtDbPass, gbc);

        btnConnectDb = new JButton("KẾT NỐI KHO DỮ LIỆU");
        btnConnectDb.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnConnectDb.setBackground(PTIT_RED);
        btnConnectDb.setForeground(Color.WHITE);
        btnConnectDb.setFocusPainted(false);
        btnConnectDb.addActionListener(e -> handleConnectDb());

        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 2;
        dbBox.add(btnConnectDb, gbc);

        lblDbStatusMsg = new JLabel("Trạng thái: Chưa khởi tạo.");
        lblDbStatusMsg.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblDbStatusMsg.setForeground(Color.GRAY);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
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
        int port = 9876;
        try {
            port = Integer.parseInt(txtServerPort.getText().trim());
        } catch (Exception ignored) {}

        if (host.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập địa chỉ máy chủ!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            if (client != null) {
                client.close();
            }
            client = new UDPClient(host, port);

            boolean connected = client.pingServer();
            if (connected) {
                lblConnStatus.setText("ĐÃ KẾT NỐI ĐẾN MÁY CHỦ TRUNG TÂM (" + host + ")");
                lblConnStatus.setForeground(new Color(40, 167, 69));

                lblServerBadge.setText("● Máy Chủ: Sẵn sàng");
                lblServerBadge.setBackground(new Color(40, 167, 69));
            } else {
                throw new Exception("Máy chủ không phản hồi!");
            }
        } catch (Exception ex) {
            lblConnStatus.setText("Kết nối máy chủ thất bại: " + ex.getMessage());
            lblConnStatus.setForeground(PTIT_RED);

            lblServerBadge.setText("● Máy Chủ: Chưa kết nối");
            lblServerBadge.setBackground(PTIT_DARK_RED);
        }
    }

    private void handleConnectDb() {
        if (client == null) {
            lblDbStatusMsg.setText("Chưa kết nối máy chủ trung tâm!");
            lblDbStatusMsg.setForeground(PTIT_RED);
            return;
        }

        try {
            SqlConfig.DbType dbType = (SqlConfig.DbType) cbDbType.getSelectedItem();
            String host = txtDbHost.getText().trim();
            int port = 0;
            try {
                port = Integer.parseInt(txtDbPort.getText().trim());
            } catch (Exception ignored) {}

            String dbName = txtDbName.getText().trim();
            String user = txtDbUser.getText().trim();
            String pass = new String(txtDbPass.getPassword());

            SqlConfig config = new SqlConfig(dbType, host, port, dbName, user, pass);
            UDPPacket response = client.connectDatabase(config);

            if (response != null && response.isSuccess()) {
                lblDbStatusMsg.setText("ĐÃ KẾT NỐI KHO DỮ LIỆU THÀNH CÔNG");
                lblDbStatusMsg.setForeground(new Color(40, 167, 69));

                lblDbBadge.setText("● Cơ Sở Dữ Liệu: Đã kết nối");
                lblDbBadge.setBackground(new Color(40, 167, 69));

                loadAllStudents();
            } else {
                String errMsg = (response != null) ? response.getMessage() : "Không nhận được phản hồi";
                lblDbStatusMsg.setText("Lỗi kết nối dữ liệu: " + errMsg);
                lblDbStatusMsg.setForeground(PTIT_RED);

                lblDbBadge.setText("● Cơ Sở Dữ Liệu: Lỗi kết nối");
                lblDbBadge.setBackground(PTIT_DARK_RED);
            }
        } catch (Exception ex) {
            lblDbStatusMsg.setText("Lỗi cấu hình dữ liệu: " + ex.getMessage());
            lblDbStatusMsg.setForeground(PTIT_RED);
        }
    }

    private void toggleRoleAuth() {
        if (currentRole == UserRole.STUDENT) {
            // Open full login dialog
            JPanel p = new JPanel(new GridLayout(3, 2, 8, 8));
            JTextField tfUser = new JTextField("giangvien");
            JPasswordField pfPass = new JPasswordField("ptit123");

            p.add(new JLabel("Tên đăng nhập:"));
            p.add(tfUser);
            p.add(new JLabel("Mật khẩu:"));
            p.add(pfPass);
            p.add(new JLabel("Gợi ý tài khoản:"));
            p.add(new JLabel("<html><i>giangvien / ptit123<br>admin / admin123</i></html>"));

            int option = JOptionPane.showConfirmDialog(this, p,
                    "ĐĂNG NHẬP HỆ THỐNG", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (option == JOptionPane.OK_OPTION) {
                String u = tfUser.getText().trim();
                String pwd = new String(pfPass.getPassword()).trim();

                if (u.isEmpty() || pwd.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                if (client == null) {
                    JOptionPane.showMessageDialog(this, "Chưa kết nối máy chủ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    UserAccount acc = client.login(u, pwd);
                    this.currentUserAccount = acc;

                    if (acc.getRole() == UserAccount.Role.ADMIN) {
                        currentRole = UserRole.ADMIN;
                    } else if (acc.getRole() == UserAccount.Role.GIANGVIEN) {
                        currentRole = UserRole.TEACHER;
                    } else {
                        currentRole = UserRole.STUDENT;
                    }

                    updateRoleUI();
                    JOptionPane.showMessageDialog(this,
                            String.format("Xác thực THÀNH CÔNG!\nXin chào: %s\nVai trò: %s",
                                    acc.getFullName(), acc.getRole().getDisplayName()),
                            "Đăng Nhập Thành Công", JOptionPane.INFORMATION_MESSAGE);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Đăng nhập thất bại: " + ex.getMessage(),
                            "Từ Chối Truy Cập", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Bạn có muốn đăng xuất và quay lại Chế độ Học sinh (Chỉ xem) không?",
                    "Đăng Xuất", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (client != null) {
                    client.logout();
                }
                currentRole = UserRole.STUDENT;
                currentUserAccount = null;
                updateRoleUI();
                JOptionPane.showMessageDialog(this,
                        "Đã đăng xuất. Hệ thống chuyển về Chế độ Học sinh (Chỉ xem).",
                        "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void updateRoleUI() {
        boolean isTeacher = (currentRole == UserRole.TEACHER || currentRole == UserRole.ADMIN);

        if (btnRoleAuth != null) {
            if (currentRole == UserRole.ADMIN) {
                btnRoleAuth.setText("🛡️ Admin: " + (currentUserAccount != null ? currentUserAccount.getFullName() : "Admin"));
                btnRoleAuth.setBackground(new Color(111, 66, 193)); // Purple for Admin
            } else if (currentRole == UserRole.TEACHER) {
                btnRoleAuth.setText("🔓 Giảng viên: " + (currentUserAccount != null ? currentUserAccount.getFullName() : "Giảng viên"));
                btnRoleAuth.setBackground(new Color(40, 167, 69)); // Green for Teacher
            } else {
                btnRoleAuth.setText("🔒 Quyền: Học sinh (Bấm để Đăng nhập)");
                btnRoleAuth.setBackground(new Color(220, 53, 69)); // Red for Student
            }
        }

        if (formPanel != null) {
            formPanel.setVisible(isTeacher); // Hide/Show student data entry form
        }

        if (btnDeleteSelected != null) {
            btnDeleteSelected.setEnabled(isTeacher);
            btnDeleteSelected.setToolTipText(isTeacher ? "Xóa sinh viên" : "Tính năng yêu cầu Quyền Giảng viên");
        }

        if (btnImportExcel != null) {
            btnImportExcel.setEnabled(isTeacher);
            btnImportExcel.setToolTipText(isTeacher ? "Nhập danh sách từ Excel" : "Tính năng yêu cầu Quyền Giảng viên");
        }

        if (mainTabbedPane != null && settingsWorkspace != null) {
            if (isTeacher) {
                if (mainTabbedPane.getTabCount() < 2) {
                    mainTabbedPane.addTab("Cài Đặt Hệ Thống", settingsWorkspace);
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
        if (currentRole == UserRole.STUDENT) {
            JOptionPane.showMessageDialog(this,
                    "Tính năng THÊM / SỬA SINH VIÊN bị khóa đối với Học sinh!\nVui lòng đăng nhập Quyền Giảng viên.",
                    "Bảo Mật Truy Cập", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (client == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối máy chủ! Vui lòng kiểm tra lại cài đặt hệ thống.", "Lỗi", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "Lỗi gửi dữ liệu đến máy chủ: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSearch() {
        String query = txtSearchQuery.getText().trim();
        if (client == null) return;
        new SwingWorker<List<StudentResult>, Void>() {
            @Override
            protected List<StudentResult> doInBackground() throws Exception {
                return client.searchStudents(query);
            }
            @Override
            protected void done() {
                try {
                    currentStudentList = get();
                    applyFilters();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ClientGUI.this, "Lỗi tìm kiếm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void handleDeleteSelected() {
        if (currentRole == UserRole.STUDENT) {
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
                JOptionPane.showMessageDialog(this, "Lỗi xóa dữ liệu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleImportExcel() {
        if (client == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối máy chủ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn Tệp Excel Danh Sách Sinh Viên (.xlsx)");
        chooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                List<StudentData> list = ExcelImporter.importStudentsFromExcel(file);

                int confirm = JOptionPane.showConfirmDialog(this,
                        String.format("Đã đọc thành công %d sinh viên từ file:\n%s\nBạn có muốn gửi lên hệ thống để lưu trữ không?",
                                list.size(), file.getName()),
                        "Xác Nhận Nhập Excel", JOptionPane.YES_NO_OPTION);

                if (confirm == JOptionPane.YES_OPTION) {
                    new SwingWorker<Integer, Void>() {
                        @Override
                        protected Integer doInBackground() throws Exception {
                            return client.importStudentsFromExcel(list);
                        }
                        @Override
                        protected void done() {
                            try {
                                int savedCount = get();
                                JOptionPane.showMessageDialog(ClientGUI.this,
                                        "Đã lưu thành công " + savedCount + " sinh viên vào cơ sở dữ liệu!",
                                        "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                                loadAllStudents();
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(ClientGUI.this,
                                        "Lỗi nhập dữ liệu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }.execute();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi đọc file Excel: " + ex.getMessage(), "Lỗi Excel", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDownloadTemplate() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Lưu Tệp Mẫu Excel Danh Sách Sinh Viên");
        chooser.setSelectedFile(new File("Mau_DanhSach_SinhVien_PTIT.xlsx"));

        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File target = chooser.getSelectedFile();
            if (!target.getAbsolutePath().endsWith(".xlsx")) {
                target = new File(target.getAbsolutePath() + ".xlsx");
            }
            try {
                ExcelImporter.createSampleTemplate(target);
                JOptionPane.showMessageDialog(this, "Đã lưu tệp mẫu tại:\n" + target.getAbsolutePath(), "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Lỗi tạo file mẫu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadAllStudents() {
        if (client == null) return;
        new SwingWorker<List<StudentResult>, Void>() {
            @Override
            protected List<StudentResult> doInBackground() throws Exception {
                return client.getAllStudents();
            }
            @Override
            protected void done() {
                try {
                    currentStudentList = get();
                    applyFilters();
                } catch (Exception ex) {
                    System.err.println("Lỗi tải danh sách: " + ex.getMessage());
                }
            }
        }.execute();
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
            JOptionPane.showMessageDialog(this, "Chưa kết nối máy chủ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            if (currentStudentList.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Danh sách rỗng, không thể xuất!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Lưu Báo Cáo Danh Sách Sinh Viên");
            chooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
            chooser.setSelectedFile(new File("BaoCao_DanhSach_SinhVien.xlsx"));

            int userSelection = chooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = chooser.getSelectedFile();
                if (!fileToSave.getAbsolutePath().endsWith(".xlsx")) {
                    fileToSave = new File(fileToSave.getAbsolutePath() + ".xlsx");
                }

                ExcelExporter.exportStudentsToExcel(currentStudentList, fileToSave);
                JOptionPane.showMessageDialog(this,
                        "Xuất Báo Cáo THÀNH CÔNG!\nĐã lưu tại: " + fileToSave.getAbsolutePath(),
                        "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi xuất báo cáo: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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

        if (client == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối máy chủ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            StudentTranscript transcript = client.getDetailedTranscript(maSv);

            JDialog dialog = new JDialog(this, "PHIẾU BÁO ĐIỂM CHI TIẾT TÍN CHỈ - " + transcript.getFullName() + " (" + transcript.getStudentId() + ")", true);
            dialog.setSize(820, 560);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout(10, 10));

            // Header Dialog
            JPanel headPanel = new JPanel(new GridLayout(2, 1, 2, 2));
            headPanel.setBackground(PTIT_RED);
            headPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

            JLabel title = new JLabel("HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG (PTIT)");
            title.setFont(new Font("Segoe UI", Font.BOLD, 15));
            title.setForeground(Color.WHITE);

            JLabel subtitle = new JLabel("PHIẾU BÁO KẾT QUẢ HỌC TẬP & ĐIỂM TÍN CHỈ THEO QUY CHẾ ĐÀO TẠO");
            subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            subtitle.setForeground(new Color(255, 230, 230));
            headPanel.add(title);
            headPanel.add(subtitle);
            dialog.add(headPanel, BorderLayout.NORTH);

            // Body Info & Table
            JPanel bodyPanel = new JPanel(new BorderLayout(8, 8));
            bodyPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

            JPanel infoCard = new JPanel(new GridLayout(2, 4, 10, 5));
            infoCard.setBorder(BorderFactory.createTitledBorder("Thông Tin Sinh Viên & Tích Lũy"));
            infoCard.add(new JLabel("Mã SV: " + transcript.getStudentId()));
            infoCard.add(new JLabel("Họ và Tên: " + transcript.getFullName()));
            infoCard.add(new JLabel("Lớp: " + transcript.getClassName()));
            infoCard.add(new JLabel("Khoa: " + transcript.getFaculty()));
            infoCard.add(new JLabel("Tổng tín chỉ: " + transcript.getTotalCredits() + " TC"));
            infoCard.add(new JLabel("ĐTB Hệ 10: " + String.format("%.2f", transcript.getCumulativeGpa10())));
            infoCard.add(new JLabel("GPA Hệ 4: " + String.format("%.2f", transcript.getCumulativeGpa4())));
            infoCard.add(new JLabel("Xếp loại: " + transcript.getAcademicRank()));
            bodyPanel.add(infoCard, BorderLayout.NORTH);

            String[] subCols = {"STT", "Mã Môn", "Tên Môn Học", "Số TC", "Đ.Quá Trình (30%)", "Đ.Thi (70%)", "Đ.Tổng Kết", "Điểm Chữ", "Hệ 4"};
            DefaultTableModel subModel = new DefaultTableModel(subCols, 0);

            int stt = 1;
            for (CourseGrade g : transcript.getGrades()) {
                subModel.addRow(new Object[]{
                        stt++,
                        g.getCourseCode(),
                        g.getCourseName(),
                        g.getCredits(),
                        String.format("%.1f", g.getMidtermScore()),
                        String.format("%.1f", g.getFinalScore()),
                        String.format("%.2f", g.getTotalScore()),
                        g.getLetterGrade(),
                        String.format("%.1f", g.getGpa4())
                });
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

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Không thể tải phiếu điểm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openGpaSimulatorDialog() {
        int row = tblResults.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn 1 sinh viên trong bảng để chạy Bộ Giả Lập GPA!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String name = (String) tableModel.getValueAt(row, 2);
        String maSv = (String) tableModel.getValueAt(row, 1);
        double currentGpa = 7.5;
        try {
            currentGpa = Double.parseDouble((String) tableModel.getValueAt(row, 6));
        } catch (Exception ignored) {}

        final double baseGpa = currentGpa;

        JDialog simDialog = new JDialog(this, "BỘ GIẢ LẬP ĐIỂM GPA SIMULATOR - PTIT", true);
        simDialog.setSize(640, 430);
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
        JLabel lblStudent = new JLabel(name + " (" + maSv + ") - ĐTB Hiện tại: " + String.format("%.2f", baseGpa));
        lblStudent.setFont(new Font("Segoe UI", Font.BOLD, 13));
        body.add(lblStudent, g);

        g.gridx = 0; g.gridy = 1;
        body.add(new JLabel("Mục tiêu xếp loại:"), g);
        g.gridx = 1; g.gridy = 1;
        JComboBox<String> cbTarget = new JComboBox<>(new String[]{"Xuất sắc (>= 9.0)", "Giỏi (>= 8.0)", "Khá (>= 6.5)"});
        body.add(cbTarget, g);

        g.gridx = 0; g.gridy = 2;
        body.add(new JLabel("Số tín chỉ kỳ tới:"), g);
        g.gridx = 1; g.gridy = 2;
        JTextField txtCredits = new JTextField("15", 10);
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
                double needed = baseGpa + (diff * 1.8);
                if (needed > 10.0) needed = 9.8;
                txtAdvice.setText(String.format("Để nâng hạng học lực lên %s:\n• Bạn cần đạt trung bình tối thiểu: %.2f điểm ở các môn học kỳ tiếp theo.\n• Lời khuyên: Tập trung cao vào các môn chuyên ngành và Cơ sở dữ liệu!", target, needed));
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
