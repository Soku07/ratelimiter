package com.ratelimiter.ratelimitalgorithm;

public record TokenBucketState(
        double tokenCount,
        long lastRefillTimeStamp,
        boolean isAllowed

) {

}