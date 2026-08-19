package com.thesis.microservices.payment.service;

import com.thesis.microservices.payment.domain.PaymentTransaction;
import com.thesis.microservices.payment.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@Transactional
public class PaymentProcessingService {

    private final PaymentTransactionRepository paymentTransactionRepository;

    public PaymentProcessingService(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        validate(request);

        if (request.artificialPaymentDelayMs() > 0) {
            sleep(request.artificialPaymentDelayMs());
        }

        if (request.simulatePaymentFailure()) {
            PaymentTransaction failedTransaction = paymentTransactionRepository.save(
                    new PaymentTransaction(request.customerId(), request.amount(), "FAILED", Instant.now())
            );
            throw new PaymentFailedException("Payment simulation triggered. Transaction id: " + failedTransaction.getId());
        }

        PaymentTransaction successfulTransaction = paymentTransactionRepository.save(
                new PaymentTransaction(request.customerId(), request.amount(), "SUCCESS", Instant.now())
        );

        return new PaymentResponse(
                successfulTransaction.getId(),
                successfulTransaction.getCustomerId(),
                successfulTransaction.getAmount(),
                successfulTransaction.getStatus(),
                successfulTransaction.getCreatedAt()
        );
    }

    private void validate(PaymentRequest request) {
        if (request.customerId() == null || request.customerId().isBlank()) {
            throw new BadRequestException("customerId is required");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("amount must be greater than zero");
        }
        if (request.artificialPaymentDelayMs() < 0) {
            throw new BadRequestException("artificialPaymentDelayMs must be zero or greater");
        }
    }

    private void sleep(long artificialPaymentDelayMs) {
        try {
            Thread.sleep(artificialPaymentDelayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PaymentFailedException("Payment delay interrupted");
        }
    }
}
