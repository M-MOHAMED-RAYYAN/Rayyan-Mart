package com.shopping.system;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void productRetrievalWorks() throws Exception {
        MockHttpSession session = registerAndLogin("browser@example.com");

        mockMvc.perform(get("/api/products").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$[0].name").isNotEmpty());
    }

    @Test
    void adminCanCreateProduct() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        String body = "{\"name\":\"New Gadget\",\"description\":\"A shiny gadget\","
                + "\"price\":99.99,\"quantity\":10,\"imageUrl\":\"https://placehold.co/400x300\",\"available\":true}";

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON).content(body).session(admin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("New Gadget"))
                .andExpect(jsonPath("$.price").value(99.99));
    }

    @Test
    void customerCannotCreateProduct() throws Exception {
        MockHttpSession customer = registerAndLogin("customer@example.com");
        String body = "{\"name\":\"Sneaky Product\",\"price\":1.00,\"quantity\":1}";

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON).content(body).session(customer))
                .andExpect(status().isForbidden());
    }

    @Test
    void productValidationRejectsInvalidData() throws Exception {
        MockHttpSession admin = loginAsAdmin();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-5,\"quantity\":1}").session(admin))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Valid Name\",\"price\":10,\"quantity\":-1}").session(admin))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanUpdateAndDeleteProduct() throws Exception {
        MockHttpSession admin = loginAsAdmin();

        var productId = createProduct("Editable Product", new java.math.BigDecimal("5.00"), 7).getId();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Edited Product\",\"price\":6.50,\"quantity\":9}").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Edited Product"))
                .andExpect(jsonPath("$.price").value(6.50));

        mockMvc.perform(delete("/api/products/" + productId).session(admin))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/products").session(admin))
                .andExpect(jsonPath("$[?(@.id == " + productId + ")]").value(empty()));
    }
}
