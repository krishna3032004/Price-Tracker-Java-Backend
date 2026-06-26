package com.pricetracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricetracker.model.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PriceUpdateService {

    private static final Logger log = LoggerFactory.getLogger(PriceUpdateService.class);

    private final ProductService productService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    @Value("${scraper.api.url}")
    private String scraperApiUrl;

    private final HttpClient httpClient = HttpClient.newBuilder().build();

    public PriceUpdateService(ProductService productService,
                              EmailService emailService,
                              ObjectMapper objectMapper) {
        this.productService = productService;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    // Har din raat 2 baje IST (8:30 PM UTC)
    @Scheduled(cron = "0 30 20 * * *", zone = "UTC")
    public void scheduledPriceUpdate() {
        log.info("Scheduled price update starting...");
        updatePrices();
    }

    public void updatePrices() {
        log.info("Price update chalu hua...");
        List<Product> products = productService.getAllProducts();
        if (products.isEmpty()) {
            log.info("No products found.");
            return;
        }

        List<List<Product>> batches = partitionList(products, 5);
        for (List<Product> batch : batches) {
            try {
                processBatch(batch);
            } catch (Exception e) {
                log.error("Batch processing failed: {}", e.getMessage());
            }
        }
        log.info("Price update complete!");
    }

    private void processBatch(List<Product> batch) throws Exception {
        List<Map<String, String>> urls = batch.stream()
                .map(p -> Map.of("productLink", p.getProductLink()))
                .toList();

        String requestBody = objectMapper.writeValueAsString(Map.of("urls", urls));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(scraperApiUrl + "/api/scrape-prices"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Scraper API failed: {}", response.statusCode());
            return;
        }

        JsonNode jsonResponse = objectMapper.readTree(response.body());
        JsonNode results = jsonResponse.get("results");
        if (results == null || !results.isArray()) return;

        for (Product product : batch) {
            try {
                updateProductFromScrapedData(product, results);
            } catch (Exception e) {
                log.error("Failed to update {}: {}", product.getTitle(), e.getMessage());
            }
        }
    }

    private void updateProductFromScrapedData(Product product, JsonNode results) {
        Double lastPrice = null;
        if (!product.getPriceHistory().isEmpty()) {
            lastPrice = product.getPriceHistory().get(product.getPriceHistory().size() - 1).getPrice();
        }

        Double newPrice = null;
        for (JsonNode result : results) {
            JsonNode urlNode = result.get("url");
            if (urlNode != null && product.getProductLink().equals(urlNode.asText())) {
                JsonNode priceNode = result.get("price");
                if (priceNode != null && !priceNode.isNull()) {
                    newPrice = priceNode.asDouble();
                }
                break;
            }
        }

        if (newPrice == null) return;

        if (!newPrice.equals(lastPrice)) {
            Double oldPrice = lastPrice;
            productService.updateProductPrice(product, newPrice);

            if (oldPrice != null && newPrice < oldPrice && !product.getNotify().isEmpty()) {
                for (Product.NotifyUser user : product.getNotify()) {
                    try {
                        emailService.sendPriceDropAlert(
                                user.getEmail(), user.getName(),
                                product.getTitle(), oldPrice, newPrice,
                                product.getProductLink()
                        );
                    } catch (Exception e) {
                        log.error("Email failed for {}: {}", user.getEmail(), e.getMessage());
                    }
                }
            }
        }
    }

    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }
}
