package com.mycompany.bidverse.client.dto;

public class ImagesDto {
    private Long imageId;
    private String filePath;
    private Long auctionItemId;

    public Long getImageId(){
        return imageId;
    }
    public void setImageId(Long imageId){
        this.imageId=imageId;
    }

    public String getFilePath(){
        return filePath;
    }
    public void setFilePath(String filePath){
        this.filePath=filePath;
    }

    public Long getAuctionItemId(){
        return auctionItemId;
    }
    public void setAuctionItemId(Long auctionItemId){
        this.auctionItemId=auctionItemId;
    }
}
