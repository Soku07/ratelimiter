package com.ratelimiter.algorithm;

public record FixedWindowAlgorithmState( long counter, long timeStampCreated,boolean isAllowed) implements RateLimitAlgorithmDecision {
}
