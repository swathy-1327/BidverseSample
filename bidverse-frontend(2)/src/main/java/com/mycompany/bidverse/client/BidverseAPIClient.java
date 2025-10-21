package com.mycompany.bidverse.client;

import com.mycompany.bidverse.AuthService;
import com.mycompany.bidverse.client.dto.AuctionItemDto;
import com.mycompany.bidverse.client.dto.BidDto;
import com.mycompany.bidverse.client.dto.BidderDto;
import com.mycompany.bidverse.client.dto.ImagesDto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.AuthProvider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Implements the communication layer with the Spring Boot Bidverse REST API.
 * This client now uses the Jackson library for reliable JSON serialization/deserialization.
 */
public class BidverseAPIClient {

    // --- Jackson ObjectMapper for JSON handling ---
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // Ignore unknown fields in JSON payload

    // --- Configuration ---
    // Change this to the actual URL of your Spring Boot API
    private static final String BASE_URL = "http://localhost:8080"; 

    // --- Dummy Data (USED ONLY IF HTTP FAILS) ---
    private final List<AuctionItemDto> dummyAuctions = Arrays.asList(
        createDummyAuction(1L, "Classic Rolex Submariner", "A vintage timepiece in excellent condition.", 5000.0, 7500.0, "1h 30m", "OPEN"),
        createDummyAuction(2L, "Signed Messi Jersey", "2022 World Cup signed jersey, authenticated.", 1000.0, 1500.0, "3h 45m", "OPEN"),
        createDummyAuction(3L, "Rare Pokemon Card", "First edition Charizard holo.", 10000.0, 12000.0, "CLOSED", "CLOSED")
    );
    
    private AuctionItemDto createDummyAuction(Long id, String title, String desc, double basePrice, double highestBid, String endsIn, String status) {
        AuctionItemDto dto = new AuctionItemDto();
        dto.setAuctionId(id);
        dto.setTitle(title);
        dto.setDescription(desc);
        dto.setBasePrice(basePrice);
        dto.setHighestBid(highestBid);
        dto.setEndsIn(endsIn);
        dto.setStatus(status);
        return dto;
    }


    // --- Core HTTP Helper ---

    /**
     * Sends the HTTP request and returns the raw JSON response string.
     */
    private String sendRequest(String endpoint, String method, String jsonInput) throws Exception {
        URL url = new URL(BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        
        // Timeout configuration
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        
        if (jsonInput != null && (method.equals("POST") || method.equals("PUT"))) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonInput.getBytes());
                os.flush();
            }
        }
        
        int responseCode = conn.getResponseCode();
        
        if (responseCode >= 200 && responseCode < 300) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            }
        } else {
            // Read error stream for meaningful message
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line);
                }
                String errorMessage = errorResponse.length() > 0 ? errorResponse.toString() : conn.getResponseMessage();
                throw new RuntimeException("HTTP Request Failed. Code: " + responseCode + " | Message: " + errorMessage);
            }
        }
    }

    // --- Jackson JSON Helper Methods ---

    /**
     * Converts a Java object to a JSON string.
     */
    private <T> String toJsonString(T object) throws Exception {
        return MAPPER.writeValueAsString(object);
    }

    /**
     * Converts a JSON string to a single Java object.
     */
    private <T> T parseJsonToObject(String json, Class<T> clazz) throws Exception {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return MAPPER.readValue(json, clazz);
    }

    /**
     * Converts a JSON array string to a List of Java objects.
     */
    private <T> List<T> parseJsonToList(String json, Class<T> clazz) throws Exception {
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return new ArrayList<>();
        }
        // Use TypeReference to correctly deserialize generic List types
        return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
    }

    // --- Public API Methods ---

    /**
     * GET /auctions - Fetches all open auction items.
     */
    public List<AuctionItemDto> getAllAuctions() {
        try {
            System.out.println("API Client: Fetching all auctions from backend...");
            String json = sendRequest("/auctions", "GET", null);
            List<AuctionItemDto> result = parseJsonToList(json, AuctionItemDto.class);
            return result; 
        } catch (Exception e) {
            System.err.println("API Client Failed to fetch auctions. Falling back to dummy data. Error: " + e.getMessage());
            return dummyAuctions;
        }
    }

    /**
     * GET /auctions/{auctionId} - Fetches a single auction to refresh details.
     */
    public Optional<AuctionItemDto> getAuctionId(Long auctionId) {
        try {
            System.out.println("API Client: Refreshing auction ID " + auctionId);
            String json = sendRequest("/auctions/" + auctionId, "GET", null);
            AuctionItemDto result = parseJsonToObject(json, AuctionItemDto.class);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            System.err.println("API Client Failed to refresh auction ID " + auctionId + ". Falling back to dummy data. Error: " + e.getMessage());
            // Fallback: Use the dummy logic to simulate a refreshed item
            Optional<AuctionItemDto> current = dummyAuctions.stream().filter(a -> a.getAuctionId().equals(auctionId)).findFirst();
            if (current.isPresent()) {
                AuctionItemDto dto = current.get();
                // Simulate a higher bid or time change
                dto.setHighestBid(dto.getHighestBid() + new Random().nextInt(100) + 1.0);
                dto.setEndsIn("Now!");
                return Optional.of(dto);
            }
            return Optional.empty();
        }
    }

    /**
     * GET /bidders/email/{email} - Fetches a bidder's profile.
     */
    public Optional<BidderDto> getBidderProfileByEmail(String email) {
        try {
            System.out.println("API Client: Fetching profile for " + email);
            String json = sendRequest("/bidders/email/" + email, "GET", null);
            BidderDto result = parseJsonToObject(json, BidderDto.class);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            System.err.println("API Client Failed to fetch profile. Falling back to dummy data. Error: " + e.getMessage());
            // Fallback: Returning DUMMY profile
            BidderDto dto = new BidderDto();
            dto.setBidderId(101L);
            dto.setBidderName("John Smith");
            dto.setBidderEmail(email);
            dto.setPhno("555-1234");
            dto.setAddress("123 Auction Lane, Bid City");
            return Optional.of(dto);
        }
    }
    
    /**
 * GET /auction/{auctionId}/highest - Fetches the highest bid for a specific auction.
 */
