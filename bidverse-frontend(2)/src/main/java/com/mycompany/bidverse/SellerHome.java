package com.mycompany.bidverse;
import javax.swing.*;
import java.awt.*;

public class SellerHome extends JPanel {

    // --- Color Palette ---
    private static final Color SIDEBAR_COLOR = new Color(76, 0, 153);          // Deep Purple
    private static final Color ACCENT_COLOR = new Color(147, 112, 219);        // Soft Purple
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 255);    // Light Lavender
    private static final Color BUTTON_COLOR = new Color(98, 0, 238);           // Indigo
    private static final Color BUTTON_HOVER = new Color(123, 31, 162);         // Darker purple
    private static final Color TEXT_COLOR = new Color(51, 51, 51);             // Dark gray text
    private static final Font MAIN_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font BUTTON_FONT = new Font("Segoe UI Semibold", Font.BOLD, 14);

    private final JFrame mainFrame;

    public SellerHome(JFrame frame) {
        this.mainFrame = frame;
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);

        // --- Left Sidebar ---
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(SIDEBAR_COLOR);
        sidebar.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel logoLabel = new JLabel("BidVerse Seller");
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        logoLabel.setForeground(Color.WHITE);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        sidebar.add(logoLabel);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        // Sidebar Buttons
        JButton homeButton = createSidebarButton("HOME", true);

        JButton profileButton = createSidebarButton("PROFILE 👨🏻‍💼", false);
        JButton logoutButton = createSidebarButton("LOGOUT", false);

        sidebar.add(homeButton);
    
        sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        sidebar.add(profileButton);
        sidebar.add(Box.createVerticalGlue());
        sidebar.add(logoutButton);

        add(sidebar, BorderLayout.WEST);

        // --- Main Content Area ---
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBackground(BACKGROUND_COLOR);

        JLabel welcomeLabel = new JLabel("Welcome to your Seller Dashboard!");
        welcomeLabel.setFont(new Font("Segoe UI Semibold", Font.BOLD, 20));
        welcomeLabel.setForeground(SIDEBAR_COLOR);

        JButton createBidButton = createMainButton("➕ Create New Auction");
        JButton viewCurrentBidsButton = createMainButton("View Current Auctions");
        JButton viewPastBidsButton = createMainButton("View Past Auctions");

        // --- Button Actions ---
        createBidButton.addActionListener(e -> new CreateBidWin(mainFrame).setVisible(true));

        viewCurrentBidsButton.addActionListener(e -> {
            SellerCurrentBids currentBidsPanel = new SellerCurrentBids(mainFrame);
            Main.switchToPanel(mainFrame, currentBidsPanel);
        });

        viewPastBidsButton.addActionListener(e -> {
            SellerPastBids pastBidsPanel = new SellerPastBids(mainFrame);
            Main.switchToPanel(mainFrame, pastBidsPanel);
        });

        profileButton.addActionListener(e -> {
            SellerProfile profilePanel = new SellerProfile(mainFrame);
            Main.switchToPanel(mainFrame, profilePanel);
        });

        logoutButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(mainFrame,
                    "Are you sure you want to log out?",
                    "Logout Confirmation", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(mainFrame,
                        "Logged out successfully.", "Logout", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            }
        });

        // --- Layout Setup ---
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(15, 0, 15, 0);
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        gbc.gridy = 0;
        contentPanel.add(welcomeLabel, gbc);

        gbc.gridy = 1;
        contentPanel.add(createBidButton, gbc);

        gbc.gridy = 2;
        contentPanel.add(viewCurrentBidsButton, gbc);

        gbc.gridy = 3;
        contentPanel.add(viewPastBidsButton, gbc);

        add(contentPanel, BorderLayout.CENTER);
    }

    // --- Helper Methods ---

    private JButton createSidebarButton(String text, boolean selected) {
        JButton button = new JButton(text);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.setFocusPainted(false);
        button.setBackground(selected ? BUTTON_HOVER : SIDEBAR_COLOR);
        button.setForeground(Color.WHITE);
        button.setFont(BUTTON_FONT);
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        button.setHorizontalAlignment(SwingConstants.LEFT);

        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_HOVER);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                if (!selected) button.setBackground(SIDEBAR_COLOR);
            }
        });
        return button;
    }

    private JButton createMainButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(BUTTON_COLOR);
        button.setForeground(Color.WHITE);
        button.setFont(BUTTON_FONT);
        button.setPreferredSize(new Dimension(260, 55));
        button.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_HOVER);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_COLOR);
            }
        });
        return button;
    }
}
