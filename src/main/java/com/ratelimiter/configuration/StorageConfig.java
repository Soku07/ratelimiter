package com.ratelimiter.configuration;

import com.github.benmanes.caffeine.cache.Cache;
import com.ratelimiter.model.RateLimitSpecs;
import com.ratelimiter.storageprovider.CaffieneStorageProvider;
import com.ratelimiter.storageprovider.RedisStorageProvider;
import com.ratelimiter.storageprovider.StorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Map;

@Configuration
@Slf4j
public class StorageConfig {

    @Value("${ratelimiter.storage.type:caffeine}")
    private String storageType;
    @Autowired
    private  Cache<String, Object> cache;
    @Autowired
    private  RedisTemplate<String,String> redisTemplate;
    @Autowired
    private  Map<RateLimitSpecs.Algorithm, RedisScript<Long>> algorithmRedisScriptMap;


    @Bean
    @ConditionalOnProperty(name = "ratelimiter.storage.type", havingValue = "caffeine", matchIfMissing = true)
    public StorageProvider caffeineStorageProvider() {
        log.warn("Initialized Storage: Caffeine");
        return new CaffieneStorageProvider(cache);
    }

    @Bean
    @ConditionalOnProperty(name = "ratelimiter.storage.type", havingValue = "redis")
    public StorageProvider redisStorageProvider(RedisConnectionFactory connectionFactory) {
        try {
            // Basic check to see if Redis is actually reachable
            connectionFactory.getConnection().close();
            log.warn("Initialized Storage: Redis");
            return new RedisStorageProvider(redisTemplate,algorithmRedisScriptMap);
        } catch (Exception e) {
            log.error("Redis requested but connection failed. Falling back to Caffeine.", e);
            return new CaffieneStorageProvider(cache);
        }
    }
}
