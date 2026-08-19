package com.thesis.microservices.catalog.config;

import com.thesis.microservices.catalog.domain.Product;
import com.thesis.microservices.catalog.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() > 0) {
                return;
            }

            productRepository.saveAll(List.of(
                    new Product("SKU-1001", "Mechanical Keyboard", "Compact keyboard for productivity", new BigDecimal("99.90")),
                    new Product("SKU-1002", "Wireless Mouse", "Ergonomic mouse for daily work", new BigDecimal("39.50")),
                    new Product("SKU-1003", "USB-C Dock", "Docking station for modern laptops", new BigDecimal("129.00"))
            ));
        };
    }
}
