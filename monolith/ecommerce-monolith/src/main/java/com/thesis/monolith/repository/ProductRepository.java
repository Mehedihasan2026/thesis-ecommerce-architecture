package com.thesis.monolith.repository;

import com.thesis.monolith.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
