package com.thesis.monolith.controller;

import com.thesis.monolith.dto.OrderResponse;
import com.thesis.monolith.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/checkout/{customerId}")
    public OrderResponse checkout(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "false") boolean simulatePaymentFailure,
            @RequestParam(defaultValue = "false") boolean simulateInventoryFailure,
            @RequestParam(defaultValue = "0") long artificialPaymentDelayMs
    ) {
        return orderService.checkout(
                customerId,
                simulatePaymentFailure,
                simulateInventoryFailure,
                artificialPaymentDelayMs
        );
    }

    @GetMapping
    public List<OrderResponse> getOrders() {
        return orderService.getOrders();
    }
}
