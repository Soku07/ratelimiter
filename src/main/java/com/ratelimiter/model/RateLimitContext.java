package com.ratelimiter.model;

public record RateLimitContext(double capacity,
                               long currentTime,
                               long windowMillis,
                               double limit,
                               long ttlSeconds
) {
}
