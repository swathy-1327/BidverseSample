
package com.mycompany.bidverse;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/**
 * Profile selection panel with icon buttons
 */
public class ProfileSelectionPage extends JPanel {
    private MainFrame navigator;

    // --- Helper Icon for "Press Down" Effect ---
    // This icon shifts the image 1 pixel down and 1 pixel right.
    private Icon createShiftedIcon(ImageIcon originalIcon) {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return originalIcon.getIconWidth();
            }

            @Override
            public int getIconHeight() {
                return originalIcon.getIconHeight();
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                originalIcon.paintIcon(c, g, x + 1, y + 1); // Shifted
            }
        };
    }
    // --- End Helper ---

    public ProfileSelectionPage(MainFrame navigator) {
        this.navigator = navigator;
        setLayout(null);

        GradientPanel bg = new GradientPanel();
        bg.setLayout(null);
        bg.setBounds(0, 0, 800, 600);

        // Title
        JLabel title = new JLabel("Register as", SwingConstants.CENTER);
        title.setFont(Theme.TITLE_FONT);
        title.setBounds(250, 40, 300, 50);
        bg.add(title);

        // --- ICON LOADING ---
        java.net.URL imageUrl = getClass().getResource("profile.png");
        ImageIcon profileIcon;

        if (imageUrl != null) {
            profileIcon = new ImageIcon(imageUrl);
        } else {
            System.err.println("Error: 'profile.png' not found in package directory.");
            profileIcon = new ImageIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB));
        }

        Image scaled = profileIcon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaled);

        // Create the shifted (pressed) icon using the helper
        Icon pressedIcon = createShiftedIcon(scaledIcon);

        // NOTE: For the Rollover/Hover effect (Glow), you would need to load a second
        // image file
        // named "profile_hover.png" here. For simplicity, we'll use the unshifted icon
        // for rollover.
        Icon rolloverIcon = scaledIcon;
        // --- END ICON LOADING ---

        // Define borders for the Border Highlight effect
        Border defaultBorder = BorderFactory.createEmptyBorder();
        // Use a color that matches your theme, like a darker purple.
        Border focusedBorder = new LineBorder(new Color(150, 100, 200), 2);

        // --- BIDDER ICON BUTTON ---
        JButton bidderIcon = new JButton(scaledIcon);
        bidderIcon.setBounds(250, 130, 120, 120);

        // 1. Transparency Fix
        bidderIcon.setContentAreaFilled(false);
        // bidderIcon.setBackground(Color.WHITE); // Already commented out

        // 2. Rollover Effect (On Hover) - Uses the standard icon, change to
        // 'profile_hover.png' for a glow.
        bidderIcon.setRolloverIcon(rolloverIcon);
        bidderIcon.setRolloverEnabled(true);

        // 3. Press Down Effect
        bidderIcon.setPressedIcon(pressedIcon);

        // 4. Border Highlight Effect (On Press)
        bidderIcon.setBorder(defaultBorder);
        bidderIcon.setFocusPainted(false); // Keep this false

        bidderIcon.addActionListener(e -> navigator.showCard("bidder"));

        // 5. Add custom mouse listener for the border effect
        bidderIcon.addMouseListener(createButtonMouseListener(defaultBorder, focusedBorder));

        bg.add(bidderIcon);

        // --- SELLER ICON BUTTON ---
        JButton sellerIcon = new JButton(scaledIcon);
        sellerIcon.setBounds(430, 130, 120, 120);

        // 1. Transparency Fix
        sellerIcon.setContentAreaFilled(false);
        // sellerIcon.setBackground(Color.WHITE); // Already commented out

        // 2. Rollover Effect
        sellerIcon.setRolloverIcon(rolloverIcon);
        sellerIcon.setRolloverEnabled(true);

        // 3. Press Down Effect
        sellerIcon.setPressedIcon(pressedIcon);

        // 4. Border Highlight Effect (On Press)
        sellerIcon.setBorder(defaultBorder);
        sellerIcon.setFocusPainted(false);

        sellerIcon.addActionListener(e -> navigator.showCard("seller"));

        // 5. Add custom mouse listener for the border effect
        sellerIcon.addMouseListener(createButtonMouseListener(defaultBorder, focusedBorder));

        bg.add(sellerIcon);

        // Label under icons (No changes needed here)
        JLabel bidderLabel = new JLabel("Bidder", SwingConstants.CENTER);
        bidderLabel.setFont(Theme.BUTTON_FONT);
        bidderLabel.setBounds(250, 250, 120, 30);
        bg.add(bidderLabel);

        JLabel sellerLabel = new JLabel("Seller", SwingConstants.CENTER);
        sellerLabel.setFont(Theme.BUTTON_FONT);
        sellerLabel.setBounds(430, 250, 120, 30);
        bg.add(sellerLabel);

        // Back to login button (No animation applied for now)
        JButton backBtn = new JButton("Back to Login");
        backBtn.setBounds(250, 330, 300, 44);
        backBtn.setFont(Theme.BUTTON_FONT);
        backBtn.addActionListener(e -> navigator.showCard("login"));
        bg.add(backBtn);

        this.add(bg);
    }

    // Helper method to create the MouseListener for the border effect
    private MouseAdapter createButtonMouseListener(Border defaultBorder, Border focusedBorder) {
        return new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Change border on hover
                ((JButton) e.getComponent()).setBorder(focusedBorder);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Restore default border when mouse leaves
                ((JButton) e.getComponent()).setBorder(defaultBorder);
            }
        };
    }
}