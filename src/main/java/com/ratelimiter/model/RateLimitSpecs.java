package com.ratelimiter.model;

public final class RateLimitSpecs {
    private RateLimitSpecs() {}

    public enum Identity{
        IP_ADDRESS,
        AUTH_TOKEN
    }

    public enum Algorithm{
        TOKEN_BUCKET ,
        PROBABILISTIC_SLIDING_WINDOW,
        FIXED_WINDOW
    }
}
