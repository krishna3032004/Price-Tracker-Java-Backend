# Price Tracker — Spring Boot Backend

A production-ready REST API backend for tracking Amazon and Flipkart product prices, built with Java 17 and Spring Boot 3.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3 |
| Primary Database | MongoDB Atlas |
| Price History DB | PostgreSQL (Neon) |
| Email | Spring Mail (Gmail SMTP) |
| Scheduler | Spring `@Scheduled` (cron) |
| Build Tool | Maven |

---

## Getting Started

**Requirements:** Java 17+, Maven 3.6+

```bash
# Step 1 — Build the project
mvn clean install

# Step 2 — Start the server
mvn spring-boot:run
```

Server starts at **http://localhost:8080**

All database credentials, email config, and external API URLs are already set in `src/main/resources/application.properties` — no `.env` setup needed.

---

## API Endpoints

### `GET /api/getProductByLink?url=<product_url>`
Fetches product details for an Amazon or Flipkart link.

- If the product already exists in MongoDB, it returns the cached data instantly.
- If it's a new product, it calls the external scraper server, saves the result to MongoDB with an initial price history entry, and returns the data.
- Automatically normalizes Amazon URLs to `amazon.in/dp/ASIN` format and Flipkart URLs to `pid`-only format to avoid duplicate entries.
- Follows redirects (short links, affiliate links, etc.) before processing.

**Example:**
```
GET /api/getProductByLink?url=https://www.amazon.in/dp/B0CHX3QBCH
```

---

### `GET /api/search?q=<query>`
Searches for products in MongoDB by title.

- Supports multi-word queries — each word is matched independently using case-insensitive regex.
- Returns up to 20 matching products.

**Example:**
```
GET /api/search?q=samsung phone
```

---

### `GET /api/trending-products`
Returns the 10 most recently tracked products, sorted by when they were added.

---

### `POST /api/price-alert`
Subscribes a user to price drop notifications for a product.

- Checks if the email is already subscribed to avoid duplicates.
- Saves the subscriber under the product's `notify` array in MongoDB.
- Sends a confirmation email to the subscriber immediately after sign-up.

**Request Body:**
```json
{
  "productId": "64a1f2...",
  "name": "Rahul",
  "email": "rahul@gmail.com"
}
```

---

### `GET /api/update-prices`
Manually triggers a full price refresh for all products in the database.

This is the same logic that runs automatically every night via the cron job. Useful for testing or forcing an immediate update.

---

## Automatic Price Update (Cron Job)

Every night at **2:00 AM IST**, the backend automatically:

1. Fetches all products from MongoDB
2. Sends them to the scraper server in batches of 5
3. Compares the new price with the last recorded price
4. If the price changed — updates MongoDB, saves a new entry to PostgreSQL price history
5. If the price dropped — sends a price drop alert email to all subscribers of that product

No GitHub Actions, no external triggers needed. It runs as long as the Spring Boot server is running.

---

## Email Notifications

Two types of emails are sent automatically:

**Subscription Confirmation** — sent immediately when a user signs up for alerts on a product.

**Price Drop Alert** — sent to all subscribers when a product's price decreases during the nightly update. Includes old price, new price, and a direct link to the product.

Gmail SMTP is configured via `lamashopping6@gmail.com` using an app password.

---

## Databases

**MongoDB Atlas** — stores all product data including title, image, current price, MRP, lowest/highest/average prices, discount, rating, price history array, and the list of alert subscribers.

**PostgreSQL (Neon)** — stores a separate `price_history` table with `product_id`, `price`, and `date` for each price change. This is used for historical analysis and charts.

Both databases are already connected. Tables/collections are created automatically on first run.

---

## Project Structure

```
src/main/java/com/pricetracker/
│
├── PriceTrackerApplication.java        ← App entry point, enables scheduling
│
├── config/
│   └── AppConfig.java                  ← Jackson ObjectMapper configuration
│
├── controller/
│   └── ProductController.java          ← All 5 REST endpoints
│
├── dto/
│   └── Dtos.java                       ← Request/response data classes
│
├── model/
│   ├── Product.java                    ← MongoDB document (products collection)
│   └── PriceHistoryEntity.java         ← PostgreSQL JPA entity (price_history table)
│
├── repository/
│   ├── ProductRepository.java          ← MongoDB queries
│   └── PriceHistoryRepository.java     ← PostgreSQL queries
│
└── service/
    ├── ProductService.java             ← Core business logic (scrape, search, subscribe)
    ├── PriceUpdateService.java         ← Cron job + batch price update logic
    └── EmailService.java               ← Sends confirmation and price drop emails
```

---

## External Services (Unchanged)

The scraper server (`https://scrap-product-server.onrender.com`) and Python prediction server (`https://python-prediction-server.onrender.com`) are called from the backend as-is — no changes were made to them.