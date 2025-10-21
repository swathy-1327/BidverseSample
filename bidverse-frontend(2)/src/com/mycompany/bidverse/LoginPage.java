package com.mycompany.bidverse;

import javax.swing.*;

import java.awt.*;

/**
 * LoginPage is a JPanel used inside MainFrame.
 */
public class LoginPage extends JPanel {
    private MainFrame navigator;
    private JTextField emailField;
    private JPasswordField passwordField;

    public LoginPage(MainFrame navigator) {
        this.navigator = navigator;
        setLayout(null);
        GradientPanel bg = new GradientPanel();
        bg.setLayout(null);
        bg.setBounds(0, 0, 800, 600);

        JLabel title = new JLabel("Bidverse", SwingConstants.CENTER);
        title.setFont(Theme.TITLE_FONT);
        title.setBounds(250, 50, 300, 40);
        bg.add(title);

        // Email
        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setFont(Theme.LABEL_FONT);
        emailLabel.setBounds(220, 150, 80, 25);
        bg.add(emailLabel);

        emailField = new JTextField();
        emailField.setBounds(220, 180, 360, 36);
        bg.add(emailField);

        // Password
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(Theme.LABEL_FONT);
        passLabel.setBounds(220, 240, 80, 25);
        bg.add(passLabel);

        passwordField = new JPasswordField();
        passwordField.setBounds(220, 270, 360, 36);
        bg.add(passwordField);

        // Login button
        JButton loginBtn = new JButton("Login");
        loginBtn.setFont(Theme.BUTTON_FONT);
        loginBtn.setBackground(Theme.PURPLE);
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setBounds(220, 350, 360, 44);
        loginBtn.addActionListener(e -> onLogin());
        bg.add(loginBtn);

        // Register button
        JButton registerBtn = new JButton("Register");
        registerBtn.setFont(Theme.BUTTON_FONT);
        registerBtn.setBackground(Theme.LIGHT_PURPLE);
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setBounds(220, 410, 360, 44);
        registerBtn.addActionListener(e -> navigator.showCard("profile"));
        bg.add(registerBtn);

        add(bg);
    }

    private void onLogin() {
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter email and password", "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Call backend (still returns boolean)
        boolean ok = AuthService.login(email, password);

        if (ok) {
            // Retrieve the role from the static field set by AuthService
            String userRole = AuthService.getCurrentUserRole();

            // Clear fields after successful login
            emailField.setText("");
            passwordField.setText("");

            JOptionPane.showMessageDialog(this, "Login successful as " + userRole, "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            // Navigate based on the retrieved role
            if ("bidder".equalsIgnoreCase(userRole)) {
                // Get bidder information and pass it to the dashboard
                Long bidderId = AuthService.getCurrentUserId();
                String bidderEmail = AuthService.getCurrentUserEmail();
                String bidderName = AuthService.getCurrentUserName();
                
                // Set bidder info in the dashboard before showing it
                navigator.setBidderInfo(bidderId, bidderEmail, bidderName);
                navigator.showCard("bidderdashboard");
            } else if ("seller".equalsIgnoreCase(userRole)) {
                // Assuming your Seller Dashboard card name is "seller_dashboard"
                navigator.showCard("seller_dashboard");
            } else {
                // Default action if role is missing or unexpected
                navigator.closeAppWithSuccess("Login successful, but role is undetermined.");
            }

        } else {
            // Login failed (AuthService.login returned false)
            JOptionPane.showMessageDialog(this, "Invalid credentials or user not found", "Login failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
