package com.mycompany.bidverse.client;

import com.mycompany.bidverse.AuthService;
import com.mycompany.bidverse.MainFrame;
import com.mycompany.bidverse.client.dto.AuctionItemDto;
import com.mycompany.bidverse.client.dto.BidDto;
import com.mycompany.bidverse.client.dto.BidderDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.mycompany.bidverse.client.dto.AuctionItemDto;

public class BidderDashboard extends JPanel {

    // --- Core Fields ---
    private MainFrame mainFrame;
    // Map is now defined to hold your real DTO objects
    private Map<String, AuctionItemDto> auctionMap = new LinkedHashMap<>();

    private DefaultListModel<String> auctionListModel;
    private JList<String> auctionJList;

    // Detail Panel UI components
    private JLabel detailTitle;
    private JTextArea detailDesc;
    private JLabel detailBasePrice;
    private JLabel detailHighestBid;
    private JLabel detailEndsIn;
    private JLabel statusLabel;

    // UI component for bid amount
    private JTextField bidAmountField;

    // API & Bidder Info
    // This client is now your actual implementation from BidverseAPIClient.java
    private final BidverseAPIClient apiClient = new BidverseAPIClient();
    private Long currentBidderId=AuthService.getCurrentBidderId();
    private String currentBidderName;
    private Long currentAuctionId;
    
    // Timer for refreshing details
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private String currentBidderEmail;

    public BidderDashboard(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        initializeDashboard();
    }

