package com.thesis.monolith.exception;

public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(String message) {
        super(message);
    }
}
