package com.thesis.microservices.cart;

import com.thesis.microservices.cart.service.CatalogClient;
import com.thesis.microservices.cart.service.CatalogProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CartServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogClient catalogClient;

    @Test
    void shouldAddItemToCart() throws Exception {
        given(catalogClient.getProduct(1L))
                .willReturn(new CatalogProductResponse(1L, "SKU-1001", "Mechanical Keyboard", "Compact keyboard", new BigDecimal("99.90")));

        mockMvc.perform(post("/api/carts/customer-1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 1,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value("customer-1"))
                .andExpect(jsonPath("$.items[0].productName").value("Mechanical Keyboard"))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void shouldClearCart() throws Exception {
        given(catalogClient.getProduct(2L))
                .willReturn(new CatalogProductResponse(2L, "SKU-1002", "Wireless Mouse", "Ergonomic mouse", new BigDecimal("39.50")));

        mockMvc.perform(post("/api/carts/customer-2/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": 2,
                                  "quantity": 1
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/carts/customer-2"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/carts/customer-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }
}
