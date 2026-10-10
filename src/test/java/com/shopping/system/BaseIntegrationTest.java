package com.shopping.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.system.model.Cart;
import com.shopping.system.model.Product;
import com.shopping.system.model.Role;
import com.shopping.system.model.User;
import com.shopping.system.repository.CartItemRepository;
import com.shopping.system.repository.CartRepository;
import com.shopping.system.repository.OrderRepository;
import com.shopping.system.repository.ProductRepository;
import com.shopping.system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class BaseIntegrationTest {

    protected static final String PASSWORD = "secret123";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected ProductRepository productRepository;

    @Autowired
    protected CartRepository cartRepository;

    @Autowired
    protected CartItemRepository cartItemRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected User createUser(String email, Role role) {
        User user = new User();
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user = userRepository.save(user);

        Cart cart = new Cart();
        cart.setUser(user);
        cartRepository.save(cart);
        return user;
    }

    protected MockHttpSession registerAndLogin(String email) throws Exception {
        MockHttpSession session = new MockHttpSession();
        register(session, email);
        login(session, email, PASSWORD);
        return session;
    }

    protected void register(MockHttpSession session, String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Test User", "email", email, "password", PASSWORD)))
                        .session(session))
                .andExpect(status().isCreated());
    }

    protected void login(MockHttpSession session, String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", password)))
                        .session(session))
                .andExpect(status().isOk());
    }

    protected MockHttpSession loginAsAdmin() throws Exception {
        createUser("admin@test.com", Role.ADMIN);
        MockHttpSession session = new MockHttpSession();
        login(session, "admin@test.com", PASSWORD);
        return session;
    }

    protected Product createProduct(String name, BigDecimal price, Integer quantity) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("Description for " + name);
        product.setPrice(price);
        product.setQuantity(quantity);
        product.setImageUrl("https://placehold.co/400x300");
        product.setAvailable(true);
        return productRepository.save(product);
    }

    protected void addToCart(MockHttpSession session, Long productId, Integer quantity) throws Exception {
        mockMvc.perform(post("/api/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("productId", productId, "quantity", quantity)))
                        .session(session))
                .andExpect(status().isOk());
    }
}
