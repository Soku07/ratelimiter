package com.ratelimiter.algorithm;

import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitSpecs;

public interface RateLimitAlgorithm {


    boolean isAllowed(String key, RateLimitPolicy policy);
    RateLimitSpecs.Algorithm getAlgorithmType();
}