    private void initializeDashboard() {
        // Set up the panel layout
        setLayout(new BorderLayout());
        System.out.println(currentBidderId+"cccccccccccc");
        
        // Main gradient panel
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(106, 13, 173),
                                                     0, getHeight() / 2f, Color.WHITE);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight() / 2);
                gp = new GradientPaint(0, getHeight() / 2f, Color.WHITE, 0, getHeight(), Color.BLACK);
                g2.setPaint(gp);
                g2.fillRect(0, getHeight() / 2, getWidth(), getHeight() / 2);
                g2.dispose();
            }
        };
        mainPanel.setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        // Side menu
        JPanel sideMenu = setupSideMenu();
        mainPanel.add(sideMenu, BorderLayout.WEST);

        // Content panel
        JPanel contentPanel = new JPanel(new CardLayout());
        contentPanel.setOpaque(false);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        // Home / Browse panel
        JPanel browsePanel = createBrowsePanel();
        contentPanel.add(browsePanel, "Home");

        // Fetch auctions for home panel
        fetchAndDisplayAuctions();

        // Fetch Profile Panel
        new Thread(() -> {
            Optional<BidderDto> profileOpt = apiClient.getBidderProfileByEmail(currentBidderEmail);
            profileOpt.ifPresent(profile -> SwingUtilities.invokeLater(() -> {
                JPanel profilePanel = createProfilePanel(profile);
                contentPanel.add(profilePanel, "Profile");
            }));
        }).start();


        // Fetch My Bids Panel
        new Thread(() -> {
            List<BidDto> myBids = apiClient.getMyBids(AuthService.getCurrentBidderId());
            SwingUtilities.invokeLater(() -> {
                JPanel myBidsPanel = createMyBidsPanel(myBids);
                contentPanel.add(myBidsPanel, "MyBids");
            });
        }).start();

        // Fetch Won Auctions Panel
        new Thread(() -> {
            List<BidDto> wonBids = apiClient.getWonBids(AuthService.getCurrentBidderId());
            SwingUtilities.invokeLater(() -> {
                JPanel wonPanel = createWonPanel(wonBids);
                contentPanel.add(wonPanel, "Won");
            });
        }).start();

        CardLayout cl = (CardLayout) contentPanel.getLayout();

        // Menu Button Actions
        Component[] menuComponents = sideMenu.getComponents();
        RoundedButton homeBtn = (RoundedButton) menuComponents[1];
        RoundedButton myBidsBtn = (RoundedButton) menuComponents[3];
        RoundedButton wonBtn = (RoundedButton) menuComponents[5];
        RoundedButton profileBtn = (RoundedButton) menuComponents[7];
        RoundedButton logoutBtn = (RoundedButton) menuComponents[9];

        homeBtn.addActionListener(e -> cl.show(contentPanel, "Home"));
        myBidsBtn.addActionListener(e -> cl.show(contentPanel, "MyBids"));
        wonBtn.addActionListener(e -> cl.show(contentPanel, "Won"));
        profileBtn.addActionListener(e -> cl.show(contentPanel, "Profile"));
        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Logout", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                scheduler.shutdownNow();
                mainFrame.showCard("login");
                JOptionPane.showMessageDialog(null, "Logged out successfully!");
            }
        });

        // Start a scheduled task to refresh auction details every 5 seconds
        scheduler.scheduleWithFixedDelay(this::refreshSelectedAuctionDetails, 0, 5, TimeUnit.SECONDS);
    }

    public void setBidderInfo(Long bidderId, String bidderEmail, String bidderName) {
        this.currentBidderName = bidderName;
        this.currentBidderEmail = bidderEmail;
        
        // Fetch auctions for home panel
        fetchAndDisplayAuctions();
        
        // Fetch Profile Panel
        new Thread(() -> {
            Optional<BidderDto> profileOpt = apiClient.getBidderProfileByEmail(currentBidderEmail);
            profileOpt.ifPresent(profile -> SwingUtilities.invokeLater(() -> {
                JPanel profilePanel = createProfilePanel(profile);
                // Find the content panel and add profile panel
                Component[] components = getComponents();
                if (components.length > 0 && components[0] instanceof JPanel) {
                    JPanel mainPanel = (JPanel) components[0];
                    Component[] mainComponents = mainPanel.getComponents();
                    if (mainComponents.length > 1 && mainComponents[1] instanceof JPanel) {
                        JPanel contentPanel = (JPanel) mainComponents[1];
                        contentPanel.add(profilePanel, "Profile");
                    }
                }
            }));
        }).start();

        // Fetch My Bids Panel
        new Thread(() -> {
            List<BidDto> myBids = apiClient.getMyBids(AuthService.getCurrentBidderId());
            SwingUtilities.invokeLater(() -> {
                JPanel myBidsPanel = createMyBidsPanel(myBids);
                // Find the content panel and add my bids panel
                Component[] components = getComponents();
                if (components.length > 0 && components[0] instanceof JPanel) {
                    JPanel mainPanel = (JPanel) components[0];
                    Component[] mainComponents = mainPanel.getComponents();
                    if (mainComponents.length > 1 && mainComponents[1] instanceof JPanel) {
                        JPanel contentPanel = (JPanel) mainComponents[1];
                        contentPanel.add(myBidsPanel, "MyBids");
                    }
                }
            });
        }).start();

        // Fetch Won Auctions Panel
        new Thread(() -> {
            List<BidDto> wonBids = apiClient.getWonBids(AuthService.getCurrentBidderId());
            SwingUtilities.invokeLater(() -> {
                JPanel wonPanel = createWonPanel(wonBids);
                // Find the content panel and add won panel
                Component[] components = getComponents();
                if (components.length > 0 && components[0] instanceof JPanel) {
                    JPanel mainPanel = (JPanel) components[0];
                    Component[] mainComponents = mainPanel.getComponents();
                    if (mainComponents.length > 1 && mainComponents[1] instanceof JPanel) {
                        JPanel contentPanel = (JPanel) mainComponents[1];
                        contentPanel.add(wonPanel, "Won");
                    }
                }
            });
        }).start();
    }

    private JPanel createProfilePanel(BidderDto profile) {
        JPanel panel = new RoundedPanel(20, new Color(255, 255, 255, 220));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel titleLabel = new JLabel("Profile");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(20));

        JTextArea textArea = new JTextArea(
            "Name: " + profile.getBidderName() + "\n" +
            "Email: " + profile.getBidderEmail() + "\n" +
            "Phone: " + profile.getPhno() + "\n" +
            "Address: " + profile.getAddress()
        );
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(textArea);

        return panel;
    }

    private JPanel createMyBidsPanel(List<BidDto> bids) {
        JPanel panel = new RoundedPanel(20, new Color(255, 255, 255, 220));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel titleLabel = new JLabel("My Bids");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(20));

        if (bids.isEmpty()) {
            JTextArea emptyMsg = new JTextArea("No bids placed yet.");
            emptyMsg.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            emptyMsg.setEditable(false);
            emptyMsg.setOpaque(false);
            emptyMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(emptyMsg);
        } else {
            for (BidDto bid : bids) {
                JTextArea text = new JTextArea(
                    apiClient.getAuctionId(bid.getAuctionItemId()).get().getTitle()+ "\n" +
                    "Amount: " + formatCurrency(bid.getBidAmount()) + "\n" +
                    "Status: " + apiClient.getAuctionId(bid.getAuctionItemId()).get().getStatus()
                );
                text.setFont(new Font("Segoe UI", Font.PLAIN, 18));
                text.setEditable(false);
                text.setOpaque(false);
                text.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
                panel.add(text);
                panel.add(Box.createVerticalStrut(12));
            }
        }

        return panel;
    }

    private JPanel createWonPanel(List<BidDto> wonBids) {
        JPanel panel = new RoundedPanel(20, new Color(255, 255, 255, 220));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JLabel titleLabel = new JLabel("Won Auctions");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(20));

        if (wonBids.isEmpty()) {
            JTextArea emptyMsg = new JTextArea("No auctions won yet.");
            emptyMsg.setFont(new Font("Segoe UI", Font.PLAIN, 18));
            emptyMsg.setEditable(false);
            emptyMsg.setOpaque(false);
            emptyMsg.setAlignmentX(Component.CENTER_ALIGNMENT);
            panel.add(emptyMsg);
        } else {
            for (BidDto bid : wonBids) {
                JTextArea text = new JTextArea(
                    bid.getAuctionTitle() + "\n" +
                    "Amount: " + formatCurrency(bid.getBidAmount()) + "\n" +
                    "Status: " + bid.getStatus()
                );
                text.setFont(new Font("Segoe UI", Font.PLAIN, 18));
                text.setEditable(false);
                text.setOpaque(false);
                text.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY));
                panel.add(text);
                panel.add(Box.createVerticalStrut(12));
            }
        }

        return panel;
    }

    private JPanel setupSideMenu() {
        JPanel sideMenu = new JPanel();
        sideMenu.setLayout(new BoxLayout(sideMenu, BoxLayout.Y_AXIS));
        sideMenu.setBackground(new Color(60, 0, 100));
        sideMenu.setPreferredSize(new Dimension(200, getHeight()));

        RoundedButton homeBtn = new RoundedButton("Home", new Color(106, 13, 173), Color.WHITE);
        RoundedButton myBidsBtn = new RoundedButton("My Bids", new Color(106, 13, 173), Color.WHITE);
        RoundedButton wonBtn = new RoundedButton("Won Auctions", new Color(106, 13, 173), Color.WHITE);
        RoundedButton profileBtn = new RoundedButton("Profile", Color.WHITE, new Color(106, 13, 173));
        RoundedButton logoutBtn = new RoundedButton("Logout", Color.WHITE, new Color(106, 13, 173));

        homeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        myBidsBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        wonBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        profileBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        sideMenu.add(Box.createVerticalStrut(30));
        sideMenu.add(homeBtn);
        sideMenu.add(Box.createVerticalStrut(20));
        sideMenu.add(myBidsBtn);
        sideMenu.add(Box.createVerticalStrut(20));
        sideMenu.add(wonBtn);
        sideMenu.add(Box.createVerticalStrut(20));
        sideMenu.add(profileBtn);
        sideMenu.add(Box.createVerticalGlue());
        sideMenu.add(logoutBtn);
        sideMenu.add(Box.createVerticalStrut(20));

        return sideMenu;
    }

    private void fetchAndDisplayAuctions() {
        // Real API call is now expected here
        new Thread(() -> {
            try {
                // 1. Make the REAL API call using the implemented client
                List<AuctionItemDto> auctions = apiClient.getAllAuctions();

                // 2. Update the Swing components on the Event Dispatch Thread (EDT)
                SwingUtilities.invokeLater(() -> {
                    auctionListModel.clear();
                    auctionMap.clear();

                    for (AuctionItemDto auction : auctions) {
                        // Assuming getStatus() returns "OPEN" or "CLOSED" string
                        if ("OPEN".equalsIgnoreCase(auction.getStatus())) {
                            auctionListModel.addElement(auction.getTitle());
                            auctionMap.put(auction.getTitle(), auction);
                        }
                    }

                    if (!auctionListModel.isEmpty()) {
                        auctionJList.setSelectedIndex(0);
                        showSelectedAuctionDetails();
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Failed to fetch auctions. Check API client configuration: " + ex.getMessage(), "API Error", JOptionPane.ERROR_MESSAGE)
                );
            }
        }).start();
    }

    private void refreshSelectedAuctionDetails() {
        String selectedTitle = auctionJList.getSelectedValue();
        if (selectedTitle == null) return;

        AuctionItemDto currentDto = auctionMap.get(selectedTitle);
        if (currentDto == null) return;

        // Fetch the latest version of the auction from the REAL API
        // NOTE: This runs on the scheduler's background thread
        Optional<AuctionItemDto> latestOptional = apiClient.getAuctionId(currentDto.getAuctionId());

        if (latestOptional.isPresent()) {
            AuctionItemDto latest = latestOptional.get();
            auctionMap.put(selectedTitle, latest); // Update map

            SwingUtilities.invokeLater(() -> {
                if (selectedTitle.equals(auctionJList.getSelectedValue())) {
                    showDetailsFromDto(latest);
                }
            });
        }
    }


    private JPanel createBrowsePanel() {
        JPanel browsePanel = new RoundedPanel(25, new Color(255, 255, 255, 220));
        browsePanel.setLayout(new BorderLayout(20, 20));
        browsePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel heading = new JLabel("Available Auctions");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 28));
        heading.setForeground(new Color(30, 30, 30));
        browsePanel.add(heading, BorderLayout.NORTH);

        auctionListModel = new DefaultListModel<>();
        auctionJList = new JList<>(auctionListModel);
        auctionJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        auctionJList.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        JScrollPane scrollPane = new JScrollPane(auctionJList);
        browsePanel.add(scrollPane, BorderLayout.CENTER);

        // Details panel below
        JPanel detailPanel = new RoundedPanel(18, new Color(255, 255, 255, 230));
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        detailTitle = new JLabel("Select an auction to view details");
        detailTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        detailPanel.add(detailTitle);
        detailPanel.add(Box.createVerticalStrut(12));

        detailDesc = new JTextArea();
        detailDesc.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        detailDesc.setEditable(false);
        detailDesc.setLineWrap(true);
        detailDesc.setWrapStyleWord(true);
        detailDesc.setOpaque(false);
        detailPanel.add(detailDesc);
        detailPanel.add(Box.createVerticalStrut(12));

        // Info Labels
        JPanel infoBar = new JPanel(new GridLayout(2, 2, 10, 6));
        infoBar.setOpaque(false);
        detailBasePrice = createInfoLabel("Base: -");
        detailHighestBid = createInfoLabel("Highest: -");
        detailEndsIn = createInfoLabel("Ends in: -");
        statusLabel = createInfoLabel("Status: -");
        infoBar.add(detailBasePrice);
        infoBar.add(detailHighestBid);
        infoBar.add(detailEndsIn);
        infoBar.add(statusLabel);
        detailPanel.add(infoBar);
        detailPanel.add(Box.createVerticalStrut(12));

        // Bid Input and Buttons
        JPanel bidInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bidInputPanel.setOpaque(false);
        bidInputPanel.add(new JLabel("Your Bid:"));
        bidAmountField = new JTextField(10);
        bidAmountField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        bidInputPanel.add(bidAmountField);
        detailPanel.add(bidInputPanel);
        detailPanel.add(Box.createVerticalStrut(12));

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        buttonBar.setOpaque(false);
        RoundedButton placeBidBtn = new RoundedButton("Place Bid", new Color(106, 13, 173), Color.WHITE);
        RoundedButton payNowBtn = new RoundedButton("Pay Now (Fake QR)", new Color(34, 139, 34), Color.WHITE);
        buttonBar.add(placeBidBtn);
        buttonBar.add(payNowBtn);
        detailPanel.add(buttonBar);

        browsePanel.add(detailPanel, BorderLayout.SOUTH);

        // List Selection Event
        auctionJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedAuctionDetails();
            }
        });

        placeBidBtn.addActionListener(e -> placeBidFlow());
        payNowBtn.addActionListener(e -> openPaymentDialog());

        return browsePanel;
    }

    private void showSelectedAuctionDetails() {
        String key = auctionJList.getSelectedValue();
        if (key == null) return;

        AuctionItemDto dto = auctionMap.get(key);
        if (dto == null) return;

        this.currentAuctionId = dto.getAuctionId();
        System.out.println(currentAuctionId+"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

        showDetailsFromDto(dto);
    }

    private void showDetailsFromDto(AuctionItemDto dto) {
        detailTitle.setText(dto.getTitle());
        detailDesc.setText(dto.getDescription());

        // FIX 1: Convert Double to BigDecimal for formatting
        detailBasePrice.setText("Base: " + formatCurrency(BigDecimal.valueOf(dto.getBasePrice())));

        // FIX 2: Convert Double from the highestBid field to BigDecimal
        detailHighestBid.setText("Highest: " + formatCurrency(BigDecimal.valueOf(dto.getHighestBid())));

        // FIX 3: Use the endsIn string
        detailEndsIn.setText("Ends in: " + dto.getEndsIn());

        statusLabel.setText("Status: " + dto.getStatus());
    }

    // Helper for currency formatting
    private String formatCurrency(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getCurrencyInstance();
        return nf.format(amount);
    }

    private void placeBidFlow() {
        System.out.println(currentAuctionId+"fffffffffffffff\n");
        System.out.println(AuthService.getCurrentBidderId()+"eeeeeeeeee");
        if (currentAuctionId == null || AuthService.getCurrentBidderId() == null) {
            JOptionPane.showMessageDialog(this, "Please select an auction and log in.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String amountText = bidAmountField.getText();
        BigDecimal bidAmount;

        try {
            bidAmount = new BigDecimal(amountText);
            if (bidAmount.compareTo(BigDecimal.ZERO) <= 0) {
                 JOptionPane.showMessageDialog(this, "Bid amount must be positive.", "Input Error", JOptionPane.ERROR_MESSAGE);
                 return;
            }
        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(this, "Please enter a valid number for the bid amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        BidDto newBid = new BidDto();
        // Assumes DTO setters are available
        newBid.setAuctionItemId(currentAuctionId);
        newBid.setBidderId(AuthService.getCurrentBidderId());
        newBid.setBidAmount(bidAmount);

        new Thread(() -> {
            try {
                // REAL API call to place the bid
                Optional<BidDto> savedBid = apiClient.placeBid(newBid);

                SwingUtilities.invokeLater(() -> {
                    if (savedBid.isPresent()) {
                        JOptionPane.showMessageDialog(this, "Bid of " + formatCurrency(bidAmount) + " placed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        refreshSelectedAuctionDetails();
                        refreshMyBidsPanel(); // <--- added line
                    }

                });
            } catch (RuntimeException rex) {
                // This catches business logic errors thrown by your API client (e.g., bid too low, auction closed)
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, rex.getMessage(), "Bid Failed", JOptionPane.WARNING_MESSAGE)
                );
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Network error placing bid.", "Error", JOptionPane.ERROR_MESSAGE)
                );
            }
        }).start();
    }

    private void openPaymentDialog() {
        String key = auctionJList.getSelectedValue();
        if (key == null) return;

        // This relies on the DTO having a setter for status for the *minor dummy logic*
        AuctionItemDto a = auctionMap.get(key);

        // Convert the highestBid Double to BigDecimal for the payment amount
        BigDecimal amountToPay = BigDecimal.valueOf(a.getHighestBid());

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Fake QR Payment - " + a.getTitle(), true);
        dialog.setSize(520, 620);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(12, 12));

        JPanel top = new RoundedPanel(16, new Color(255, 255, 255, 230));
        top.setLayout(new BorderLayout());
        top.setBorder(new EmptyBorder(12, 12, 12, 12));
        JLabel lbl = new JLabel("Pay for: " + a.getTitle());
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        top.add(lbl, BorderLayout.NORTH);
        JLabel amt = new JLabel("Amount: " + formatCurrency(amountToPay));
        amt.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        top.add(amt, BorderLayout.SOUTH);
        dialog.add(top, BorderLayout.NORTH);

        // Uses the auctionId and amount to generate a visually consistent but fake QR
        BufferedImage qr = generateFakeQRImage(a.getAuctionId(), amountToPay, 320);

        JPanel center = new RoundedPanel(16, new Color(255, 255, 255, 230));
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(new EmptyBorder(12, 12, 12, 12));
        JLabel qrLabel = new JLabel(new ImageIcon(qr));
        qrLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(qrLabel);
        center.add(Box.createVerticalStrut(12));
        JLabel instr = new JLabel("<html><div style='text-align:center'>Scan this fake QR with your payment app<br>(Simulated payment only.)</div></html>");
        instr.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        instr.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(instr);
        center.add(Box.createVerticalStrut(8));

        // Minor dummy logic remains here:
        RoundedButton simulateBtn = new RoundedButton("Simulate Payment", new Color(34, 139, 34), Color.WHITE);
        simulateBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(Box.createVerticalStrut(12));
        center.add(simulateBtn);
        dialog.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);
        RoundedButton closeBtn = new RoundedButton("Close", Color.WHITE, new Color(106, 13, 173));
        bottom.add(closeBtn);
        dialog.add(bottom, BorderLayout.SOUTH);

        simulateBtn.addActionListener(e -> {
            simulateBtn.setEnabled(false);
            JOptionPane.showMessageDialog(dialog, "Payment simulated successfully for " + formatCurrency(amountToPay) + "!");
            // Minor dummy logic: Local DTO update instead of API call
            a.setStatus("CLOSED");
            statusLabel.setText("Status: " + a.getStatus());
            dialog.dispose();
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        dialog.setVisible(true);
    }

    private void refreshMyBidsPanel() {
    new Thread(() -> {
        System.out.println("Refreshing My Bids Panel..."+currentBidderId);
        List<BidDto> myBids = apiClient.getMyBids(AuthService.getCurrentBidderId());
        SwingUtilities.invokeLater(() -> {
            JPanel myBidsPanel = createMyBidsPanel(myBids);

            // Locate the content panel
            Component[] components = getComponents();
            if (components.length > 0 && components[0] instanceof JPanel mainPanel) {
                Component[] mainComponents = mainPanel.getComponents();
                if (mainComponents.length > 1 && mainComponents[1] instanceof JPanel contentPanel) {
                    contentPanel.add(myBidsPanel, "MyBids");
                }
            }
        });
    }).start();
}


    // --- Utility UI Methods & Minor Dummy Logic (QR Code) ---

    private JLabel createInfoLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        l.setForeground(new Color(50, 50, 50));
        l.setOpaque(false);
        return l;
    }

    private BufferedImage generateFakeQRImage(Long auctionId, BigDecimal amount, int sizePx) {
        int cells = 21;
        int cellSize = sizePx / cells;
        BufferedImage img = new BufferedImage(cellSize * cells, cellSize * cells, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, img.getWidth(), img.getHeight());
        // Use auctionId and amount to create a predictable seed for consistent "QR"
        long seed = auctionId * 31 + amount.unscaledValue().longValue();
        Random rnd = new Random(seed);
        drawFinderPattern(g2, 0, 0, cellSize);
        drawFinderPattern(g2, (cells - 7) * cellSize, 0, cellSize);
        drawFinderPattern(g2, 0, (cells - 7) * cellSize, cellSize);
        g2.setColor(Color.BLACK);
        for (int i = 0; i < cells; i++) {
            for (int j = 0; j < cells; j++) {
                if ((i < 7 && j < 7) || (i >= cells - 7 && j < 7) || (i < 7 && j >= cells - 7)) continue;
                if (rnd.nextBoolean()) g2.fillRect(i * cellSize, j * cellSize, cellSize, cellSize);
            }
        }
        g2.dispose();
        return img;
    }

    private void drawFinderPattern(Graphics2D g2, int x, int y, int cellSize) {
        g2.setColor(Color.BLACK);
        g2.fillRect(x, y, 7 * cellSize, 7 * cellSize);
        g2.setColor(Color.WHITE);
        g2.fillRect(x + cellSize, y + cellSize, 5 * cellSize, 5 * cellSize);
        g2.setColor(Color.BLACK);
        g2.fillRect(x + 2 * cellSize, y + 2 * cellSize, 3 * cellSize, 3 * cellSize);
    }

    // --- Rounded panel & button classes (Unchanged) ---

    static class RoundedPanel extends JPanel {
        private final int radius; private final Color bgColor;
        public RoundedPanel(int radius, Color bgColor) { this.radius = radius; this.bgColor = bgColor; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class RoundedButton extends JButton {
        private final Color bgColor, textColor; private Color currentBg;
        public RoundedButton(String text, Color bg, Color fg) {
            super(text); bgColor = bg; textColor = fg; currentBg = bg;
            setFocusPainted(false); setContentAreaFilled(false); setBorderPainted(false); setOpaque(false);
            setFont(new Font("Segoe UI", Font.BOLD, 16));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { currentBg = brighten(bgColor, 0.2f); repaint(); }
                public void mouseExited(MouseEvent e) { currentBg = bgColor; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(currentBg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
            g2.setColor(textColor);
            FontMetrics fm = g2.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() + fm.getAscent()) / 2 - 3;
            g2.drawString(getText(), x, y);
            g2.dispose();
        }
        @Override public Dimension getPreferredSize() { return new Dimension(160, 45); }
        private static Color brighten(Color color, float factor) {
            int r = Math.min(255, (int) (color.getRed() + 255 * factor));
            int g = Math.min(255, (int) (color.getGreen() + 255 * factor));
            int b = Math.min(255, (int) (color.getBlue() + 255 * factor));
            return new Color(r, g, b, color.getAlpha());
        }
    }
}

