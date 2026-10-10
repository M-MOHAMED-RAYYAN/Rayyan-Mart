package com.shopping.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.system.model.Role;
import com.shopping.system.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.util.Map;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SellerOrderViewIntegrationTest extends BaseIntegrationTest {

    private MockHttpSession sellerSession(String email) {
        createUser(email, Role.SELLER);
        MockHttpSession session = new MockHttpSession();
        try {
            login(session, email, PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return session;
    }

    private Long createSellerProduct(MockHttpSession session, String name, double price, int quantity) throws Exception {
        String body = objectMapper.writeValueAsString(
                Map.of("name", name, "description", "desc", "price", price, "quantity", quantity));
        var result = mockMvc.perform(post("/api/seller/products")
                        .contentType(MediaType.APPLICATION_JSON).content(body).session(session))
                .andExpect(status().isCreated())
                .andReturn();
        return ((Number) new ObjectMapper().readValue(result.getResponse().getContentAsString(), Map.class).get("id")).longValue();
    }

    @Test
    void sellerSeesOrdersContainingTheirProducts() throws Exception {
        MockHttpSession sellerA = sellerSession("orders-a@test.com");
        MockHttpSession sellerB = sellerSession("orders-b@test.com");
        Long productA = createSellerProduct(sellerA, "Seller A Widget", 10.00, 20);
        createSellerProduct(sellerB, "Seller B Widget", 15.00, 20);

        MockHttpSession customer = registerAndLogin("order-buyer@example.com");
        addToCart(customer, productA, 2);
        mockMvc.perform(post("/api/orders").session(customer)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/seller/orders").session(sellerA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].items", hasSize(1)))
                .andExpect(jsonPath("$[0].items[0].productName").value("Seller A Widget"));

        mockMvc.perform(get("/api/seller/orders").session(sellerB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void sellerOrderViewDoesNotExposeCustomerIdentity() throws Exception {
        MockHttpSession seller = sellerSession("orders-priv@test.com");
        Long productId = createSellerProduct(seller, "Private Product", 9.00, 10);

        MockHttpSession customer = registerAndLogin("private-buyer@example.com");
        addToCart(customer, productId, 1);
        mockMvc.perform(post("/api/orders").session(customer)).andExpect(status().isCreated());

        mockMvc.perform(get("/api/seller/orders").session(seller))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerName").doesNotExist())
                .andExpect(jsonPath("$[0].userEmail").doesNotExist());
    }

    @Test
    void customerCannotAccessSellerOrders() throws Exception {
        MockHttpSession customer = registerAndLogin("cust-no-seller@example.com");

        mockMvc.perform(get("/api/seller/orders").session(customer))
                .andExpect(status().isForbidden());
    }
}