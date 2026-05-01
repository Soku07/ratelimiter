package com.ratelimiter.model;

public record RateLimitContext(double capacity,
                               long currentTimeMilliSeconds,
                               long windowMillis,
                               double limit,
                               long ttlMilliSeconds
) {
}
