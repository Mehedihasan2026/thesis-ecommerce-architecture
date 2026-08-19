package com.thesis.microservices.catalog.repository;

import com.thesis.microservices.catalog.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
