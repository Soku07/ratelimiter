package com.ratelimiter.algorithm;

import com.ratelimiter.model.RateLimitPolicy;

public interface RateLimitAlgorithm {


    boolean isAllowed(String key, RateLimitPolicy policy);
}
