package com.ratelimiter.algorithm;

public record ProbabilisticSlidingWindowState(Bucket currBucket, Bucket prevBucket, boolean isAllowed) implements RateLimitDecision {
}

record Bucket (
        long counter,
        long timeStampCreated,
        long timeStampEnd
){}
