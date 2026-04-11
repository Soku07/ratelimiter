package com.ratelimiter.model;

import java.time.Duration;

public record RateLimitPolicy(
        int limit,
        Duration window,
        String algorithmKey,
        String identityStrategy
) {
}
