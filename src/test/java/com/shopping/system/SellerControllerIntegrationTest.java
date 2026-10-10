package com.shopping.system;

import com.shopping.system.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SellerControllerIntegrationTest extends BaseIntegrationTest {

    private MockHttpSession registerAndLoginAs(String email, String role) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Test " + role, "email", email,
                                        "password", PASSWORD, "role", role)))
                        .session(session))
                .andExpect(status().isCreated());
        login(session, email, PASSWORD);
        return session;
    }

    private MockHttpSession loginAsSeller(String email) throws Exception {
        createUser(email, Role.SELLER);
        MockHttpSession session = new MockHttpSession();
        login(session, email, PASSWORD);
        return session;
    }

    private String createSellerProduct(MockHttpSession session, String name, double price, int quantity) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("name", name, "description", "desc " + name,
                        "price", price, "quantity", quantity,
                        "imageUrl", "https://placehold.co/400x300"));
        var result = mockMvc.perform(post("/api/seller/products")
                        .contentType(MediaType.APPLICATION_JSON).content(body).session(session))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private Long extractId(String json) throws Exception {
        return ((Number) objectMapper.readValue(json, Map.class).get("id")).longValue();
    }

    @Test
    void sellerRoleExistsAndRegistrationWorks() throws Exception {
        MockHttpSession session = registerAndLoginAs("newseller@test.com", "SELLER");

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SELLER"));
    }

    @Test
    void adminRegistrationIsRejected() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Evil", "email", "evil@test.com",
                                        "password", PASSWORD, "role", "ADMIN")))
                        .session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sellerCanAccessDashboard() throws Exception {
        MockHttpSession seller = loginAsSeller("dash@test.com");

        mockMvc.perform(get("/api/seller/dashboard").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sellerName").isNotEmpty())
                .andExpect(jsonPath("$.totalProducts").isNumber())
                .andExpect(jsonPath("$.availableProducts").isNumber())
                .andExpect(jsonPath("$.outOfStockProducts").isNumber())
                .andExpect(jsonPath("$.totalStock").isNumber())
                .andExpect(jsonPath("$.recentProducts").isArray());
    }

    @Test
    void customerCannotAccessSellerDashboard() throws Exception {
        MockHttpSession customer = registerAndLogin("cust1@test.com");

        mockMvc.perform(get("/api/seller/dashboard").session(customer))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedSellerAccessIs401() throws Exception {
        mockMvc.perform(get("/api/seller/dashboard"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/seller/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sellerCanCreateProductWithOwnership() throws Exception {
        MockHttpSession seller = loginAsSeller("owner@test.com");

        String json = createSellerProduct(seller, "Seller Gadget", 49.99, 20);

        Map<?, ?> created = objectMapper.readValue(json, Map.class);
        assert created.get("sellerId") != null;
    }

    @Test
    void sellerSeesOnlyOwnProducts() throws Exception {
        MockHttpSession sellerA = loginAsSeller("a@test.com");
        MockHttpSession sellerB = loginAsSeller("b@test.com");

        createSellerProduct(sellerA, "A Product", 10.00, 5);
        createSellerProduct(sellerB, "B Product", 20.00, 3);

        mockMvc.perform(get("/api/seller/products").session(sellerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("A Product"));

        mockMvc.perform(get("/api/seller/products").session(sellerB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("B Product"));
    }

    @Test
    void sellerCannotUpdateAnotherSellersProduct() throws Exception {
        MockHttpSession sellerA = loginAsSeller("upd-a@test.com");
        MockHttpSession sellerB = loginAsSeller("upd-b@test.com");

        Long productId = extractId(createSellerProduct(sellerA, "Owned Item", 15.00, 4));

        mockMvc.perform(put("/api/seller/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hijacked\",\"price\":1.00,\"quantity\":1}")
                        .session(sellerB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("permission")));
    }

    @Test
    void sellerCanUpdateOwnProduct() throws Exception {
        MockHttpSession seller = loginAsSeller("selfupd@test.com");
        Long productId = extractId(createSellerProduct(seller, "Before", 10.00, 5));

        mockMvc.perform(put("/api/seller/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"After\",\"description\":\"new\",\"price\":12.50,\"quantity\":7}")
                        .session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("After"))
                .andExpect(jsonPath("$.price").value(12.50));
    }

    @Test
    void sellerCannotDeleteAnotherSellersProduct() throws Exception {
        MockHttpSession sellerA = loginAsSeller("del-a@test.com");
        MockHttpSession sellerB = loginAsSeller("del-b@test.com");

        Long productId = extractId(createSellerProduct(sellerA, "Keep Me", 9.99, 2));

        mockMvc.perform(delete("/api/seller/products/" + productId).session(sellerB))
                .andExpect(status().isForbidden());
    }

    @Test
    void sellerCanDeleteOwnProduct() throws Exception {
        MockHttpSession seller = loginAsSeller("selfdel@test.com");
        Long productId = extractId(createSellerProduct(seller, "Temp Item", 5.00, 1));

        mockMvc.perform(delete("/api/seller/products/" + productId).session(seller))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/seller/products").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void sellerCanUpdateStockAndAvailability() throws Exception {
        MockHttpSession seller = loginAsSeller("stock@test.com");
        Long productId = extractId(createSellerProduct(seller, "Stocked", 30.00, 10));

        mockMvc.perform(patch("/api/seller/products/" + productId + "/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":50}")
                        .session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(50))
                .andExpect(jsonPath("$.available").value(true));

        mockMvc.perform(patch("/api/seller/products/" + productId + "/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":0}")
                        .session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(0))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void negativeStockIsRejected() throws Exception {
        MockHttpSession seller = loginAsSeller("negstock@test.com");
        Long productId = extractId(createSellerProduct(seller, "Neg Test", 10.00, 5));

        mockMvc.perform(patch("/api/seller/products/" + productId + "/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":-3}")
                        .session(seller))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/seller/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad\",\"price\":5,\"quantity\":-1}")
                        .session(seller))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customerShoppingFlowStillWorks() throws Exception {
        MockHttpSession seller = loginAsSeller("shop-seller@test.com");
        Long productId = extractId(createSellerProduct(seller, "Shop Item", 25.00, 10));

        MockHttpSession customer = registerAndLogin("shopper@test.com");
        addToCart(customer, productId, 2);

        mockMvc.perform(post("/api/orders").session(customer))
                .andExpect(status().isCreated());
    }

    @Test
    void adminDashboardStillWorks() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        mockMvc.perform(get("/api/admin/dashboard").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").isNumber());
    }

    @Test
    void customerCannotCreateViaSellerApi() throws Exception {
        MockHttpSession customer = registerAndLogin("cust2@test.com");
        mockMvc.perform(post("/api/seller/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sneaky\",\"price\":1.00,\"quantity\":1}")
                        .session(customer))
                .andExpect(status().isForbidden());
    }
}
