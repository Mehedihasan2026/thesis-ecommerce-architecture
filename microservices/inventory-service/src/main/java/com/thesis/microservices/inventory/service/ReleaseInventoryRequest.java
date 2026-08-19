package com.thesis.microservices.inventory.service;

import java.util.List;

public record ReleaseInventoryRequest(
        String customerId,
        List<ReservationLineItem> items
) {
}
