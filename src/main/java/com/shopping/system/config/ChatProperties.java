package com.shopping.system.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the product assistant.
 *
 * <p>All secrets are read from environment variables only ({@code GEMINI_API_KEY}).
 * The browser never sees any of these values.
 */
@Component
@ConfigurationProperties(prefix = "app.chat")
public class ChatProperties {

    /** Provider to use: {@code mock} (deterministic FAQ, no API key) or {@code gemini}. */
    private String provider = "mock";

    /** Set {@code true} to disable the assistant entirely. */
    private boolean enabled = true;

    /** Maximum accepted message length. */
    private int maxMessageLength = 500;

    /** Per-session rate limit. */
    private int rateLimitPerMinute = 10;

    /** In-memory answer cache size per session. */
    private int cacheSize = 50;

    /** How long cached answers stay valid, in seconds. */
    private long cacheTtlSeconds = 120;

    /** Optional Gemini configuration. Key comes from the GEMINI_API_KEY env var. */
    private Gemini gemini = new Gemini();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxMessageLength() {
        return maxMessageLength;
    }

    public void setMaxMessageLength(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public Gemini getGemini() {
        return gemini;
    }

    public void setGemini(Gemini gemini) {
        this.gemini = gemini;
    }

    public static class Gemini {

        private String apiKey = "";
        private String model = "gemini-2.0-flash";
        private int timeoutMs = 8000;

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}