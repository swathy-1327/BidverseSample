package com.mycompany.bidverse;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

import static javax.swing.JOptionPane.showMessageDialog;

public class SellerPastBids extends JPanel {

    // --- Custom Color Palette (consistent with other panels) ---
    private static final Color SIDEBAR_COLOR = new Color(76, 0, 153);         // Deep Purple
    private static final Color ACCENT_COLOR = new Color(147, 112, 219);       // Soft Purple
    private static final Color BACKGROUND_COLOR = new Color(245, 245, 255);   // Light lavender-gray
    private static final Color TABLE_HEADER_COLOR = new Color(98, 0, 238);    // Indigo
    private static final Color TABLE_SELECTION = new Color(153, 102, 255);    // Light violet highlight
    private static final Color TEXT_COLOR = new Color(51, 51, 51);            // Dark gray text
    private static final Color BUTTON_COLOR = new Color(98, 0, 238);          // Purple buttons
    private static final Color BUTTON_HOVER = new Color(123, 31, 162);        // Darker purple

    private final JFrame mainFrame;
    private JTable pastBidsTable;
    private DefaultTableModel tableModel;

    public SellerPastBids(JFrame frame) {
        this.mainFrame = frame;

        // --- Layout & Background ---
        setLayout(new BorderLayout(15, 15));
        setBackground(BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- Top Navigation (Back Button) ---
        JButton backButton = new JButton("← Back to Home");
        styleButton(backButton, BUTTON_COLOR, Color.WHITE);
        backButton.addActionListener(e -> Main.switchToPanel(mainFrame, new SellerHome(mainFrame)));

        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        northPanel.setBackground(BACKGROUND_COLOR);
        northPanel.add(backButton);
        add(northPanel, BorderLayout.NORTH);

        // --- Table Setup ---
        String[] columnNames = {"S.No.", "Auction ID", "Item Name", "Final Amount", "Winner", "Details"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only "Details" column is editable (button)
            }
        };

        pastBidsTable = new JTable(tableModel);
        pastBidsTable.setRowHeight(30);
        pastBidsTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pastBidsTable.setBackground(Color.WHITE);
        pastBidsTable.setForeground(TEXT_COLOR);
        pastBidsTable.setSelectionBackground(TABLE_SELECTION);
        pastBidsTable.setSelectionForeground(Color.WHITE);
        pastBidsTable.setGridColor(new Color(230, 230, 250));

        // --- Table Header Styling ---
        pastBidsTable.getTableHeader().setBackground(TABLE_HEADER_COLOR);
        pastBidsTable.getTableHeader().setForeground(Color.WHITE);
        pastBidsTable.getTableHeader().setFont(new Font("Segoe UI Semibold", Font.BOLD, 14));
        pastBidsTable.getTableHeader().setReorderingAllowed(false);
        pastBidsTable.getTableHeader().setResizingAllowed(false);

        // --- Action Button Column ---
        pastBidsTable.getColumn("Details").setCellRenderer(new ButtonRenderer());
        pastBidsTable.getColumn("Details").setCellEditor(new ButtonEditor(new JTextField(), this::viewWinnerDetails));

        // --- Add ScrollPane ---
        JScrollPane scrollPane = new JScrollPane(pastBidsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 250), 1));
        scrollPane.getViewport().setBackground(Color.WHITE);
        add(scrollPane, BorderLayout.CENTER);

        loadPastBids();
    }

    private void styleButton(JButton button, Color bg, Color fg) {
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI Semibold", Font.BOLD, 13));
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_HOVER);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bg);
            }
        });
    }

    private void loadPastBids() {
        tableModel.setRowCount(0); // Clear existing data

        List<BackendClient.AuctionItemDto> allAuctions = BackendClient.getAuctionsBySellerEmail(Main.email);

        // Filter closed auctions
        List<BackendClient.AuctionItemDto> closedAuctions = allAuctions.stream()
            .filter(a -> "CLOSE".equalsIgnoreCase(a.status()))
            .collect(Collectors.toList());

        for (int i = 0; i < closedAuctions.size(); i++) {
            BackendClient.AuctionItemDto auction = closedAuctions.get(i);
            Long auctionId = auction.auctionId();

            java.util.concurrent.atomic.AtomicReference<String> finalAmountRef = new java.util.concurrent.atomic.AtomicReference<>("N/A");
            java.util.concurrent.atomic.AtomicReference<String> winnerNameRef = new java.util.concurrent.atomic.AtomicReference<>("N/A");

            BackendClient.getHighestBid(auctionId).ifPresent(bid -> {
                finalAmountRef.set(String.format("$%.2f", bid.bidAmount()));
                if (bid.bidderId() != null) {
                    BackendClient.getBidderById(bid.bidderId()).ifPresent(bidder -> {
                        winnerNameRef.set(bidder.bidderName());
                    });
                }
            });

            tableModel.addRow(new Object[]{
                i + 1,
                auctionId,
                auction.title(),
                finalAmountRef.get(),
                winnerNameRef.get(),
                "View Details"
            });
        }

        if (closedAuctions.isEmpty()) {
            tableModel.addRow(new Object[]{"-", "-", "No Past Auctions Found.", "-", "-", "-"});
        }
    }

    private void viewWinnerDetails(int row) {
        if (row < 0 || row >= tableModel.getRowCount()) return;
        Long auctionId = (Long) tableModel.getValueAt(row, 1);

        BackendClient.getHighestBid(auctionId).ifPresentOrElse(bid -> {
            if (bid.bidderId() == null) {
                showMessageDialog(mainFrame, "No winner found for this auction (no bids placed).", "No Winner", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            BackendClient.getBidderById(bid.bidderId()).ifPresentOrElse(bidder -> {
                String details = String.format(
                    "Winner Name: %s\nEmail: %s\nPhone No: %s\nAddress: %s\n\nFinal Bid Amount: $%.2f",
                    bidder.bidderName(),
                    bidder.bidderEmail(),
                    bidder.phno(),
                    bidder.address(),
                    bid.bidAmount()
                );

                JTextArea textArea = new JTextArea(details);
                textArea.setEditable(false);
                textArea.setBackground(Color.WHITE);
                textArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(320, 200));

                showMessageDialog(mainFrame, scrollPane, "Winner Details - " + tableModel.getValueAt(row, 2), JOptionPane.INFORMATION_MESSAGE);
            }, () -> showMessageDialog(mainFrame, "Failed to load winner profile.", "Error", JOptionPane.ERROR_MESSAGE));
        }, () -> showMessageDialog(mainFrame, "Could not find final bid details.", "Error", JOptionPane.ERROR_MESSAGE));
    }

    // --- Button Renderer ---
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBackground(BUTTON_COLOR);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setFocusPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            if (isSelected) setBackground(BUTTON_HOVER);
            else setBackground(BUTTON_COLOR);
            return this;
        }
    }

    // --- Button Editor ---
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
            button.setBackground(BUTTON_COLOR);
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Segoe UI", Font.BOLD, 12));
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
            if (isPushed) actionHandler.accept(pastBidsTable.getSelectedRow());
            isPushed = false;
            return label;
        }
    }
}
