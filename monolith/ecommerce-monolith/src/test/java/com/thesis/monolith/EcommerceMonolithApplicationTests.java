package com.thesis.monolith;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EcommerceMonolithApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldListSeededProducts() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").isNotEmpty())
                .andExpect(jsonPath("$[0].availableQuantity").isNumber());
    }

    @Test
    void shouldAddItemToCart() throws Exception {
        long productId = firstProductId();

        mockMvc.perform(post("/api/carts/customer-1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d,
                                  "quantity": 2
                                }
                                """.formatted(productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("customer-1"))
                .andExpect(jsonPath("$.items[0].productId").value(productId))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void shouldCheckoutSuccessfully() throws Exception {
        long productId = firstProductId();
        addItemToCart("customer-2", productId, 2);

        mockMvc.perform(post("/api/orders/checkout/customer-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value("customer-2"))
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.paymentStatus").value("SUCCESS"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].customerId").value("customer-2"));

        mockMvc.perform(get("/api/carts/customer-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void shouldReturnBadGatewayOnPaymentFailure() throws Exception {
        long productId = firstProductId();
        addItemToCart("customer-3", productId, 1);

        mockMvc.perform(post("/api/orders/checkout/customer-3")
                        .param("simulatePaymentFailure", "true"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Payment simulation triggered")));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void shouldReturnConflictOnInventoryFailure() throws Exception {
        long productId = firstProductId();
        addItemToCart("customer-4", productId, 1);

        mockMvc.perform(post("/api/orders/checkout/customer-4")
                        .param("simulateInventoryFailure", "true"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Inventory simulation triggered")));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private long firstProductId() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode products = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(products).isNotEmpty();
        return products.get(0).get("id").asLong();
    }

    private void addItemToCart(String customerId, long productId, int quantity) throws Exception {
        mockMvc.perform(post("/api/carts/{customerId}/items", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d,
                                  "quantity": %d
                                }
                                """.formatted(productId, quantity)))
                .andExpect(status().isCreated());
    }
}
