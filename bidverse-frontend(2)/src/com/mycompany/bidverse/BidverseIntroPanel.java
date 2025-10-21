package com.mycompany.bidverse;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// CHANGE 1: Extend JPanel, not JFrame
public class BidverseIntroPanel extends JPanel {

    // FIX 1: Declare the MainFrame reference (navigator)
    private MainFrame navigator;

    // FIX 2: Constructor must accept MainFrame
    public BidverseIntroPanel(MainFrame navigator) {
        this.navigator = navigator; // FIX 2: Store the MainFrame reference

        // REMOVE all JFrame methods (setTitle, setSize, setDefaultCloseOperation, etc.)
        this.setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                GradientPaint gp = new GradientPaint(
                        0, 0, Color.WHITE,
                        getWidth(), getHeight(), new Color(106, 13, 173));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        mainPanel.setLayout(new GridBagLayout());

        JPanel contentPanel = new JPanel();
        contentPanel.setOpaque(false);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        JLabel heading = new JLabel("Welcome to BidVerse");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 36));
        heading.setForeground(new Color(30, 30, 30));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(heading);

        contentPanel.add(Box.createVerticalStrut(20));

        JLabel caption = new JLabel(
                "<html><div style='text-align: center;'>Experience live auctions, bid on exciting items,<br>and win your dream products securely!</div></html>");
        caption.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        caption.setForeground(new Color(20, 20, 20));
        caption.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(caption);

        contentPanel.add(Box.createVerticalStrut(40));

        RoundedButton getStartedButton = new RoundedButton("Get Started", new Color(106, 13, 173), Color.WHITE);
        getStartedButton.setFont(new Font("Segoe UI", Font.BOLD, 20));
        getStartedButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // The 'navigator' is now resolved!
        getStartedButton.addActionListener(e -> {
            navigator.showCard("login");
        });

        contentPanel.add(getStartedButton);

        mainPanel.add(contentPanel);
        this.add(mainPanel, BorderLayout.CENTER); // ADD mainPanel to this JPanel
    }

    // Static inner class RoundedButton (no changes needed)
    static class RoundedButton extends JButton {
        // ... (body of RoundedButton) ...
        private final Color bgColor;
        private final Color textColor;
        private Color currentBg;
        private int shadowOffset = 4;

        public RoundedButton(String text, Color bg, Color fg) {
            super(text);
            this.bgColor = bg;
            this.textColor = fg;
            this.currentBg = bg;
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    shadowOffset = 8;
                    currentBg = brighten(bgColor, 0.2f);
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    shadowOffset = 4;
                    currentBg = bgColor;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int arc = 30;

            g2.setColor(new Color(0, 0, 0, 60));
            g2.fillRoundRect(shadowOffset, shadowOffset, width - shadowOffset, height - shadowOffset, arc, arc);

            g2.setColor(currentBg);
            g2.fillRoundRect(0, 0, width - shadowOffset, height - shadowOffset, arc, arc);

            g2.setColor(textColor);
            FontMetrics fm = g2.getFontMetrics();
            int x = (width - fm.stringWidth(getText())) / 2;
            int y = (height + fm.getAscent()) / 2 - 3;
            g2.drawString(getText(), x, y);

            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 60);
        }

        private static Color brighten(Color color, float factor) {
            int r = Math.min(255, (int) (color.getRed() + 255 * factor));
            int g = Math.min(255, (int) (color.getGreen() + 255 * factor));
            int b = Math.min(255, (int) (color.getBlue() + 255 * factor));
            return new Color(r, g, b, color.getAlpha());
        }
    }

    // REMOVE the unnecessary main(Object object) method here.
}