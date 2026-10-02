package com.qlsv.client;

import com.formdev.flatlaf.FlatDarkLaf;
import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.network.UDPPacket;
import com.qlsv.util.ExcelExporter;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.List;

public class ClientGUI extends JFrame {

    private UDPClient client;

    private JTabbedPane tabbedPane;

    // Step 1 Controls: Server Connection
    private JTextField txtServerHost;
    private JTextField txtServerPort;
    private JButton btnConnectServer;
    private JLabel lblStep1Status;

    // Step 2 Controls: Database Setup
    private JComboBox<SqlConfig.DbType> cbDbType;
    private JTextField txtDbHost;
    private JTextField txtDbPort;
    private JTextField txtDbName;
    private JTextField txtDbUser;
    private JPasswordField txtDbPass;
    private JButton btnConnectDb;
    private JLabel lblStep2Status;

    // Step 3 Controls: Student Data & CRUD Table
    private JTextField txtStudentName;
    private JTextField txtStudentId;
    private JTextField txtScoreMath;
    private JTextField txtScoreLit;
    private JTextField txtScoreEng;
    private JTextField txtSearchQuery;

    private JButton btnSendStudent;
    private JButton btnClearForm;
    private JButton btnSearch;
    private JButton btnRefreshAll;
    private JButton btnDeleteSelected;
    private JButton btnExportExcel;

    private JTable tblResults;
    private DefaultTableModel tableModel;

