package com.shopping.system;

import com.shopping.system.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductFilterIntegrationTest extends BaseIntegrationTest {

    private MockHttpSession loggedIn() throws Exception {
        return registerAndLogin("filter@example.com");
    }

    @Test
    void searchByProductName() throws Exception {
        MockHttpSession session = loggedIn();

        mockMvc.perform(get("/api/products").param("search", "Wireless").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Wireless Mouse"));
    }

    @Test
    void searchByDescription() throws Exception {
        MockHttpSession session = loggedIn();

        mockMvc.perform(get("/api/products").param("search", "RFID").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Leather Wallet"));
    }

    @Test
    void filterByCategory() throws Exception {
        MockHttpSession session = loggedIn();

        mockMvc.perform(get("/api/products").param("category", "Electronics").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Wireless Mouse"));
    }

    @Test
    void filterByPriceRange() throws Exception {
        MockHttpSession session = loggedIn();

        mockMvc.perform(get("/api/products")
                        .param("minPrice", "20")
                        .param("maxPrice", "30")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void filterByAvailability() throws Exception {
        MockHttpSession session = loggedIn();
        Product outOfStock = createProduct("Out Of Stock Thing", new BigDecimal("1.00"), 0);
        outOfStock.setAvailable(false);
        productRepository.save(outOfStock);

        mockMvc.perform(get("/api/products").param("available", "true").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + outOfStock.getId() + ")]").isEmpty());

        mockMvc.perform(get("/api/products").param("available", "false").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Out Of Stock Thing"));
    }

    @Test
    void sortByPriceAscending() throws Exception {
        MockHttpSession session = loggedIn();

        mockMvc.perform(get("/api/products").param("sort", "price_asc").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price").value(15.75));
    }

    @Test
    void sortByPriceDescending() throws Exception {
        MockHttpSession session = loggedIn();

        mockMvc.perform(get("/api/products").param("sort", "price_desc").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].price").value(45.00));
    }

    @Test
    void combinedSearchAndFilter() throws Exception {
        MockHttpSession session = loggedIn();
        createProduct("Cheap Laptop Bag", new BigDecimal("12.00"), 10);
        createProduct("Expensive Laptop", new BigDecimal("900.00"), 5);

        mockMvc.perform(get("/api/products")
                        .param("search", "laptop")
                        .param("maxPrice", "20")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Cheap Laptop Bag"));
    }

    @Test
    void categoriesEndpointReturnsDistinctCategories() throws Exception {
        MockHttpSession session = loggedIn();

        mockMvc.perform(get("/api/products/categories").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(5)));
    }

    private static org.hamcrest.Matcher<Integer> greaterThanOrEqualTo(int v) {
        return org.hamcrest.Matchers.greaterThanOrEqualTo(v);
    }
}