package com.thesis.microservices.cart.web;

import com.thesis.microservices.cart.service.AddCartItemRequest;
import com.thesis.microservices.cart.service.CartManagementService;
import com.thesis.microservices.cart.service.CartResponse;
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

    private final CartManagementService cartManagementService;

    public CartController(CartManagementService cartManagementService) {
        this.cartManagementService = cartManagementService;
    }

    @PostMapping("/{customerId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(@PathVariable String customerId, @RequestBody AddCartItemRequest request) {
        return cartManagementService.addItem(customerId, request);
    }

    @GetMapping("/{customerId}")
    public CartResponse getCart(@PathVariable String customerId) {
        return cartManagementService.getCart(customerId);
    }

    @DeleteMapping("/{customerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(@PathVariable String customerId) {
        cartManagementService.clearCart(customerId);
    }
}
