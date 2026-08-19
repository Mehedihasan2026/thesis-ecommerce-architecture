package com.thesis.monolith.service;

import com.thesis.monolith.dto.ProductResponse;
import com.thesis.monolith.entity.Product;
import com.thesis.monolith.exception.NotFoundException;
import com.thesis.monolith.repository.InventoryRepository;
import com.thesis.monolith.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public ProductService(ProductRepository productRepository, InventoryRepository inventoryRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public List<ProductResponse> getProducts() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse getProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found: " + productId));
        return toResponse(product);
    }

    private ProductResponse toResponse(Product product) {
        Integer availableQuantity = inventoryRepository.findByProductId(product.getId())
                .map(inventory -> inventory.getAvailableQuantity())
                .orElse(0);
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                availableQuantity
        );
    }
}
