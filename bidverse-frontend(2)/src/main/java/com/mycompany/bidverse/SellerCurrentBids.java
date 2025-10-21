package com.mycompany.bidverse;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

import static javax.swing.JOptionPane.showMessageDialog;

public class SellerCurrentBids extends JPanel {

    // --- PURPLE THEME COLORS ---
    private static final Color PRIMARY_PURPLE = new Color(90, 0, 160);       // Deep purple
    private static final Color ACCENT_PURPLE = new Color(130, 0, 200);       // Lighter accent
    private static final Color BACKGROUND_COLOR = new Color(245, 240, 255);  // Soft lavender-gray
    private static final Color PANEL_BG = Color.WHITE;                       // White panels/tables
    private static final Color TEXT_COLOR = new Color(40, 40, 40);           // Dark text
    private static final Color DELETE_COLOR = new Color(150, 0, 80);         // Dark magenta/red-violet
    private static final Color END_BID_COLOR = new Color(120, 60, 200);      // Purple accent for "End Bid"

    private final JFrame mainFrame;
    private JTable currentBidsTable;
    private DefaultTableModel tableModel;

    public SellerCurrentBids(JFrame frame) {
        this.mainFrame = frame;

        setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- BACK BUTTON ---
        JButton backButton = new JButton("← Back to Home");
        stylePrimaryButton(backButton);
        backButton.addActionListener(e -> Main.switchToPanel(mainFrame, new SellerHome(mainFrame)));

        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        northPanel.setBackground(BACKGROUND_COLOR);
        northPanel.add(backButton);
        add(northPanel, BorderLayout.NORTH);

        // --- TABLE SETUP ---
        String[] columnNames = {"S.No.", "Auction ID", "Item Name", "Highest Bid", "Winner", "Actions"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only "Actions" is interactive
            }
        };
        currentBidsTable = new JTable(tableModel);
        currentBidsTable.setRowHeight(32);
        currentBidsTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        currentBidsTable.setBackground(PANEL_BG);
        currentBidsTable.setForeground(TEXT_COLOR);
        currentBidsTable.setGridColor(new Color(225, 220, 240));
        currentBidsTable.setSelectionBackground(ACCENT_PURPLE);
        currentBidsTable.setSelectionForeground(Color.WHITE);

        // --- HEADER STYLE ---
        currentBidsTable.getTableHeader().setBackground(PRIMARY_PURPLE);
        currentBidsTable.getTableHeader().setForeground(Color.WHITE);
        currentBidsTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        currentBidsTable.getTableHeader().setReorderingAllowed(false);
        currentBidsTable.getTableHeader().setResizingAllowed(false);

        // --- BUTTON COLUMN ---
        currentBidsTable.getColumn("Actions").setCellRenderer(new ButtonRenderer());
        currentBidsTable.getColumn("Actions").setCellEditor(new ButtonEditor(new JTextField(), this::handleAction));

        JScrollPane scrollPane = new JScrollPane(currentBidsTable);
        scrollPane.getViewport().setBackground(PANEL_BG);
        add(scrollPane, BorderLayout.CENTER);

