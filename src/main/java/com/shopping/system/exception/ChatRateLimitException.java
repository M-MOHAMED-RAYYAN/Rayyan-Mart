package com.shopping.system.exception;

/**
 * Thrown when a session exceeds its per-minute chat message quota.
 */
public class ChatRateLimitException extends RuntimeException {

    public ChatRateLimitException(String message) {
        super(message);
    }
}