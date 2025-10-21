package com.mycompany.bidverse;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Color;

public class GradientPanel extends JPanel {
    private Color top = Theme.LIGHT_PURPLE;
    private Color bottom = Theme.OFF_WHITE;

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        Graphics2D g2 = (Graphics2D) g;
        GradientPaint gp = new GradientPaint(0, 0, top, 0, h, bottom);
        g2.setPaint(gp);
        g2.fillRect(0, 0, w, h);
    }
}
