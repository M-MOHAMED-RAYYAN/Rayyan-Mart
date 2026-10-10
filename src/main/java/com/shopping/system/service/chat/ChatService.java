package com.shopping.system.service.chat;

import com.shopping.system.config.ChatProperties;
import com.shopping.system.dto.ChatResponse;
import com.shopping.system.exception.ChatRateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the RayyanMart shopping assistant.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>validate messages and enforce a length limit</li>
 *   <li>per-session rate limiting (default 10 messages / minute)</li>
 *   <li>in-memory per-session caching of repeated questions</li>
 *   <li>enforce the shopping/product domain before calling a provider</li>
 *   <li>graceful fallback when the configured provider fails</li>
 * </ul>
 *
 * <p>The assistant is optional: when disabled, a short notice is returned and
 * no other shopping feature is affected.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final String FALLBACK_REPLY =
            "I'm having trouble reaching the assistant right now. While I recover, here's a quick answer: RayyanMart "
                    + "sells products across its catalogue, and after registering you can browse products, add them to "
                    + "your cart and place orders. You can also check the Products page directly. Please try again in "
                    + "a moment.";

    private final ChatProperties properties;
    private final ChatDomainGuard domainGuard;
    private final FAQChatProvider faqProvider;
    private final GeminiChatProvider geminiProvider;

    private final Map<String, Deque<Long>> rateLimitBuckets = new ConcurrentHashMap<>();
    private final Map<String, Map<String, CacheEntry>> answerCache = new ConcurrentHashMap<>();

    public ChatService(ChatProperties properties,
                       ChatDomainGuard domainGuard,
                       FAQChatProvider faqProvider,
                       GeminiChatProvider geminiProvider) {
        this.properties = properties;
        this.domainGuard = domainGuard;
        this.faqProvider = faqProvider;
        this.geminiProvider = geminiProvider;
    }

    /**
     * Answer a user message for a given session.
     *
     * @param sessionId the servlet session id (rate limit + cache key)
     * @param message   the raw user message
     * @throws ChatRateLimitException when the per-minute quota is exceeded
     */
    public ChatResponse chat(String sessionId, String message) {
        String question = normalize(message);

        if (!properties.isEnabled()) {
            return new ChatResponse(
                    "The RayyanMart assistant is currently turned off. You can still browse products, add items to "
                            + "your cart and place orders normally.",
                    "none", false, LocalDateTime.now());
        }
        if (question.isBlank()) {
            throw new IllegalArgumentException("Please enter a message.");
        }
        if (question.length() > properties.getMaxMessageLength()) {
            throw new IllegalArgumentException("Messages can be at most " + properties.getMaxMessageLength()
                    + " characters long.");
        }

        String key = safeKey(sessionId);
        assertRateLimit(key, properties.getRateLimitPerMinute());

        CacheEntry cached = getCached(key, question);
        if (cached != null) {
            return new ChatResponse(cached.answer, cached.provider, false, LocalDateTime.now());
        }

        if (!domainGuard.isInDomain(question)) {
            return new ChatResponse(domainGuard.refusal(), "guard", false, LocalDateTime.now());
        }

        String reply;
        String providerName;
        boolean fallback = false;
        try {
            ChatProvider provider = resolveProvider();
            reply = provider.answer(question);
            providerName = provider.name();
        } catch (RuntimeException e) {
            log.warn("Chat provider unavailable, using fallback: {}", e.toString());
            reply = FALLBACK_REPLY;
            providerName = "fallback";
            fallback = true;
        }

        putCache(key, question, new CacheEntry(reply, providerName));
        return new ChatResponse(reply, providerName, fallback, LocalDateTime.now());
    }

    private ChatProvider resolveProvider() {
        String configured = properties.getProvider() == null ? "" : properties.getProvider().trim().toLowerCase();
        if ("gemini".equals(configured)) {
            return geminiProvider;
        }
        return faqProvider;
    }

    private void assertRateLimit(String key, int limit) {
        if (key == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Deque<Long> stamps = rateLimitBuckets.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (stamps) {
            long windowStart = now - 60_000L;
            long usable = limit;
            if (limit <= 0) {
                return;
            }
            usable = limit;
            while (!stamps.isEmpty() && stamps.peekFirst() < windowStart) {
                stamps.pollFirst();
            }
            if (stamps.size() >= usable) {
                throw new ChatRateLimitException(
                        "You are sending messages too quickly. Please wait a moment before continuing.");
            }
            stamps.addLast(now);
        }
    }

    private CacheEntry getCached(String key, String question) {
        if (key == null || question == null) {
            return null;
        }
        Map<String, CacheEntry> sessionCache = answerCache.get(key);
        if (sessionCache == null) {
            return null;
        }
        CacheEntry entry = sessionCache.get(question);
        if (entry == null) {
            return null;
        }
        long ttlMillis = properties.getCacheTtlSeconds() * 1000L;
        if (System.currentTimeMillis() - entry.createdAtMillis > ttlMillis) {
            sessionCache.remove(question);
            return null;
        }
        return entry;
    }

    private void putCache(String key, String question, CacheEntry entry) {
        if (key == null || question == null) {
            return;
        }
        Map<String, CacheEntry> sessionCache = answerCache.computeIfAbsent(key, k -> new LinkedHashMap<>() {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > properties.getCacheSize();
            }
        });
        sessionCache.put(question, entry);
    }

    private static String normalize(String message) {
        return message == null ? "" : message.trim();
    }

    private static String safeKey(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? "anonymous" : sessionId;
    }

    private static final class CacheEntry {
        private final String answer;
        private final String provider;
        private final long createdAtMillis;

        private CacheEntry(String answer, String provider) {
            this.answer = answer;
            this.provider = provider;
            this.createdAtMillis = System.currentTimeMillis();
        }
    }
}