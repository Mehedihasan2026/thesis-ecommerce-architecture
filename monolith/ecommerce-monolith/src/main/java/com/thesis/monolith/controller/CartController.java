package com.thesis.monolith.controller;

import com.thesis.monolith.dto.AddCartItemRequest;
import com.thesis.monolith.dto.CartResponse;
import com.thesis.monolith.service.CartService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/{customerId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(@PathVariable String customerId, @RequestBody AddCartItemRequest request) {
        return cartService.addItem(customerId, request);
    }

    @GetMapping("/{customerId}")
    public CartResponse getCart(@PathVariable String customerId) {
        return cartService.getCart(customerId);
    }

    @DeleteMapping("/{customerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@PathVariable String customerId) {
        cartService.clearCart(customerId);
    }
}
