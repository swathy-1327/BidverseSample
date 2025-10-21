package com.mycompany.bidverse;
import javax.swing.*;
import java.awt.*;
import static javax.swing.JOptionPane.showMessageDialog;

public class SellerProfile extends JPanel {
    // --- COLOR CONSTANTS (Theme-aligned) ---
    private static final Color PRIMARY_PURPLE = new Color(90, 0, 160);     // Main purple
    private static final Color ACCENT_PURPLE = new Color(130, 0, 200);     // Lighter purple accent
    private static final Color DANGER_COLOR = new Color(200, 30, 60);      // Red for delete
    private static final Color HEADER_BG = new Color(230, 220, 255);       // Soft lavender
    private static final Color PANEL_BG = Color.WHITE;                     // Content background
    private static final Color PAGE_BG = new Color(245, 240, 255);         // Very light lavender-gray

    private final JFrame mainFrame;
    private BackendClient.SellerDto currentSeller;

    private final JLabel nameValue = new JLabel();
    private final JLabel emailValue = new JLabel(Main.email);
    private final JLabel phoneValue = new JLabel();
    private final JLabel paymentValue = new JLabel();

    public SellerProfile(JFrame frame) {
        this.mainFrame = frame;
        setLayout(new BorderLayout(15, 15));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setBackground(PAGE_BG);

        // --- Top Bar / Back Button (styled like a link) ---
        JButton backButton = new JButton("← Back to Home");
        backButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        backButton.setForeground(PRIMARY_PURPLE);
        backButton.setContentAreaFilled(false);
        backButton.setBorderPainted(false);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> Main.switchToPanel(mainFrame, new SellerHome(mainFrame)));

        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        northPanel.setBackground(HEADER_BG);
        northPanel.add(backButton);
        add(northPanel, BorderLayout.NORTH);

        // --- Profile Content Panel ---
        JPanel profilePanel = new JPanel(new GridBagLayout());
        profilePanel.setBackground(PANEL_BG);
        profilePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210), 1),
                BorderFactory.createEmptyBorder(30, 30, 30, 30)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel title = new JLabel("Seller Profile");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(PRIMARY_PURPLE);
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        profilePanel.add(title, gbc);

        gbc.gridy = 1;
        profilePanel.add(new JSeparator(SwingConstants.HORIZONTAL), gbc);
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        // --- Helper for adding label/value pairs ---
        var helper = new Object() {
            int row = 2;
            void addField(String labelText, JLabel valueLabel) {
                JLabel label = new JLabel(labelText);
                label.setFont(new Font("Segoe UI", Font.BOLD, 14));
                gbc.gridy = row;
                gbc.gridx = 0;
                profilePanel.add(label, gbc);

                valueLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                gbc.gridx = 1;
                profilePanel.add(valueLabel, gbc);
                row++;
            }
        };

        // Profile Fields
        helper.addField("Name:", nameValue);
        helper.addField("Email:", emailValue);
        helper.addField("Phone No.:", phoneValue);
        helper.addField("Payment Details:", paymentValue);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setBackground(PAGE_BG);
        centerWrapper.add(profilePanel);
        add(centerWrapper, BorderLayout.CENTER);

        // --- Bottom Buttons ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 20));
        buttonPanel.setBackground(PAGE_BG);

        JButton updateButton = new JButton("Update Profile");
        styleButton(updateButton, PRIMARY_PURPLE, Color.WHITE);
        updateButton.addActionListener(e -> openUpdateWindow());

        JButton deleteButton = new JButton("Delete User");
        styleButton(deleteButton, DANGER_COLOR, Color.WHITE);
        deleteButton.addActionListener(e -> deleteUser());

        buttonPanel.add(updateButton);
        buttonPanel.add(deleteButton);
        add(buttonPanel, BorderLayout.SOUTH);

        loadProfileData();
    }

    // --- Utility for consistent button styling ---
    private void styleButton(JButton button, Color bg, Color fg) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
    }

    // --- Load Seller Data ---
    private void loadProfileData() {
        BackendClient.getSellerByEmail(Main.email).ifPresentOrElse(seller -> {
            this.currentSeller = seller;
            nameValue.setText(seller.sellerName());
            phoneValue.setText(String.valueOf(seller.phno()));
            paymentValue.setText(seller.paymentDetails());
            Main.sellerName = seller.sellerName();
            mainFrame.setTitle("BidVerse - Seller Panel (" + Main.sellerName + ")");
        }, () -> {
            showMessageDialog(mainFrame, "Failed to load profile. Connection error.", "Error", JOptionPane.ERROR_MESSAGE);
            nameValue.setText("Error Loading Data");
        });
    }

    // --- Update Window ---
    private void openUpdateWindow() {
        if (currentSeller == null) return;

        JDialog updateDialog = new JDialog(mainFrame, "Update Profile", true);
        updateDialog.setLayout(new GridBagLayout());
        updateDialog.setSize(400, 300);
        updateDialog.setLocationRelativeTo(mainFrame);
        updateDialog.getContentPane().setBackground(PANEL_BG);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField nameField = new JTextField(currentSeller.sellerName(), 20);
        JTextField phoneField = new JTextField(String.valueOf(currentSeller.phno()), 20);
        JTextArea paymentArea = new JTextArea(currentSeller.paymentDetails(), 4, 20);
        paymentArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(paymentArea);

        gbc.gridx = 0; gbc.gridy = 0; updateDialog.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1; updateDialog.add(nameField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; updateDialog.add(new JLabel("Phone No.:"), gbc);
        gbc.gridx = 1; updateDialog.add(phoneField, gbc);
        gbc.gridx = 0; gbc.gridy = 2; updateDialog.add(new JLabel("Payment Details:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        updateDialog.add(scrollPane, gbc);

        JButton okButton = new JButton("Save Changes");
        styleButton(okButton, ACCENT_PURPLE, Color.WHITE);
        okButton.addActionListener(e -> {
            try {
                BackendClient.SellerDto updated = new BackendClient.SellerDto(
                        currentSeller.sellerId(),
                        nameField.getText(),
                        currentSeller.sellerEmail(),
                        Long.parseLong(phoneField.getText()),
                        paymentArea.getText()
                );

                BackendClient.updateSeller(Main.email, updated).ifPresentOrElse(
                        saved -> {
                            showMessageDialog(mainFrame, "Profile Updated Successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                            loadProfileData();
                            updateDialog.dispose();
                        },
                        () -> showMessageDialog(mainFrame, "Update failed. Server error.", "Error", JOptionPane.ERROR_MESSAGE)
                );
            } catch (NumberFormatException ex) {
                showMessageDialog(updateDialog, "Invalid Phone Number format.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        gbc.gridx = 1; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        updateDialog.add(okButton, gbc);

        updateDialog.setVisible(true);
    }

    // --- Delete User ---
    private void deleteUser() {
        int confirm = JOptionPane.showConfirmDialog(mainFrame,
                "Are you sure you want to delete your account? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            if (BackendClient.deleteSeller(Main.email)) {
                showMessageDialog(mainFrame, "Account deleted. Logging out.", "Success", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            } else {
                showMessageDialog(mainFrame, "Failed to delete account. Server error.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
