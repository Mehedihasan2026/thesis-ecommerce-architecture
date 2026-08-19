package com.thesis.microservices.order;

import com.thesis.microservices.order.service.CartClient;
import com.thesis.microservices.order.service.CartItemResponse;
import com.thesis.microservices.order.service.CartResponse;
import com.thesis.microservices.order.service.InventoryClient;
import com.thesis.microservices.order.service.InventoryResponse;
import com.thesis.microservices.order.service.PaymentClient;
import com.thesis.microservices.order.service.PaymentFailedException;
import com.thesis.microservices.order.service.PaymentResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartClient cartClient;

    @MockitoBean
    private InventoryClient inventoryClient;

    @MockitoBean
    private PaymentClient paymentClient;

    @Test
    void shouldCheckoutSuccessfully() throws Exception {
        given(cartClient.getCart("customer-1")).willReturn(cartResponse());
        given(inventoryClient.getInventory(1L)).willReturn(new InventoryResponse(1L, "SKU-1001", "Mechanical Keyboard", 25));
        doNothing().when(inventoryClient).reserve(any(), eq(false));
        given(paymentClient.processPayment(any()))
                .willReturn(new PaymentResponse(10L, "customer-1", new BigDecimal("199.80"), "SUCCESS", Instant.now()));
        doNothing().when(cartClient).clearCart("customer-1");

        mockMvc.perform(post("/api/orders/checkout/customer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldReturnBadGatewayWhenPaymentFails() throws Exception {
        given(cartClient.getCart("customer-2")).willReturn(cartResponse());
        given(inventoryClient.getInventory(1L)).willReturn(new InventoryResponse(1L, "SKU-1001", "Mechanical Keyboard", 25));
        doNothing().when(inventoryClient).reserve(any(), eq(false));
        given(paymentClient.processPayment(any())).willThrow(new PaymentFailedException("Payment simulation triggered"));
        doNothing().when(inventoryClient).release(any());

        mockMvc.perform(post("/api/orders/checkout/customer-2")
                        .param("simulatePaymentFailure", "true"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Payment simulation triggered"));
    }

    private CartResponse cartResponse() {
        return new CartResponse(
                "customer-1",
                List.of(new CartItemResponse(1L, "Mechanical Keyboard", new BigDecimal("99.90"), 2, new BigDecimal("199.80"))),
                new BigDecimal("199.80")
        );
    }
}
