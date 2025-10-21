package com.mycompany.bidverse.client.dto;

public class BidderDto {
    private Long bidderId;
    private String bidderName;
    private String bidderEmail;
    private String phno;
    private String address;
    private String bidderPassword;
    private Long id; // Included as placeholder for DTO completeness

    // Getters
    public Long getBidderId() { return bidderId; }
    public String getBidderName() { return bidderName; }
    public String getBidderEmail() { return bidderEmail; }
    public String getPhno() { return phno; }
    public String getAddress() { return address; }
    public Long getId() { return id; }

    // Setters (required for JSON mapping / profile updates)
    public void setBidderId(Long bidderId) { this.bidderId = bidderId; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }
    public void setBidderEmail(String bidderEmail) { this.bidderEmail = bidderEmail; }
    public void setPhno(String phno) { this.phno = phno; }
    public void setAddress(String address) { this.address = address; }
    public void setBidderPassword(String bidderPassword) { this.bidderPassword = bidderPassword; }
    public void setId(Long id) { this.id = id; }
}
