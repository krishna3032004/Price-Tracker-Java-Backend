package com.pricetracker.repository;

import com.pricetracker.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB Repository
 * JS ke Product.findOne(), Product.find() ka equivalent
 */
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    // Product.findOne({ productLink: url }) ka equivalent
    Optional<Product> findByProductLink(String productLink);

    // Search by title - JS ke regex search ka equivalent
    // words ke saath AND condition
    @Query("{ 'title': { $regex: ?0, $options: 'i' } }")
    List<Product> findByTitleRegex(String keyword);

    // Multiple keywords se search (JS ke $and + regex ka equivalent)
    @Query("{ $and: ?0 }")
    List<Product> findByMultipleKeywords(List<Object> conditions);
}
