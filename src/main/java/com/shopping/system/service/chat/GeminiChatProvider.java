package com.shopping.system.service.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.system.config.ChatProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini provider used when {@code app.chat.provider=gemini} and a
 * {@code GEMINI_API_KEY} environment variable is configured.
 *
 * <p>The API key is read server-side only and is never exposed to the browser,
 * responses or logs. Outbound requests have a configurable timeout. Any failure
 * is wrapped in a {@link RuntimeException} so the chat service can respond with
 * a graceful fallback.
 */
@Component
public class GeminiChatProvider implements ChatProvider {

    private static final String SYSTEM_PROMPT =
            "You are the RayyanMart shopping assistant. Only answer questions about RayyanMart: "
                    + "its products, prices, categories, stock, shopping guidance, orders, delivery, payments and "
                    + "returns. If the user asks about anything outside that shopping domain, politely decline and "
                    + "redirect them to RayyanMart shopping topics. Keep answers short (under 5 sentences).";

    private final ChatProperties properties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public GeminiChatProvider(ChatProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = createRestTemplate(properties.getGemini().getTimeoutMs());
    }

    private static RestTemplate createRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        return new RestTemplate(factory);
    }

    @Override
    public String name() {
        return "gemini";
    }

    @Override
    public String answer(String message) {
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + properties.getGemini().getModel()
                + ":generateContent?key=" + apiKey;

        Map<String, Object> body = Map.of("contents", List.of(Map.of(
                "parts", List.of(Map.of("text", SYSTEM_PROMPT + "\n\nQuestion: " + message))
        )));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Gemini returned status " + response.getStatusCode());
            }
            String text = extractText(response.getBody());
            if (text == null || text.isBlank()) {
                throw new IllegalStateException("Gemini returned an empty answer");
            }
            return text.trim();
        } catch (RestClientException e) {
            throw new IllegalStateException("Could not reach the AI provider: " + e.getMessage(), e);
        }
    }

    private String extractText(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText(null);
                }
            }
        } catch (Exception ignored) {
            // malformed response handled by caller
        }
        return null;
    }
}