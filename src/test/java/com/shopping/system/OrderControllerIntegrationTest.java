package com.shopping.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.shopping.system.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void placeOrderSuccessfully() throws Exception {
        MockHttpSession session = registerAndLogin("order1@example.com");
        Product product = createProduct("Widget", new BigDecimal("10.00"), 50);
        addToCart(session, product.getId(), 2);

        MvcResult result = mockMvc.perform(post("/api/orders").session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andReturn();

        JsonNode order = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(order.path("totalAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(order.path("items").get(0).path("productName").asText()).isEqualTo("Widget");
        assertThat(order.path("items").get(0).path("price").decimalValue()).isEqualByComparingTo(new BigDecimal("10.00"));
    }

    @Test
    void emptyCartOrderIsRejected() throws Exception {
        MockHttpSession session = registerAndLogin("order2@example.com");

        mockMvc.perform(post("/api/orders").session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void insufficientStockOrderIsRejected() throws Exception {
        MockHttpSession admin = loginAsAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Scarce Item\",\"price\":3.00,\"quantity\":5}")
                        .session(admin))
                .andExpect(status().isCreated())
                .andReturn();
        long productId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("id").asLong();

        MockHttpSession customer = registerAndLogin("order3@example.com");
        addToCart(customer, productId, 5);

        mockMvc.perform(put("/api/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Scarce Item\",\"price\":3.00,\"quantity\":3}")
                        .session(admin))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders").session(customer))
                .andExpect(status().isConflict());
    }

    @Test
    void stockDecreasesAfterSuccessfulOrder() throws Exception {
        MockHttpSession session = registerAndLogin("order4@example.com");
        Product product = createProduct("Stocked Item", new BigDecimal("2.00"), 40);
        addToCart(session, product.getId(), 5);

        mockMvc.perform(post("/api/orders").session(session))
                .andExpect(status().isCreated());

        Product after = productRepository.findById(product.getId()).orElseThrow();
        assertThat(after.getQuantity()).isEqualTo(35);
        assertThat(after.getAvailable()).isTrue();
    }

    @Test
    void cartIsClearedAfterSuccessfulOrder() throws Exception {
        MockHttpSession session = registerAndLogin("order5@example.com");
        Product product = createProduct("Clear Me", new BigDecimal("1.00"), 10);
        addToCart(session, product.getId(), 2);

        mockMvc.perform(post("/api/orders").session(session))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cart").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void orderHistoryWorks() throws Exception {
        MockHttpSession session = registerAndLogin("order6@example.com");
        Product product = createProduct("History Item", new BigDecimal("4.00"), 20);
        addToCart(session, product.getId(), 1);

        MvcResult placeResult = mockMvc.perform(post("/api/orders").session(session))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(placeResult.getResponse().getContentAsString())
                .path("id").asLong();

        mockMvc.perform(get("/api/orders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(orderId));
    }

    @Test
    void customerCannotAccessAnotherCustomersOrder() throws Exception {
        MockHttpSession alice = registerAndLogin("order7a@example.com");
        MockHttpSession bob = registerAndLogin("order7b@example.com");
        Product product = createProduct("Private Item", new BigDecimal("7.00"), 10);
        addToCart(alice, product.getId(), 1);

        MvcResult placeResult = mockMvc.perform(post("/api/orders").session(alice))
                .andExpect(status().isCreated())
                .andReturn();
        long orderId = objectMapper.readTree(placeResult.getResponse().getContentAsString())
                .path("id").asLong();

        mockMvc.perform(get("/api/orders/" + orderId).session(bob))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanViewAllOrders() throws Exception {
        MockHttpSession alice = registerAndLogin("order8a@example.com");
        MockHttpSession bob = registerAndLogin("order8b@example.com");
        Product product = createProduct("Shared Item", new BigDecimal("5.00"), 100);

        addToCart(alice, product.getId(), 1);
        mockMvc.perform(post("/api/orders").session(alice)).andExpect(status().isCreated());

        addToCart(bob, product.getId(), 2);
        mockMvc.perform(post("/api/orders").session(bob)).andExpect(status().isCreated());

        MockHttpSession admin = loginAsAdmin();
        mockMvc.perform(get("/api/admin/orders").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void customerCannotAccessAdminEndpoints() throws Exception {
        MockHttpSession customer = registerAndLogin("order9@example.com");

        mockMvc.perform(get("/api/admin/orders").session(customer))
                .andExpect(status().isForbidden());
    }
}