        loadCurrentBids();
    }

    // --- STYLING HELPERS ---
    private void stylePrimaryButton(JButton button) {
        button.setBackground(PRIMARY_PURPLE);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void styleSecondaryButton(JButton button, Color bg) {
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void loadCurrentBids() {
        tableModel.setRowCount(0);

        List<BackendClient.AuctionItemDto> allAuctions = BackendClient.getAuctionsBySellerEmail(Main.email);
        List<BackendClient.AuctionItemDto> openAuctions = allAuctions.stream()
                .filter(a -> "OPEN".equalsIgnoreCase(a.status()))
                .collect(Collectors.toList());

        for (int i = 0; i < openAuctions.size(); i++) {
            BackendClient.AuctionItemDto auction = openAuctions.get(i);
            Long auctionId = auction.auctionId();

            java.util.concurrent.atomic.AtomicReference<String> highestBidRef = new java.util.concurrent.atomic.AtomicReference<>("N/A");
            java.util.concurrent.atomic.AtomicReference<String> winnerNameRef = new java.util.concurrent.atomic.AtomicReference<>("N/A");

            BackendClient.getHighestBid(auctionId).ifPresent(bid -> {
                highestBidRef.set(String.format("$%.2f", bid.bidAmount()));
                if (bid.bidderId() != null) {
                    BackendClient.getBidderById(bid.bidderId()).ifPresent(bidder ->
                            winnerNameRef.set(bidder.bidderName()));
                }
            });

            tableModel.addRow(new Object[]{
                    i + 1,
                    auctionId,
                    auction.title(),
                    highestBidRef.get(),
                    winnerNameRef.get(),
                    "View Details / End Bid"
            });
        }

        if (openAuctions.isEmpty()) {
            tableModel.addRow(new Object[]{"-", "-", "No Current Auctions Found.", "-", "-", "-"});
        }
    }

    private void handleAction(int row) {
        if (row < 0 || row >= tableModel.getRowCount()) return;
        Long auctionId = (Long) tableModel.getValueAt(row, 1);
        openAuctionDetailsWindow(auctionId);
    }

    // --- AUCTION DETAILS WINDOW ---
    private void openAuctionDetailsWindow(Long auctionId) {
        JDialog detailDialog = new JDialog(mainFrame, "Auction Details (ID: " + auctionId + ")", true);
        detailDialog.setLayout(new BorderLayout(10, 10));
        detailDialog.setSize(560, 520);
        detailDialog.setLocationRelativeTo(mainFrame);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(PANEL_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        BackendClient.getAuctionById(auctionId).ifPresentOrElse(auction -> {
            JTextField titleField = new JTextField(auction.title(), 20);
            JTextField categoryField = new JTextField(auction.category(), 20);
            JTextArea descriptionArea = new JTextArea(auction.description(), 3, 20);
            JTextField startTimeField = new JTextField(auction.startTime(), 15);
            JTextField endTimeField = new JTextField(auction.endTime(), 15);
            JTextField basePriceField = new JTextField(String.valueOf(auction.basePrice()), 10);

            java.util.concurrent.atomic.AtomicInteger row = new java.util.concurrent.atomic.AtomicInteger(0);
            var helper = new Object() {
                void addField(String labelText, JComponent comp) {
                    JLabel label = new JLabel(labelText);
                    label.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    label.setForeground(PRIMARY_PURPLE);

                    gbc.gridx = 0;
                    gbc.gridy = row.get();
                    panel.add(label, gbc);

                    gbc.gridx = 1;
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    if (comp instanceof JScrollPane) {
                        comp.setPreferredSize(new Dimension(250, 70));
                    }
                    panel.add(comp, gbc);
                    gbc.fill = GridBagConstraints.NONE;
                    row.incrementAndGet();
                }
            };

            helper.addField("Item Name:", titleField);
            helper.addField("Category:", categoryField);
            helper.addField("Description:", new JScrollPane(descriptionArea));
            helper.addField("Start Time:", startTimeField);
            helper.addField("End Time:", endTimeField);
            helper.addField("Base Price:", basePriceField);

            List<BackendClient.ImagesDto> images = BackendClient.getImagesByAuction(auctionId);
            String imagePaths = images.stream().map(BackendClient.ImagesDto::filePath)
                    .collect(Collectors.joining(", "));
            helper.addField("Image Paths:", new JLabel(imagePaths));

            detailDialog.add(new JScrollPane(panel), BorderLayout.CENTER);

            // --- BUTTONS ---
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
            buttonPanel.setBackground(BACKGROUND_COLOR);

            JButton updateButton = new JButton("Update Auction");
            JButton deleteButton = new JButton("Delete Auction");
            JButton endBidButton = new JButton("End Bid");

            stylePrimaryButton(updateButton);
            styleSecondaryButton(deleteButton, DELETE_COLOR);
            styleSecondaryButton(endBidButton, END_BID_COLOR);

            updateButton.addActionListener(e -> {
                try {
                    BackendClient.AuctionItemDto updatedDto = new BackendClient.AuctionItemDto(
                            auctionId, titleField.getText(), Double.parseDouble(basePriceField.getText()),
                            categoryField.getText(), descriptionArea.getText(), auction.status(),
                            Main.sell_id, auction.winnerId(), startTimeField.getText(), endTimeField.getText());

                    BackendClient.updateAuction(auctionId, updatedDto).ifPresentOrElse(
                            res -> {
                                showMessageDialog(detailDialog, "Auction updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                                loadCurrentBids();
                            },
                            () -> showMessageDialog(detailDialog, "Failed to update auction.", "Error", JOptionPane.ERROR_MESSAGE)
                    );
                } catch (NumberFormatException ex) {
                    showMessageDialog(detailDialog, "Invalid Base Price.", "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            deleteButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(detailDialog, "Delete this auction? This cannot be undone.",
                        "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (BackendClient.deleteAuction(auctionId)) {
                        showMessageDialog(mainFrame, "Auction deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        detailDialog.dispose();
                        loadCurrentBids();
                    } else {
                        showMessageDialog(detailDialog, "Failed to delete auction.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });

            endBidButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(detailDialog, "End this auction and determine winner?",
                        "Confirm Close", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    BackendClient.closeAuction(auctionId).ifPresentOrElse(
                            res -> {
                                showMessageDialog(mainFrame, "Auction closed successfully. Winner has been notified.",
                                        "Success", JOptionPane.INFORMATION_MESSAGE);
                                detailDialog.dispose();
                                loadCurrentBids();
                            },
                            () -> showMessageDialog(detailDialog, "Failed to close auction. Ensure bids exist.",
                                    "Error", JOptionPane.ERROR_MESSAGE)
                    );
                }
            });

            buttonPanel.add(updateButton);
            buttonPanel.add(deleteButton);
            buttonPanel.add(endBidButton);
            detailDialog.add(buttonPanel, BorderLayout.SOUTH);

        }, () -> showMessageDialog(mainFrame, "Failed to load auction details.", "Error", JOptionPane.ERROR_MESSAGE));

        detailDialog.setVisible(true);
    }

    // --- TABLE BUTTON RENDERER ---
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBackground(PRIMARY_PURPLE);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 11));
            setFocusPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            setBackground(isSelected ? ACCENT_PURPLE.darker() : PRIMARY_PURPLE);
            return this;
        }
    }

    // --- TABLE BUTTON EDITOR ---
    class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private String label;
        private boolean isPushed;
        private final java.util.function.Consumer<Integer> actionHandler;

        public ButtonEditor(JTextField textField, java.util.function.Consumer<Integer> actionHandler) {
            super(textField);
            this.actionHandler = actionHandler;
            button = new JButton();
            button.setOpaque(true);
            button.setBackground(PRIMARY_PURPLE);
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Segoe UI", Font.BOLD, 11));
            button.setFocusPainted(false);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                actionHandler.accept(currentBidsTable.getSelectedRow());
            }
            isPushed = false;
            return label;
        }
    }
}
