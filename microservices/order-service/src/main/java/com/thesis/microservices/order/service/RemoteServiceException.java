package com.thesis.microservices.order.service;

public class RemoteServiceException extends RuntimeException {

    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
