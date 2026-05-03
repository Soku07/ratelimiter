package com.ratelimiter.algorithm;

import com.ratelimiter.ConstEnum;
import com.ratelimiter.model.RateLimitContext;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitSpecs;
import com.ratelimiter.storageprovider.StorageProvider;
import org.springframework.stereotype.Component;

@Component
public class FixedWindowAlgorithm  implements RateLimitAlgorithm{

    private final StorageProvider storageProvider;

    public FixedWindowAlgorithm(StorageProvider storageProvider) {

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
        RateLimitAlgorithmDecision rateLimitAlgorithmDecision = storageProvider.atomicCompute(key, RateLimitSpecs.Algorithm.FIXED_WINDOW,rateLimitContext,
                (k, currentState) -> applyFixedWindowAlgorithm(key, (FixedWindowAlgorithmState) currentState,policy),
                policy.window()
                );
        return rateLimitAlgorithmDecision.isAllowed();
    }

    @Override
    public RateLimitSpecs.Algorithm getAlgorithmType() {
        return RateLimitSpecs.Algorithm.FIXED_WINDOW;
    }

    protected FixedWindowAlgorithmState applyFixedWindowAlgorithm(String key, FixedWindowAlgorithmState state, RateLimitPolicy policy){
        long now = System.currentTimeMillis();
        if (state == null || now > (state.timeStampCreated() + policy.window().toMillis())) {
            return new FixedWindowAlgorithmState(1, now, true);
        }
        long totalRequestCount = state.counter() + 1;
        if(totalRequestCount <= policy.limit()){
            return new FixedWindowAlgorithmState(totalRequestCount,state.timeStampCreated(),true);
        }
        return new FixedWindowAlgorithmState(state.counter(),state.timeStampCreated(),false);

    }


}
