package com.mycompany.bidverse;

import javax.swing.*;

/**
 * Bidder registration form
 */
public class BidderRegisterPage extends JPanel {
    private MainFrame navigator;
    private final JTextField nameField = new JTextField();
    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JTextField phoneField = new JTextField();
    private final JTextField addressField = new JTextField();

    public BidderRegisterPage(MainFrame navigator) {
        this.navigator = navigator;
        setLayout(null);
        GradientPanel bg = new GradientPanel();
        bg.setLayout(null);
        bg.setBounds(0, 0, 480, 640);

        JLabel title = new JLabel("Bidder Registration", SwingConstants.CENTER);
        title.setFont(Theme.TITLE_FONT);
        title.setBounds(40, 18, 400, 36);
        bg.add(title);

        nameField.setBorder(BorderFactory.createTitledBorder("Full name"));
        nameField.setBounds(60, 80, 360, 40);
        bg.add(nameField);

        emailField.setBorder(BorderFactory.createTitledBorder("Email"));
        emailField.setBounds(60, 140, 360, 40);
        bg.add(emailField);

        passwordField.setBorder(BorderFactory.createTitledBorder("Password"));
        passwordField.setBounds(60, 200, 360, 40);
        bg.add(passwordField);

        phoneField.setBorder(BorderFactory.createTitledBorder("Phone"));
        phoneField.setBounds(60, 260, 360, 40);
        bg.add(phoneField);

        addressField.setBorder(BorderFactory.createTitledBorder("Address"));
        addressField.setBounds(60, 320, 360, 40);
        bg.add(addressField);

        JButton registerBtn = new JButton("Register");
        registerBtn.setBounds(60, 400, 360, 44);
        registerBtn.setBackground(Theme.PURPLE);
        registerBtn.setForeground(java.awt.Color.WHITE);
        registerBtn.setFont(Theme.BUTTON_FONT);
        registerBtn.addActionListener(e -> onRegister());
        bg.add(registerBtn);

        JButton backBtn = new JButton("Back");
        backBtn.setBounds(60, 460, 360, 40);
        backBtn.addActionListener(e -> navigator.showCard("profile"));
        bg.add(backBtn);

        add(bg);
    }

    private void onRegister() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String phone = phoneField.getText().trim();
        String address = addressField.getText().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Name, email and password are required", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean ok = BidderService.register(name, email, password, phone, address);
        if (ok) {
            // Display success message
            JOptionPane.showMessageDialog(this, "Bidder registered successfully. Please log in.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            // --- CRITICAL CHANGE MADE HERE ---
            // Go back to the login page (assuming its card name is "login")
            navigator.showCard("login");
            // --- END CRITICAL CHANGE ---

            // Optional: Clear fields after successful registration
            nameField.setText("");
            emailField.setText("");
            passwordField.setText("");
            phoneField.setText("");

        } else {
            JOptionPane.showMessageDialog(this, "Registration failed", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