    public ClientGUI() {
        setTitle("CLIENT - Quản Lý Sinh Viên UDP (Bảo Mật AES/DES & Export Excel)");
        setSize(1020, 740);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(33, 43, 67));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("CHƯƠNG TRÌNH QUẢN LÝ SINH VIÊN (CLIENT UDP)");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);

        JLabel subtitleLabel = new JLabel("Kết nối Server UDP -> Cấu hình SQL -> Quản lý Sinh viên (CRUD, Search, Export Excel)");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(180, 195, 215));

        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);
        titleBox.add(titleLabel);
        titleBox.add(subtitleLabel);

        headerPanel.add(titleBox, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JPanel panelStep1 = createStep1Panel();
        JPanel panelStep2 = createStep2Panel();
        JPanel panelStep3 = createStep3Panel();

        tabbedPane.addTab("Bước 1: Kết Nối Server UDP", panelStep1);
        tabbedPane.addTab("Bước 2: Kết Nối CSDL SQL", panelStep2);
        tabbedPane.addTab("Bước 3: Quản Lý Sinh Viên (CRUD & Export)", panelStep3);

        tabbedPane.setEnabledAt(1, false);
        tabbedPane.setEnabledAt(2, false);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createStep1Panel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel box = new JPanel(new GridBagLayout());
        box.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                " Nhập Thông Tin Kết Nối Server UDP ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), new Color(90, 160, 250)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        box.add(new JLabel("Địa chỉ IP / Host Server:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        txtServerHost = new JTextField("127.0.0.1", 18);
        txtServerHost.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        box.add(txtServerHost, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        box.add(new JLabel("Cổng Kết Nối Server UDP:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        txtServerPort = new JTextField("9876", 18);
        txtServerPort.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        box.add(txtServerPort, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        btnConnectServer = new JButton("KẾT NỐI SERVER");
        btnConnectServer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnConnectServer.setBackground(new Color(0, 122, 255));
        btnConnectServer.setForeground(Color.WHITE);
        btnConnectServer.setFocusPainted(false);
        btnConnectServer.setPreferredSize(new Dimension(200, 40));
        btnConnectServer.addActionListener(e -> handleConnectServer());
        box.add(btnConnectServer, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        lblStep1Status = new JLabel("Chưa kết nối Server. Vui lòng kiểm tra và ấn 'Kết Nối Server'.", SwingConstants.CENTER);
        lblStep1Status.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblStep1Status.setForeground(Color.GRAY);
        box.add(lblStep1Status, gbc);

        GridBagConstraints outerGbc = new GridBagConstraints();
        outerGbc.gridx = 0; outerGbc.gridy = 0; outerGbc.weightx = 1.0; outerGbc.weighty = 1.0;
        outerGbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(box, outerGbc);

        return panel;
    }

    private void handleConnectServer() {
        String host = txtServerHost.getText().trim();
        String portStr = txtServerPort.getText().trim();

        if (host.isEmpty() || portStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ IP và Cổng!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            client = new UDPClient(host, port);

            boolean connected = client.pingServer();
            if (connected) {
                lblStep1Status.setText("KẾT NỐI SERVER THÀNH CÔNG! Đang chuyển sang Bước 2...");
                lblStep1Status.setForeground(new Color(40, 167, 69));

                JOptionPane.showMessageDialog(this,
                        "Kết nối với Server UDP (" + host + ":" + port + ") THÀNH CÔNG!\nNhấn OK để chuyển sang cấu hình CSDL.",
                        "Thông Báo Thành Công", JOptionPane.INFORMATION_MESSAGE);

                tabbedPane.setEnabledAt(1, true);
                tabbedPane.setSelectedIndex(1);
            } else {
                throw new Exception("Server phản hồi không đúng!");
            }
        } catch (Exception ex) {
            lblStep1Status.setText("Kết nối không thành công! " + ex.getMessage());
            lblStep1Status.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this, "Lỗi kết nối Server UDP: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createStep2Panel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel box = new JPanel(new GridBagLayout());
        box.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                " Cấu Hình CSDL SQL Gửi Server ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), new Color(90, 160, 250)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        box.add(new JLabel("Loại CSDL SQL:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        cbDbType = new JComboBox<>(SqlConfig.DbType.values());
        cbDbType.setSelectedItem(SqlConfig.DbType.SQL_SERVER);
        cbDbType.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbDbType.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                SqlConfig.DbType type = (SqlConfig.DbType) cbDbType.getSelectedItem();
                if (type == SqlConfig.DbType.SQL_SERVER) {
                    txtDbPort.setText("1433");
                    txtDbUser.setText("sa");
                    txtDbName.setText("QLSV_DB");
                } else if (type == SqlConfig.DbType.MYSQL) {
                    txtDbPort.setText("3306");
                    txtDbUser.setText("root");
                    txtDbName.setText("QLSV_DB");
                } else if (type == SqlConfig.DbType.POSTGRESQL) {
                    txtDbPort.setText("5432");
                    txtDbUser.setText("postgres");
                    txtDbName.setText("postgres");
                } else {
                    txtDbPort.setText("0");
                    txtDbUser.setText("sa");
                    txtDbName.setText("qlsv_db");
                }
            }
        });
        box.add(cbDbType, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        box.add(new JLabel("Địa chỉ Server CSDL:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        txtDbHost = new JTextField("localhost", 18);
        box.add(txtDbHost, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        box.add(new JLabel("Cổng CSDL:"), gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        txtDbPort = new JTextField("1433", 18);
        box.add(txtDbPort, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        box.add(new JLabel("Tên CSDL:"), gbc);

        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        txtDbName = new JTextField("QLSV_DB", 18);
        box.add(txtDbName, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0;
        box.add(new JLabel("Username SQL:"), gbc);

        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0;
        txtDbUser = new JTextField("sa", 18);
        box.add(txtDbUser, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.0;
        box.add(new JLabel("Password SQL:"), gbc);

        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 1.0;
        txtDbPass = new JPasswordField("123456", 18);
        box.add(txtDbPass, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        btnConnectDb = new JButton("GỬI THÔNG SỐ KẾT NỐI CSDL LÊN SERVER");
        btnConnectDb.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConnectDb.setBackground(new Color(40, 167, 69));
        btnConnectDb.setForeground(Color.WHITE);
        btnConnectDb.setFocusPainted(false);
        btnConnectDb.setPreferredSize(new Dimension(220, 38));
        btnConnectDb.addActionListener(e -> handleConnectDb());
        box.add(btnConnectDb, gbc);

        gbc.gridx = 0; gbc.gridy = 7; gbc.gridwidth = 2;
        lblStep2Status = new JLabel("Chưa kết nối CSDL trên Server.", SwingConstants.CENTER);
        lblStep2Status.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblStep2Status.setForeground(Color.GRAY);
        box.add(lblStep2Status, gbc);

        GridBagConstraints outerGbc = new GridBagConstraints();
        outerGbc.gridx = 0; outerGbc.gridy = 0; outerGbc.weightx = 1.0; outerGbc.weighty = 1.0;
        outerGbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(box, outerGbc);

        return panel;
    }

    private void handleConnectDb() {
        if (client == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối Server UDP!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            tabbedPane.setSelectedIndex(0);
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
                lblStep2Status.setText("Server đã kết nối CSDL thành công!");
                lblStep2Status.setForeground(new Color(40, 167, 69));

                JOptionPane.showMessageDialog(this,
                        "Server kết nối CSDL THÀNH CÔNG!\nChuyển sang Bước 3: Quản lý sinh viên.",
                        "Thông Báo Thành Công", JOptionPane.INFORMATION_MESSAGE);

                tabbedPane.setEnabledAt(2, true);
                tabbedPane.setSelectedIndex(2);
                loadAllStudents();
            } else {
                String errMsg = (response != null) ? response.getMessage() : "Không nhận được phản hồi";
                lblStep2Status.setText("Server kết nối CSDL Thất bại: " + errMsg);
                lblStep2Status.setForeground(Color.RED);
                JOptionPane.showMessageDialog(this, "Lỗi kết nối CSDL: " + errMsg, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            lblStep2Status.setText("Lỗi gửi thông số CSDL: " + ex.getMessage());
            lblStep2Status.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createStep3Panel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form Panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                " Form Thêm / Cập Nhật Sinh Viên qua UDP ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), new Color(90, 160, 250)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Họ tên sinh viên:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        txtStudentName = new JTextField(15);
        formPanel.add(txtStudentName, gbc);

        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Mã sinh viên:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 1.0;
        txtStudentId = new JTextField(10);
        formPanel.add(txtStudentId, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Điểm Toán:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        txtScoreMath = new JTextField(10);
        formPanel.add(txtScoreMath, gbc);

        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Điểm Văn:"), gbc);
        gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 1.0;
        txtScoreLit = new JTextField(10);
        formPanel.add(txtScoreLit, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Điểm Anh:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        txtScoreEng = new JTextField(10);
        formPanel.add(txtScoreEng, gbc);

        JPanel btnFormBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnClearForm = new JButton("Xóa Form");
        btnClearForm.addActionListener(e -> clearForm());

        btnSendStudent = new JButton("GỬI / LƯU SINH VIÊN VIA UDP");
        btnSendStudent.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSendStudent.setBackground(new Color(40, 167, 69));
        btnSendStudent.setForeground(Color.WHITE);
        btnSendStudent.setFocusPainted(false);
        btnSendStudent.addActionListener(e -> handleSendStudent());

        btnFormBox.add(btnClearForm);
        btnFormBox.add(btnSendStudent);

        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 2;
        formPanel.add(btnFormBox, gbc);

        panel.add(formPanel, BorderLayout.NORTH);

        // CRUD & Search Toolbar + Table
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        toolBar.setBorder(BorderFactory.createTitledBorder(" Tìm Kiếm & Thao Tác "));

        txtSearchQuery = new JTextField(15);
        btnSearch = new JButton("Tìm Kiếm");
        btnSearch.addActionListener(e -> handleSearch());

        btnRefreshAll = new JButton("Tải Lại Tất Cả");
        btnRefreshAll.addActionListener(e -> loadAllStudents());

        btnDeleteSelected = new JButton("Xóa Dòng Chọn");
        btnDeleteSelected.setBackground(new Color(220, 53, 69));
        btnDeleteSelected.setForeground(Color.WHITE);
        btnDeleteSelected.addActionListener(e -> handleDeleteSelected());

        btnExportExcel = new JButton("Xuất File Excel (.xlsx)");
        btnExportExcel.setBackground(new Color(40, 167, 69));
        btnExportExcel.setForeground(Color.WHITE);
        btnExportExcel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnExportExcel.addActionListener(e -> handleExportExcel());

        toolBar.add(new JLabel("Từ khóa:"));
        toolBar.add(txtSearchQuery);
        toolBar.add(btnSearch);
        toolBar.add(btnRefreshAll);
        toolBar.add(btnDeleteSelected);
        toolBar.add(Box.createHorizontalStrut(15));
        toolBar.add(btnExportExcel);

        centerPanel.add(toolBar, BorderLayout.NORTH);

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
        tblResults.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scrollPane = new JScrollPane(tblResults);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    private void handleSendStudent() {
        if (client == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối Server UDP!", "Lỗi", JOptionPane.ERROR_MESSAGE);
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

            StudentData student = new StudentData(id, name, math, lit, eng);
            StudentResult result = client.sendStudentData(student);

            JOptionPane.showMessageDialog(this,
                    String.format("Server phản hồi thành công!\nSinh viên: %s (%s)\nĐiểm TB = %.2f - Xếp loại: %s",
                            result.getFullName(), result.getStudentId(), result.getAverageScore(), result.getAcademicRank()),
                    "Thông Báo Server UDP", JOptionPane.INFORMATION_MESSAGE);

            clearForm();
            loadAllStudents();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi gửi dữ liệu: " + ex.getMessage(), "Lỗi UDP", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleSearch() {
        String query = txtSearchQuery.getText().trim();
        try {
            List<StudentResult> list = client.searchStudents(query);
            updateTableData(list);
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
                "Xác Nhận Xóa", JOptionPane.YES_NO_OPTION);

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
            List<StudentResult> list = client.getAllStudents();
            updateTableData(list);
        } catch (Exception ex) {
            System.err.println("Lỗi tải danh sách: " + ex.getMessage());
        }
    }

    private void updateTableData(List<StudentResult> list) {
        tableModel.setRowCount(0);
        int stt = 1;
        for (StudentResult s : list) {
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
    }

    private void handleExportExcel() {
        if (client == null) {
            JOptionPane.showMessageDialog(this, "Chưa kết nối Server UDP!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            List<StudentResult> list = client.getAllStudents();
            if (list.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Danh sách sinh viên rỗng, không có dữ liệu xuất!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Lưu Báo Cáo Excel Danh Sách Sinh Viên");
            chooser.setFileFilter(new FileNameExtensionFilter("Excel Workbook (*.xlsx)", "xlsx"));
            chooser.setSelectedFile(new File("DanhSachSinhVien_BaoCao.xlsx"));

            int userSelection = chooser.showSaveDialog(this);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = chooser.getSelectedFile();
                if (!fileToSave.getAbsolutePath().endsWith(".xlsx")) {
                    fileToSave = new File(fileToSave.getAbsolutePath() + ".xlsx");
                }

                ExcelExporter.exportStudentsToExcel(list, fileToSave);
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
            FlatDarkLaf.setup();
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            ClientGUI frame = new ClientGUI();
            frame.setVisible(true);
        });
    }
}
