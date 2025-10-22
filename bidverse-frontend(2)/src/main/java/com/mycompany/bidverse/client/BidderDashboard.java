package com.mycompany.bidverse.client;
import com.mycompany.bidverse.AuthService;
import com.mycompany.bidverse.MainFrame;
import com.mycompany.bidverse.client.dto.AuctionItemDto;
import com.mycompany.bidverse.client.dto.BidDto;
import com.mycompany.bidverse.client.dto.BidderDto;
import com.mycompany.bidverse.client.dto.ImagesDto;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;

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
    private JPanel imagePanel;
    private JLabel imageLabel;

    // UI component for bid amount
    private JTextField bidAmountField;

    // API & Bidder Info
    // This client is now your actual implementation from BidverseAPIClient.java
    private final BidverseAPIClient apiClient = new BidverseAPIClient();
    private Long currentBidderId=AuthService.getCurrentBidderId();
    private String currentBidderName;
    private Long currentAuctionId;
    private final Map<Long, ImageIcon> auctionImageCache = new HashMap<>();

    
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
            if(AuthService.getCurrentBidderId()!=null){
            List<BidDto> myBids = apiClient.getMyBids(AuthService.getCurrentBidderId());
            SwingUtilities.invokeLater(() -> {
                JPanel myBidsPanel = createMyBidsPanel(myBids);
                contentPanel.add(myBidsPanel, "MyBids");
            
            });}
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
        scheduler.scheduleWithFixedDelay(this::refreshSelectedAuctionDetails, 0, 150, TimeUnit.SECONDS);
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
                System.out.println(
    "Bid Details → " +
    "bidId=" + bid.getBidId() + ", " +
    "auctionItemId=" + bid.getAuctionItemId() + ", " +
    "bidderId=" + bid.getBidderId() + ", " +
    "bidAmount=" + bid.getBidAmount() + ", " +
    "bidTime=" + bid.getBidTime() + ", " +
    "auctionTitle=" + bid.auctionTitle + ", " +
    "status=" + bid.status
);

                System.out.println(bid.getAuctionItemId()+"pppp");
                AuctionItemDto auc=apiClient.getAuctionId(bid.getAuctionItemId()).get();
                JTextArea text = new JTextArea(
                    auc.getTitle()+ "\n" +
                    "Amount: " + formatCurrency(bid.getBidAmount()) + "\n" );
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
    new Thread(() -> {
        try {
            // 1. Make the REAL API call using the implemented client
            List<AuctionItemDto> auctions = apiClient.getAllAuctions();

            // 2. Update the Swing components on the Event Dispatch Thread (EDT)
            SwingUtilities.invokeLater(() -> {
                auctionListModel.clear();
                auctionMap.clear();

                for (AuctionItemDto auction : auctions) {
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

            // 3. Pre-load images for all auctions in the background
            preloadAuctionImages(auctions);

        } catch (Exception ex) {
            ex.printStackTrace();
            SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(this, "Failed to fetch auctions. Check API client configuration: " + ex.getMessage(), "API Error", JOptionPane.ERROR_MESSAGE)
            );
        }
    }).start();
}

private void preloadAuctionImages(List<AuctionItemDto> auctions) {
    for (AuctionItemDto auction : auctions) {
        if ("OPEN".equalsIgnoreCase(auction.getStatus())) {
            loadAuctionImage(auction.getAuctionId(), auction.getTitle());
        }
    }
}

private void loadAuctionImage(Long auctionId, String auctionTitle) {
    // If image is already cached, display it immediately
    if (auctionImageCache.containsKey(auctionId)) {
        imageLabel.setIcon(auctionImageCache.get(auctionId));
        imageLabel.setText("");
        return;
    }

    // Otherwise, fetch in background
    new Thread(() -> {
        try {
            List<ImagesDto> images = apiClient.getImagesByAuction(auctionId);
            if (images == null || images.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    imageLabel.setIcon(null);
                    imageLabel.setText("No image available");
                });
                return;
            }

            ImagesDto firstImage = images.get(0);
            String filePath = firstImage.getFilePath();
            if (filePath == null || filePath.trim().isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    imageLabel.setIcon(null);
                    imageLabel.setText("No image available");
                });
                return;
            }

            String imageUrl = "http://localhost:8080" + filePath;
            System.out.println("Loading image from: " + imageUrl);

            ImageIcon rawIcon = new ImageIcon(new URL(imageUrl));
            
            // Wait until image is fully loaded
            Image img = rawIcon.getImage();
            MediaTracker tracker = new MediaTracker(imageLabel);
            tracker.addImage(img, 0);
            tracker.waitForID(0);

            Image scaled = img.getScaledInstance(280, 150, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaled);

            auctionImageCache.put(auctionId, scaledIcon);

            SwingUtilities.invokeLater(() -> {
                // Only show if still selected
                if (auctionId.equals(currentAuctionId)) {
                    imageLabel.setIcon(scaledIcon);
                    imageLabel.setText("");
                }
            });

        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                imageLabel.setIcon(null);
                imageLabel.setText("Failed to load image");
            });
            e.printStackTrace();
        }
    }).start();
}


