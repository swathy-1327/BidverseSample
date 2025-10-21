package com.mycompany.bidverse.client.dto;
import java.math.BigDecimal;
public class BidDto {
    private Long bidId;
    private Long auctionItemId;
    private Long bidderId;
    private BigDecimal bidAmount;
    private String bidTime; // Assuming time is returned as a string
    public String auctionTitle; // Assuming this is joined/included in the API response
    public String status; // Assuming bid status (e.g., "Winning", "Outbid", "Won")

    // Getters
    public Long getBidId() { return bidId; }
    public Long getAuctionItemId() { return auctionItemId; }
    public Long getBidderId() { return bidderId; }
    public BigDecimal getBidAmount() { return bidAmount; }
    public String getBidTime() { return bidTime; }
    public String getAuctionTitle() { return auctionTitle; }
    public String getStatus() { return status; }

    // Setters (required for creating a new bid object)
    public void setAuctionTitle(String auctionTitle) { this.auctionTitle = auctionTitle; }
    public void setAuctionItemId(Long auctionItemId) { this.auctionItemId = auctionItemId; }
    public void setBidderId(Long bidderId) { this.bidderId = bidderId; }
    public void setBidAmount(BigDecimal bidAmount) { this.bidAmount = bidAmount; }
}
