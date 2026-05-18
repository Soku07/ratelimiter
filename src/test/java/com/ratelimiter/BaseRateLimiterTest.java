package com.ratelimiter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.ratelimiter.model.RateLimitSpecs;
import com.ratelimiter.storageprovider.CaffieneStorageProvider;
import com.ratelimiter.storageprovider.RedisStorageProvider;
import io.lettuce.core.api.StatefulConnection;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;

@Testcontainers
public class BaseRateLimiterTest {

    @Container
    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    protected RedisStorageProvider redisProvider;
    protected CaffieneStorageProvider caffieneStorageProvider;
    protected RedisTemplate<String, String> redisTemplate;

    @BeforeEach
    protected void setUp () {
        Cache<String,Object> cache = Caffeine.newBuilder().maximumSize(ConstEnum.LOCAL_CACHE_MAX_SIZE).recordStats().build();
        this.caffieneStorageProvider = new CaffieneStorageProvider(cache);


        //Redis
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(
                REDIS.getHost(),
                REDIS.getMappedPort(6379)
        );
        LettuceConnectionFactory factory = new LettuceConnectionFactory(redisConfig);
        factory.afterPropertiesSet(); // Initialize the connection pool

        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        this.redisTemplate = template;
        this.redisProvider = new RedisStorageProvider(template,loadScripts());
    }

    private Map<RateLimitSpecs.Algorithm, RedisScript<Long>> loadScripts() {
        Map<RateLimitSpecs.Algorithm, RedisScript<Long>> scripts = new HashMap<>();
        scripts.put(RateLimitSpecs.Algorithm.TOKEN_BUCKET,
                RedisScript.of(new ClassPathResource("luascripts/tokenbucketalgorithm.lua"), Long.class));
        scripts.put(RateLimitSpecs.Algorithm.SLIDING_WINDOW_LOG,
                RedisScript.of(new ClassPathResource("luascripts/SlidingWindowLog.lua"), Long.class));
        scripts.put(RateLimitSpecs.Algorithm.FIXED_WINDOW,
                RedisScript.of(new ClassPathResource("luascripts/FixedWindowAlgorithm.lua"), Long.class));
        scripts.put(RateLimitSpecs.Algorithm.PROBABILISTIC_SLIDING_WINDOW,
                RedisScript.of(new ClassPathResource("luascripts/ProbablisticSlidingWindowAlgorithm.lua"), Long.class));
        return scripts;
    }
}
