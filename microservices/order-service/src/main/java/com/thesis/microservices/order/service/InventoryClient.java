package com.thesis.microservices.order.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class InventoryClient {

    private final RestClient restClient;

    public InventoryClient(RestClient.Builder restClientBuilder, @Value("${services.inventory.base-url}") String inventoryBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(inventoryBaseUrl).build();
    }

    public InventoryResponse getInventory(Long productId) {
        try {
            return restClient.get()
                    .uri("/api/inventory/{productId}", productId)
                    .retrieve()
                    .body(InventoryResponse.class);
        } catch (HttpClientErrorException exception) {
            HttpStatusCode statusCode = exception.getStatusCode();
            if (statusCode.value() == 404) {
                throw new NotFoundException("Inventory not found for product: " + productId);
            }
            throw new RemoteServiceException("Inventory service returned an error", exception);
        } catch (RestClientException exception) {
            throw new RemoteServiceException("Inventory service is unavailable", exception);
        }
    }

    public void reserve(ReserveInventoryRequest request, boolean simulateInventoryFailure) {
        try {
            restClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/api/inventory/reserve")
                            .queryParam("simulateInventoryFailure", simulateInventoryFailure)
                            .build())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException exception) {
            HttpStatusCode statusCode = exception.getStatusCode();
            if (statusCode.value() == 409) {
                throw new InventoryUnavailableException("Inventory reservation failed");
            }
            throw new RemoteServiceException("Inventory service returned an error", exception);
        } catch (RestClientException exception) {
            throw new RemoteServiceException("Inventory service is unavailable", exception);
        }
    }

    public void release(ReleaseInventoryRequest request) {
        try {
            restClient.post()
                    .uri("/api/inventory/release")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new RemoteServiceException("Inventory compensation failed", exception);
        }
    }
}
