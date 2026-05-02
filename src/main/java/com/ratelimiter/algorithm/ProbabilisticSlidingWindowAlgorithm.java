package com.ratelimiter.algorithm;

import com.ratelimiter.ConstEnum;
import com.ratelimiter.model.RateLimitContext;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitSpecs;
import com.ratelimiter.storageprovider.StorageProvider;
import org.springframework.stereotype.Component;

import static java.lang.Math.max;

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
                (k, currentState) -> applyProbabilisticSlidingWindowAlgorithm(k,(ProbabilisticSlidingWindowState) currentState, policy),
                policy.window()
        );
        return rateLimitDecision.isAllowed();
    }

    @Override
    public RateLimitSpecs.Algorithm getAlgorithmType() {
        return RateLimitSpecs.Algorithm.PROBABILISTIC_SLIDING_WINDOW;
    }

    /*
       Cases to be considered :
       Fresh start -> Old state is null or current bucket is invalid and currentTimeStamp is greater than curr bucket's end time stamp + window size
                       In this case the curr bucket cannot become the prev bucket so its like a fresh start
       Current bucket is invalid (now > bucket.endTimeStamp)
    */
    protected  RateLimitDecision applyProbabilisticSlidingWindowAlgorithm(String key, ProbabilisticSlidingWindowState state, RateLimitPolicy policy){
        long now = System.currentTimeMillis();
        long windowMs = policy.window().toMillis();
        if (state == null || now > state.currBucket().timeStampEnd() + windowMs) {
            Bucket newBucket = new Bucket(1, now, now + windowMs);
            return new ProbabilisticSlidingWindowState(newBucket, null, true);
        }

        Bucket curr = state.currBucket();
        Bucket prev = state.prevBucket();

        if (now > curr.timeStampEnd()) {
            prev = curr;
            curr = null;
        }
        double weightedPrevCount = 0;
        if (prev != null) {
            double weight = Math.max(0, 1.0 - (double)(now - prev.timeStampEnd()) / windowMs);
            weightedPrevCount = prev.counter() * weight;
        }
        long currentCount = (curr != null) ? curr.counter() : 0;
        double totalPredictedRequestCount = currentCount + weightedPrevCount + 1;

        if(totalPredictedRequestCount <= policy.limit()){
            Bucket updatedCurr;
            if (curr == null) {
                updatedCurr = new Bucket(1, now, now + windowMs);
            } else {
                updatedCurr = new Bucket(curr.counter() + 1, curr.timeStampCreated(), curr.timeStampEnd());
            }
            return new ProbabilisticSlidingWindowState(updatedCurr, prev, true);
        }
        return new ProbabilisticSlidingWindowState(
                curr != null ? curr : new Bucket(0, now, now + windowMs),
                prev,
                false
        );

    }


    //Manual iteration that tries to handle cases and edge cases

//    protected RateLimitDecision applyProbabilisticSlidingWindowAlgorithm(String key, ProbabilisticSlidingWindowState oldBucketState, RateLimitPolicy policy) {
//        long requestCount = 1;
//        boolean isAllowed = false;
//        long now = System.currentTimeMillis();
//        if(oldBucketState == null){
//            Bucket currBucket = new Bucket(requestCount,now,now+policy.window().toMillis());
//
//            //Application handles that limit is always greater than or equal to 1. Should we handle for safety?
//            return new ProbabilisticSlidingWindowState(currBucket,null,true);
//        }
//        if(oldBucketState.prevBucket() == null ){ //currBucket is never null. Either entire old state is null or prevBucket is null. currBucket cannot be null because this is the only function that handles set and no where currBucket is explicitly set to null
//            if(now >= oldBucketState.currBucket().timeStampCreated() && now <= oldBucketState.currBucket().timeStampEnd()){
//                requestCount += oldBucketState.currBucket().counter();
//                if(requestCount <= policy.limit()){
//                    Bucket currBucket = new Bucket(requestCount, oldBucketState.currBucket().timeStampCreated(),oldBucketState.currBucket().timeStampEnd());
//                    return new ProbabilisticSlidingWindowState(currBucket, null, true);
//                }
//                return new ProbabilisticSlidingWindowState(oldBucketState.currBucket(), null, false);
//
//            }
//            // At this line now > currBucket.timeStampEnd
//            if(now > oldBucketState.currBucket().timeStampEnd() + policy.window().toMillis()){ //curr Bucket cannot become prev bucket
//                Bucket currBucket = new Bucket(requestCount,now,now+policy.window().toMillis());
//                //Application handles that limit is always greater than or equal to 1. Should we handle for safety?
//                return new ProbabilisticSlidingWindowState(currBucket,null,true);
//            }
//            long historicalCount = oldBucketState.prevBucket().counter() * max(0,(1 -((now - oldBucketState.prevBucket().timeStampEnd())/policy.window().toMillis())));
//
//            if(requestCount + historicalCount <= policy.limit()){
//                Bucket currBucket = new Bucket(requestCount, now,now + policy.window().toMillis());
//                return new ProbabilisticSlidingWindowState(currBucket,oldBucketState.currBucket(),true);
//            }
//            Bucket currBucket = new Bucket(0, now,now + policy.window().toMillis());
//
//            return new ProbabilisticSlidingWindowState(currBucket,oldBucketState.currBucket(),true);
//
//        }
//        // this line means currbucket and prev bucket both exist
//        // Curr bucket is invalid and the curr bucket can also not become prev bucket
//        if(now > oldBucketState.currBucket().timeStampEnd() + policy.window().toMillis()){
//            Bucket currBucket = new Bucket(requestCount,now,now+policy.window().toMillis());
//
//            //Application handles that limit is always greater than or equal to 1. Should we handle for safety?
//            return new ProbabilisticSlidingWindowState(currBucket,null,true);
//        }
//        //curr bucket is invalid
//        if(now > oldBucketState.currBucket().timeStampEnd()){
//            long historicalCount = oldBucketState.prevBucket().counter() * max(0,(1 -((now - oldBucketState.prevBucket().timeStampEnd())/policy.window().toMillis())));
//            if(requestCount + historicalCount <= policy.limit()){
//                Bucket currBucket = new Bucket(requestCount, now,now + policy.window().toMillis());
//                return new ProbabilisticSlidingWindowState(currBucket,oldBucketState.currBucket(),true);
//            }
//            Bucket currBucket = new Bucket(0, now,now + policy.window().toMillis());
//
//            return new ProbabilisticSlidingWindowState(currBucket,oldBucketState.currBucket(),true);
//
//        }
//        if(now > oldBucketState.prevBucket().timeStampEnd() && now < oldBucketState.currBucket().timeStampEnd()){
//            long historicalCount = oldBucketState.prevBucket().counter() + max(0,(1 - ((now - oldBucketState.prevBucket().timeStampEnd())/policy.window().toMillis())));
//            if(requestCount + oldBucketState.currBucket().counter() + historicalCount <= policy.limit()){
//                Bucket currBucket = new Bucket(requestCount + oldBucketState.currBucket().counter() + historicalCount,oldBucketState.currBucket().timeStampCreated(),oldBucketState.currBucket().timeStampEnd());
//                return new ProbabilisticSlidingWindowState(currBucket,oldBucketState.prevBucket(),true);
//            }
//            return  new ProbabilisticSlidingWindowState(oldBucketState.currBucket(),oldBucketState.prevBucket(),false);
//        }
//
//
//    }
}
