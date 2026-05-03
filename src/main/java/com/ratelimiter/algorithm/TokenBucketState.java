package com.ratelimiter.algorithm;

public record TokenBucketState (
        double tokenCount,
        long lastRefillTimeStamp,
        boolean isAllowed

) implements RateLimitAlgorithmDecision {

}
