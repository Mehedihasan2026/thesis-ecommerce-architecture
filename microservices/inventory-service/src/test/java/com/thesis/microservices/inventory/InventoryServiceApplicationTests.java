package com.thesis.microservices.inventory;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InventoryServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldGetInventoryForProduct() throws Exception {
        mockMvc.perform(get("/api/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.availableQuantity").value(25));
    }

    @Test
    void shouldReserveInventory() throws Exception {
        mockMvc.perform(post("/api/inventory/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "customer-1",
                                  "items": [
                                    {
                                      "productId": 1,
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESERVED"));

        mockMvc.perform(get("/api/inventory/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableQuantity").value(23));
    }

    @Test
    void shouldReturnConflictWhenSimulationIsEnabled() throws Exception {
        mockMvc.perform(post("/api/inventory/reserve")
                        .param("simulateInventoryFailure", "true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "customer-2",
                                  "items": [
                                    {
                                      "productId": 1,
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Inventory simulation triggered"));
    }
}
