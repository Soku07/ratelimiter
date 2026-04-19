package com.ratelimiter.orchestrator;

public interface RateLimitOrchestrator {
    // Interface is used so that this rate limiter can also be extended in later versions to support rate limiting for non rest api use cases like gRpc

    boolean isAllowed(String resourcePath, Object requestSource);
}
