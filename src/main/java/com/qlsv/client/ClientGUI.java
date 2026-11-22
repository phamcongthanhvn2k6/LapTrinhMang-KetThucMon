package com.qlsv.client;

import com.formdev.flatlaf.FlatLightLaf;
import com.qlsv.AppLauncher;
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

    // Pagination Controls
    private int currentPage = 1;
    private int pageSize = 20;
    private int totalPages = 1;
    private int totalStudents = 0;
    private JButton btnFirstPage;
    private JButton btnPrevPage;
    private JButton btnNextPage;
    private JButton btnLastPage;
    private JLabel lblPageInfo;
    private JComboBox<String> cbPageSize;

    // Student Portal Controls
    private JTextField txtPortalSearchMaSV;
    private JLabel lblPortalHoTen;
    private JLabel lblPortalMaSV;
    private JLabel lblPortalLop;
    private JLabel lblPortalKhoa;
    private JLabel lblPortalNienKhoa;
    private JLabel lblPortalStatus;
    private JLabel lblPortalCredits;
    private JLabel lblPortalGpa10;
    private JLabel lblPortalGpa4;
    private JLabel lblPortalRank;
    private DefaultTableModel portalTableModel;
    private JTable tblPortalGrades;
    private StudentTranscript currentPortalTranscript;

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

        // Menubar Trình Diễn Demo
        setJMenuBar(createDemoMenuBar());

        // Status Badges & Role Switcher Panel
        JPanel statusBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        statusBox.setOpaque(false);

        JButton btnDemoParallel = new JButton("🎬 Xếp Demo Song Song");
        btnDemoParallel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnDemoParallel.setBackground(new Color(255, 193, 7)); // Gold
        btnDemoParallel.setForeground(Color.BLACK);
        btnDemoParallel.setFocusPainted(false);
        btnDemoParallel.setToolTipText("Mở và tự động chia màn hình: Server + Giảng viên + Sinh viên");
        btnDemoParallel.addActionListener(e -> AppLauncher.launchDemoParallelLayout());

        JButton btnQuickSwitch = new JButton("⚡ Đổi Vai Trò Nhanh ▼");
        btnQuickSwitch.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnQuickSwitch.setBackground(Color.WHITE);
        btnQuickSwitch.setForeground(PTIT_RED);
        btnQuickSwitch.setFocusPainted(false);
        btnQuickSwitch.addActionListener(e -> {
            JPopupMenu pop = new JPopupMenu();
            JMenuItem m1 = new JMenuItem("🎓 Sang Cổng Sinh Viên (SV001)");
            m1.addActionListener(evt -> switchDemoRole(UserRole.STUDENT, "SV001"));
            JMenuItem m2 = new JMenuItem("👨‍🏫 Sang Quyền Giảng Viên (Chấm Điểm)");
            m2.addActionListener(evt -> switchDemoRole(UserRole.TEACHER, null));
            JMenuItem m3 = new JMenuItem("🛡️ Sang Quyền Admin (Toàn Quyền)");
            m3.addActionListener(evt -> switchDemoRole(UserRole.ADMIN, null));
            JMenuItem m4 = new JMenuItem("🔑 Hộp thoại đăng nhập...");
            m4.addActionListener(evt -> toggleRoleAuth());
            pop.add(m1);
            pop.add(m2);
            pop.add(m3);
            pop.addSeparator();
            pop.add(m4);
            pop.show(btnQuickSwitch, 0, btnQuickSwitch.getHeight());
        });

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

        btnRoleAuth = new JButton("[Quyền: Học Sinh - Nhấn để Đăng nhập]");
        btnRoleAuth.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnRoleAuth.setBackground(new Color(220, 53, 69));
        btnRoleAuth.setForeground(Color.WHITE);
        btnRoleAuth.setFocusPainted(false);
        btnRoleAuth.addActionListener(e -> toggleRoleAuth());

        statusBox.add(btnDemoParallel);
        statusBox.add(btnQuickSwitch);
        statusBox.add(lblServerBadge);
        statusBox.add(lblDbBadge);
        statusBox.add(btnRoleAuth);

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(statusBox, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // Main Tabbed Workspace
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JPanel studentPortalTab = createStudentPortalTab();
        JPanel managementWorkspace = createStudentWorkspaceTab();
        settingsWorkspace = createSettingsTab();

        mainTabbedPane.addTab("🎓 Cổng Thông Tin Sinh Viên", studentPortalTab);
        mainTabbedPane.addTab("📋 Quản Lý Đào Tạo & Lớp Học Phần", managementWorkspace);
        mainTabbedPane.addTab("⚙️ Cài Đặt Hệ Thống", settingsWorkspace);

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
        formPanel.add(new JLabel("Điểm Chuyên cần (10%):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        txtScoreMath = new JTextField(10);
        txtScoreMath.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(txtScoreMath, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Điểm Giữa kỳ (20%):"), gbc);
        gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 1.0;
        txtScoreLit = new JTextField(10);
        txtScoreLit.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        formPanel.add(txtScoreLit, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Điểm Thi kết thúc (70%):"), gbc);
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
        txtSearchQuery.setToolTipText("Nhập Mã SV hoặc Tên rồi nhấn Enter để tìm kiếm");
        txtSearchQuery.addActionListener(e -> handleSearch());
        btnSearch = new JButton("Tìm Kiếm");
        btnSearch.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnSearch.addActionListener(e -> handleSearch());

        cbFilterRank = new JComboBox<>(new String[]{
                "Tất cả xếp loại", "Xuất sắc (A/A+)", "Giỏi (B+)", "Khá (B)", "Trung bình (C/D)", "Cảnh báo học vụ (Yếu/Kém)"
        });
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
        String[] columnNames = {
                "STT", "Mã SV", "Họ và Tên", "Lớp Học Phần",
                "Chuyên Cần (10%)", "Giữa Kỳ (20%)", "Thi Kết Thúc (70%)",
                "Điểm HP (10)", "GPA Hệ 4", "Điểm Chữ", "Xếp Loại", "Trạng Thái"
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblResults = new JTable(tableModel);
        tblResults.setRowHeight(28);
        tblResults.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblResults.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblResults.getTableHeader().setBackground(new Color(240, 242, 245));
        tblResults.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Alignment & Widths
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int col = 0; col < columnNames.length; col++) {
            if (col != 2) {
                tblResults.getColumnModel().getColumn(col).setCellRenderer(centerRenderer);
            }
        }

        // Custom Highlight Renderer for Trạng Thái (Col 11) and Xếp Loại (Col 10)
        tblResults.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                String val = value != null ? value.toString() : "";
                if (!isSelected) {
                    if (val.contains("Xuất sắc") || val.contains("Giỏi")) {
                        setForeground(new Color(0, 130, 50));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else if (val.contains("Yếu") || val.contains("Kém")) {
                        setForeground(new Color(200, 16, 46));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(Color.BLACK);
                    }
                }
                return c;
            }
        });

        tblResults.getColumnModel().getColumn(11).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);
                String val = value != null ? value.toString() : "";
                if (!isSelected) {
                    if (val.contains("Cảnh báo")) {
                        setForeground(new Color(200, 16, 46));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(new Color(0, 120, 40));
                    }
                }
                return c;
            }
        });

        tblResults.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblResults.getColumnModel().getColumn(1).setPreferredWidth(90);
        tblResults.getColumnModel().getColumn(2).setPreferredWidth(170);
        tblResults.getColumnModel().getColumn(3).setPreferredWidth(110);
        tblResults.getColumnModel().getColumn(4).setPreferredWidth(110);
        tblResults.getColumnModel().getColumn(5).setPreferredWidth(100);
        tblResults.getColumnModel().getColumn(6).setPreferredWidth(110);
        tblResults.getColumnModel().getColumn(7).setPreferredWidth(90);
        tblResults.getColumnModel().getColumn(8).setPreferredWidth(75);
        tblResults.getColumnModel().getColumn(9).setPreferredWidth(75);
        tblResults.getColumnModel().getColumn(10).setPreferredWidth(100);
        tblResults.getColumnModel().getColumn(11).setPreferredWidth(125);

        // Click row: In Teacher Mode -> Auto-fill form. Double-click -> Open detailed transcript dialog
        tblResults.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = tblResults.getSelectedRow();
                if (row >= 0) {
                    if (currentRole == UserRole.TEACHER || currentRole == UserRole.ADMIN) {
                        txtStudentId.setText(String.valueOf(tableModel.getValueAt(row, 1)));
                        txtStudentName.setText(String.valueOf(tableModel.getValueAt(row, 2)));
                        txtScoreMath.setText(String.valueOf(tableModel.getValueAt(row, 4)));
                        txtScoreLit.setText(String.valueOf(tableModel.getValueAt(row, 5)));
                        txtScoreEng.setText(String.valueOf(tableModel.getValueAt(row, 6)));
                    }
                    if (e.getClickCount() == 2) {
                        openTranscriptDialog();
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblResults);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // Pagination & Footer Bar
        JPanel bottomBar = new JPanel(new BorderLayout(10, 0));
        bottomBar.setBackground(Color.WHITE);
        bottomBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(225, 225, 225)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
        ));

        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        paginationPanel.setOpaque(false);

        btnFirstPage = new JButton("<< Đầu");
        btnFirstPage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnFirstPage.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage = 1;
                loadAllStudents();
            }
        });

        btnPrevPage = new JButton("< Trước");
        btnPrevPage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnPrevPage.addActionListener(e -> {
            if (currentPage > 1) {
                currentPage--;
                loadAllStudents();
            }
        });

        lblPageInfo = new JLabel("Trang 1 / 1");
        lblPageInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));

        btnNextPage = new JButton("Sau >");
        btnNextPage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnNextPage.addActionListener(e -> {
            if (currentPage < totalPages) {
                currentPage++;
                loadAllStudents();
            }
        });

        btnLastPage = new JButton("Cuối >>");
        btnLastPage.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnLastPage.addActionListener(e -> {
            if (currentPage < totalPages) {
                currentPage = totalPages;
                loadAllStudents();
            }
        });

        cbPageSize = new JComboBox<>(new String[]{"20 SV / Trang", "50 SV / Trang", "100 SV / Trang"});
        cbPageSize.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cbPageSize.addActionListener(e -> {
            int idx = cbPageSize.getSelectedIndex();
            if (idx == 1) pageSize = 50;
            else if (idx == 2) pageSize = 100;
            else pageSize = 20;
            currentPage = 1;
            loadAllStudents();
        });

        paginationPanel.add(btnFirstPage);
        paginationPanel.add(btnPrevPage);
        paginationPanel.add(Box.createHorizontalStrut(5));
        paginationPanel.add(lblPageInfo);
        paginationPanel.add(Box.createHorizontalStrut(5));
        paginationPanel.add(btnNextPage);
        paginationPanel.add(btnLastPage);
        paginationPanel.add(Box.createHorizontalStrut(15));
        paginationPanel.add(new JLabel("Hiển thị:"));
        paginationPanel.add(cbPageSize);

        lblTableSummary = new JLabel("Tổng số: 0 sinh viên", SwingConstants.RIGHT);
        lblTableSummary.setFont(new Font("Segoe UI", Font.BOLD, 12));

        bottomBar.add(paginationPanel, BorderLayout.WEST);
        bottomBar.add(lblTableSummary, BorderLayout.EAST);
        centerPanel.add(bottomBar, BorderLayout.SOUTH);

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

                lblDbBadge.setText("● Cơ Sở Dữ Liệu: Đã kết nối");
                lblDbBadge.setBackground(new Color(40, 167, 69));

                loadAllStudents();
                loadStudentPortal("SV001");
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
                    loadAllStudents();
                    if (currentRole == UserRole.TEACHER || currentRole == UserRole.ADMIN) {
                        mainTabbedPane.setSelectedIndex(1); // Quản lý
                    } else {
                        mainTabbedPane.setSelectedIndex(0); // Cổng sinh viên
                        if (acc.getStudentId() != null && !acc.getStudentId().isEmpty()) {
                            loadStudentPortal(acc.getStudentId());
                        }
                    }
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

    public void switchDemoRole(UserRole role, String defaultId) {
        if (role == UserRole.STUDENT) {
            if (client != null) {
                client.logout();
            }
            this.currentRole = UserRole.STUDENT;
            this.currentUserAccount = null;
            updateRoleUI();
            if (mainTabbedPane != null) {
                mainTabbedPane.setSelectedIndex(0);
            }
            loadStudentPortal(defaultId != null && !defaultId.isEmpty() ? defaultId : "SV001");
        } else if (role == UserRole.TEACHER) {
            if (client != null) {
                try {
                    this.currentUserAccount = client.login("giangvien", "ptit123");
                } catch (Exception ignored) {
                    this.currentUserAccount = new UserAccount("giangvien", "ThS. Giảng Viên Mạng", UserAccount.Role.GIANGVIEN, "");
                }
            } else {
                this.currentUserAccount = new UserAccount("giangvien", "ThS. Giảng Viên Mạng", UserAccount.Role.GIANGVIEN, "");
            }
            this.currentRole = UserRole.TEACHER;
            updateRoleUI();
            if (mainTabbedPane != null) {
                mainTabbedPane.setSelectedIndex(1);
            }
            loadAllStudents();
        } else if (role == UserRole.ADMIN) {
            if (client != null) {
                try {
                    this.currentUserAccount = client.login("admin", "admin123");
                } catch (Exception ignored) {
                    this.currentUserAccount = new UserAccount("admin", "Quản Trị Viên Hệ Thống", UserAccount.Role.ADMIN, "");
                }
            } else {
                this.currentUserAccount = new UserAccount("admin", "Quản Trị Viên Hệ Thống", UserAccount.Role.ADMIN, "");
            }
            this.currentRole = UserRole.ADMIN;
            updateRoleUI();
            if (mainTabbedPane != null) {
                mainTabbedPane.setSelectedIndex(1);
            }
            loadAllStudents();
        }
    }

    private JMenuBar createDemoMenuBar() {
        JMenuBar mb = new JMenuBar();

        // 1. Menu Vai Trò
        JMenu menuRole = new JMenu("⚡ Chuyển Nhanh Vai Trò");
        menuRole.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JMenuItem miStudent = new JMenuItem("🎓 Chuyển sang Cổng Sinh Viên (SV001)");
        miStudent.addActionListener(e -> switchDemoRole(UserRole.STUDENT, "SV001"));

        JMenuItem miTeacher = new JMenuItem("👨‍🏫 Chuyển sang Quyền Giảng Viên (Chấm điểm & Quản lý)");
        miTeacher.addActionListener(e -> switchDemoRole(UserRole.TEACHER, null));

        JMenuItem miAdmin = new JMenuItem("🛡️ Chuyển sang Quyền Admin (Toàn quyền)");
        miAdmin.addActionListener(e -> switchDemoRole(UserRole.ADMIN, null));

        JMenuItem miAuthDialog = new JMenuItem("🔑 Hộp thoại đăng nhập...");
        miAuthDialog.addActionListener(e -> toggleRoleAuth());

        menuRole.add(miStudent);
        menuRole.add(miTeacher);
        menuRole.add(miAdmin);
        menuRole.addSeparator();
        menuRole.add(miAuthDialog);

        // 2. Menu Điều Khiển Demo
        JMenu menuDemo = new JMenu("🎛️ Bảng Điều Khiển Demo");
        menuDemo.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JMenuItem miParallel = new JMenuItem("🎬 Chế độ Demo song song (Xếp Server + Giảng viên + Sinh viên)");
        miParallel.addActionListener(e -> AppLauncher.launchDemoParallelLayout());

        JMenuItem miTile = new JMenuItem("🔄 Tự động sắp xếp lại cửa sổ trên màn hình");
        miTile.addActionListener(e -> AppLauncher.tileAllWindows());

        JMenuItem miOpenServer = new JMenuItem("🖥️ Hiện / Mở Trung Tâm Máy Chủ Server");
        miOpenServer.addActionListener(e -> AppLauncher.launchOrFocusServer());

        JMenuItem miNewStudent = new JMenuItem("➕ Mở thêm 1 Cửa Sổ Cổng Sinh Viên");
        miNewStudent.addActionListener(e -> AppLauncher.launchStudentClient());

        JMenuItem miNewTeacher = new JMenuItem("➕ Mở thêm 1 Cửa Sổ Giảng Viên");
        miNewTeacher.addActionListener(e -> AppLauncher.launchTeacherClient());

        JMenuItem miOpenLauncher = new JMenuItem("🎛️ Mở Bảng Điều Khiển Launcher PTIT");
        miOpenLauncher.addActionListener(e -> AppLauncher.showLauncher());

        menuDemo.add(miParallel);
        menuDemo.add(miTile);
        menuDemo.addSeparator();
        menuDemo.add(miOpenServer);
        menuDemo.add(miNewStudent);
        menuDemo.add(miNewTeacher);
        menuDemo.addSeparator();
        menuDemo.add(miOpenLauncher);

        // 3. Menu Dữ liệu & Làm mới
        JMenu menuData = new JMenu("🔄 Dữ Liệu");
        menuData.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        JMenuItem miRefresh = new JMenuItem("🔄 Tải lại dữ liệu từ Server RUDP");
        miRefresh.addActionListener(e -> {
            loadAllStudents();
            if (txtPortalSearchMaSV != null && !txtPortalSearchMaSV.getText().trim().isEmpty()) {
                loadStudentPortal(txtPortalSearchMaSV.getText().trim());
            }
        });
        menuData.add(miRefresh);

        mb.add(menuRole);
        mb.add(menuDemo);
        mb.add(menuData);
        return mb;
    }

    private void updateRoleUI() {
        boolean isTeacher = (currentRole == UserRole.TEACHER || currentRole == UserRole.ADMIN);

        if (btnRoleAuth != null) {
            if (currentRole == UserRole.ADMIN) {
                btnRoleAuth.setText("🛡️ Admin: " + (currentUserAccount != null ? currentUserAccount.getFullName() : "Admin"));
                btnRoleAuth.setBackground(new Color(111, 66, 193)); // Purple for Admin
            } else if (currentRole == UserRole.TEACHER) {
                btnRoleAuth.setText("[Giảng viên: " + (currentUserAccount != null ? currentUserAccount.getFullName() : "Giảng viên") + "]");
                btnRoleAuth.setBackground(new Color(40, 167, 69)); // Green for Teacher
            } else {
                btnRoleAuth.setText("[Quyền: Học sinh - Nhấn để Đăng nhập]");
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
        currentPage = 1;
        loadAllStudents();
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
        String query = txtSearchQuery != null ? txtSearchQuery.getText().trim() : "";
        new SwingWorker<com.qlsv.model.PageResult<StudentResult>, Void>() {
            @Override
            protected com.qlsv.model.PageResult<StudentResult> doInBackground() throws Exception {
                return client.getStudentsPage(currentPage, pageSize, query);
            }
            @Override
            protected void done() {
                try {
                    com.qlsv.model.PageResult<StudentResult> res = get();
                    currentPage = res.getCurrentPage();
                    totalPages = res.getTotalPages();
                    totalStudents = res.getTotalItems();
                    currentStudentList = res.getItems();
                    updatePaginationUI();
                    applyFilters();
                } catch (Exception ex) {
                    System.err.println("Lỗi tải phân trang: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void updatePaginationUI() {
        if (lblPageInfo != null) {
            lblPageInfo.setText(String.format("Trang %d / %d", currentPage, totalPages));
        }
        if (btnFirstPage != null) btnFirstPage.setEnabled(currentPage > 1);
        if (btnPrevPage != null) btnPrevPage.setEnabled(currentPage > 1);
        if (btnNextPage != null) btnNextPage.setEnabled(currentPage < totalPages);
        if (btnLastPage != null) btnLastPage.setEnabled(currentPage < totalPages);
        if (lblTableSummary != null) {
            lblTableSummary.setText(String.format("Tổng số: %d sinh viên trên hệ thống (Trang %d/%d)", totalStudents, currentPage, totalPages));
        }
    }

    private void applyFilters() {
        String filterRank = (String) cbFilterRank.getSelectedItem();
        List<StudentResult> filtered = currentStudentList;

        if (filterRank != null && !filterRank.equalsIgnoreCase("Tất cả xếp loại") && filtered != null) {
            filtered = currentStudentList.stream()
                    .filter(s -> {
                        if (filterRank.contains("Xuất sắc")) return s.getAcademicRankCredit().equals("Xuất sắc");
                        if (filterRank.contains("Giỏi")) return s.getAcademicRankCredit().equals("Giỏi");
                        if (filterRank.contains("Khá")) return s.getAcademicRankCredit().equals("Khá");
                        if (filterRank.contains("Trung bình")) return s.getAcademicRankCredit().equals("Trung bình");
                        if (filterRank.contains("Cảnh báo")) return s.getAcademicStatus().contains("Cảnh báo") || s.getAcademicRankCredit().equals("Yếu") || s.getAcademicRankCredit().equals("Kém");
                        return true;
                    })
                    .collect(Collectors.toList());
        }

        tableModel.setRowCount(0);
        int stt = (currentPage - 1) * pageSize + 1;
        if (filtered != null) {
            for (StudentResult s : filtered) {
                tableModel.addRow(new Object[]{
                        stt++,
                        s.getStudentId(),
                        s.getFullName(),
                        s.getClassName() != null && !s.getClassName().isEmpty() ? s.getClassName() : "D21CQCN01-N",
                        String.format("%.2f", s.getScoreCC()),
                        String.format("%.2f", s.getScoreGK()),
                        String.format("%.2f", s.getScoreCK()),
                        String.format("%.2f", s.getAverageScore()),
                        String.format("%.2f", s.getGpa4()),
                        s.getLetterGrade(),
                        s.getAcademicRankCredit(),
                        s.getAcademicStatus()
                });
            }
        }
        if (filtered != null && !filtered.isEmpty()) {
            double maxGpa = 0.0, minGpa = 10.0, sumGpa = 0.0;
            for (StudentResult s : filtered) {
                double avg = s.getAverageScore();
                if (avg > maxGpa) maxGpa = avg;
                if (avg < minGpa) minGpa = avg;
                sumGpa += avg;
            }
            double classAvg = sumGpa / filtered.size();
            lblTableSummary.setText(String.format("Hiển thị: %d SV / %d tổng số  |  Trang %d / %d  |  ĐTB Học Phần: %.2f",
                    filtered.size(), totalStudents, currentPage, totalPages, classAvg));
        } else {
            lblTableSummary.setText(String.format("Tổng số: %d sinh viên trên hệ thống (Không có dữ liệu phù hợp)", totalStudents));
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
            currentGpa = Double.parseDouble(String.valueOf(tableModel.getValueAt(row, 7)));
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

    private JPanel createStudentPortalTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(PTIT_LIGHT_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));

        // 1. Top Card: Quick Search & Profile Box & 4 KPI Cards
        JPanel northPanel = new JPanel(new BorderLayout(8, 8));
        northPanel.setOpaque(false);

        // Search Bar for Student
        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        searchBar.setBackground(Color.WHITE);
        searchBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 225, 225)),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));

        searchBar.add(new JLabel("Mã Sinh Viên cần tra cứu:"));
        txtPortalSearchMaSV = new JTextField("SV001", 10);
        txtPortalSearchMaSV.setFont(new Font("Segoe UI", Font.BOLD, 13));
        txtPortalSearchMaSV.addActionListener(e -> loadStudentPortal(txtPortalSearchMaSV.getText().trim()));
        searchBar.add(txtPortalSearchMaSV);

        JButton btnPortalSearch = new JButton("Xem Hồ Sơ");
        btnPortalSearch.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPortalSearch.setBackground(PTIT_RED);
        btnPortalSearch.setForeground(Color.WHITE);
        btnPortalSearch.addActionListener(e -> loadStudentPortal(txtPortalSearchMaSV.getText().trim()));
        searchBar.add(btnPortalSearch);

        JButton btnReloadPortal = new JButton("Làm Mới");
        btnReloadPortal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btnReloadPortal.addActionListener(e -> loadStudentPortal(txtPortalSearchMaSV.getText().trim()));
        searchBar.add(btnReloadPortal);

        northPanel.add(searchBar, BorderLayout.NORTH);

        // Profile & KPI Container (Split in 2 sides: Left Info, Right KPI Cards)
        JPanel profileContainer = new JPanel(new GridLayout(1, 2, 12, 0));
        profileContainer.setOpaque(false);

        // Left Card: Student Info Card
        JPanel infoCard = new JPanel(new GridLayout(4, 2, 6, 6));
        infoCard.setBackground(Color.WHITE);
        infoCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230)),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        lblPortalHoTen = new JLabel("Vũ Khánh Bích");
        lblPortalHoTen.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblPortalHoTen.setForeground(PTIT_RED);

        lblPortalMaSV = new JLabel("SV001");
        lblPortalMaSV.setFont(new Font("Segoe UI", Font.BOLD, 13));

        lblPortalLop = new JLabel("D21CQCN01-N");
        lblPortalLop.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        lblPortalKhoa = new JLabel("Công nghệ Thông tin 1");
        lblPortalKhoa.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        infoCard.add(new JLabel("Họ và Tên:"));
        infoCard.add(lblPortalHoTen);
        infoCard.add(new JLabel("Mã Sinh Viên:"));
        infoCard.add(lblPortalMaSV);
        infoCard.add(new JLabel("Lớp Niên Chế:"));
        infoCard.add(lblPortalLop);
        infoCard.add(new JLabel("Khoa Quản Lý:"));
        infoCard.add(lblPortalKhoa);

        // Right Card: 4 KPI Cards (GridLayout 2x2)
        JPanel kpiContainer = new JPanel(new GridLayout(2, 2, 10, 10));
        kpiContainer.setOpaque(false);

        lblPortalCredits = new JLabel("15 / 132 TC", SwingConstants.CENTER);
        lblPortalGpa10 = new JLabel("8.52 / 10.0", SwingConstants.CENTER);
        lblPortalGpa4 = new JLabel("3.60 / 4.00", SwingConstants.CENTER);
        lblPortalRank = new JLabel("Xuất sắc", SwingConstants.CENTER);

        kpiContainer.add(createKpiCard("TÍN CHỈ TÍCH LŨY", lblPortalCredits, new Color(0, 123, 255)));
        kpiContainer.add(createKpiCard("ĐTB HỌC KỲ (10)", lblPortalGpa10, new Color(111, 66, 193)));
        kpiContainer.add(createKpiCard("GPA TÍCH LŨY (HỆ 4)", lblPortalGpa4, new Color(40, 167, 69)));
        kpiContainer.add(createKpiCard("XẾP LOẠI HỌC LỰC", lblPortalRank, PTIT_RED));

        profileContainer.add(infoCard);
        profileContainer.add(kpiContainer);
        northPanel.add(profileContainer, BorderLayout.CENTER);

        panel.add(northPanel, BorderLayout.NORTH);

        // 2. Center: Grade Table
        String[] portalCols = {
                "STT", "Mã Học Phần", "Tên Học Phần", "Số TC",
                "Chuyên Cần (10%)", "Giữa Kỳ (20%)", "Thi Kết Thúc (70%)",
                "Điểm HP (10)", "GPA Hệ 4", "Điểm Chữ", "Kết Quả"
        };
        portalTableModel = new DefaultTableModel(portalCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tblPortalGrades = new JTable(portalTableModel);
        tblPortalGrades.setRowHeight(30);
        tblPortalGrades.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblPortalGrades.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblPortalGrades.getTableHeader().setBackground(new Color(240, 242, 245));

        DefaultTableCellRenderer centerR = new DefaultTableCellRenderer();
        centerR.setHorizontalAlignment(JLabel.CENTER);
        for (int c = 0; c < portalCols.length; c++) {
            if (c != 2) {
                tblPortalGrades.getColumnModel().getColumn(c).setCellRenderer(centerR);
            }
        }

        // Color renderer for Kết Quả (Col 10)
        tblPortalGrades.getColumnModel().getColumn(10).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(JLabel.CENTER);
                String val = value != null ? value.toString() : "";
                if (!isSelected) {
                    if (val.equalsIgnoreCase("ĐẠT")) {
                        setForeground(new Color(40, 167, 69));
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setForeground(new Color(220, 53, 69));
                        setFont(getFont().deriveFont(Font.BOLD));
                    }
                }
                return comp;
            }
        });

        tblPortalGrades.getColumnModel().getColumn(0).setPreferredWidth(45);
        tblPortalGrades.getColumnModel().getColumn(1).setPreferredWidth(100);
        tblPortalGrades.getColumnModel().getColumn(2).setPreferredWidth(250);
        tblPortalGrades.getColumnModel().getColumn(3).setPreferredWidth(65);
        tblPortalGrades.getColumnModel().getColumn(4).setPreferredWidth(110);
        tblPortalGrades.getColumnModel().getColumn(5).setPreferredWidth(110);
        tblPortalGrades.getColumnModel().getColumn(6).setPreferredWidth(115);
        tblPortalGrades.getColumnModel().getColumn(7).setPreferredWidth(95);
        tblPortalGrades.getColumnModel().getColumn(8).setPreferredWidth(80);
        tblPortalGrades.getColumnModel().getColumn(9).setPreferredWidth(75);
        tblPortalGrades.getColumnModel().getColumn(10).setPreferredWidth(90);

        JScrollPane scroll = new JScrollPane(tblPortalGrades);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                " Bảng Điểm Kết Quả Học Tập Tín Chỉ Sinh Viên ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 13), PTIT_RED
        ));
        panel.add(scroll, BorderLayout.CENTER);

        // 3. South Action Bar
        JPanel southBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        southBar.setBackground(Color.WHITE);
        southBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(225, 225, 225)),
                BorderFactory.createEmptyBorder(4, 10, 4, 10)
        ));

        JButton btnPortalGpaSim = new JButton("Giả Lập GPA Mục Tiêu");
        btnPortalGpaSim.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPortalGpaSim.setBackground(new Color(111, 66, 193));
        btnPortalGpaSim.setForeground(Color.WHITE);
        btnPortalGpaSim.addActionListener(e -> {
            if (currentPortalTranscript != null) {
                openGpaSimulatorForTranscript(currentPortalTranscript);
            }
        });

        JButton btnPortalExport = new JButton("Xuất Phiếu Báo Điểm (Excel)");
        btnPortalExport.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnPortalExport.setBackground(new Color(40, 167, 69));
        btnPortalExport.setForeground(Color.WHITE);
        btnPortalExport.addActionListener(e -> exportPortalTranscript());

        southBar.add(btnPortalGpaSim);
        southBar.add(btnPortalExport);
        panel.add(southBar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(3, 0, 0, 0, accentColor),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(228, 230, 235)),
                        BorderFactory.createEmptyBorder(8, 10, 8, 10)
                )
        ));

        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblTitle.setForeground(new Color(110, 115, 125));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        valueLabel.setForeground(accentColor);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public void loadStudentPortal(String maSv) {
        if (client == null || maSv == null || maSv.trim().isEmpty()) return;
        final String targetMaSv = maSv.trim().toUpperCase();
        new SwingWorker<StudentTranscript, Void>() {
            @Override
            protected StudentTranscript doInBackground() throws Exception {
                return client.getDetailedTranscript(targetMaSv);
            }
            @Override
            protected void done() {
                try {
                    StudentTranscript t = get();
                    currentPortalTranscript = t;
                    updateStudentPortalUI(t);
                } catch (Exception ex) {
                    System.err.println("Lỗi tải hồ sơ sinh viên: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void updateStudentPortalUI(StudentTranscript t) {
        if (t == null) return;
        if (lblPortalHoTen != null) lblPortalHoTen.setText(t.getFullName());
        if (lblPortalMaSV != null) lblPortalMaSV.setText(t.getStudentId());
        if (lblPortalLop != null) lblPortalLop.setText(t.getClassName() != null ? t.getClassName() : "D21CQCN01-N");
        if (lblPortalKhoa != null) lblPortalKhoa.setText(t.getFaculty() != null ? t.getFaculty() : "Công nghệ Thông tin 1");
        if (lblPortalCredits != null) lblPortalCredits.setText(t.getTotalCredits() + " / 132 TC");
        if (lblPortalGpa10 != null) lblPortalGpa10.setText(String.format("%.2f / 10.0", t.getCumulativeGpa10()));
        if (lblPortalGpa4 != null) lblPortalGpa4.setText(String.format("%.2f / 4.00", t.getCumulativeGpa4()));
        if (lblPortalRank != null) lblPortalRank.setText(t.getAcademicRank() != null ? t.getAcademicRank() : "Khá");
        if (txtPortalSearchMaSV != null && !txtPortalSearchMaSV.getText().equalsIgnoreCase(t.getStudentId())) {
            txtPortalSearchMaSV.setText(t.getStudentId());
        }

        if (portalTableModel != null) {
            portalTableModel.setRowCount(0);
            int stt = 1;
            for (CourseGrade g : t.getGrades()) {
                portalTableModel.addRow(new Object[]{
                        stt++,
                        g.getCourseCode(),
                        g.getCourseName(),
                        g.getCredits(),
                        String.format("%.2f", g.getScoreProcess()),
                        String.format("%.2f", g.getScoreExam()),
                        String.format("%.2f", g.getTotalScore()),
                        String.format("%.2f", g.getTotalScore()),
                        String.format("%.2f", g.getGpa4()),
                        g.getLetterGrade(),
                        g.isPassed() ? "ĐẠT" : "HỌC LẠI"
                });
            }
        }
    }

    private void exportPortalTranscript() {
        if (currentPortalTranscript == null) {
            JOptionPane.showMessageDialog(this, "Chưa có dữ liệu bảng điểm để xuất!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Lưu Phiếu Báo Điểm Cá Nhân");
            chooser.setSelectedFile(new File("PhieuDiem_" + currentPortalTranscript.getStudentId() + ".xlsx"));
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File file = chooser.getSelectedFile();
                if (!file.getAbsolutePath().endsWith(".xlsx")) {
                    file = new File(file.getAbsolutePath() + ".xlsx");
                }
                ExcelExporter.exportTranscriptToExcel(currentPortalTranscript, file);
                JOptionPane.showMessageDialog(this, "Đã xuất phiếu điểm thành công:\n" + file.getAbsolutePath(), "Thành Công", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi xuất phiếu điểm: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openGpaSimulatorForTranscript(StudentTranscript t) {
        if (t == null) return;
        JDialog simDialog = new JDialog(this, "BỘ GIẢ LẬP ĐIỂM GPA SIMULATOR - " + t.getFullName(), true);
        simDialog.setSize(640, 430);
        simDialog.setLocationRelativeTo(this);
        simDialog.setLayout(new BorderLayout(10, 10));

        JPanel head = new JPanel(new GridLayout(2, 1));
        head.setBackground(PTIT_RED);
        head.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        JLabel lblT = new JLabel("BỘ GIẢ LẬP ĐIỂM GPA & DỰ ĐOÁN HỌC LỰC MỤC TIÊU");
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblT.setForeground(Color.WHITE);
        JLabel lblSub = new JLabel("Sinh viên: " + t.getFullName() + " (" + t.getStudentId() + ") | CPA Hệ 4: " + String.format("%.2f", t.getCumulativeGpa4()));
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
        body.add(new JLabel("Mục tiêu xếp loại:"), g);
        g.gridx = 1; g.gridy = 0;
        JComboBox<String> cbTarget = new JComboBox<>(new String[]{"Xuất sắc (CPA >= 3.6)", "Giỏi (CPA >= 3.2)", "Khá (CPA >= 2.5)"});
        body.add(cbTarget, g);

        g.gridx = 0; g.gridy = 1;
        body.add(new JLabel("Số tín chỉ kỳ tới:"), g);
        g.gridx = 1; g.gridy = 1;
        JTextField txtCredits = new JTextField("15", 10);
        body.add(txtCredits, g);

        JTextArea txtAdvice = new JTextArea(4, 30);
        txtAdvice.setEditable(false);
        txtAdvice.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtAdvice.setBackground(new Color(245, 248, 255));
        txtAdvice.setBorder(BorderFactory.createTitledBorder("Kết Quả Dự Đoán Lộ Trình"));
        txtAdvice.setText("Nhấn nút 'Tính Toán Lộ Trình' bên dưới để nhận kết quả phân tích.");

        g.gridx = 0; g.gridy = 2; g.gridwidth = 2;
        body.add(txtAdvice, g);
        simDialog.add(body, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        JButton btnCalc = new JButton("Tính Toán Lộ Trình");
        btnCalc.setBackground(PTIT_RED);
        btnCalc.setForeground(Color.WHITE);
        btnCalc.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnCalc.addActionListener(e -> {
            String target = (String) cbTarget.getSelectedItem();
            double targetScore = target.contains("3.6") ? 3.6 : (target.contains("3.2") ? 3.2 : 2.5);
            double baseGpa = t.getCumulativeGpa4();
            if (baseGpa >= targetScore) {
                txtAdvice.setText("Chúc mừng! CPA hiện tại (" + String.format("%.2f", baseGpa) + ") đã đạt mức mục tiêu (" + target + ").\nHãy duy trì phong độ tối thiểu 3.0 ở các môn tiếp theo!");
            } else {
                double diff = targetScore - baseGpa;
                double needed = baseGpa + (diff * 1.6);
                if (needed > 4.0) needed = 3.9;
                txtAdvice.setText(String.format("Để nâng hạng học lực lên %s:\n• Bạn cần đạt GPA trung bình tối thiểu: %.2f (Thang 4) ở kỳ tới.\n• Lời khuyên: Cố gắng đạt điểm A ở các môn 3 và 4 tín chỉ!", target, needed));
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

    public JTabbedPane getMainTabbedPane() {
        return mainTabbedPane;
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
