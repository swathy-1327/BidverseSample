package com.mycompany.bidverse;

import javax.swing.*;
import java.awt.*;
import static javax.swing.JOptionPane.showMessageDialog;

public class CreateBidWin extends JDialog {
    // --- THEME COLORS ---
    private static final Color PRIMARY_PURPLE = new Color(90, 0, 160);     // Main purple
    private static final Color ACCENT_PURPLE = new Color(130, 0, 200);     // Lighter accent
    private static final Color HEADER_BG = new Color(230, 220, 255);       // Soft lavender
    private static final Color PAGE_BG = new Color(245, 240, 255);         // Light lavender-gray
    private static final Color PANEL_BG = Color.WHITE;                     // White form background
    private static final Color NEUTRAL_GRAY = new Color(200, 200, 200);    // For neutral buttons

    // --- Form Fields ---
    private final JTextField titleField = new JTextField(25);
    private final JTextField categoryField = new JTextField(25);
    private final JTextArea descriptionArea = new JTextArea(3, 25);
    private final JTextField startTimeField = new JTextField(15);
    private final JTextField endTimeField = new JTextField(15);
    private final JTextField basePriceField = new JTextField(10);
    private final JTextField imagePathField = new JTextField(20);

    private final JFrame mainFrame;

    public CreateBidWin(JFrame parent) {
        super(parent, "Create New Auction", true);
        this.mainFrame = parent;

        // Dialog setup
        getContentPane().setBackground(PAGE_BG);
        setLayout(new BorderLayout(15, 15));
        setSize(500, 550);
        setLocationRelativeTo(parent);

        // --- FORM WRAPPER PANEL ---
        JPanel formWrapperPanel = new JPanel(new GridBagLayout());
        formWrapperPanel.setBackground(PANEL_BG);
        formWrapperPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(210, 210, 210), 1),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // --- Title ---
        JLabel formTitle = new JLabel("Create New Auction Item");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        formTitle.setForeground(PRIMARY_PURPLE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formWrapperPanel.add(formTitle, gbc);

        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;

        // --- Helper for adding fields ---
        var helper = new Object() {
            int row = 1;
            void addField(String labelText, JComponent component, int span) {
                JLabel label = new JLabel(labelText);
                label.setFont(new Font("Segoe UI", Font.BOLD, 13));
                gbc.gridx = 0;
                gbc.gridy = row;
                formWrapperPanel.add(label, gbc);

                gbc.gridx = 1;
                gbc.gridy = row;
                gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.gridwidth = span;
                formWrapperPanel.add(component, gbc);

                gbc.weightx = 0;
                gbc.fill = GridBagConstraints.NONE;
                gbc.gridwidth = 1;
                row++;
            }
        };

        // --- Input Fields ---
        helper.addField("Title:", titleField, 1);
        helper.addField("Category:", categoryField, 1);

        JScrollPane descriptionScrollPane = new JScrollPane(descriptionArea);
        descriptionScrollPane.setBorder(BorderFactory.createLineBorder(new Color(210, 210, 210)));
        helper.addField("Description:", descriptionScrollPane, 1);

        // --- Time Fields Row ---
        JPanel timePanel = new JPanel(new GridLayout(1, 4, 10, 0));
        timePanel.setBackground(PANEL_BG);
        timePanel.add(new JLabel("Start Time (HH:mm:ss):"));
        timePanel.add(startTimeField);
        timePanel.add(new JLabel("End Time (HH:mm:ss):"));
        timePanel.add(endTimeField);

        gbc.gridx = 0;
        gbc.gridy = helper.row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        formWrapperPanel.add(timePanel, gbc);
        helper.row++;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;

        helper.addField("Base Price:", basePriceField, 1);

        // --- Image Upload Row ---
        JPanel imagePanel = new JPanel(new BorderLayout(5, 0));
        imagePanel.setBackground(PANEL_BG);
        imagePanel.add(imagePathField, BorderLayout.CENTER);

        JButton browseButton = new JButton("Browse");
        styleSecondaryButton(browseButton);
        browseButton.addActionListener(e -> browseImageFile());
        imagePanel.add(browseButton, BorderLayout.EAST);
        helper.addField("Image:", imagePanel, 1);

        add(formWrapperPanel, BorderLayout.CENTER);

        // --- Button Panel (Bottom) ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 15));
        buttonPanel.setBackground(PAGE_BG);

        JButton createButton = new JButton("Create Bid");
        stylePrimaryButton(createButton);
        createButton.addActionListener(e -> createAuction());

        JButton cancelButton = new JButton("Cancel");
        styleSecondaryButton(cancelButton);
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(createButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // --- STYLE HELPERS ---
    private void stylePrimaryButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBackground(PRIMARY_PURPLE);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
    }

    private void styleSecondaryButton(JButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(NEUTRAL_GRAY);
        button.setForeground(Color.DARK_GRAY);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
    }

    // --- FILE BROWSER ---
    private void browseImageFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            imagePathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    // --- CREATE AUCTION ---
    private void createAuction() {
        try {
            String title = titleField.getText();
            String category = categoryField.getText();
            String description = descriptionArea.getText();
            Double basePrice = Double.parseDouble(basePriceField.getText());
            String startTime = startTimeField.getText();
            String endTime = endTimeField.getText();
            String imagePath = imagePathField.getText();

            if (title.isEmpty() || category.isEmpty() || description.isEmpty() ||
                    startTime.isEmpty() || endTime.isEmpty()) {
                showMessageDialog(this, "Please fill in all text fields.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            BackendClient.AuctionItemDto newAuctionDto = new BackendClient.AuctionItemDto(
                    null, title, basePrice, category, description,
                    "OPEN", Main.sell_id, null, startTime, endTime);

            BackendClient.createAuction(newAuctionDto).ifPresentOrElse(savedAuction -> {
                Long newAuctionId = savedAuction.auctionId();
                if (newAuctionId != null) {
                    if (imagePath != null && !imagePath.trim().isEmpty()) {
                        if (BackendClient.uploadImage(imagePath, newAuctionId)) {
                            showMessageDialog(mainFrame, "Auction Created and Image Uploaded Successfully!",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            showMessageDialog(mainFrame, "Auction Created, but Image Upload Failed.",
                                    "Warning", JOptionPane.WARNING_MESSAGE);
                        }
                    } else {
                        showMessageDialog(mainFrame, "Auction Created Successfully (No Image Provided).",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                    dispose();
                } else {
                    showMessageDialog(mainFrame, "Failed to create auction: Server returned null ID.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }, () -> showMessageDialog(mainFrame, "Failed to create auction. Check server status/logs.",
                    "Error", JOptionPane.ERROR_MESSAGE));

        } catch (NumberFormatException ex) {
            showMessageDialog(this, "Invalid Base Price format. Please use numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            showMessageDialog(this, "An unexpected error occurred: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}
