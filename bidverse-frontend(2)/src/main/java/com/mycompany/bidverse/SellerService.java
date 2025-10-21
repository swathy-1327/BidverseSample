package com.mycompany.bidverse;

public class SellerService {
    /**
     * POST /sellers
     * JSON:
     * {"sellerName":"...","sellerEmail":"...","sellerPassword":"...","phno":"...","paymentDetails":"...","address":"..."}
     */
    public static boolean register(String name, String email, String password, String phone, String shopName,
            String address) {

        // CORRECTION: Field names updated to match the Backend SellerDto fields
        // shopName (from frontend) is mapped to paymentDetails (in SellerDto)
        String json = String.format(
                "{\"sellerName\":\"%s\",\"sellerEmail\":\"%s\",\"sellerPassword\":\"%s\",\"phno\":\"%s\",\"paymentDetails\":\"%s\",\"address\":\"%s\"}",
                esc(name), esc(email), esc(password), esc(phone), esc(shopName), esc(address));

        String resp = ApiClient.postJson("/sellers", json);
        return resp != null;
    }

    private static String esc(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}