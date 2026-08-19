package com.thesis.monolith.service;

import com.thesis.monolith.dto.OrderItemResponse;
import com.thesis.monolith.dto.OrderResponse;
import com.thesis.monolith.entity.Cart;
import com.thesis.monolith.entity.CartItem;
import com.thesis.monolith.entity.CustomerOrder;
import com.thesis.monolith.entity.Inventory;
import com.thesis.monolith.entity.OrderItem;
import com.thesis.monolith.entity.PaymentTransaction;
import com.thesis.monolith.exception.BadRequestException;
import com.thesis.monolith.exception.InventoryUnavailableException;
import com.thesis.monolith.exception.PaymentFailedException;
import com.thesis.monolith.repository.CustomerOrderRepository;
import com.thesis.monolith.repository.InventoryRepository;
import com.thesis.monolith.repository.PaymentTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class OrderService {

    private final CartService cartService;
    private final InventoryRepository inventoryRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public OrderService(
            CartService cartService,
            InventoryRepository inventoryRepository,
            CustomerOrderRepository customerOrderRepository,
            PaymentTransactionRepository paymentTransactionRepository
    ) {
        this.cartService = cartService;
        this.inventoryRepository = inventoryRepository;
        this.customerOrderRepository = customerOrderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
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

        Cart cart = cartService.getCartEntity(customerId);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cart is empty for customer: " + customerId);
        }

        if (simulateInventoryFailure) {
            throw new InventoryUnavailableException("Inventory simulation triggered for customer: " + customerId);
        }

        for (CartItem item : cart.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new InventoryUnavailableException("Inventory not found for product: " + item.getProductId()));
            if (inventory.getAvailableQuantity() < item.getQuantity()) {
                throw new InventoryUnavailableException("Insufficient inventory for product: " + item.getProductId());
            }
        }

        BigDecimal totalAmount = cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (artificialPaymentDelayMs > 0) {
            sleep(artificialPaymentDelayMs);
        }

        if (simulatePaymentFailure) {
            PaymentTransaction failedPayment = paymentTransactionRepository.save(
                    new PaymentTransaction(customerId, totalAmount, "FAILED", Instant.now())
            );
            throw new PaymentFailedException("Payment simulation triggered. Transaction id: " + failedPayment.getId());
        }

        for (CartItem item : cart.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProductId())
                    .orElseThrow(() -> new InventoryUnavailableException("Inventory not found for product: " + item.getProductId()));
            inventory.decrease(item.getQuantity());
        }

        PaymentTransaction paymentTransaction = paymentTransactionRepository.save(
                new PaymentTransaction(customerId, totalAmount, "SUCCESS", Instant.now())
        );

        CustomerOrder order = new CustomerOrder(customerId, Instant.now(), totalAmount, "PLACED");
        for (CartItem item : cart.getItems()) {
            order.addItem(new OrderItem(item.getProductId(), item.getProductName(), item.getUnitPrice(), item.getQuantity()));
        }
        CustomerOrder savedOrder = customerOrderRepository.save(order);

        cart.clearItems();
        cartService.save(cart);

        return toResponse(savedOrder, paymentTransaction);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrders() {
        return customerOrderRepository.findAll().stream()
                .map(order -> toResponse(order, null))
                .toList();
    }

    private OrderResponse toResponse(CustomerOrder order, PaymentTransaction paymentTransaction) {
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

        Long paymentId = paymentTransaction != null ? paymentTransaction.getId() : null;
        String paymentStatus = paymentTransaction != null ? paymentTransaction.getStatus() : null;
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                paymentId,
                paymentStatus,
                items
        );
    }

    private void sleep(long artificialPaymentDelayMs) {
        try {
            Thread.sleep(artificialPaymentDelayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PaymentFailedException("Payment delay interrupted");
        }
    }
}
