package com.ratelimiter.algorithm;

import com.ratelimiter.ConstEnum;
import com.ratelimiter.model.RateLimitContext;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitSpecs;
import com.ratelimiter.storageprovider.StorageProvider;
import org.springframework.stereotype.Component;

@Component
public class ProbabilisticSlidingWindowAlgorithm implements RateLimitAlgorithm {
    private final StorageProvider storageProvider;

    public ProbabilisticSlidingWindowAlgorithm(StorageProvider storageProvider){
        this.storageProvider = storageProvider;
    }

    @Override
    public boolean isAllowed(String key, RateLimitPolicy policy) {
        RateLimitContext rateLimitContext = new RateLimitContext((1 + ConstEnum.BURST_FACTOR) * policy.limit(),
                System.currentTimeMillis(),
                policy.window().toMillis(),
                policy.limit(),
                policy.window().toMillis() * 2
        );
        RateLimitDecision rateLimitDecision = storageProvider.atomicCompute(key, RateLimitSpecs.Algorithm.TOKEN_BUCKET,rateLimitContext,
                (k, currentState) -> applyprobabilisticSlidingWindowAlgorithm(k,(TokenBucketState) currentState, policy),
                policy.window()
        );
        return rateLimitDecision.isAllowed();
    }

    @Override
    public RateLimitSpecs.Algorithm getAlgorithmType() {
        return RateLimitSpecs.Algorithm.PROBABILISTIC_SLIDING_WINDOW;
    }
    protected RateLimitDecision applyprobabilisticSlidingWindowAlgorithm(String key, TokenBucketState oldBucketSTate, RateLimitPolicy policy) {

        return new RateLimitDecision() {
            @Override
            public boolean isAllowed() {
                return false;
            }
        };
    }
}
