package com.mycompany.bidverse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
// removed unused LocalTime import
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

public class BackendClient {
    private static final String BASE_URL = "http://localhost:8080";
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    // --- Frontend DTOs (Simplified Records to match JSON structure) ---

    // Note: LocalTime is used here to match your backend DTOs, but recommend changing to LocalDateTime.

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuctionItemDto(
            Long auctionId, String title, Double basePrice, String category,
            String description, String status, Long sellerId, Long winnerId,
            String startTime, String endTime) {}


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SellerDto(
            Long sellerId, String sellerName, String sellerEmail, Long phno,
            String paymentDetails) {}


    @JsonIgnoreProperties(ignoreUnknown = true)        
    public record BidDto(
            Long bidId, Double bidAmount, String timestamp, Long auctionItemId, Long bidderId) {}


    @JsonIgnoreProperties(ignoreUnknown = true)        
    public record BidderDto(
            Long bidderId, String bidderName, String bidderEmail, Long phno, String address) {}

    @JsonIgnoreProperties(ignoreUnknown = true)        
    public record ImagesDto(
            Long imageId, String filePath, Long auctionItemId) {}

    // --- Core HTTP Methods ---

    private static Optional<String> sendRequest(String url, String method, Optional<Object> body) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + url))
                    .header("Content-Type", "application/json");

            if (body.isPresent()) {
                String jsonBody = mapper.writeValueAsString(body.get());
                builder.method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
            } else if (method.equals("POST")) {
                 builder.POST(HttpRequest.BodyPublishers.noBody());
            } else if (method.equals("PUT") && !body.isPresent()){
                builder.PUT(HttpRequest.BodyPublishers.noBody());
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return Optional.of(response.body());
            } else {
                System.err.println("API Error " + method + " " + url + ": " + response.statusCode() + " | " + response.body());
                return Optional.empty();
            }
        } catch (Exception e) {
            System.err.println("Network/Serialization Error: " + e.getMessage());
            return Optional.empty();
        }
    }
    
    // --- API Call Implementations ---

    public static Optional<SellerDto> getSellerByEmail(String email) {
        return sendRequest("/sellers/email/" + email, "GET", Optional.empty())
        
                .flatMap(json -> {
                    System.out.println("Debugging JSON Response: " + json);
                    try {
                        // The backend SellerDto has password and userId, but we only map what we need.
                        System.out.println("haha");
                        return Optional.of(mapper.readValue(json, SellerDto.class));
                        
                    } catch (IOException e) {
                        System.out.println("hihi");
                        return Optional.empty();
                    }
                });
    }

    public static Optional<SellerDto> updateSeller(String email, SellerDto dto) {
        return sendRequest("/sellers/email/" + email, "PUT", Optional.of(dto))
                .flatMap(json -> {
                    try {
                        return Optional.of(mapper.readValue(json, SellerDto.class));
                    } catch (IOException e) {
                        return Optional.empty();
                    }
                });
    }

    /*public static boolean deleteSeller(String email) {
        // Backend uses GET for delete, following the backend code
        return sendRequest("/sellers/delete/" + email, "GET", Optional.empty()).isPresent();
    }*/

    public static boolean deleteSeller(String email) {
    Optional<String> response = sendRequest("/sellers/delete/" + email, "GET", Optional.empty());
    if (response.isPresent()) {
        String result = response.get();
        // Only consider it successful if backend confirms deletion
        return result.equalsIgnoreCase("Seller deleted successfully");
    } else {
        return false;
    }
}


    public static List<AuctionItemDto> getAuctionsBySellerEmail(String email) {
        return sendRequest("/auctions/seller/email/" + email, "GET", Optional.empty())
                .map(json -> {
                    try {
                        return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, BackendClient.AuctionItemDto.class));
                    } catch (IOException e) {
                        return Collections.<BackendClient.AuctionItemDto>emptyList();
                    }
                }).orElse(Collections.emptyList());
    }

    public static Optional<AuctionItemDto> createAuction(AuctionItemDto dto) {
        System.out.println(dto);
        
        return sendRequest("/auctions", "POST", Optional.of(dto))
                .flatMap(json -> {
                    
                    try {
                        
                        return Optional.of(mapper.readValue(json, AuctionItemDto.class));
                    } catch (Exception e) {
                        System.out.println(e);
                        return Optional.empty();
                    }
                });
    }

    public static Optional<BidDto> getHighestBid(Long auctionId) {
        return sendRequest("/bids/auction/" + auctionId + "/highest", "GET", Optional.empty())
                .flatMap(json -> {
                    try {
                        // The backend returns a single BidDto or an error/null if no bids.
                        if (json == null || json.trim().isEmpty() || json.equalsIgnoreCase("null")) return Optional.empty();
                        return Optional.of(mapper.readValue(json, BidDto.class));
                    } catch (IOException e) {
                        return Optional.empty();
                    }
                });
    }
    
    public static Optional<BidderDto> getBidderById(Long bidderId) {
        return sendRequest("/bidders/id/" + bidderId, "GET", Optional.empty())
                .flatMap(json -> {
                    try {
                        return Optional.of(mapper.readValue(json, BidderDto.class));
                    } catch (IOException e) {
                        return Optional.empty();
                    }
                });
    }

    public static Optional<AuctionItemDto> getAuctionById(Long auctionId) {
        return sendRequest("/auctions/" + auctionId, "GET", Optional.empty())
                .flatMap(json -> {
                    try {
                        return Optional.of(mapper.readValue(json, AuctionItemDto.class));
                    } catch (IOException e) {
                        return Optional.empty();
                    }
                });
    }
    
    public static List<ImagesDto> getImagesByAuction(Long auctionId) {
        return sendRequest("/images/auction/" + auctionId, "GET", Optional.empty())
                .map(json -> {
                    try {
                        return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, BackendClient.ImagesDto.class));
                    } catch (IOException e) {
                        return Collections.<BackendClient.ImagesDto>emptyList();
                    }
                }).orElse(Collections.emptyList());
    }

    public static Optional<AuctionItemDto> updateAuction(Long auctionId, AuctionItemDto dto) {
        return sendRequest("/auctions/" + auctionId, "PUT", Optional.of(dto))
                .flatMap(json -> {
                    try {
                        return Optional.of(mapper.readValue(json, AuctionItemDto.class));
                    } catch (IOException e) {
                        return Optional.empty();
                    }
                });
    }

    public static boolean deleteAuction(Long auctionId) {
        return sendRequest("/auctions/" + auctionId, "DELETE", Optional.empty()).isPresent();
    }

    public static Optional<AuctionItemDto> closeAuction(Long auctionId) {
        // PUT to /auctions/{id}/close with no body
        return sendRequest("/auctions/" + auctionId + "/close", "PUT", Optional.empty())
                .flatMap(json -> {
                    try {
                        return Optional.of(mapper.readValue(json, AuctionItemDto.class));
                    } catch (IOException e) {
                        return Optional.empty();
                    }
                });
    }

    // Special Multipart Form Data Upload
    public static boolean uploadImage(String filePath, Long auctionItemId) {
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("File not found at path: " + filePath);
            return false;
        }

        String boundary = "---boundary---" + System.currentTimeMillis();
        try {
            // Build the Multipart Body
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            String fileName = file.getName();
            
            String header = "--" + boundary + "\r\n" +
                            "Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n" +
                            "Content-Type: " + Files.probeContentType(file.toPath()) + "\r\n\r\n";
            
            String footer = "\r\n--" + boundary + "\r\n" +
                            "Content-Disposition: form-data; name=\"auctionItemId\"\r\n\r\n" +
                            auctionItemId + "\r\n" +
                            "--" + boundary + "--\r\n";

            byte[] headerBytes = header.getBytes();
            byte[] footerBytes = footer.getBytes();
            byte[] body = new byte[headerBytes.length + fileBytes.length + footerBytes.length];

            System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
            System.arraycopy(fileBytes, 0, body, headerBytes.length, fileBytes.length);
            System.arraycopy(footerBytes, 0, body, headerBytes.length + fileBytes.length, footerBytes.length);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/images"))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) { // 201 Created is returned by ImagesController
                return true;
            } else {
                System.err.println("Image Upload Failed: " + response.statusCode() + " | " + response.body());
                return false;
            }
        } catch (Exception e) {
            System.err.println("Image Upload Error: " + e.getMessage());
            return false;
        }
    }
}