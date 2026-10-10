package com.shopping.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.shopping.system.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void addProductToCart() throws Exception {
        MockHttpSession session = registerAndLogin("alice@example.com");
        Product product = productRepository.findAll().get(0);

        mockMvc.perform(post("/api/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.shopping.system.dto.CartRequest(product.getId(), 2)))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void updateCartQuantity() throws Exception {
        MockHttpSession session = registerAndLogin("bob@example.com");
        Product product = productRepository.findAll().get(0);
        MvcResult addResult = addAndReturn(session, product.getId(), 1);
        long itemId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("items").get(0).path("id").asLong();

        mockMvc.perform(put("/api/cart/item/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].subtotal").isNumber());
    }

    @Test
    void removeCartItem() throws Exception {
        MockHttpSession session = registerAndLogin("carol@example.com");
        Product product = productRepository.findAll().get(0);
        MvcResult addResult = addAndReturn(session, product.getId(), 1);
        long itemId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("items").get(0).path("id").asLong();

        mockMvc.perform(delete("/api/cart/item/" + itemId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void invalidQuantityIsRejected() throws Exception {
        MockHttpSession session = registerAndLogin("dave@example.com");
        Product product = productRepository.findAll().get(0);

        mockMvc.perform(post("/api/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + product.getId() + ",\"quantity\":0}")
                        .session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void quantityGreaterThanStockIsRejected() throws Exception {
        MockHttpSession session = registerAndLogin("erin@example.com");
        Product product = productRepository.findAll().get(0);
        int overStock = product.getQuantity() + 1;

        mockMvc.perform(post("/api/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + product.getId() + ",\"quantity\":" + overStock + "}")
                        .session(session))
                .andExpect(status().isConflict());
    }

    @Test
    void usersCannotAccessAnotherUsersCart() throws Exception {
        MockHttpSession alice = registerAndLogin("frank@example.com");
        MockHttpSession bob = registerAndLogin("gina@example.com");
        Product product = productRepository.findAll().get(0);

        MvcResult addResult = addAndReturn(alice, product.getId(), 1);
        long itemId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("items").get(0).path("id").asLong();

        mockMvc.perform(put("/api/cart/item/" + itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":5}")
                        .session(bob))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/cart/item/" + itemId).session(bob))
                .andExpect(status().isNotFound());
    }

    @Test
    void clearCart() throws Exception {
        MockHttpSession session = registerAndLogin("henry@example.com");
        Product product = productRepository.findAll().get(0);
        addAndReturn(session, product.getId(), 1);

        mockMvc.perform(delete("/api/cart/clear").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    private MvcResult addAndReturn(MockHttpSession session, Long productId, Integer quantity) throws Exception {
        return mockMvc.perform(post("/api/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.shopping.system.dto.CartRequest(productId, quantity)))
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
    }
}
