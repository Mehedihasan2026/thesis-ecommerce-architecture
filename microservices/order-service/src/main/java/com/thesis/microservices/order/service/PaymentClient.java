package com.thesis.microservices.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(RestClient.Builder restClientBuilder, @Value("${services.payment.base-url}") String paymentBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(paymentBaseUrl).build();
    }

    public PaymentResponse processPayment(PaymentRequest request) {
        try {
            return restClient.post()
                    .uri("/api/payments/process")
                    .body(request)
                    .retrieve()
                    .body(PaymentResponse.class);
        } catch (HttpClientErrorException exception) {
            HttpStatusCode statusCode = exception.getStatusCode();
            if (statusCode.value() == 400) {
                throw new BadRequestException("Invalid payment request");
            }
            if (statusCode.value() == 502) {
                throw new PaymentFailedException("Payment simulation triggered");
            }
            throw new RemoteServiceException("Payment service returned an error", exception);
        } catch (RestClientException exception) {
            throw new RemoteServiceException("Payment service is unavailable", exception);
        }
    }
}
