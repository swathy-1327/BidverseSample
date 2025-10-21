package com.mycompany.bidverse;

import com.mycompany.bidverse.MainFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame();
        });
    }
}
