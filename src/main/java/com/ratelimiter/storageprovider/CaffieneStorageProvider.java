package com.ratelimiter.storageprovider;

import com.github.benmanes.caffeine.cache.Cache;
import com.ratelimiter.algorithm.RateLimitDecision;
import com.ratelimiter.exceptions.StorageException;
import com.ratelimiter.model.RateLimitContext;
import com.ratelimiter.model.RateLimitSpecs;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.BiFunction;

@Component
//@Primary
public class CaffieneStorageProvider implements StorageProvider {
    private final Cache<String, Object> cache;
    public CaffieneStorageProvider(Cache<String, Object> cache){
        this.cache = cache;
    }

    @Override
    public <T extends RateLimitDecision> T atomicCompute(String key, RateLimitSpecs.Algorithm algorithm, RateLimitContext context, BiFunction<String, T, T> algorithmLogic, Duration timeToLive) {
        @SuppressWarnings("unchecked")
        T result = (T) cache.asMap().compute(
                key,(k, oldValue) -> {
                    try{
                        return algorithmLogic.apply(k,(T)oldValue);
                    }catch (Exception e){
                        throw  new StorageException("Failed to perform atomic operation for key : "+ k,e);
                    }

                }
        );
        return result;

    }
}
