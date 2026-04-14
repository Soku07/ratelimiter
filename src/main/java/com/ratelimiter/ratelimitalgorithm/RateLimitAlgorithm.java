package com.ratelimiter.ratelimitalgorithm;

import com.ratelimiter.model.RateLimitPolicy;

public interface RateLimitAlgorithm {


    public  boolean isAllowed(String key, RateLimitPolicy policy);
}