private void displayAuctionImage(List<ImagesDto> images) {
    if (images == null || images.isEmpty()) {
        imageLabel.setIcon(null);
        imageLabel.setText("No image available");
        return;
    }

    // Use the first image
    ImagesDto firstImage = images.get(0);
    String filePath = firstImage.getFilePath();
    
    if (filePath == null || filePath.trim().isEmpty()) {
        imageLabel.setIcon(null);
        imageLabel.setText("No image available");
        return;
    }

    // Try to load and display the image
    new Thread(() -> {
        try {
            // Construct full URL for the image
            String imageUrl = "http://localhost:8080" + filePath;
            URL url = new URL(imageUrl);
            ImageIcon imageIcon = new ImageIcon(url);
            
            SwingUtilities.invokeLater(() -> {
                if (imageIcon.getImageLoadStatus() == MediaTracker.COMPLETE) {
                    // Scale image to fit the display area
                    Image image = imageIcon.getImage();
                    Image scaledImage = image.getScaledInstance(280, 150, Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(scaledImage));
                    imageLabel.setText("");
                } else {
                    imageLabel.setIcon(null);
                    imageLabel.setText("Image load failed");
                }
            });
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                imageLabel.setIcon(null);
                imageLabel.setText("No image available");
            });
        }
    }).start();
}


    private void refreshSelectedAuctionDetails() {
    String selectedTitle = auctionJList.getSelectedValue();
    if (selectedTitle == null) return;

    AuctionItemDto currentDto = auctionMap.get(selectedTitle);
    if (currentDto == null) return;

    // Fetch the latest version of the auction from the REAL API
    new Thread(() -> {
        try {
            Optional<AuctionItemDto> latestOptional = apiClient.getAuctionId(currentDto.getAuctionId());

            if (latestOptional.isPresent()) {
                AuctionItemDto latest = latestOptional.get();
                auctionMap.put(selectedTitle, latest); // Update map

                SwingUtilities.invokeLater(() -> {
                    // Only update if this auction is still selected
                    if (selectedTitle.equals(auctionJList.getSelectedValue())) {
                        showDetailsFromDto(latest);
                        
                        // Update the status label color
                        if ("OPEN".equalsIgnoreCase(latest.getStatus())) {
                            statusLabel.setForeground(new Color(0, 128, 0));
                        } else if ("CLOSED".equalsIgnoreCase(latest.getStatus())) {
                            statusLabel.setForeground(Color.RED);
                        }
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Failed to refresh auction details: " + e.getMessage());
        }
    }).start();
}


    private JPanel createBrowsePanel() {
    JPanel browsePanel = new RoundedPanel(25, new Color(255, 255, 255, 220));
    browsePanel.setLayout(new BorderLayout(20, 20));
    browsePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

    JLabel heading = new JLabel("Available Auctions");
    heading.setFont(new Font("Segoe UI", Font.BOLD, 28));
    heading.setForeground(new Color(30, 30, 30));
    heading.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
    browsePanel.add(heading, BorderLayout.NORTH);

    // Create the auction list with proper sizing
    auctionListModel = new DefaultListModel<>();
    auctionJList = new JList<>(auctionListModel);
    auctionJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    auctionJList.setFont(new Font("Segoe UI", Font.PLAIN, 16));
    auctionJList.setFixedCellHeight(35); // Consistent row height
    
    // FIX: Create a properly sized scroll pane
    JScrollPane scrollPane = new JScrollPane(auctionJList);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
        BorderFactory.createEmptyBorder(5, 5, 5, 5)
    ));
    
    // FIX: Set preferred size to ensure scrolling works
    scrollPane.setPreferredSize(new Dimension(280, 300));
    
    // FIX: Wrap scroll pane in a panel to control sizing
    JPanel listPanel = new JPanel(new BorderLayout());
    listPanel.setOpaque(false);
    listPanel.add(scrollPane, BorderLayout.CENTER);
    listPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
    
    browsePanel.add(listPanel, BorderLayout.CENTER);

    // Details panel below - FIX: Use a fixed size for details panel
    JPanel detailPanel = createDetailPanel();
    detailPanel.setPreferredSize(new Dimension(400, 350));
    browsePanel.add(detailPanel, BorderLayout.SOUTH);

    return browsePanel;
}

private JPanel createDetailPanel() {
    JPanel detailPanel = new RoundedPanel(18, new Color(255, 255, 255, 230));
    detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
    detailPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
    detailPanel.setPreferredSize(new Dimension(400, 350));

    detailTitle = new JLabel("Select an auction to view details");
    detailTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
    detailTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
    detailPanel.add(detailTitle);
    detailPanel.add(Box.createVerticalStrut(10));

    // Image display panel
    imagePanel = new JPanel(new BorderLayout());
    imagePanel.setOpaque(false);
    imagePanel.setPreferredSize(new Dimension(300, 150));
    imagePanel.setMaximumSize(new Dimension(300, 150));
    imagePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    imageLabel = new JLabel("No image available", JLabel.CENTER);
    imageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    imageLabel.setForeground(Color.GRAY);
    imageLabel.setVerticalTextPosition(JLabel.BOTTOM);
    imageLabel.setHorizontalTextPosition(JLabel.CENTER);
    imagePanel.add(imageLabel, BorderLayout.CENTER);
    detailPanel.add(imagePanel);
    detailPanel.add(Box.createVerticalStrut(8));

    detailDesc = new JTextArea();
    detailDesc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    detailDesc.setEditable(false);
    detailDesc.setLineWrap(true);
    detailDesc.setWrapStyleWord(true);
    detailDesc.setOpaque(false);
    detailDesc.setMaximumSize(new Dimension(350, 60));
    detailPanel.add(detailDesc);
    detailPanel.add(Box.createVerticalStrut(8));

    // Info Labels
    JPanel infoBar = new JPanel(new GridLayout(2, 2, 8, 4));
    infoBar.setOpaque(false);
    infoBar.setMaximumSize(new Dimension(350, 60));
    detailBasePrice = createInfoLabel("Base: -");
    detailHighestBid = createInfoLabel("Highest: -");
    detailEndsIn = createInfoLabel("Ends in: -");
    statusLabel = createInfoLabel("Status: -");
    infoBar.add(detailBasePrice);
    infoBar.add(detailHighestBid);
    infoBar.add(detailEndsIn);
    infoBar.add(statusLabel);
    detailPanel.add(infoBar);
    detailPanel.add(Box.createVerticalStrut(8));

    // Bid Input and Buttons
    JPanel bidInputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    bidInputPanel.setOpaque(false);
    bidInputPanel.setMaximumSize(new Dimension(350, 35));
    bidInputPanel.add(new JLabel("Your Bid:"));
    bidAmountField = new JTextField(8);
    bidAmountField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    bidInputPanel.add(bidAmountField);
    detailPanel.add(bidInputPanel);
    detailPanel.add(Box.createVerticalStrut(8));

    JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    buttonBar.setOpaque(false);
    buttonBar.setMaximumSize(new Dimension(350, 45));
    RoundedButton placeBidBtn = new RoundedButton("Place Bid", new Color(106, 13, 173), Color.WHITE);
    RoundedButton payNowBtn = new RoundedButton("Pay Now (Fake QR)", new Color(34, 139, 34), Color.WHITE);
    buttonBar.add(placeBidBtn);
    buttonBar.add(payNowBtn);
    detailPanel.add(buttonBar);

    // List Selection Event
    auctionJList.addListSelectionListener(e -> {
        if (!e.getValueIsAdjusting()) {
            showSelectedAuctionDetails();
        }
    });

    placeBidBtn.addActionListener(e -> placeBidFlow());
    payNowBtn.addActionListener(e -> openPaymentDialog());

    return detailPanel;
}


    private void showSelectedAuctionDetails() {
    String key = auctionJList.getSelectedValue();
    if (key == null) return;

    AuctionItemDto dto = auctionMap.get(key);
    if (dto == null) return;

    this.currentAuctionId = dto.getAuctionId();
    System.out.println(currentAuctionId + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");

    showDetailsFromDto(dto);
    
    // Load and display images for the selected auction
    loadAuctionImage(currentAuctionId, key);
}



private void showLoadingState() {
    detailTitle.setText("Loading...");
    detailDesc.setText("Fetching auction details...");
    detailBasePrice.setText("Base: Loading...");
    detailHighestBid.setText("Highest: Loading...");
    detailEndsIn.setText("Ends in: Loading...");
    statusLabel.setText("Status: Loading...");
    
    // Clear image while loading
    imageLabel.setIcon(null);
    imageLabel.setText("Loading image...");
}

private void fetchAndDisplayAuctionDetails(Long auctionId, String auctionTitle) {
    new Thread(() -> {
        try {
            // Fetch fresh auction details using getAuctionId
            Optional<AuctionItemDto> freshAuctionOpt = apiClient.getAuctionId(auctionId);
            
            SwingUtilities.invokeLater(() -> {
                if (freshAuctionOpt.isPresent()) {
                    AuctionItemDto freshAuction = freshAuctionOpt.get();
                    
                    // Update the cache with fresh data
                    auctionMap.put(auctionTitle, freshAuction);
                    
                    // Display the updated details
                    showDetailsFromDto(freshAuction);
                    
                    // Load images for this auction
                    loadAuctionImage(auctionId, auctionTitle);
                    
                } else {
                    // Fallback to cached data if API fails
                    AuctionItemDto cachedDto = auctionMap.get(auctionTitle);
                    if (cachedDto != null) {
                        showDetailsFromDto(cachedDto);
                    }
                    JOptionPane.showMessageDialog(this, "Failed to fetch updated auction details", "Warning", JOptionPane.WARNING_MESSAGE);
                }
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
                // Fallback to cached data on error
                AuctionItemDto cachedDto = auctionMap.get(auctionTitle);
                if (cachedDto != null) {
                    showDetailsFromDto(cachedDto);
                }
                JOptionPane.showMessageDialog(this, "Error fetching auction details: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            });
        }
    }).start();
}

private void showDetailsFromDto(AuctionItemDto dto) {
    if (dto == null) return;
    
    detailTitle.setText(dto.getTitle());
    detailDesc.setText(dto.getDescription());

    // Show loading text while image loads
    imageLabel.setIcon(null);
    imageLabel.setText("Loading image...");

    // Display individual auction pricing and status
    if (dto.getBasePrice() != null) {
        detailBasePrice.setText("Base: " + formatCurrency(BigDecimal.valueOf(dto.getBasePrice())));
    } else {
        detailBasePrice.setText("Base: Not available");
    }
    
    
        BigDecimal highest=apiClient.getHighestBid(dto.getAuctionId()).get().getBidAmount();
    if (highest != null) {
        Double doub=highest.doubleValue();
        System.out.println("Highest:"+highest+"\n"+doub);
        detailHighestBid.setText("Highest: $" + String.format("%.2f", doub));
    } else {
        detailHighestBid.setText("Highest: No bids yet");
    }
    
    if (dto.getEndsIn() != null) {
        detailEndsIn.setText("Ends in: " + dto.getEndsIn());
    } else {
        detailEndsIn.setText("Ends in: Unknown");
    }
    
    if (dto.getStatus() != null) {
        statusLabel.setText("Status: " + dto.getStatus());
        
        // Visual status indicator
        if ("OPEN".equalsIgnoreCase(dto.getStatus())) {
            statusLabel.setForeground(new Color(0, 128, 0)); // Green for open
        } else if ("CLOSED".equalsIgnoreCase(dto.getStatus())) {
            statusLabel.setForeground(Color.RED); // Red for closed
        } else {
            statusLabel.setForeground(Color.BLUE); // Blue for other statuses
        }
    } else {
        statusLabel.setText("Status: Unknown");
        statusLabel.setForeground(Color.GRAY);
    }
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

    private String formatCurrency(BigDecimal amount) {
    if (amount == null) {
        return "$0.00";
    }
    NumberFormat nf = NumberFormat.getCurrencyInstance();
    return nf.format(amount);
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

