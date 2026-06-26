package com.pricetracker.dto;

/**
 * DTOs - Lombok hataya, manual getters/setters daale
 * (Lombok annotation processing issue fix)
 */
public class Dtos {

    // POST /api/price-alert ka request body
    public static class PriceAlertRequest {
        private String productId;
        private String name;
        private String email;

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    // Scraper server se aane wala response
    public static class ScraperResponse {
        private boolean success;
        private ScrapedProductData data;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public ScrapedProductData getData() { return data; }
        public void setData(ScrapedProductData data) { this.data = data; }
    }

    public static class ScrapedProductData {
        private String title;
        private String image;
        private Double currentPrice;
        private Double mrp;
        private Double lowest;
        private Double highest;
        private Double average;
        private Double discount;
        private Double rating;
        private String amazonLink;
        private String predictionText;

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
        public String getAmazonLink() { return amazonLink; }
        public void setAmazonLink(String amazonLink) { this.amazonLink = amazonLink; }
        public String getPredictionText() { return predictionText; }
        public void setPredictionText(String predictionText) { this.predictionText = predictionText; }
    }

    // Generic API response
    public static class ApiResponse {
        private String message;
        private String error;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }

        public static ApiResponse success(String message) {
            ApiResponse r = new ApiResponse();
            r.message = message;
            return r;
        }

        public static ApiResponse error(String error) {
            ApiResponse r = new ApiResponse();
            r.error = error;
            return r;
        }
    }
}
