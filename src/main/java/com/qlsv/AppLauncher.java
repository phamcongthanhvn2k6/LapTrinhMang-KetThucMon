package com.qlsv;

import com.formdev.flatlaf.FlatDarkLaf;
import com.qlsv.client.ClientGUI;
import com.qlsv.server.ServerGUI;

import javax.swing.*;
import java.awt.*;

public class AppLauncher {

    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("CHƯƠNG TRÌNH QUẢN LÝ SINH VIÊN (UDP - CLIENT/SERVER)");
            frame.setSize(520, 320);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);

            JPanel panel = new JPanel(new BorderLayout(15, 15));
            panel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

            JLabel lblTitle = new JLabel("ĐỀ TÀI 12: QUẢN LÝ SINH VIÊN VIA UDP", SwingConstants.CENTER);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
            lblTitle.setForeground(new Color(90, 160, 250));

            JLabel lblSub = new JLabel("<html><center>Mô hình Client-Server | Mã hóa DES | Kết nối CSDL SQL<br>Chọn giao diện ứng dụng bạn muốn khởi chạy:</center></html>", SwingConstants.CENTER);
            lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            JPanel headerBox = new JPanel(new GridLayout(2, 1, 5, 5));
            headerBox.add(lblTitle);
            headerBox.add(lblSub);
            panel.add(headerBox, BorderLayout.NORTH);

            JPanel btnPanel = new JPanel(new GridLayout(3, 1, 10, 12));

            JButton btnBoth = new JButton("Khởi chạy CẢ HAI (Server GUI & Client GUI)");
            btnBoth.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnBoth.setBackground(new Color(40, 167, 69));
            btnBoth.setForeground(Color.WHITE);
            btnBoth.setFocusPainted(false);
            btnBoth.addActionListener(e -> {
                frame.dispose();
                launchServer();
                launchClient();
            });

            JButton btnServer = new JButton("Chỉ chạy SERVER GUI");
            btnServer.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnServer.setBackground(new Color(0, 122, 255));
            btnServer.setForeground(Color.WHITE);
            btnServer.setFocusPainted(false);
            btnServer.addActionListener(e -> {
                frame.dispose();
                launchServer();
            });

            JButton btnClient = new JButton("Chỉ chạy CLIENT GUI");
            btnClient.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnClient.setBackground(new Color(108, 117, 125));
            btnClient.setForeground(Color.WHITE);
            btnClient.setFocusPainted(false);
            btnClient.addActionListener(e -> {
                frame.dispose();
                launchClient();
            });

            btnPanel.add(btnBoth);
            btnPanel.add(btnServer);
            btnPanel.add(btnClient);

            panel.add(btnPanel, BorderLayout.CENTER);

            frame.add(panel);
            frame.setVisible(true);
        });
    }

    private static void launchServer() {
        SwingUtilities.invokeLater(() -> {
            ServerGUI serverGUI = new ServerGUI();
            serverGUI.setLocation(100, 100);
            serverGUI.setVisible(true);
        });
    }

    private static void launchClient() {
        SwingUtilities.invokeLater(() -> {
            ClientGUI clientGUI = new ClientGUI();
            clientGUI.setLocation(600, 100);
            clientGUI.setVisible(true);
        });
    }
}
