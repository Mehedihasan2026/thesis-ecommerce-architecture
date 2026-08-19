package com.thesis.microservices.payment.web;

import com.thesis.microservices.payment.service.PaymentProcessingService;
import com.thesis.microservices.payment.service.PaymentRequest;
import com.thesis.microservices.payment.service.PaymentResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentProcessingService paymentProcessingService;

    public PaymentController(PaymentProcessingService paymentProcessingService) {
        this.paymentProcessingService = paymentProcessingService;
    }

    @PostMapping("/process")
    public PaymentResponse process(@RequestBody PaymentRequest request) {
        return paymentProcessingService.processPayment(request);
    }
}
