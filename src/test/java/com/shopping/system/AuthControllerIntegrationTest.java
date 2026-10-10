package com.shopping.system;

import com.shopping.system.model.Role;
import com.shopping.system.model.User;
import com.shopping.system.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends BaseIntegrationTest {

    @Test
    void userRegistrationSucceeds() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"John Doe\",\"email\":\"john@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("John Doe"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String body = "{\"name\":\"John Doe\",\"email\":\"dup@example.com\",\"password\":\"secret123\"}";
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void passwordIsStoredHashed() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Hash User\",\"email\":\"hash@example.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("hash@example.com").orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", user.getPassword())).isTrue();
    }

    @Test
    void loginSucceedsWithValidCredentials() throws Exception {
        MockHttpSession session = new MockHttpSession();
        register(session, "login@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@example.com\",\"password\":\"secret123\"}")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("login@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        assertThat(session.getAttribute(SessionService.USER_ID_ATTR)).isNotNull();
    }

    @Test
    void loginFailsWithInvalidPassword() throws Exception {
        MockHttpSession session = new MockHttpSession();
        register(session, "wrong@example.com");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"wrong@example.com\",\"password\":\"wrongpass\"}")
                        .session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpointRejectsUnauthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        MockHttpSession session = registerAndLogin("logout@example.com");
        assertThat(session.getAttribute(SessionService.USER_ID_ATTR)).isNotNull();

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void adminRoleIsStoredInSessionOnLogin() throws Exception {
        createUser("admin2@test.com", Role.ADMIN);
        MockHttpSession session = new MockHttpSession();
        login(session, "admin2@test.com", PASSWORD);

        assertThat(session.getAttribute(SessionService.ROLE_ATTR)).isEqualTo("ADMIN");
    }
}
