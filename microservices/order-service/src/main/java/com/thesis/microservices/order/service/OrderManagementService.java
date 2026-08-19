package com.thesis.microservices.order.service;

import com.thesis.microservices.order.domain.CustomerOrder;
import com.thesis.microservices.order.domain.OrderItem;
import com.thesis.microservices.order.repository.CustomerOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderManagementService {

    private final CustomerOrderRepository customerOrderRepository;
    private final CartClient cartClient;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;

    public OrderManagementService(
            CustomerOrderRepository customerOrderRepository,
            CartClient cartClient,
            InventoryClient inventoryClient,
            PaymentClient paymentClient
    ) {
        this.customerOrderRepository = customerOrderRepository;
        this.cartClient = cartClient;
        this.inventoryClient = inventoryClient;
        this.paymentClient = paymentClient;
    }

    public OrderResponse checkout(
            String customerId,
            boolean simulatePaymentFailure,
            boolean simulateInventoryFailure,
            long artificialPaymentDelayMs
    ) {
        if (artificialPaymentDelayMs < 0) {
            throw new BadRequestException("artificialPaymentDelayMs must be zero or greater");
        }

        CartResponse cart = cartClient.getCart(customerId);
        if (cart.items() == null || cart.items().isEmpty()) {
            throw new BadRequestException("Cart is empty for customer: " + customerId);
        }

        List<ReservationLineItem> reservationItems = new ArrayList<>();
        for (CartItemResponse item : cart.items()) {
            InventoryResponse inventory = inventoryClient.getInventory(item.productId());
            if (inventory.availableQuantity() < item.quantity()) {
                throw new InventoryUnavailableException("Insufficient inventory for product: " + item.productId());
            }
            reservationItems.add(new ReservationLineItem(item.productId(), item.quantity()));
        }

        inventoryClient.reserve(new ReserveInventoryRequest(customerId, reservationItems), simulateInventoryFailure);

        PaymentResponse paymentResponse;
        try {
            paymentResponse = paymentClient.processPayment(new PaymentRequest(
                    customerId,
                    cart.totalAmount(),
                    simulatePaymentFailure,
                    artificialPaymentDelayMs
            ));
        } catch (PaymentFailedException exception) {
            inventoryClient.release(new ReleaseInventoryRequest(customerId, reservationItems));
            throw exception;
        }

        CustomerOrder order = new CustomerOrder(customerId, Instant.now(), cart.totalAmount(), "PLACED");
        for (CartItemResponse item : cart.items()) {
            order.addItem(new OrderItem(item.productId(), item.productName(), item.unitPrice(), item.quantity()));
        }
        CustomerOrder savedOrder = customerOrderRepository.save(order);
        cartClient.clearCart(customerId);

        return toResponse(savedOrder, paymentResponse);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders() {
        return customerOrderRepository.findAll().stream()
                .map(order -> toResponse(order, null))
                .toList();
    }

    private OrderResponse toResponse(CustomerOrder order, PaymentResponse paymentResponse) {
        List<OrderItemResponse> items = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            items.add(new OrderItemResponse(
                    item.getProductId(),
                    item.getProductName(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    lineTotal
            ));
        }

        Long paymentTransactionId = paymentResponse != null ? paymentResponse.transactionId() : null;
        String paymentStatus = paymentResponse != null ? paymentResponse.status() : null;

        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                paymentTransactionId,
                paymentStatus,
                items
        );
    }
}
