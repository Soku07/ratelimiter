package com.ratelimiter.algorithm;

import com.ratelimiter.ConstEnum;
import com.ratelimiter.model.RateLimitContext;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitSpecs;
import com.ratelimiter.storageprovider.StorageProvider;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.List;

@Component
public class SlidingWindowLog implements RateLimitAlgorithm{
    private final StorageProvider storageProvider;

    public SlidingWindowLog(StorageProvider storageProvider){
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
        RateLimitAlgorithmDecision rateLimitAlgorithmDecision = storageProvider.atomicCompute(key, RateLimitSpecs.Algorithm.SLIDING_WINDOW_LOG,rateLimitContext,
                (k, state) -> applySlidingWindowLogAlgorithm((SlidingWindowLogState) state,policy),policy.window()
                );
        return rateLimitAlgorithmDecision.isAllowed();
    }

    @Override
    public RateLimitSpecs.Algorithm getAlgorithmType() {
        return RateLimitSpecs.Algorithm.SLIDING_WINDOW_LOG;
    }

    protected SlidingWindowLogState applySlidingWindowLogAlgorithm(SlidingWindowLogState state , RateLimitPolicy policy){
        long now = System.currentTimeMillis();
        long windowMillis = policy.window().toMillis();
        if(state == null || now > state.timeStampLog().getLast() + windowMillis){
            return new SlidingWindowLogState(new ArrayDeque<>(List.of(now)),true);
        }
        ArrayDeque<Long> currTimeStampLog = state.timeStampLog();
        while(!currTimeStampLog.isEmpty() && currTimeStampLog.peekFirst() < now - policy.window().toMillis()){
            currTimeStampLog.pollFirst();
        }
        if(currTimeStampLog.size() + 1 <= policy.limit())
        {
            currTimeStampLog.add(now);
            return new SlidingWindowLogState(currTimeStampLog,true);
        }
        return new SlidingWindowLogState(currTimeStampLog,false);
    }
}
