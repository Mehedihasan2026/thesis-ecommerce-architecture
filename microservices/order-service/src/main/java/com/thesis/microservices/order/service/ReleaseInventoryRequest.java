package com.thesis.microservices.order.service;

import java.util.List;

public record ReleaseInventoryRequest(
        String customerId,
        List<ReservationLineItem> items
) {
}
