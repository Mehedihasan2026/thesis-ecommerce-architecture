package com.thesis.microservices.order.service;

public class InventoryUnavailableException extends RuntimeException {

    public InventoryUnavailableException(String message) {
        super(message);
    }
}
