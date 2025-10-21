package com.mycompany.bidverse;

public class BidderService {
    /**
     * POST /bidders
     * JSON:
     * {"bidderName":"...","bidderEmail":"...","bidderPassword":"...","phno":"...","address":"..."}
     */
    public static boolean register(String name, String email, String password, String phone, String address) {
        // CORRECTION: Field names updated to match the Backend BidderDto
        String json = String.format(
                "{\"bidderName\":\"%s\",\"bidderEmail\":\"%s\",\"bidderPassword\":\"%s\",\"phno\":\"%s\",\"address\":\"%s\"}",
                esc(name), esc(email), esc(password), esc(phone), esc(address));

        String resp = ApiClient.postJson("/bidders", json);
        return resp != null;
    }

    private static String esc(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}