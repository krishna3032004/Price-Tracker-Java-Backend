package com.pricetracker.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "products")
public class Product {

    @Id
    private String id;

    @Indexed
    private String title;
    private String image;
    private Double currentPrice;
    private Double mrp;
    private Double lowest;
    private Double highest;
    private Double average;
    private Double discount;
    private Double rating;
    private String time;
    private String platform;

    @Indexed
    private String productLink;
    private String amazonLink;
    private List<NotifyUser> notify = new ArrayList<>();
    private List<PriceHistory> priceHistory = new ArrayList<>();
    private String predictionText;

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Double currentPrice) { this.currentPrice = currentPrice; }
    public Double getMrp() { return mrp; }
    public void setMrp(Double mrp) { this.mrp = mrp; }
    public Double getLowest() { return lowest; }
    public void setLowest(Double lowest) { this.lowest = lowest; }
    public Double getHighest() { return highest; }
    public void setHighest(Double highest) { this.highest = highest; }
    public Double getAverage() { return average; }
    public void setAverage(Double average) { this.average = average; }
    public Double getDiscount() { return discount; }
    public void setDiscount(Double discount) { this.discount = discount; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getProductLink() { return productLink; }
    public void setProductLink(String productLink) { this.productLink = productLink; }
    public String getAmazonLink() { return amazonLink; }
    public void setAmazonLink(String amazonLink) { this.amazonLink = amazonLink; }
    public List<NotifyUser> getNotify() { return notify; }
    public void setNotify(List<NotifyUser> notify) { this.notify = notify; }
    public List<PriceHistory> getPriceHistory() { return priceHistory; }
    public void setPriceHistory(List<PriceHistory> priceHistory) { this.priceHistory = priceHistory; }
    public String getPredictionText() { return predictionText; }
    public void setPredictionText(String predictionText) { this.predictionText = predictionText; }

    // ========================
    // Nested Classes
    // ========================
    public static class NotifyUser {
        private String name;
        private String email;

        public NotifyUser() {}
        public NotifyUser(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class PriceHistory {
        private Double price;
        private String date;

        public PriceHistory() {}
        public PriceHistory(Double price, String date) {
            this.price = price;
            this.date = date;
        }

        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
    }
}
