package com.shopping.system.service.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopping.system.config.ChatProperties;
import com.shopping.system.dto.ChatResponse;
import com.shopping.system.exception.ChatRateLimitException;
import com.shopping.system.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ChatServiceTest {

    private ChatProperties properties;
    private ProductRepository productRepository;
    private ChatDomainGuard domainGuard;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        properties = new ChatProperties();
        properties.setEnabled(true);
        properties.setProvider("mock");
        properties.setMaxMessageLength(50);
        properties.setRateLimitPerMinute(10);
        properties.getGemini().setApiKey("");
        productRepository = mock(ProductRepository.class);
        domainGuard = new ChatDomainGuard();
        objectMapper = new ObjectMapper();
    }

    private ChatService serviceWithFaq() {
        FAQChatProvider faq = new FAQChatProvider(productRepository);
        return new ChatService(properties, domainGuard, faq, new GeminiChatProvider(properties, objectMapper));
    }

    @Test
    void blankMessageIsRejected() {
        ChatService service = serviceWithFaq();
        assertThrows(IllegalArgumentException.class, () -> service.chat("session-1", "   "));
    }

    @Test
    void emptyMessageIsRejected() {
        ChatService service = serviceWithFaq();
        assertThrows(IllegalArgumentException.class, () -> service.chat("session-1", null));
    }

    @Test
    void tooLongMessageIsRejected() {
        ChatService service = serviceWithFaq();
        String message = "a".repeat(properties.getMaxMessageLength() + 1);
        assertThrows(IllegalArgumentException.class, () -> service.chat("session-1", message));
    }

    @Test
    void validQuestionIsAnsweredByMockProvider() {
        ChatService service = serviceWithFaq();
        ChatResponse response = service.chat("session-1", "How do I place an order?");
        assertNotNull(response.reply());
        assertFalse(response.reply().isBlank());
        assertEquals("mock", response.provider());
        assertFalse(response.fallback());
    }

    @Test
    void outOfDomainQuestionIsRefusedInDomain() {
        ChatService service = serviceWithFaq();
        ChatResponse response = service.chat("session-1", "write a poem about the moon");
        assertEquals("guard", response.provider());
        assertTrue(response.reply().contains("RayyanMart"));
    }

    @Test
    void repeatedQuestionIsServedFromCache() {
        FAQChatProvider faq = org.mockito.Mockito.spy(new FAQChatProvider(productRepository));
        ChatService service = new ChatService(properties, domainGuard, faq,
                new GeminiChatProvider(properties, objectMapper));

        String message = "What products do you sell?";
        ChatResponse first = service.chat("session-cache", message);
        ChatResponse second = service.chat("session-cache", message);

        assertEquals(first.reply(), second.reply());
        verify(faq, times(1)).answer(message);
    }

    @Test
    void paymentQuestionExplainsNoPaymentProcessing() {
        ChatService service = serviceWithFaq();
        ChatResponse response = service.chat("session-1", "How do I pay for my order?");
        assertNotNull(response.reply());
        assertTrue(response.reply().contains("no money is transferred"));
        assertTrue(response.reply().contains("PENDING"));
        assertFalse(response.reply().contains("card payments"));

        ChatResponse response2 = service.chat("session-1", "What payment methods are available?");
        assertTrue(response2.reply().contains("no money is transferred"),
                () -> "expected accurate payment answer but got: " + response2.reply());
    }

    @Test
    void providerFailureFallsBackGracefully() {
        properties.setProvider("gemini");
        ChatService service = serviceWithFaq();

        ChatResponse response = service.chat("session-1", "Show me the products");
        assertTrue(response.fallback());
        assertEquals("fallback", response.provider());
        assertNotNull(response.reply());
        assertFalse(response.reply().isBlank());
    }

    @Test
    void rateLimitIsEnforcedPerSession() {
        properties.setRateLimitPerMinute(2);
        ChatService service = serviceWithFaq();

        service.chat("session-rate", "What products do you sell?");
        service.chat("session-rate", "How do I place an order?");
        assertThrows(ChatRateLimitException.class,
                () -> service.chat("session-rate", "What is the return policy?"));
    }

    @Test
    void rateLimitIsIsolatedPerSession() {
        properties.setRateLimitPerMinute(2);
        ChatService service = serviceWithFaq();

        service.chat("session-a", "What products do you sell?");
        service.chat("session-a", "How do I place an order?");
        ChatResponse response = service.chat("session-b", "What is your delivery time?");
        assertNotNull(response.reply());
    }

    @Test
    void disabledAssistantReturnsNotice() {
        properties.setEnabled(false);
        ChatService service = serviceWithFaq();
        ChatResponse response = service.chat("session-1", "Show me the products");
        assertEquals("none", response.provider());
        assertNotNull(response.reply());
    }
}