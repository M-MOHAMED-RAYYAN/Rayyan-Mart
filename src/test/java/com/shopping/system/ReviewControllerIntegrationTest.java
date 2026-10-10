package com.shopping.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.shopping.system.model.Product;
import com.shopping.system.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewControllerIntegrationTest extends BaseIntegrationTest {

    private Product createPurchasableProduct() {
        return createProduct("Reviewable Widget", new BigDecimal("8.00"), 30);
    }

    private Long placeOrderFor(MockHttpSession session, Long productId) throws Exception {
        addToCart(session, productId, 1);
        MvcResult result = mockMvc.perform(post("/api/orders").session(session))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    @Test
    void customerCanReviewPurchasedProduct() throws Exception {
        MockHttpSession session = registerAndLogin("reviewer@example.com");
        Product product = createPurchasableProduct();
        placeOrderFor(session, product.getId());

        mockMvc.perform(post("/api/products/" + product.getId() + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Excellent product!\"}")
                        .session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Excellent product!"))
                .andExpect(jsonPath("$.userName").isNotEmpty());
    }

    @Test
    void cannotReviewProductWithoutPurchase() throws Exception {
        MockHttpSession session = registerAndLogin("notbuyer@example.com");
        Product product = createPurchasableProduct();

        mockMvc.perform(post("/api/products/" + product.getId() + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"comment\":\"Never bought\"}")
                        .session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidRatingsAreRejected() throws Exception {
        MockHttpSession session = registerAndLogin("ratingcheck@example.com");
        Product product = createPurchasableProduct();
        placeOrderFor(session, product.getId());
        String url = "/api/products/" + product.getId() + "/reviews";

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":0,\"comment\":\"zero\"}")
                        .session(session))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":6,\"comment\":\"six\"}")
                        .session(session))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"missing rating\"}")
                        .session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateReviewIsRejected() throws Exception {
        MockHttpSession session = registerAndLogin("doublerev@example.com");
        Product product = createPurchasableProduct();
        placeOrderFor(session, product.getId());
        String body = "{\"rating\":3,\"comment\":\"Okay\"}";

        mockMvc.perform(post("/api/products/" + product.getId() + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON).content(body).session(session))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/products/" + product.getId() + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON).content(body).session(session))
                .andExpect(status().isConflict());
    }

    @Test
    void customerCanUpdateAndDeleteOwnReview() throws Exception {
        MockHttpSession session = registerAndLogin("ownreview@example.com");
        Product product = createPurchasableProduct();
        placeOrderFor(session, product.getId());

        MvcResult created = mockMvc.perform(post("/api/products/" + product.getId() + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":2,\"comment\":\"Meh\"}")
                        .session(session))
                .andExpect(status().isCreated())
                .andReturn();
        long reviewId = objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(put("/api/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"comment\":\"Actually good\"}")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Actually good"));

        mockMvc.perform(delete("/api/reviews/" + reviewId).session(session))
                .andExpect(status().isNoContent());
    }

    @Test
    void customerCannotModifyAnotherCustomersReview() throws Exception {
        MockHttpSession alice = registerAndLogin("rw-alice@example.com");
        MockHttpSession bob = registerAndLogin("rw-bob@example.com");
        Product product = createPurchasableProduct();
        placeOrderFor(alice, product.getId());

        MvcResult created = mockMvc.perform(post("/api/products/" + product.getId() + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Alice review\"}")
                        .session(alice))
                .andExpect(status().isCreated())
                .andReturn();
        long reviewId = objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(put("/api/reviews/" + reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":1,\"comment\":\"Hacked\"}")
                        .session(bob))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/reviews/" + reviewId).session(bob))
                .andExpect(status().isForbidden());
    }

    @Test
    void productRatingAggregatesCorrectly() throws Exception {
        MockHttpSession alice = registerAndLogin("agg-a@example.com");
        MockHttpSession bob = registerAndLogin("agg-b@example.com");
        Product product = createPurchasableProduct();
        placeOrderFor(alice, product.getId());
        placeOrderFor(bob, product.getId());

        mockMvc.perform(post("/api/products/" + product.getId() + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"A\"}")
                        .session(alice))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/products/" + product.getId() + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":3,\"comment\":\"B\"}")
                        .session(bob))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/products/" + product.getId()).session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.0))
                .andExpect(jsonPath("$.ratingCount").value(2))
                .andExpect(jsonPath("$.ratingDistribution", hasSize(5)));

        mockMvc.perform(get("/api/products/" + product.getId() + "/reviews").session(alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void adminCanModerateAnyReview() throws Exception {
        MockHttpSession alice = registerAndLogin("mod-a@example.com");
        MockHttpSession admin = loginAsAdmin();
        Product product = createPurchasableProduct();
        placeOrderFor(alice, product.getId());

        MvcResult created = mockMvc.perform(post("/api/products/" + product.getId() + "/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":1,\"comment\":\"Spam\"}")
                        .session(alice))
                .andExpect(status().isCreated())
                .andReturn();
        long reviewId = objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(get("/api/admin/reviews").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/admin/reviews/" + reviewId).session(admin))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/admin/reviews").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void sellerCannotAccessAdminReviewEndpoints() throws Exception {
        createUser("review-seller@test.com", Role.SELLER);
        MockHttpSession seller = new MockHttpSession();
        login(seller, "review-seller@test.com", PASSWORD);

        mockMvc.perform(get("/api/admin/reviews").session(seller))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedReviewAccessIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/products/1/reviews"))
                .andExpect(status().isUnauthorized());
    }
}