package com.ratelimiter.algorithm;

import com.ratelimiter.exceptions.StorageException;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitSpecs;
import com.ratelimiter.storageprovider.StorageProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TokenBucketAlgorithmTest {

    private static final String USER_KEY = "userKey";
    private static final String ADMIN_KEY = "adminKey";


    private static final RateLimitPolicy STANDARD_POLICY = new RateLimitPolicy(10, Duration.ofMinutes(1), RateLimitSpecs.Algorithm.TOKEN_BUCKET, RateLimitSpecs.Identity.IP_ADDRESS);

    private TokenBucketAlgorithm tokenBucketAlgorithm;
    private StorageProvider  mockStorage;

    @BeforeEach
    void setUp() {
        mockStorage = Mockito.mock(StorageProvider.class);
        tokenBucketAlgorithm = new TokenBucketAlgorithm(mockStorage);
    }
    @Test
    void testInitialRequest (){
        mockStorageExecution(null);
        boolean allowed = tokenBucketAlgorithm.isAllowed(USER_KEY,STANDARD_POLICY);
        assertTrue(allowed);
        verify(mockStorage, times(1)).atomicCompute(any(),any(),any(),any(),any());
    }

    @Test
    void testRequestAllowed (){
        mockStorageExecution(new TokenBucketState(10, 12345678,false));
        boolean allowed = tokenBucketAlgorithm.isAllowed(USER_KEY,STANDARD_POLICY);
        assertTrue(allowed);
    }
    // The count depends on burst factor. So apply that logic
//    @Test
//    void checkIfTokensAreRefilled() {
//
//        TokenBucketState newState = tokenBucketAlgorithm.applyTokenBucketAlgorithm(USER_KEY,new TokenBucketState(1, System.currentTimeMillis() - 60000,false),STANDARD_POLICY);
//
//        assertTrue(newState.tokenCount() >= 10 && newState.tokenCount() < 11);
//
//    }
//
//    @Test
//    void checkIfFalseWhenTokensAreExhausted () {
//        TokenBucketState newState = tokenBucketAlgorithm.applyTokenBucketAlgorithm(USER_KEY,new TokenBucketState(0.5, System.currentTimeMillis() - 6,false),STANDARD_POLICY);
//        assertTrue(newState.tokenCount() > 0.5 && newState.tokenCount() < 0.6 && !newState.isAllowed());
//    }

    @Test
    void methodInvocationInCaseOfStorageFailure () {
        when(mockStorage.atomicCompute(any(),any(),any(),any(),any())).thenThrow(new StorageException("Storage failed",null));
        assertThrows(StorageException.class, () -> {
            tokenBucketAlgorithm.isAllowed(USER_KEY, STANDARD_POLICY);
        });

    }
    private void mockStorageExecution(TokenBucketState stateToMock){
        when(mockStorage.atomicCompute(anyString(),any(),any(),any(BiFunction.class),any()))
                .thenAnswer(invocation -> {
                    BiFunction<String, TokenBucketState,TokenBucketState> logicPassedAsFunction = invocation.getArgument(3);
                    return  logicPassedAsFunction.apply(invocation.getArgument(0),stateToMock);
                });
    }

}
