package com.ratelimiter.algorithm;

public record ProbabilisticSlidingWindowState(Bucket currBucket, Bucket prevBucket, boolean isAllowed) implements RateLimitAlgorithmDecision {
}

record Bucket (
        long counter,
        long timeStampCreated,
        long timeStampEnd
){}
