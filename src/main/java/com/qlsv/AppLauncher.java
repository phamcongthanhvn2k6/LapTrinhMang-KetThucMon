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
            frame.setSize(580, 420);
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

            JLabel lblSub = new JLabel("HỆ THỐNG QUẢN LÝ THÔNG TIN & ĐÀO TẠO SINH VIÊN", SwingConstants.CENTER);
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
            contentPanel.setBorder(BorderFactory.createEmptyBorder(18, 25, 18, 25));
            contentPanel.setBackground(Color.WHITE);

            JLabel lblPrompt = new JLabel("Vui lòng chọn phân hệ bạn muốn khởi chạy:", SwingConstants.CENTER);
            lblPrompt.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblPrompt.setForeground(new Color(40, 40, 40));
            contentPanel.add(lblPrompt, BorderLayout.NORTH);

            JPanel btnPanel = new JPanel(new GridLayout(4, 1, 10, 10));
            btnPanel.setOpaque(false);

            JButton btnBoth = new JButton("1. Khởi Chạy Toàn Bộ Hệ Thống (Server & Cổng Sinh Viên)");
            btnBoth.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnBoth.setBackground(PTIT_RED);
            btnBoth.setForeground(Color.WHITE);
            btnBoth.setFocusPainted(false);
            btnBoth.addActionListener(e -> {
                frame.dispose();
                launchServer();
                javax.swing.Timer timer = new javax.swing.Timer(400, evt -> launchClient(0));
                timer.setRepeats(false);
                timer.start();
            });

            JButton btnStudent = new JButton("2. Khởi Chạy Cổng Thông Tin Sinh Viên (Tra Cứu Học Tập)");
            btnStudent.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnStudent.setBackground(new Color(0, 123, 255));
            btnStudent.setForeground(Color.WHITE);
            btnStudent.setFocusPainted(false);
            btnStudent.addActionListener(e -> {
                frame.dispose();
                launchClient(0);
            });

            JButton btnTeacher = new JButton("3. Khởi Chạy Phân Hệ Giảng Viên (Quản Lý & Chấm Điểm)");
            btnTeacher.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnTeacher.setBackground(new Color(40, 167, 69));
            btnTeacher.setForeground(Color.WHITE);
            btnTeacher.setFocusPainted(false);
            btnTeacher.addActionListener(e -> {
                frame.dispose();
                launchClient(1);
            });

            JButton btnServer = new JButton("4. Khởi Chạy Trung Tâm Máy Chủ Server (RUDP & Database)");
            btnServer.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btnServer.setBackground(new Color(50, 50, 50));
            btnServer.setForeground(Color.WHITE);
            btnServer.setFocusPainted(false);
            btnServer.addActionListener(e -> {
                frame.dispose();
                launchServer();
            });

            btnPanel.add(btnBoth);
            btnPanel.add(btnStudent);
            btnPanel.add(btnTeacher);
            btnPanel.add(btnServer);

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

    private static void launchClient(int tabIndex) {
        SwingUtilities.invokeLater(() -> {
            ClientGUI clientGUI = new ClientGUI();
            clientGUI.setLocation(650, 80);
            if (clientGUI.getMainTabbedPane() != null && tabIndex >= 0) {
                clientGUI.getMainTabbedPane().setSelectedIndex(tabIndex);
            }
            clientGUI.setVisible(true);
        });
    }
}
