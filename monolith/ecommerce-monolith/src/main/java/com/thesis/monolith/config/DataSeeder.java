package com.thesis.monolith.config;

import com.thesis.monolith.entity.Inventory;
import com.thesis.monolith.entity.Product;
import com.thesis.monolith.repository.InventoryRepository;
import com.thesis.monolith.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(ProductRepository productRepository, InventoryRepository inventoryRepository) {
        return args -> {
            if (productRepository.count() > 0) {
                return;
            }

            List<Product> products = productRepository.saveAll(List.of(
                    new Product("SKU-1001", "Mechanical Keyboard", "Compact keyboard for productivity", new BigDecimal("99.90")),
                    new Product("SKU-1002", "Wireless Mouse", "Ergonomic mouse for daily work", new BigDecimal("39.50")),
                    new Product("SKU-1003", "USB-C Dock", "Docking station for modern laptops", new BigDecimal("129.00"))
            ));

            inventoryRepository.saveAll(List.of(
                    // Load tests create many short-lived carts and orders, so the seeded stock
                    // needs to be high enough to avoid inventory exhaustion skewing success scenarios.
                    new Inventory(products.get(0), 1_000_000),
                    new Inventory(products.get(1), 1_000_000),
                    new Inventory(products.get(2), 1_000_000)
            ));
        };
    }
}
