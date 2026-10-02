package com.qlsv;

import com.formdev.flatlaf.FlatLightLaf;
import com.qlsv.client.ClientGUI;
import com.qlsv.server.ServerGUI;

import javax.swing.*;
import java.awt.*;

public class AppLauncher {

    public static final Color PTIT_RED = new Color(200, 16, 46);       // #C8102E
    public static final Color PTIT_DARK_RED = new Color(150, 10, 30);  // #960A1E
    public static final Color PTIT_LIGHT_BG = new Color(248, 249, 250);

    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG - PTIT");
            frame.setSize(560, 360);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);

            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.setBackground(Color.WHITE);

            // PTIT Header Banner (Red background, white text)
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(PTIT_RED);
            headerPanel.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

            JLabel lblTitle = new JLabel("HỌC VIỆN CÔNG NGHỆ BƯU CHÍNH VIỄN THÔNG", SwingConstants.CENTER);
            lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
            lblTitle.setForeground(Color.WHITE);

            JLabel lblSub = new JLabel("ĐỀ TÀI 12: CHƯƠNG TRÌNH QUẢN LÝ SINH VIÊN (UDP & MÃ HÓA)", SwingConstants.CENTER);
            lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lblSub.setForeground(new Color(255, 230, 230));

            JPanel headerBox = new JPanel(new GridLayout(2, 1, 4, 4));
            headerBox.setOpaque(false);
            headerBox.add(lblTitle);
            headerBox.add(lblSub);
            headerPanel.add(headerBox, BorderLayout.CENTER);

            mainPanel.add(headerPanel, BorderLayout.NORTH);

            // Content Body
            JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
            contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
            contentPanel.setBackground(Color.WHITE);

            JLabel lblPrompt = new JLabel("Vui lòng chọn giao diện phân hệ bạn muốn khởi chạy:", SwingConstants.CENTER);
            lblPrompt.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblPrompt.setForeground(new Color(40, 40, 40));
            contentPanel.add(lblPrompt, BorderLayout.NORTH);

            JPanel btnPanel = new JPanel(new GridLayout(3, 1, 10, 10));
            btnPanel.setOpaque(false);

            JButton btnBoth = new JButton("Khởi Chạy CẢ HAI (Server GUI & Client GUI)");
            btnBoth.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnBoth.setBackground(PTIT_RED);
            btnBoth.setForeground(Color.WHITE);
            btnBoth.setFocusPainted(false);
            btnBoth.addActionListener(e -> {
                frame.dispose();
                launchServer();
                launchClient();
            });

            JButton btnServer = new JButton("Khởi Chạy SERVER GUI (Quản lý UDP & CSDL)");
            btnServer.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnServer.setBackground(new Color(40, 40, 40));
            btnServer.setForeground(Color.WHITE);
            btnServer.setFocusPainted(false);
            btnServer.addActionListener(e -> {
                frame.dispose();
                launchServer();
            });

            JButton btnClient = new JButton("Khởi Chạy CLIENT GUI (Nhập liệu & Tra cứu)");
            btnClient.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnClient.setBackground(new Color(100, 100, 100));
            btnClient.setForeground(Color.WHITE);
            btnClient.setFocusPainted(false);
            btnClient.addActionListener(e -> {
                frame.dispose();
                launchClient();
            });

            btnPanel.add(btnBoth);
            btnPanel.add(btnServer);
            btnPanel.add(btnClient);

            contentPanel.add(btnPanel, BorderLayout.CENTER);
            mainPanel.add(contentPanel, BorderLayout.CENTER);

            frame.add(mainPanel);
            frame.setVisible(true);
        });
    }

    private static void launchServer() {
        SwingUtilities.invokeLater(() -> {
            ServerGUI serverGUI = new ServerGUI();
            serverGUI.setLocation(80, 80);
            serverGUI.setVisible(true);
        });
    }

    private static void launchClient() {
        SwingUtilities.invokeLater(() -> {
            ClientGUI clientGUI = new ClientGUI();
            clientGUI.setLocation(650, 80);
            clientGUI.setVisible(true);
        });
    }
}
