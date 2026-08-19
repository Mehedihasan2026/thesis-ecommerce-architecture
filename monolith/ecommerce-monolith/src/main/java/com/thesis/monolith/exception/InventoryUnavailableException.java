package com.thesis.monolith.exception;

public class InventoryUnavailableException extends RuntimeException {

    public InventoryUnavailableException(String message) {
        super(message);
    }
}
