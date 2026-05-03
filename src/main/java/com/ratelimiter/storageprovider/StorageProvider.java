package com.ratelimiter.storageprovider;

import com.ratelimiter.algorithm.RateLimitAlgorithmDecision;
import com.ratelimiter.model.RateLimitContext;
import com.ratelimiter.model.RateLimitSpecs;

import java.time.Duration;
import java.util.function.BiFunction;

public interface StorageProvider {


    <T extends RateLimitAlgorithmDecision> T atomicCompute(String key, RateLimitSpecs.Algorithm algorithm, RateLimitContext context, BiFunction<String, T, T> algorithmLogic, Duration timeToLive);
}
