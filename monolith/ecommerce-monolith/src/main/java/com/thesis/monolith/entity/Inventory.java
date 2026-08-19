package com.thesis.monolith.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private Integer availableQuantity;

    protected Inventory() {
    }

    public Inventory(Product product, Integer availableQuantity) {
        this.product = product;
        this.availableQuantity = availableQuantity;
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public void decrease(int quantity) {
        this.availableQuantity -= quantity;
    }
}
