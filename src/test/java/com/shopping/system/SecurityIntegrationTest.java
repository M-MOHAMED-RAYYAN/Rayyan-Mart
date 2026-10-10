package com.shopping.system;

import com.shopping.system.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends BaseIntegrationTest {

    @Test
    void sellerCannotAccessAdminDashboard() throws Exception {
        MockHttpSession seller = sellerSince("sec-seller-admin@test.com");

        mockMvc.perform(get("/api/admin/dashboard").session(seller))
                .andExpect(status().isForbidden());
    }

    @Test
    void sellerCannotAccessAdminUserManagement() throws Exception {
        MockHttpSession seller = sellerSince("sec-seller-users@test.com");

        mockMvc.perform(get("/api/admin/users").session(seller))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedAdminAccessIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedSellerAccessIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/seller/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedCartAccessIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanListUsers() throws Exception {
        MockHttpSession admin = loginAsAdmin();

        mockMvc.perform(get("/api/admin/users").session(admin))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$[?(@.email == 'admin@test.com')]").exists());
    }

    @Test
    void customerCannotUseAdminProductManagement() throws Exception {
        MockHttpSession customer = registerAndLogin("sec-cust-prod@example.com");

        mockMvc.perform(post("/api/products")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"price\":1,\"quantity\":1}")
                        .session(customer))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/products/1").session(customer))
                .andExpect(status().isForbidden());
    }

    private MockHttpSession sellerSince(String email) {
        createUser(email, Role.SELLER);
        MockHttpSession session = new MockHttpSession();
        try {
            login(session, email, PASSWORD);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return session;
    }
}