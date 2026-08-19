package com.thesis.microservices.inventory.web;

import com.thesis.microservices.inventory.service.BadRequestException;
import com.thesis.microservices.inventory.service.InventoryUnavailableException;
import com.thesis.microservices.inventory.service.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InventoryUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleConflict(InventoryUnavailableException exception, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, String path) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        ));
    }
}

record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
