package com.thesis.microservices.order.web;

import com.thesis.microservices.order.service.OrderManagementService;
import com.thesis.microservices.order.service.OrderResponse;
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

    private final OrderManagementService orderManagementService;

    public OrderController(OrderManagementService orderManagementService) {
        this.orderManagementService = orderManagementService;
    }

    @PostMapping("/checkout/{customerId}")
    public OrderResponse checkout(
            @PathVariable String customerId,
            @RequestParam(defaultValue = "false") boolean simulatePaymentFailure,
            @RequestParam(defaultValue = "false") boolean simulateInventoryFailure,
            @RequestParam(defaultValue = "0") long artificialPaymentDelayMs
    ) {
        return orderManagementService.checkout(
                customerId,
                simulatePaymentFailure,
                simulateInventoryFailure,
                artificialPaymentDelayMs
        );
    }

    @GetMapping
    public List<OrderResponse> getOrders() {
        return orderManagementService.getOrders();
    }
}
