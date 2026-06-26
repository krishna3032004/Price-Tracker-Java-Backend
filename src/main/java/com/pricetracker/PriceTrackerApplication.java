package com.pricetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling  // Cron jobs enable karta hai
public class PriceTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(PriceTrackerApplication.class, args);
    }
}
