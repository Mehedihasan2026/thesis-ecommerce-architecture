package com.thesis.monolith.service;

import com.thesis.monolith.dto.AddCartItemRequest;
import com.thesis.monolith.dto.CartItemResponse;
import com.thesis.monolith.dto.CartResponse;
import com.thesis.monolith.entity.Cart;
import com.thesis.monolith.entity.CartItem;
import com.thesis.monolith.entity.Product;
import com.thesis.monolith.exception.BadRequestException;
import com.thesis.monolith.exception.NotFoundException;
import com.thesis.monolith.repository.CartRepository;
import com.thesis.monolith.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

@Service
@Transactional
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public CartResponse addItem(String customerId, AddCartItemRequest request) {
        validateAddRequest(request);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found: " + request.getProductId()));

        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseGet(() -> new Cart(customerId));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProductId().equals(product.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().increaseQuantity(request.getQuantity());
        } else {
            cart.addItem(new CartItem(product.getId(), product.getName(), product.getPrice(), request.getQuantity()));
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
        cartRepository.delete(cart);
    }

    @Transactional(readOnly = true)
    public Cart getCartEntity(String customerId) {
        return cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new NotFoundException("Cart not found for customer: " + customerId));
    }

    public void save(Cart cart) {
        cartRepository.save(cart);
    }

    private void validateAddRequest(AddCartItemRequest request) {
        if (request.getProductId() == null) {
            throw new BadRequestException("productId is required");
        }
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("quantity must be greater than zero");
        }
    }

    public CartResponse toResponse(Cart cart) {
        var items = new ArrayList<CartItemResponse>();
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getItems()) {
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            items.add(new CartItemResponse(
                    item.getProductId(),
                    item.getProductName(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    lineTotal
            ));
            total = total.add(lineTotal);
        }
        return new CartResponse(cart.getCustomerId(), items, total);
    }
}
