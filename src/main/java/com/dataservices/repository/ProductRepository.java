package com.dataservices.repository;

import com.dataservices.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends MongoRepository<Product, String> {

    @Query("{'tenantId': ?0}")
    List<Product> findByTenantId(String tenantId);

    @Query("{'tenantId': ?0, '_id': ?1}")
    Optional<Product> findByTenantIdAndId(String tenantId, String id);
}