package com.qlsv.client;

import com.formdev.flatlaf.FlatDarkLaf;
import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.network.UDPPacket;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ItemEvent;

public class ClientGUI extends JFrame {

    private UDPClient client;

    // Card/Step Navigation
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

    // Step 3 Controls: Student Data & Server Results
    private JTextField txtStudentName;
    private JTextField txtStudentId;
    private JTextField txtScoreMath;
    private JTextField txtScoreLit;
    private JTextField txtScoreEng;
    private JButton btnSendStudent;
    private JButton btnClearForm;
    private JTable tblResults;
    private DefaultTableModel tableModel;
    private int resultCounter = 0;

    public ClientGUI() {
        setTitle("CLIENT - Quản Lý Sinh Viên qua UDP");
        setSize(950, 700);
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

        JLabel subtitleLabel = new JLabel("Nhập địa chỉ Server -> Kết nối CSDL SQL -> Nhập sinh viên & Xem kết quả DTB");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(180, 195, 215));

        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);
        titleBox.add(titleLabel);
        titleBox.add(subtitleLabel);

        headerPanel.add(titleBox, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // Main Step Tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));

        // Create 3 Steps
        JPanel panelStep1 = createStep1Panel();
        JPanel panelStep2 = createStep2Panel();
        JPanel panelStep3 = createStep3Panel();

        tabbedPane.addTab("Bước 1: Kết Nối Server UDP", panelStep1);
        tabbedPane.addTab("Bước 2: Kết Nối CSDL SQL", panelStep2);
        tabbedPane.addTab("Bước 3: Nhập Sinh Viên & Kết Quả", panelStep3);

        // Lock steps 2 and 3 initially
        tabbedPane.setEnabledAt(1, false);
        tabbedPane.setEnabledAt(2, false);

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==========================================
    // BƯỚC 1: KẾT NỐI SERVER UDP
    // ==========================================
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

        // Host
        gbc.gridx = 0; gbc.gridy = 0;
        box.add(new JLabel("Địa chỉ IP / Host Server:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        txtServerHost = new JTextField("127.0.0.1", 18);
        txtServerHost.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        box.add(txtServerHost, gbc);

        // Port
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        box.add(new JLabel("Cổng Kết Nối Server UDP:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        txtServerPort = new JTextField("9876", 18);
        txtServerPort.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        box.add(txtServerPort, gbc);

        // Connect Button
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        btnConnectServer = new JButton("KẾT NỐI SERVER");
        btnConnectServer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnConnectServer.setBackground(new Color(0, 122, 255));
        btnConnectServer.setForeground(Color.WHITE);
        btnConnectServer.setFocusPainted(false);
        btnConnectServer.setPreferredSize(new Dimension(200, 40));
        btnConnectServer.addActionListener(e -> handleConnectServer());
        box.add(btnConnectServer, gbc);

        // Status Label
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
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ địa chỉ IP và Cổng kết nối!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            client = new UDPClient(host, port);

            // Test Ping Server via UDP
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
                throw new Exception("Server phản hồi gói tin không đúng!");
            }
        } catch (NumberFormatException nfe) {
            lblStep1Status.setText("Cổng kết nối phải là số nguyên hợp lệ!");
            lblStep1Status.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this,
                    "Kết nối không thành công! Cổng kết nối phải là số nguyên.\nVui lòng thông báo nhập lại.",
                    "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            lblStep1Status.setText("Kết nối không thành công! " + ex.getMessage());
            lblStep1Status.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this,
                    "Kết nối với Server KHÔNG THÀNH CÔNG!\nChi tiết: " + ex.getMessage() + "\n\nVui lòng thông báo nhập lại thông tin địa chỉ và cổng.",
                    "Lỗi Kết Nối UDP", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // BƯỚC 2: CẤU HÌNH CSDL SQL GỬI LÊN SERVER
    // ==========================================
    private JPanel createStep2Panel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel box = new JPanel(new GridBagLayout());
        box.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                " Nhập Thông Số SQL gửi lên Server để thực hiện kết nối CSDL ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), new Color(90, 160, 250)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // DB Type
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

        // Host
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        box.add(new JLabel("Địa chỉ Server CSDL:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        txtDbHost = new JTextField("localhost", 18);
        box.add(txtDbHost, gbc);

        // Port
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        box.add(new JLabel("Cổng CSDL:"), gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        txtDbPort = new JTextField("1433", 18);
        box.add(txtDbPort, gbc);

        // DB Name
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.0;
        box.add(new JLabel("Tên CSDL (Database Name):"), gbc);

        gbc.gridx = 1; gbc.gridy = 3; gbc.weightx = 1.0;
        txtDbName = new JTextField("QLSV_DB", 18);
        box.add(txtDbName, gbc);

        // Username
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.0;
        box.add(new JLabel("Username SQL:"), gbc);

        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 1.0;
        txtDbUser = new JTextField("sa", 18);
        box.add(txtDbUser, gbc);

        // Password
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.0;
        box.add(new JLabel("Password SQL:"), gbc);

        gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 1.0;
        txtDbPass = new JPasswordField("123456", 18);
        box.add(txtDbPass, gbc);

        // Connect DB Button
        gbc.gridx = 0; gbc.gridy = 6; gbc.gridwidth = 2;
        btnConnectDb = new JButton("GỬI THÔNG SỐ KẾT NỐI CSDL LÊN SERVER");
        btnConnectDb.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnConnectDb.setBackground(new Color(40, 167, 69));
        btnConnectDb.setForeground(Color.WHITE);
        btnConnectDb.setFocusPainted(false);
        btnConnectDb.setPreferredSize(new Dimension(220, 38));
        btnConnectDb.addActionListener(e -> handleConnectDb());
        box.add(btnConnectDb, gbc);

        // Status Label
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
            JOptionPane.showMessageDialog(this, "Chưa kết nối Server UDP! Vui lòng quay lại Bước 1.", "Lỗi", JOptionPane.ERROR_MESSAGE);
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

            // Send Connect DB packet to Server
            UDPPacket response = client.connectDatabase(config);

            if (response != null && response.isSuccess()) {
                lblStep2Status.setText("Server đã kết nối CSDL thành công!");
                lblStep2Status.setForeground(new Color(40, 167, 69));

                JOptionPane.showMessageDialog(this,
                        "Server kết nối CSDL THÀNH CÔNG!\nChuyển sang Bước 3: Nhập dữ liệu sinh viên.",
                        "Thông Báo Thành Công", JOptionPane.INFORMATION_MESSAGE);

                tabbedPane.setEnabledAt(2, true);
                tabbedPane.setSelectedIndex(2);
            } else {
                String errMsg = (response != null) ? response.getMessage() : "Không nhận được phản hồi";
                lblStep2Status.setText("Server kết nối CSDL Thất bại: " + errMsg);
                lblStep2Status.setForeground(Color.RED);
                JOptionPane.showMessageDialog(this,
                        "Server kết nối CSDL THẤT BẠI!\nChi tiết: " + errMsg + "\n\nVui lòng kiểm tra lại Username/Password SQL hoặc khởi động máy chủ CSDL.",
                        "Lỗi Kết Nối CSDL", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Cổng CSDL phải là số nguyên!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            lblStep2Status.setText("Lỗi gửi thông số CSDL: " + ex.getMessage());
            lblStep2Status.setForeground(Color.RED);
            JOptionPane.showMessageDialog(this, "Lỗi kết nối CSDL: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ==========================================
    // BƯỚC 3: NHẬP SINH VIÊN & XEM KẾT QUẢ
    // ==========================================
    private JPanel createStep3Panel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Form nhập dữ liệu sinh viên từng dòng
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                " Nhập Từng Dòng Dữ Liệu Sinh Viên ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), new Color(90, 160, 250)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Họ Tên
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Họ tên sinh viên:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0;
        txtStudentName = new JTextField(15);
        formPanel.add(txtStudentName, gbc);

        // Mã SV
        gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Mã sinh viên:"), gbc);
        gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 1.0;
        txtStudentId = new JTextField(10);
        formPanel.add(txtStudentId, gbc);

        // Điểm Toán
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Điểm thi Toán:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0;
        txtScoreMath = new JTextField(10);
        formPanel.add(txtScoreMath, gbc);

        // Điểm Văn
        gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Điểm thi Văn:"), gbc);
        gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 1.0;
        txtScoreLit = new JTextField(10);
        formPanel.add(txtScoreLit, gbc);

        // Điểm Tiếng Anh
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.0;
        formPanel.add(new JLabel("Điểm Tiếng Anh:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0;
        txtScoreEng = new JTextField(10);
        formPanel.add(txtScoreEng, gbc);

        // Action Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnClearForm = new JButton("Xóa Nhập Liệu");
        btnClearForm.addActionListener(e -> clearForm());

        btnSendStudent = new JButton("GỬI DỮ LIỆU LÊN SERVER");
        btnSendStudent.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSendStudent.setBackground(new Color(40, 167, 69));
        btnSendStudent.setForeground(Color.WHITE);
        btnSendStudent.setFocusPainted(false);
        btnSendStudent.addActionListener(e -> handleSendStudent());

        btnPanel.add(btnClearForm);
        btnPanel.add(btnSendStudent);

        gbc.gridx = 2; gbc.gridy = 2; gbc.gridwidth = 2;
        formPanel.add(btnPanel, gbc);

        panel.add(formPanel, BorderLayout.NORTH);

        // Results Table Panel (Bảng hiển thị kết quả từ Server trả về)
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                " Kết Quả Server Trả Về (Họ tên, Mã SV, Điểm trung bình) ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 14), new Color(40, 167, 69)
        ));

        String[] columnNames = {"STT", "Họ Tên Sinh Viên", "Mã Sinh Viên", "Điểm Trung Bình (Server Tính)"};
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

        JScrollPane scrollPane = new JScrollPane(tblResults);
        resultPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(resultPanel, BorderLayout.CENTER);

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
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ Họ tên, Mã SV và Điểm 3 môn!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            double math = Double.parseDouble(mathStr);
            double lit = Double.parseDouble(litStr);
            double eng = Double.parseDouble(engStr);

            if (math < 0 || math > 10 || lit < 0 || lit > 10 || eng < 0 || eng > 10) {
                JOptionPane.showMessageDialog(this, "Điểm thi phải nằm trong thang điểm [0.0 - 10.0]!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            StudentData student = new StudentData(id, name, math, lit, eng);

            // Send student record to Server over UDP
            StudentResult result = client.sendStudentData(student);

            // Add result returned by Server to Table
            resultCounter++;
            tableModel.addRow(new Object[]{
                    resultCounter,
                    result.getFullName(),
                    result.getStudentId(),
                    String.format("%.2f", result.getAverageScore())
            });

            // Scroll to bottom row
            tblResults.scrollRectToVisible(tblResults.getCellRect(tableModel.getRowCount() - 1, 0, true));

            JOptionPane.showMessageDialog(this,
                    String.format("Server phản hồi thành công!\nSinh viên: %s (%s)\nĐiểm trung bình = %.2f",
                            result.getFullName(), result.getStudentId(), result.getAverageScore()),
                    "Kết Quả Từ Server", JOptionPane.INFORMATION_MESSAGE);

            // Clear inputs for next entry
            clearForm();

        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Điểm thi phải là số thực hợp lệ (Ví dụ: 8.5)!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi gửi dữ liệu tới Server: " + ex.getMessage(), "Lỗi UDP Server", JOptionPane.ERROR_MESSAGE);
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
