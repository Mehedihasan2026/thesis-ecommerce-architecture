package com.thesis.microservices.inventory.config;

import com.thesis.microservices.inventory.domain.InventoryItem;
import com.thesis.microservices.inventory.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(InventoryRepository inventoryRepository) {
        return args -> {
            if (inventoryRepository.count() > 0) {
                return;
            }

            inventoryRepository.saveAll(List.of(
                    // Load tests create many short-lived carts and orders, so the seeded stock
                    // needs to be high enough to avoid inventory exhaustion skewing success scenarios.
                    new InventoryItem(1L, "SKU-1001", "Mechanical Keyboard", 1_000_000),
                    new InventoryItem(2L, "SKU-1002", "Wireless Mouse", 1_000_000),
                    new InventoryItem(3L, "SKU-1003", "USB-C Dock", 1_000_000)
            ));
        };
    }
}
