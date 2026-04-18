package com.ratelimiter.model;

import java.time.Duration;

public record RateLimitPolicy(
        int limit,
        Duration window,
        RateLimitSpecs.Algorithm algorithm,
        RateLimitSpecs.Identity identityStrategy
) {
}
