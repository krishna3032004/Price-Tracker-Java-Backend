package com.pricetracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricetracker.dto.Dtos;
import com.pricetracker.model.Product;
import com.pricetracker.model.PriceHistoryEntity;
import com.pricetracker.repository.PriceHistoryRepository;
import com.pricetracker.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    @Value("${scraper.api.url}")
    private String scraperApiUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .build();

    public ProductService(ProductRepository productRepository,
                          PriceHistoryRepository priceHistoryRepository,
                          MongoTemplate mongoTemplate,
                          ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.mongoTemplate = mongoTemplate;
        this.objectMapper = objectMapper;
    }

    // GET /api/getProductByLink
    public Product getOrScrapeProduct(String rawUrl) throws Exception {
        String resolvedUrl = resolveUrl(rawUrl);

        String normalizedUrl;
        if (resolvedUrl.contains("amazon")) {
            normalizedUrl = normalizeAmazonURL(resolvedUrl);
        } else if (resolvedUrl.contains("flipkart.com")) {
            normalizedUrl = normalizeFlipkartURL(resolvedUrl);
        } else {
            throw new IllegalArgumentException("Only Amazon and Flipkart product links are supported.");
        }

        log.info("Normalized URL: {}", normalizedUrl);

        Optional<Product> existing = productRepository.findByProductLink(normalizedUrl);
        if (existing.isPresent()) {
            log.info("Product found in DB");
            return existing.get();
        }

        String scrapeUrl = scraperApiUrl + "/scrape?url=" + URLEncoder.encode(normalizedUrl, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(scrapeUrl))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to scrape product. Status: " + response.statusCode());
        }

        Dtos.ScraperResponse scraperResponse = objectMapper.readValue(response.body(), Dtos.ScraperResponse.class);

        if (!scraperResponse.isSuccess() || scraperResponse.getData() == null) {
            throw new RuntimeException("Failed to fetch product details. Please check the URL.");
        }

        Dtos.ScrapedProductData data = scraperResponse.getData();
        String istTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy, hh:mm:ss a"));

        Product newProduct = new Product();
        newProduct.setTitle(data.getTitle());
        newProduct.setImage(data.getImage());
        newProduct.setCurrentPrice(data.getCurrentPrice());
        newProduct.setMrp(data.getMrp());
        newProduct.setLowest(data.getLowest());
        newProduct.setHighest(data.getHighest());
        newProduct.setAverage(data.getAverage());
        newProduct.setDiscount(data.getDiscount());
        newProduct.setRating(data.getRating());
        newProduct.setAmazonLink(data.getAmazonLink());
        newProduct.setPredictionText(data.getPredictionText());
        newProduct.setProductLink(normalizedUrl);
        newProduct.setTime(istTime);
        newProduct.setPlatform(normalizedUrl.contains("amazon") ? "amazon" : "flipkart");

        Product.PriceHistory initialHistory = new Product.PriceHistory(
                data.getCurrentPrice(),
                LocalDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDate().toString()
        );
        newProduct.getPriceHistory().add(initialHistory);

        return productRepository.save(newProduct);
    }

    // GET /api/search
    public List<Product> searchProducts(String query) {
        if (query == null || query.isBlank()) return List.of();

        String[] words = query.trim().split("\\s+");
        List<Criteria> criteriaList = new ArrayList<>();
        for (String word : words) {
            criteriaList.add(Criteria.where("title").regex(word, "i"));
        }

        Query mongoQuery = new Query();
        mongoQuery.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        mongoQuery.limit(20);

        return mongoTemplate.find(mongoQuery, Product.class);
    }

    // GET /api/trending-products
    public List<Product> getTrendingProducts() {
        Query query = new Query();
        query.with(Sort.by(Sort.Direction.DESC, "time"));
        query.limit(10);
        return mongoTemplate.find(query, Product.class);
    }

    // POST /api/price-alert
    public String subscribeForAlert(String productId, String name, String email) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sorry, the product you're looking for doesn't exist."));

        boolean alreadySubscribed = product.getNotify().stream()
                .anyMatch(n -> n.getEmail().equalsIgnoreCase(email));

        if (alreadySubscribed) {
            return "You've already subscribed! We'll notify you when price drops.";
        }

        product.getNotify().add(new Product.NotifyUser(name, email));
        productRepository.save(product);
        return "subscribed";
    }

    public Product getProductById(String id) {
        return productRepository.findById(id).orElse(null);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public void updateProductPrice(Product product, Double newPrice) {
        Double lastPrice = null;
        if (!product.getPriceHistory().isEmpty()) {
            lastPrice = product.getPriceHistory().get(product.getPriceHistory().size() - 1).getPrice();
        }

        if (newPrice != null && !newPrice.equals(lastPrice)) {
            String today = LocalDateTime.now(ZoneId.of("Asia/Kolkata")).toLocalDate().toString();
            product.getPriceHistory().add(new Product.PriceHistory(newPrice, today));

            try {
                priceHistoryRepository.save(new PriceHistoryEntity(product.getId(), newPrice));
                log.info("PostgreSQL mein price save hua: {}", product.getTitle());
            } catch (Exception e) {
                log.error("PostgreSQL error: {}", e.getMessage());
            }

            product.setCurrentPrice(newPrice);
            if (product.getLowest() == null || newPrice < product.getLowest()) product.setLowest(newPrice);
            if (product.getHighest() == null || newPrice > product.getHighest()) product.setHighest(newPrice);

            productRepository.save(product);
            log.info("Price updated for: {}", product.getTitle());
        }
    }

    private String normalizeAmazonURL(String url) {
        Pattern pattern = Pattern.compile("/(dp|gp/product)/([A-Z0-9]{10})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return "https://www.amazon.in/dp/" + matcher.group(2);
        }
        return url;
    }

    private String normalizeFlipkartURL(String url) {
        try {
            URI uri = URI.create(url);
            String pid = null;
            String queryStr = uri.getQuery();
            if (queryStr != null) {
                for (String param : queryStr.split("&")) {
                    if (param.startsWith("pid=")) {
                        pid = param.substring(4);
                        break;
                    }
                }
            }
            String basePath = uri.getScheme() + "://" + uri.getHost() + uri.getPath();
            return pid != null ? basePath + "?pid=" + pid : basePath;
        } catch (Exception e) {
            return url;
        }
    }

    private String resolveUrl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.uri().toString();
        } catch (Exception e) {
            return url;
        }
    }
}
