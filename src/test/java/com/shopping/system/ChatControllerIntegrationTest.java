package com.shopping.system;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatControllerIntegrationTest extends BaseIntegrationTest {

    private String chatBody(String message) {
        return "{\"message\":\"" + message.replace("\"", "\\\"") + "\"}";
    }

    @Test
    void chatAnswersAProductQuestion() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody("show available products")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", not(blankOrNullString())))
                .andExpect(jsonPath("$.reply", containsString("Wireless Mouse")))
                .andExpect(jsonPath("$.provider").value("mock"))
                .andExpect(jsonPath("$.fallback").value(false));
    }

    @Test
    void chatDoesNotRequireLogin() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody("what is the price?")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", not(blankOrNullString())));
    }

    @Test
    void chatRejectsBlankMessage() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody("   ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatRejectsTooLongMessage() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody("a".repeat(60))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatRejectsOutOfDomainQuestion() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody("write a poem about the ocean")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("guard"))
                .andExpect(jsonPath("$.reply", containsString("RayyanMart")));
    }

    @Test
    void chatAnswersGuidanceQuestion() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody("how do I place an order?")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", containsString("cart")));
    }

    @Test
    void chatRateLimitsTenMessagesPerMinute() throws Exception {
        MockHttpSession session = new MockHttpSession();
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/chat")
                            .session(session)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(chatBody("what is the price of item " + i + "?")))
                    .andExpect(status().isOk());
        }
        mockMvc.perform(post("/api/chat")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chatBody("what is the price of item ten?")))
                .andExpect(status().isTooManyRequests());
    }
}