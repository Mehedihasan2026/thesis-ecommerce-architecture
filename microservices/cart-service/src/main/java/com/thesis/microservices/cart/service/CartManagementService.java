package com.thesis.microservices.cart.service;

import com.thesis.microservices.cart.domain.Cart;
import com.thesis.microservices.cart.domain.CartItem;
import com.thesis.microservices.cart.repository.CartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

@Service
@Transactional
public class CartManagementService {

    private final CartRepository cartRepository;
    private final CatalogClient catalogClient;

    public CartManagementService(CartRepository cartRepository, CatalogClient catalogClient) {
        this.cartRepository = cartRepository;
        this.catalogClient = catalogClient;
    }

    public CartResponse addItem(String customerId, AddCartItemRequest request) {
        validate(request);
        CatalogProductResponse product = catalogClient.getProduct(request.productId());

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> new Cart(customerId));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(request.productId()))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().increaseQuantity(request.quantity());
        } else {
            cart.addItem(new CartItem(product.id(), product.name(), product.price(), request.quantity()));
        }

        return toResponse(cartRepository.save(cart));
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(String customerId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new NotFoundException("Cart not found for customer: " + customerId));
        return toResponse(cart);
    }

    public void clearCart(String customerId) {
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new NotFoundException("Cart not found for customer: " + customerId));
        cart.clearItems();
        cartRepository.save(cart);
    }

    private void validate(AddCartItemRequest request) {
        if (request.productId() == null) {
            throw new BadRequestException("productId is required");
        }
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new BadRequestException("quantity must be greater than zero");
        }
    }

    private CartResponse toResponse(Cart cart) {
        var items = new ArrayList<CartItemResponse>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            items.add(new CartItemResponse(
                    item.getProductId(),
                    item.getProductName(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    lineTotal
            ));
            totalAmount = totalAmount.add(lineTotal);
        }
        return new CartResponse(cart.getCustomerId(), items, totalAmount);
    }
}
