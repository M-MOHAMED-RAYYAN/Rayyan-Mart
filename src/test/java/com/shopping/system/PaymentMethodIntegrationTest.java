package com.shopping.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.shopping.system.model.PaymentMethod;
import com.shopping.system.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentMethodIntegrationTest extends BaseIntegrationTest {

    @Test
    void orderDefaultsToCashOnDelivery() throws Exception {
        MockHttpSession session = registerAndLogin("pay1@example.com");
        Product product = createProduct("COD Item", new BigDecimal("5.00"), 10);
        addToCart(session, product.getId(), 1);

        MvcResult result = mockMvc.perform(post("/api/orders").session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentMethod").value("CASH_ON_DELIVERY"))
                .andReturn();

        JsonNode order = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(order.path("paymentMethod").asText())
                .isEqualTo(PaymentMethod.CASH_ON_DELIVERY.name());
    }

    @Test
    void orderAcceptsExplicitCashOnDelivery() throws Exception {
        MockHttpSession session = registerAndLogin("pay2@example.com");
        Product product = createProduct("COD Explicit", new BigDecimal("6.00"), 5);
        addToCart(session, product.getId(), 1);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CASH_ON_DELIVERY\"}")
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentMethod").value("CASH_ON_DELIVERY"));
    }

    @Test
    void invalidPaymentMethodIsRejected() throws Exception {
        MockHttpSession session = registerAndLogin("pay3@example.com");
        Product product = createProduct("Bad Pay", new BigDecimal("1.00"), 5);
        addToCart(session, product.getId(), 1);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\":\"CRYPTO\"}")
                        .session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void webCheckoutPlacesCashOnDeliveryOrder() throws Exception {
        MockHttpSession session = registerAndLogin("pay4@example.com");
        Product product = createProduct("Web COD", new BigDecimal("9.00"), 5);
        addToCart(session, product.getId(), 1);

        mockMvc.perform(post("/checkout/place")
                        .param("paymentMethod", "CASH_ON_DELIVERY")
                        .session(session))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/api/orders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].paymentMethod").value("CASH_ON_DELIVERY"));
    }
}