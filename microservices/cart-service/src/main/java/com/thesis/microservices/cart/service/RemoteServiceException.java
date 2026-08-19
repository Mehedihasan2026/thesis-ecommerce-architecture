package com.thesis.microservices.cart.service;

public class RemoteServiceException extends RuntimeException {

    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
