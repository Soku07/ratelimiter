package com.ratelimiter.algorithm;

import com.ratelimiter.ConstEnum;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitSpecs;
import com.ratelimiter.storageprovider.StorageProvider;
import org.springframework.stereotype.Component;

@Component
public class TokenBucketAlgorithm implements RateLimitAlgorithm{
    private final StorageProvider storageProvider;

    public TokenBucketAlgorithm(StorageProvider storageProvider) {

        this.storageProvider = storageProvider;
    }

    @Override
    public boolean isAllowed(String key, RateLimitPolicy policy) {
        //Orchestrator has to catch the exception thrown by any algorithm and apply bypass settings
            TokenBucketState tokenBucketState = storageProvider.atomicCompute(key,
                    (k, currentState) -> applyTokenBucketAlgorithm(k,currentState, policy),
                    policy.window()
            );

        return tokenBucketState.isAllowed();
    }

    @Override
    public RateLimitSpecs.Algorithm getAlgorithmType() {
        return RateLimitSpecs.Algorithm.TOKEN_BUCKET;
    }

    //This method is doing the real calculations. If this method is private, writing unit tests would become hard.
    //Hence, making it protected
    protected TokenBucketState applyTokenBucketAlgorithm(String key, TokenBucketState oldBucketSTate, RateLimitPolicy policy) {
        double capacity = (1 + ConstEnum.BURST_FACTOR) * policy.limit();
        long currentTimeStamp = System.currentTimeMillis();
        if(oldBucketSTate == null){
            return new TokenBucketState(capacity-1,currentTimeStamp,true);
        }
        double tokenCount = oldBucketSTate.tokenCount();
        long lastRefillTimeStamp = oldBucketSTate.lastRefillTimeStamp();

        double refillTokenCount = ( (double) (currentTimeStamp - lastRefillTimeStamp) / policy.window().toMillis()) * policy.limit();
        double totalTokenCount = Math.min(capacity, tokenCount + refillTokenCount);
        if(totalTokenCount < 1){
            return new TokenBucketState(totalTokenCount, currentTimeStamp,false);
        }
        return new TokenBucketState(totalTokenCount-1, currentTimeStamp,true);
    }
}
