package com.pricetracker.controller;

import com.pricetracker.dto.Dtos;
import com.pricetracker.model.Product;
import com.pricetracker.service.EmailService;
import com.pricetracker.service.PriceUpdateService;
import com.pricetracker.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final PriceUpdateService priceUpdateService;
    private final EmailService emailService;

    public ProductController(ProductService productService,
                             PriceUpdateService priceUpdateService,
                             EmailService emailService) {
        this.productService = productService;
        this.priceUpdateService = priceUpdateService;
        this.emailService = emailService;
    }

    // GET /api/getProductByLink?url=...
    @GetMapping("/getProductByLink")
    public ResponseEntity<?> getProductByLink(@RequestParam("url") String url) {
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Dtos.ApiResponse.error("Product URL is required."));
        }
        try {
            Product product = productService.getOrScrapeProduct(url);
            return ResponseEntity.ok(product);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Dtos.ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error in getProductByLink: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Dtos.ApiResponse.error("Internal server error. Please try again later."));
        }
    }

    // GET /api/search?q=...
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam(value = "q", required = false) String query) {
        try {
            List<Product> results = productService.searchProducts(query);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Search error: {}", e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    // GET /api/trending-products
    @GetMapping("/trending-products")
    public ResponseEntity<List<Product>> getTrendingProducts() {
        try {
            List<Product> trending = productService.getTrendingProducts();
            return ResponseEntity.ok(trending);
        } catch (Exception e) {
            log.error("Trending products error: {}", e.getMessage());
            return ResponseEntity.ok(List.of());
        }
    }

    // POST /api/price-alert
    @PostMapping("/price-alert")
    public ResponseEntity<?> subscribePriceAlert(@RequestBody Dtos.PriceAlertRequest request) {
        if (request.getProductId() == null || request.getName() == null || request.getEmail() == null) {
            return ResponseEntity.badRequest().body(Dtos.ApiResponse.error("All fields are required"));
        }
        try {
            String result = productService.subscribeForAlert(
                    request.getProductId(),
                    request.getName(),
                    request.getEmail()
            );
            if ("subscribed".equals(result)) {
                Product product = productService.getProductById(request.getProductId());
                if (product != null) {
                    emailService.sendSubscriptionConfirmation(
                            request.getEmail(),
                            request.getName(),
                            product.getTitle()
                    );
                }
                return ResponseEntity.ok(
                        Dtos.ApiResponse.success("You're all set! We'll send you a notification as soon as this product price drops.")
                );
            } else {
                return ResponseEntity.ok(Dtos.ApiResponse.success(result));
            }
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("doesn't exist")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Dtos.ApiResponse.error(e.getMessage()));
            }
            log.error("Price alert error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Dtos.ApiResponse.error("Something went wrong"));
        }
    }

    // GET /api/update-prices
    @GetMapping("/update-prices")
    public ResponseEntity<?> updatePrices() {
        try {
            priceUpdateService.updatePrices();
            return ResponseEntity.ok(Map.of("message", "Price check done"));
        } catch (Exception e) {
            log.error("Update prices error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Dtos.ApiResponse.error("Failed to update prices"));
        }
    }
}
