package com.qlsv;

import com.formdev.flatlaf.FlatDarkLaf;
import com.qlsv.client.ClientGUI;
import com.qlsv.database.DatabaseManager;
import com.qlsv.model.SqlConfig;
import com.qlsv.model.StudentData;
import com.qlsv.server.ServerGUI;
import com.qlsv.server.UDPServer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class GuiDemoAutomationRunner {

    public static void main(String[] args) {
        System.out.println("========== KHỞI CHẠY GUI AUTOMATION CHO HỆ THỐNG NÂNG CẤP TOÀN DIỆN ==========");

        try {
            FlatDarkLaf.setup();
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            try {
                int port = 9876;

                ServerGUI serverGUI = new ServerGUI();
                serverGUI.setBounds(30, 30, 950, 700);
                serverGUI.setVisible(true);

                UDPServer udpServer = new UDPServer(port);
                udpServer.setLogListener(serverGUI);
                udpServer.start();

                SqlConfig h2Config = new SqlConfig(
                        SqlConfig.DbType.H2_EMBEDDED,
                        "localhost", 0, "test_qlsv_db", "sa", ""
                );
                DatabaseManager.getInstance().connect(h2Config);

                DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(
                        new StudentData("SV001", "Nguyễn Văn An", 9.0, 8.5, 9.5));
                DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(
                        new StudentData("SV002", "Lê Thị Bình", 7.5, 8.0, 8.5));
                DatabaseManager.getInstance().saveStudentEncryptedAndCalculateResult(
                        new StudentData("SV003", "Phạm Minh Cường", 9.5, 10.0, 9.0));

                ClientGUI clientGUI = new ClientGUI();
                clientGUI.setBounds(1000, 30, 880, 700);
                clientGUI.setVisible(true);

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

                        File outputFile = new File("C:/Users/LEGION/.gemini/antigravity-ide/brain/0b4aec84-87ec-47ae-a54c-e2aef6c01009/upgraded_gui_preview.png");
                        ImageIO.write(screenshot, "png", outputFile);
                        System.out.println("===> ĐÃ CHỤP MÀN HÌNH NÂNG CẤP THÀNH CÔNG: " + outputFile.getAbsolutePath());

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
