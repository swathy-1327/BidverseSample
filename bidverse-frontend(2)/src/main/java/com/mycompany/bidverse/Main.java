package com.mycompany.bidverse;

import com.mycompany.bidverse.MainFrame;
import com.mycompany.bidverse.BackendClient;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import javax.swing.JPanel;


public class Main {

    public static String email;
    public static Long sell_id;
    public static String sellerName;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new MainFrame();
        });
    }

    public static void switchToPanel(JFrame frame, JPanel panel) {
        frame.setContentPane(panel);
        frame.revalidate();
        frame.repaint();
    }   

    static void idInitialize(){
        BackendClient.getSellerByEmail(email).ifPresentOrElse(seller -> {

            sell_id = seller.sellerId();

            sellerName = seller.sellerName();

            System.out.println("Login Success: Seller ID: " + sell_id + ", Name: " + sellerName);



            // 2. Launch SellerHome

            System.out.println(email);



        }, () -> {

            // Simulated Login Failure

           

           

               
            
            System.out.println(email);

        });
    } 
    
}

