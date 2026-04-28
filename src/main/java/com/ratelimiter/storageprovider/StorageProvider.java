package com.ratelimiter.storageprovider;

import com.ratelimiter.algorithm.RateLimitDecision;
import com.ratelimiter.model.RateLimitContext;
import com.ratelimiter.model.RateLimitSpecs;

import java.time.Duration;
import java.util.function.BiFunction;

public interface StorageProvider {


    <T extends RateLimitDecision> T atomicCompute(String key, RateLimitSpecs.Algorithm algorithm, RateLimitContext context, BiFunction<String, T, T> algorithmLogic, Duration timeToLive);
}
