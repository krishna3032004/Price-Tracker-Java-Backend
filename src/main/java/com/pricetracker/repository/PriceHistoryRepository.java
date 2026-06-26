package com.pricetracker.repository;

import com.pricetracker.model.PriceHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PostgreSQL Repository
 * JS ke pool.query(...) ka JPA equivalent
 */
@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistoryEntity, Long> {

    List<PriceHistoryEntity> findByProductIdOrderByDateDesc(String productId);
}