public Optional<BidDto> getHighestBid(Long auctionId) {
    try {
        System.out.println("API Client: Fetching highest bid for auction ID " + auctionId);
        String json = sendRequest("/bids/auction/" + auctionId + "/highest", "GET", null);
        BidDto result = parseJsonToObject(json, BidDto.class);
        return Optional.ofNullable(result);
    } catch (Exception e) {
        System.err.println("API Client Failed to fetch highest bid for auction " + auctionId + ". Falling back to dummy data. Error: " + e.getMessage());
        // Optional fallback: return dummy bid with zero amount
        BidDto dummyBid = new BidDto();
        dummyBid.setAuctionItemId(auctionId);
        dummyBid.setBidAmount(BigDecimal.ZERO);
        dummyBid.status = "No Bids";
        return Optional.of(dummyBid);
    }
}


    /**
     * GET /bids/bidder/{bidderId} - Fetches all bids placed by the user.
     */
    public List<BidDto> getMyBids(Long bidderId) {
        try {

            System.out.println("API Client: Fetching bids for bidder " + bidderId);
            String json = sendRequest("/bids/bidder/" + bidderId, "GET", null);
            return parseJsonToList(json, BidDto.class);
        } catch (Exception e) {
            System.err.println("API Client Failed to fetch my bids. Falling back to dummy data. Error: " + e.getMessage());
            // Fallback: Returning DUMMY bids
            List<BidDto> bids = new ArrayList<>();
            bids.add(createDummyBid(1L, "Classic Rolex Submariner", new BigDecimal("7000.00"), "Winning"));
            bids.add(createDummyBid(2L, "Signed Messi Jersey", new BigDecimal("1400.00"), "Outbid"));
            return bids;
        }
    }

    /**
     * GET /bids/won/{bidderId} - Fetches all auctions won by the user.
     */
    public List<BidDto> getWonBids(Long bidderId) {
        try {
            System.out.println("API Client: Fetching won auctions for bidder " + bidderId);
            String json = sendRequest("/bids/bidder/won/" + AuthService.getCurrentBidderId(), "GET", null);
            return parseJsonToList(json, BidDto.class);
        } catch (Exception e) {
            System.err.println("API Client Failed to fetch won bids. Falling back to dummy data. Error: " + e.getMessage());
            // Fallback: Returning DUMMY won bids
            List<BidDto> wonBids = new ArrayList<>();
            wonBids.add(createDummyBid(3L, "Antique Desk Lamp", new BigDecimal("450.00"), "Won"));
            return wonBids;
        }
    }

    /**
     * POST /bids - Places a new bid.
     */
    public Optional<BidDto> placeBid(BidDto bid) throws RuntimeException {
        // Simple business logic check simulation (can be kept for UI/client-side validation)
        if (bid.getBidAmount().doubleValue() < 7600.00 && bid.getAuctionItemId() == 1L) {
             throw new RuntimeException("Bid must be higher than the current highest bid ($7,500.00)!");
        }

        try {
            System.out.println("API Client: Placing bid for Auction " + bid.getAuctionItemId() + " with amount " + bid.getBidAmount());
            
            // Use Jackson to serialize the BidDto object into a JSON payload
            String jsonPayload = toJsonString(bid);
            
            String json = sendRequest("/bids", "POST", jsonPayload);
            // Deserialize the successful response body (which should contain the updated BidDto)
            BidDto result = parseJsonToObject(json, BidDto.class);
            return Optional.ofNullable(result);

        } catch (Exception e) {
            System.err.println("API Client Failed to place bid. Falling back to dummy success response. Error: " + e.getMessage());
            // Fallback: Return DUMMY success response
            bid.setAuctionTitle(bid.getAuctionItemId() == 1L ? "Rolex Submariner" : "New Item");
            bid.status = "Winning";
            return Optional.of(bid); 
        }
    }
    
    // Helper for dummy data creation
    private BidDto createDummyBid(Long auctionId, String title, BigDecimal amount, String status) {
        BidDto bid = new BidDto();
        bid.setAuctionItemId(auctionId);
        bid.auctionTitle = title;
        bid.setBidAmount(amount);
        bid.status = status;
        return bid;
   }

 /* GET /images/auction/{auctionId} - Fetches all images for a specific auction.
 */
public List<ImagesDto> getImagesByAuction(Long auctionId) {
    try {
        System.out.println("API Client: Fetching images for auction ID " + auctionId);
        String json = sendRequest("/images/auction/" + auctionId, "GET", null);
        List<ImagesDto> result = parseJsonToList(json, ImagesDto.class);
        return result;
    } catch (Exception e) {
        System.err.println("API Client Failed to fetch images for auction " + auctionId + ". Error: " + e.getMessage());
        // Return empty list instead of dummy data
        return new ArrayList<>();
    }
}

}
