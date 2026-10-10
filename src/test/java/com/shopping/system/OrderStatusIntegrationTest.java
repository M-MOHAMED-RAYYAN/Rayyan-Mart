package com.shopping.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.shopping.system.model.OrderStatus;
import com.shopping.system.model.Product;
import com.shopping.system.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderStatusIntegrationTest extends BaseIntegrationTest {

    private Long createOrder(MockHttpSession session) throws Exception {
        Product product = createProduct("Status Widget", new BigDecimal("5.00"), 20);
        addToCart(session, product.getId(), 1);
        MvcResult result = mockMvc.perform(post("/api/orders").session(session))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    @Test
    void adminCanAdvanceOrderStatus() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        MockHttpSession customer = registerAndLogin("st-happy@example.com");
        Long orderId = createOrder(customer);

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}")
                        .session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}")
                        .session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}")
                        .session(admin))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}")
                        .session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        var persisted = orderRepository.findById(orderId).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void illegalTransitionIsRejected() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        MockHttpSession customer = registerAndLogin("st-illegal@example.com");
        Long orderId = createOrder(customer);

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DELIVERED\"}")
                        .session(admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deliveredOrCancelledAreTerminal() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        MockHttpSession customer = registerAndLogin("st-term@example.com");
        Long orderId = createOrder(customer);

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}")
                        .session(admin))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PENDING\"}")
                        .session(admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerCannotUpdateOrderStatus() throws Exception {
        MockHttpSession customer = registerAndLogin("st-customer@example.com");
        Long orderId = createOrder(customer);

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}")
                        .session(customer))
                .andExpect(status().isForbidden());
    }

    @Test
    void sellerCannotUpdateOrderStatus() throws Exception {
        createSellerAccount();
        MockHttpSession seller = new MockHttpSession();
        login(seller, "st-seller@test.com", PASSWORD);
        MockHttpSession customer = registerAndLogin("st-s2@example.com");
        Long orderId = createOrder(customer);

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}")
                        .session(seller))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingStatusReturnsBadRequest() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        MockHttpSession customer = registerAndLogin("st-missing@example.com");
        Long orderId = createOrder(customer);

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .session(admin))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/admin/orders/999999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}")
                        .session(admin))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerSeesOrderStatusInHistory() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        MockHttpSession customer = registerAndLogin("st-view@example.com");
        Long orderId = createOrder(customer);

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}")
                        .session(admin))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"PROCESSING\"}")
                        .session(admin))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SHIPPED\"}")
                        .session(admin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/orders/" + orderId).session(customer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));
    }

    private void createSellerAccount() {
        createUser("st-seller@test.com", com.shopping.system.model.Role.SELLER);
    }
}