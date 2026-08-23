package org.example.civitaswebapp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Stripe redirects a member's phone browser here with no session at all, so these two routes
 * must stay reachable without authentication. Runs through the real {@code SecurityConfig}
 * filter chain, unlike {@code PaymentPageControllerTest}'s standalone setup which bypasses it.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PaymentPageSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void paymentSuccessIsReachableWithoutLogin() throws Exception {
        mockMvc.perform(get("/payment-success").param("transactionId", "5"))
                .andExpect(status().isOk());
    }

    @Test
    void paymentCancelIsReachableWithoutLogin() throws Exception {
        mockMvc.perform(get("/payment-cancel"))
                .andExpect(status().isOk());
    }
}
