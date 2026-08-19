package com.thesis.microservices.cart.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CatalogClient {

    private final RestClient restClient;

    public CatalogClient(RestClient.Builder restClientBuilder, @Value("${services.catalog.base-url}") String catalogBaseUrl) {
        this.restClient = restClientBuilder.baseUrl(catalogBaseUrl).build();
    }

    public CatalogProductResponse getProduct(Long productId) {
        try {
            return restClient.get()
                    .uri("/api/products/{productId}", productId)
                    .retrieve()
                    .body(CatalogProductResponse.class);
        } catch (HttpClientErrorException exception) {
            HttpStatusCode statusCode = exception.getStatusCode();
            if (statusCode.value() == 404) {
                throw new NotFoundException("Product not found: " + productId);
            }
            throw new RemoteServiceException("Catalog service returned an error", exception);
        } catch (RestClientException exception) {
            throw new RemoteServiceException("Catalog service is unavailable", exception);
        }
    }
}
