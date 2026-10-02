package com.qlsv;

import com.formdev.flatlaf.FlatDarkLaf;
import com.qlsv.client.UDPClient;
import com.qlsv.database.DatabaseManager;
import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.model.StudentResult;
import com.qlsv.server.ServerGUI;
import com.qlsv.server.UDPServer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class GuiDemoAutomationRunner {

    public static void main(String[] args) {
        System.out.println("========== KHỞI CHẠY AUTOMATION CAPTURE MÀN HÌNH GIAO DIỆN (SERVER + CLIENT) ==========");

        try {
            FlatDarkLaf.setup();
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            try {
                int port = 9876;

                // 1. Create and show Server GUI
                ServerGUI serverGUI = new ServerGUI();
                serverGUI.setTitle("SERVER GUI - ĐỀ TÀI 12 (UDP & DES)");
                serverGUI.setBounds(50, 50, 960, 680);
                serverGUI.setVisible(true);

                // Start server via reflection or UDP Direct
                Field udpServerField = ServerGUI.class.getDeclaredField("udpServer");
                udpServerField.setAccessible(true);
                UDPServer udpServer = new UDPServer(port);
                udpServer.setLogListener(serverGUI);
                udpServer.start();
                udpServerField.set(serverGUI, udpServer);

                Field btnStartStopField = ServerGUI.class.getDeclaredField("btnStartStop");
                btnStartStopField.setAccessible(true);
                JButton btnStartStop = (JButton) btnStartStopField.get(serverGUI);
                btnStartStop.setText("Dừng Server");
                btnStartStop.setBackground(new Color(220, 53, 69));

                Field lblServerStatusField = ServerGUI.class.getDeclaredField("lblServerStatus");
                lblServerStatusField.setAccessible(true);
                JLabel lblServerStatus = (JLabel) lblServerStatusField.get(serverGUI);
                lblServerStatus.setText("Trạng thái Server: Đang lắng nghe cổng UDP " + port);
                lblServerStatus.setForeground(new Color(40, 167, 69));

                // 2. Perform DB Connection & Data Population via UDP Client
                UDPClient client = new UDPClient("127.0.0.1", port);
                client.pingServer();

                SqlConfig h2Config = new SqlConfig(
                        SqlConfig.DbType.H2_EMBEDDED,
                        "localhost", 0, "test_qlsv_db", "sa", ""
                );
                client.connectDatabase(h2Config);

                StudentData s1 = new StudentData("SV001", "Nguyễn Văn An", 9.0, 8.5, 9.5);
                StudentData s2 = new StudentData("SV002", "Lê Thị Bình", 7.5, 8.0, 8.5);
                StudentData s3 = new StudentData("SV003", "Phạm Minh Cường", 9.5, 10.0, 9.0);

                StudentResult r1 = client.sendStudentData(s1);
                StudentResult r2 = client.sendStudentData(s2);
                StudentResult r3 = client.sendStudentData(s3);

                // 3. Create and populate Client GUI
                JFrame clientFrame = new JFrame("CLIENT GUI - Quản Lý Sinh Viên via UDP");
                clientFrame.setBounds(1030, 50, 850, 680);
                clientFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
                mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                // Header
                JPanel header = new JPanel(new BorderLayout());
                header.setBackground(new Color(33, 43, 67));
                header.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
                JLabel title = new JLabel("CLIENT UDP - BƯỚC 3: NHẬP DỮ LIỆU & KẾT QUẢ");
                title.setFont(new Font("Segoe UI", Font.BOLD, 18));
                title.setForeground(Color.WHITE);
                JLabel subTitle = new JLabel("Trạng thái: Đã kết nối Server UDP & CSDL H2 Database");
                subTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                subTitle.setForeground(new Color(40, 167, 69));
                header.add(title, BorderLayout.NORTH);
                header.add(subTitle, BorderLayout.SOUTH);
                mainPanel.add(header, BorderLayout.NORTH);

                // Table
                String[] cols = {"STT", "Họ Tên Sinh Viên", "Mã Sinh Viên", "Điểm Trung Bình (Server Tính)"};
                DefaultTableModel clientModel = new DefaultTableModel(cols, 0);
                clientModel.addRow(new Object[]{1, r1.getFullName(), r1.getStudentId(), String.format("%.2f", r1.getAverageScore())});
                clientModel.addRow(new Object[]{2, r2.getFullName(), r2.getStudentId(), String.format("%.2f", r2.getAverageScore())});
                clientModel.addRow(new Object[]{3, r3.getFullName(), r3.getStudentId(), String.format("%.2f", r3.getAverageScore())});

                JTable tblClient = new JTable(clientModel);
                tblClient.setRowHeight(30);
                tblClient.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                tblClient.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
                mainPanel.add(new JScrollPane(tblClient), BorderLayout.CENTER);

                clientFrame.add(mainPanel);
                clientFrame.setVisible(true);

                // Force refresh Server GUI Data Inspection Table
                Method refreshMethod = ServerGUI.class.getDeclaredMethod("refreshDataInspection");
                refreshMethod.setAccessible(true);
                refreshMethod.invoke(serverGUI);

                // 4. Capture screen after delay
                Timer timer = new Timer(1500, e -> {
                    try {
                        Rectangle screenBounds = new Rectangle(0, 0, 1920, 1080);
                        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                        GraphicsDevice gd = ge.getDefaultScreenDevice();
                        if (gd != null && gd.getDisplayMode() != null) {
                            screenBounds = new Rectangle(0, 0, gd.getDisplayMode().getWidth(), gd.getDisplayMode().getHeight());
                        }

                        Robot robot = new Robot();
                        BufferedImage screenshot = robot.createScreenCapture(screenBounds);

                        File outputFile = new File("C:/Users/LEGION/.gemini/antigravity-ide/brain/0b4aec84-87ec-47ae-a54c-e2aef6c01009/gui_preview.png");
                        ImageIO.write(screenshot, "png", outputFile);
                        System.out.println("===> ĐÃ CHỤP MÀN HÌNH GIAO DIỆN THÀNH CÔNG: " + outputFile.getAbsolutePath());

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        System.exit(0);
                    }
                });
                timer.setRepeats(false);
                timer.start();

            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(1);
            }
        });
    }
}
