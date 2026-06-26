package com.pricetracker.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_history")
public class PriceHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(nullable = false)
    private Double price;

    @Column(name = "date", nullable = false)
    private LocalDateTime date = LocalDateTime.now();

    public PriceHistoryEntity() {}
    public PriceHistoryEntity(String productId, Double price) {
        this.productId = productId;
        this.price = price;
        this.date = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
}
