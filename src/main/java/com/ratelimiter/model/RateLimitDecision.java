package com.ratelimiter.model;

public record RateLimitDecision(
        boolean allowed,
        int httpStatus,
        String error,
        String message
) {
    public static RateLimitDecision allow() {
        return new RateLimitDecision(true, 200, null, null);
    }

    public static RateLimitDecision deny(int status, String error, String message) {
        return new RateLimitDecision(false, status, error, message);
    }
}
