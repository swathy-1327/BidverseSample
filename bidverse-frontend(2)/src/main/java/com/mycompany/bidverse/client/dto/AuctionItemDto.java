package com.mycompany.bidverse.client.dto;

import java.math.BigDecimal;

/**
 * NOTE: In a real project, these DTOs would use an external JSON library
 * (like Jackson or Gson) annotations for automatic JSON serialization/deserialization.
 * They are defined here to satisfy the compiler requirements of the Swing client.
 */

// --- 1. AuctionItemDto ---
public class AuctionItemDto {
    private Long auctionId;
    private String title;
    private String description;
    private Double basePrice;
    private Double highestBid;
    private String endsIn; // Time remaining string (e.g., "1h 30m")
    private String status;

    // Getters
    public Long getAuctionId() { return auctionId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Double getBasePrice() { return basePrice; }
    public Double getHighestBid() { return highestBid; }
    public String getEndsIn() { return endsIn; }
    public String getStatus() { return status; }

    // Setters (required for placeholder loading/simulated payment)
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setBasePrice(Double basePrice) { this.basePrice = basePrice; }
    public void setHighestBid(Double highestBid) { this.highestBid = highestBid; }
    public void setEndsIn(String endsIn) { this.endsIn = endsIn; }
    public void setStatus(String status) { this.status = status; }

    // Override toString for easy list model display
    @Override
    public String toString() {
        return title;
    }
}