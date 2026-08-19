package com.thesis.microservices.inventory.service;

import com.thesis.microservices.inventory.domain.InventoryItem;
import com.thesis.microservices.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long productId) {
        InventoryItem inventoryItem = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new NotFoundException("Inventory not found for product: " + productId));
        return toResponse(inventoryItem);
    }

    public ReservationResponse reserve(ReserveInventoryRequest request, boolean simulateInventoryFailure) {
        validateItems(request.items());
        if (simulateInventoryFailure) {
            throw new InventoryUnavailableException("Inventory simulation triggered");
        }

        for (ReservationLineItem item : request.items()) {
            InventoryItem inventoryItem = inventoryRepository.findByProductId(item.productId())
                    .orElseThrow(() -> new NotFoundException("Inventory not found for product: " + item.productId()));
            if (inventoryItem.getAvailableQuantity() < item.quantity()) {
                throw new InventoryUnavailableException("Insufficient inventory for product: " + item.productId());
            }
        }

        for (ReservationLineItem item : request.items()) {
            InventoryItem inventoryItem = inventoryRepository.findByProductId(item.productId())
                    .orElseThrow(() -> new NotFoundException("Inventory not found for product: " + item.productId()));
            inventoryItem.decrease(item.quantity());
        }

        return new ReservationResponse("RESERVED", request.customerId(), request.items());
    }

    public ReservationResponse release(ReleaseInventoryRequest request) {
        validateItems(request.items());
        for (ReservationLineItem item : request.items()) {
            InventoryItem inventoryItem = inventoryRepository.findByProductId(item.productId())
                    .orElseThrow(() -> new NotFoundException("Inventory not found for product: " + item.productId()));
            inventoryItem.increase(item.quantity());
        }
        return new ReservationResponse("RELEASED", request.customerId(), request.items());
    }

    private void validateItems(List<ReservationLineItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BadRequestException("At least one inventory item is required");
        }
        boolean hasInvalidQuantity = items.stream().anyMatch(item -> item.productId() == null || item.quantity() == null || item.quantity() <= 0);
        if (hasInvalidQuantity) {
            throw new BadRequestException("Each inventory item must include productId and quantity greater than zero");
        }
    }

    private InventoryResponse toResponse(InventoryItem item) {
        return new InventoryResponse(item.getProductId(), item.getSku(), item.getProductName(), item.getAvailableQuantity());
    }
}
