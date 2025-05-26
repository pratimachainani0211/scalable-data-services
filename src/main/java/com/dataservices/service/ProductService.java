package com.dataservices.service;

import com.dataservices.model.Product;
import com.dataservices.repository.ProductRepository;
import com.dataservices.tenant.TenantContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Cacheable(value = "products", key = "#root.methodName + ':' + T(com.dataservices.tenant.TenantContext).getTenantId()")
    public List<Product> getAllProducts() {
        return productRepository.findByTenantId(TenantContext.getTenantId());
    }

    @Cacheable(value = "products", key = "'product:' + T(com.dataservices.tenant.TenantContext).getTenantId() + ':' + #id")
    public Optional<Product> getProductById(String id) {
        return productRepository.findByTenantIdAndId(TenantContext.getTenantId(), id);
    }

    public Product upsertProduct(String id, Product productDetails) {
        Product product = Product.builder()
                .id(id)
                .tenantId(TenantContext.getTenantId())
                .name(productDetails.getName())
                .description(productDetails.getDescription())
                .price(productDetails.getPrice())
                .build();
        return productRepository.save(product);
    }

    public boolean deleteProduct(String id) {
        return productRepository.findByTenantIdAndId(TenantContext.getTenantId(), id)
                .map(product -> {
                    productRepository.delete(product);
                    return true;
                }).orElse(false);
    }
}