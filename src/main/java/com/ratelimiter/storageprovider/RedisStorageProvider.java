package com.ratelimiter.storageprovider;

import com.ratelimiter.algorithm.RateLimitDecision;
import com.ratelimiter.exceptions.StorageException;
import com.ratelimiter.model.RateLimitContext;
import com.ratelimiter.model.RateLimitSpecs;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;

@Component
@Primary
public class RedisStorageProvider implements StorageProvider{
    private final RedisTemplate<String,String> redisTemplate;
    private final Map<RateLimitSpecs.Algorithm, RedisScript<Long>> algorithmRedisScriptMap;
    private record RedisDecision(boolean isAllowed) implements RateLimitDecision {}
    public RedisStorageProvider(RedisTemplate<String,String> redisTemplate, Map<RateLimitSpecs.Algorithm, RedisScript<Long>> algorithmRedisScriptMap){
        this.redisTemplate = redisTemplate;
        this.algorithmRedisScriptMap = algorithmRedisScriptMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends RateLimitDecision> T atomicCompute(String key, RateLimitSpecs.Algorithm algorithm, RateLimitContext context, BiFunction<String, T, T> algorithmLogic, Duration timeToLive) {
        try {
            Object[] redisArgs = new Object[]{
                    String.valueOf(context.capacity()),     // ARGV[1]
                    String.valueOf(context.currentTimeMilliSeconds()),          // ARGV[2]
                    String.valueOf(context.windowMillis()), // ARGV[3]
                    String.valueOf(context.limit()),        // ARGV[4]
                    String.valueOf(context.ttlMilliSeconds())    // ARGV[5]
            };
            Long result = redisTemplate.execute(algorithmRedisScriptMap.get(algorithm), Collections.singletonList(key), redisArgs);
            return (T) new RedisDecision(result != null && result == 1);
        }
        catch (Exception e){
            throw new StorageException("Failed to perform atomic Redis operation for key: " + key, e);
        }
    }
}
