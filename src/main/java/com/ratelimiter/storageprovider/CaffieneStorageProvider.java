package com.ratelimiter.storageprovider;

import com.github.benmanes.caffeine.cache.Cache;
import com.ratelimiter.exceptions.StorageException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.BiFunction;

@Component
public class CaffieneStorageProvider implements StorageProvider {
    private final Cache<String, Object> cache;
    public CaffieneStorageProvider(Cache<String, Object> cache){
        this.cache = cache;
    }

    @Override
    public <T> T atomicCompute(String key, BiFunction<String, T, T> algorithmLogic, Duration timeToLive) {
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
