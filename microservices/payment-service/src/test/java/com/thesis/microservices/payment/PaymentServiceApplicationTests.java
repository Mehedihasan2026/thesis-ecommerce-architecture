package com.thesis.microservices.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PaymentServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldProcessPaymentSuccessfully() throws Exception {
        mockMvc.perform(post("/api/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "customer-1",
                                  "amount": 99.90,
                                  "simulatePaymentFailure": false,
                                  "artificialPaymentDelayMs": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void shouldReturnBadGatewayWhenPaymentSimulationIsEnabled() throws Exception {
        mockMvc.perform(post("/api/payments/process")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "customer-2",
                                  "amount": 49.99,
                                  "simulatePaymentFailure": true,
                                  "artificialPaymentDelayMs": 0
                                }
                                """))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").exists());
    }
}
