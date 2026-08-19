package com.thesis.microservices.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CartClient {

    private final RestClient restClient;

    public CartClient(RestClient.Builder restClientBuilder, @Value("${services.cart.base-url}") String cartBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(cartBaseUrl).build();
    }

    public CartResponse getCart(String customerId) {
        try {
            return restClient.get()
                    .uri("/api/carts/{customerId}", customerId)
                    .retrieve()
                    .body(CartResponse.class);
        } catch (HttpClientErrorException exception) {
            HttpStatusCode statusCode = exception.getStatusCode();
            if (statusCode.value() == 404) {
                throw new NotFoundException("Cart not found for customer: " + customerId);
            }
            throw new RemoteServiceException("Cart service returned an error", exception);
        } catch (RestClientException exception) {
            throw new RemoteServiceException("Cart service is unavailable", exception);
        }
    }

    public void clearCart(String customerId) {
        try {
            restClient.delete()
                    .uri("/api/carts/{customerId}", customerId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new RemoteServiceException("Failed to clear customer cart", exception);
        }
    }
}
