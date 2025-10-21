package com.mycompany.bidverse;

public class AuthService {

    // 1. Static fields to hold the logged-in user's information
    private static String currentUserRole = null;
    private static Long currentUserId = null;
    private static String currentUserEmail = null;
    private static String currentUserName = null;
    private static Long currentBidderId = null;

    // 2. Public Getters for the LoginPage to retrieve the user information
    public static String getCurrentUserRole() {
        return currentUserRole;
    }
    
    public static Long getCurrentUserId() {
        return currentUserId;
    }
    
    public static Long getCurrentBidderId() {
        System.out.println("Role: " + currentUserRole);
        if ("BIDDER".equals(currentUserRole)) {
            return currentBidderId;
        }
        return null;
    }

    public static void setCurrentBidderId(Long bidderId) {
        currentBidderId = bidderId;
    }

    public static String getCurrentUserEmail() {
        return currentUserEmail;
    }
    
    public static String getCurrentUserName() {
        return currentUserName;
    }

    /**
     * POST /login with {"email":"...","password":"..."}
     * Returns true if backend returned 2xx and login was successful, false
     * otherwise.
     */
    public static boolean login(String email, String password) {
        // Reset state before attempting a new login
        currentUserRole = null;
        currentUserId = null;
        currentUserEmail = null;
        currentUserName = null;

        if (email == null)
            email = "";
        if (password == null)
            password = "";

        String json = "{\"email\":\"" + escapeJson(email) + "\",\"password\":\"" + escapeJson(password) + "\"}";

        String resp = ApiClient.postJson("/login", json);

        if (resp != null && !resp.trim().isEmpty()) {
            try {
                // Store the email for later use
                currentUserEmail = email;
                
                // Parse role
                String role = extractJsonField(resp, "role");
                if (role != null) {
                    currentUserRole = role;
                }
                
                // Parse user ID
                String userIdStr = extractJsonField(resp, "id");
                if (userIdStr != null) {
                    try {
                        currentUserId = Long.parseLong(userIdStr);
                    } catch (NumberFormatException e) {
                        System.err.println("Could not parse user ID: " + userIdStr);
                    }
                }
                
                // Parse user name (could be bidderName or sellerName)
                String userName = extractJsonField(resp, "bidderName");
                if (userName == null) {
                    userName = extractJsonField(resp, "sellerName");
                }
                if (userName != null) {
                    currentUserName = userName;
                }

                return currentUserRole != null; // Login succeeded if we got a role

            } catch (Exception e) {
                // Unexpected error during parsing
                System.err.println("Error during user information extraction: " + e.getMessage());
                return false;
            }
        }

        // Login failed (resp was null/empty)
        return false;
    }
    
    private static String extractJsonField(String json, String fieldName) {
        String fieldKey = "\"" + fieldName + "\":\"";
        int fieldStartIndex = json.indexOf(fieldKey);
        
        if (fieldStartIndex != -1) {
            fieldStartIndex += fieldKey.length();
            int fieldEndIndex = json.indexOf("\"", fieldStartIndex);
            
            if (fieldEndIndex != -1) {
                return json.substring(fieldStartIndex, fieldEndIndex);
            }
        }
        
        return null;
    }

    private static String escapeJson(String s) {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}