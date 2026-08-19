package com.thesis.microservices.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory_items")
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long productId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer availableQuantity;

    protected InventoryItem() {
    }

    public InventoryItem(Long productId, String sku, String productName, Integer availableQuantity) {
        this.productId = productId;
        this.sku = sku;
        this.productName = productName;
        this.availableQuantity = availableQuantity;
    }

    public Long getProductId() {
        return productId;
    }

    public String getSku() {
        return sku;
    }

    public String getProductName() {
        return productName;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void decrease(int quantity) {
        this.availableQuantity -= quantity;
    }

    public void increase(int quantity) {
        this.availableQuantity += quantity;
    }
}
