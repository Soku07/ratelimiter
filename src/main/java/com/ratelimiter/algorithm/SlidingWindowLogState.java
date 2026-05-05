package com.ratelimiter.algorithm;

import java.util.ArrayDeque;

public record SlidingWindowLogState(ArrayDeque<Long> timeStampLog, boolean isAllowed) implements RateLimitAlgorithmDecision{
}
