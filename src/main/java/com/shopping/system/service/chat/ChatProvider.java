package com.shopping.system.service.chat;

/**
 * Abstraction over the AI provider that answers product/shopping questions.
 * Implementations must be safe to call from any thread and must not leak
 * secrets (e.g. API keys) into their responses or logs.
 */
public interface ChatProvider {

    /** Stable identifier, reported back in API responses. */
    String name();

    /**
     * Produce a reply to a validated, in-domain question.
     *
     * @param message the trimmed user message
     * @return the reply text (never null)
     * @throws RuntimeException if the provider cannot answer (e.g. network
     *         failure, missing API key) so the caller can fall back gracefully
     */
    String answer(String message);
}