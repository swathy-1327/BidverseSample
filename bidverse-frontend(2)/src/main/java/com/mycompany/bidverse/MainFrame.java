package com.mycompany.bidverse;

import javax.swing.*;
import com.mycompany.bidverse.client.BidderDashboard;

import java.awt.*;

/**
 * MainFrame holds CardLayout and all pages.
 * Card names: "intro", "login", "profile", "bidder", "seller"
 */
public class MainFrame extends JFrame {
    private CardLayout cardLayout;
    private JPanel cards;

    public MainFrame() {
        setTitle("Bidverse");
        // FIX SIZE TYPO: setSize(800, 600)
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        cardLayout = new CardLayout();
        cards = new JPanel(cardLayout);

        // Pages are JPanels that take a reference to MainFrame for navigation
        // ADDED: The new BidverseIntroPanel
        BidverseIntroPanel intro = new BidverseIntroPanel(this);

        LoginPage login = new LoginPage(this);
        ProfileSelectionPage profile = new ProfileSelectionPage(this);
        BidderRegisterPage bidder = new BidderRegisterPage(this);
        SellerRegisterPage seller = new SellerRegisterPage(this);
        BidderDashboard bidderdashboard=new BidderDashboard(this);
        // ADD intro card first
        cards.add(intro, "intro");

        cards.add(login, "login");
        cards.add(profile, "profile");
        cards.add(bidder, "bidder");
        cards.add(seller, "seller");
        cards.add(bidderdashboard, "bidderdashboard");
        add(cards);

        // SHOW intro card first
        showCard("intro");

        setVisible(true);
    }

    public void showCard(String name) {
        cardLayout.show(cards, name);
    }

    /**
     * Set bidder information in the dashboard before showing it
     */
    public void setBidderInfo(Long bidderId, String bidderEmail, String bidderName) {
        // Find the bidder dashboard card and set the bidder info
        for (Component component : cards.getComponents()) {
            if (component instanceof com.mycompany.bidverse.client.BidderDashboard) {
                ((com.mycompany.bidverse.client.BidderDashboard) component).setBidderInfo(bidderId, bidderEmail, bidderName);
                break;
            }
        }
    }

    /**
     * Called by pages when login/register successful to close the app.
     */
    public void closeAppWithSuccess(String message) {
        JOptionPane.showMessageDialog(this, message);
        dispose();
        System.exit(0);
    }
}