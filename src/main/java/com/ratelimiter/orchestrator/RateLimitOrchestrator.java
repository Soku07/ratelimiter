package com.ratelimiter.orchestrator;

import com.ratelimiter.model.RateLimitDecision;

public interface RateLimitOrchestrator {
    // Interface is used so that this rate limiter can also be extended in later versions to support rate limiting for non rest api use cases like gRpc

    RateLimitDecision isAllowed(String resourcePath, Object requestSource);
}
