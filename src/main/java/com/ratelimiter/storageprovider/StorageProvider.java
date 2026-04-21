package com.ratelimiter.storageprovider;

import java.time.Duration;
import java.util.function.BiFunction;

public interface StorageProvider {
    <T> T atomicCompute(String key, BiFunction<String, T, T> algorithmLogic, Duration timeToLive);
}
