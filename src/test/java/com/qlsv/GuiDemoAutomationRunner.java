package com.qlsv;

import com.formdev.flatlaf.FlatLightLaf;
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
        System.out.println("========== KHỞI CHẠY CAPTURE MÀN HÌNH GIAO DIỆN CHUẨN TONE ĐỎ TRẮNG PTIT ==========");

        try {
            FlatLightLaf.setup();
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            try {
                int port = 9876;

                ServerGUI serverGUI = new ServerGUI();
                serverGUI.setSize(950, 720);
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
                clientGUI.setSize(950, 720);
                clientGUI.setVisible(true);

                Timer timer = new Timer(1200, e -> {
                    try {
                        BufferedImage combined = new BufferedImage(1920, 750, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g = combined.createGraphics();
                        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setColor(new Color(245, 247, 250));
                        g.fillRect(0, 0, 1920, 750);

                        // Paint Server GUI
                        Graphics2D gServer = (Graphics2D) g.create(10, 10, 940, 730);
                        serverGUI.paint(gServer);
                        gServer.dispose();

                        // Paint Client GUI
                        Graphics2D gClient = (Graphics2D) g.create(960, 10, 940, 730);
                        clientGUI.paint(gClient);
                        gClient.dispose();

                        g.dispose();

                        File outputFile = new File("C:/Users/LEGION/.gemini/antigravity-ide/brain/0b4aec84-87ec-47ae-a54c-e2aef6c01009/ptit_gui_preview.png");
                        ImageIO.write(combined, "png", outputFile);
                        System.out.println("===> ĐÃ CHỤP MÀN HÌNH GIAO DIỆN CHUẨN THỰC TẾ PTIT THÀNH CÔNG: " + outputFile.getAbsolutePath());

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
