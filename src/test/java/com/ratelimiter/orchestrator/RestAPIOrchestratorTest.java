package com.ratelimiter.orchestrator;

import com.ratelimiter.algorithm.RateLimitAlgorithm;
import com.ratelimiter.configuration.RateLimiterSettings;
import com.ratelimiter.keyfactory.KeyFactory;
import com.ratelimiter.model.AbstractRule;
import com.ratelimiter.model.AntPathRule;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitSpecs;
import com.ratelimiter.provider.PolicyRegistry;
import com.ratelimiter.resolver.IdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
public class RestAPIOrchestratorTest {

    /*
        Test scenarios :
        Follow RateLimiterSettings properties.
        Matching policy not found, follow isAllowRequestOnMatchingRuleNotFound setting
        Identity not found -> throws MissingIdentityException
     */

    @Mock private PolicyRegistry  policyRegistry;
    @Mock private KeyFactory keyFactory;
    @Mock private RateLimiterSettings rateLimiterSettings;
    @Mock private RateLimitAlgorithm  rateLimitAlgorithm;
    @Mock private IdentityResolver identityResolver;
    @Mock private HttpServletRequest request;
    private final AbstractRule rule = new AntPathRule("/api/v1/get-users",5,null);;

    private RestAPIOrchestrator  restAPIOrchestrator;

    @BeforeEach
    void setUp() {
        when(rateLimitAlgorithm.getAlgorithmType()).thenReturn(RateLimitSpecs.Algorithm.TOKEN_BUCKET);
        when(identityResolver.getIdentityType()).thenReturn(RateLimitSpecs.Identity.IP_ADDRESS);

        restAPIOrchestrator = new RestAPIOrchestrator(policyRegistry,keyFactory, List.of(rateLimitAlgorithm),List.of(identityResolver),rateLimiterSettings);
    }

    @Test
    void shouldReturnFalse_whenIdentityIsMissing(){
        RateLimitPolicy policy = new RateLimitPolicy(5, Duration.ofMinutes(1), RateLimitSpecs.Algorithm.TOKEN_BUCKET, RateLimitSpecs.Identity.IP_ADDRESS);
        when(rateLimiterSettings.isEnabled()).thenReturn(true);
        when(policyRegistry.getBestMatch("/api/get-users")).thenReturn(Optional.of(rule));
        boolean result = restAPIOrchestrator.isAllowed("/api/get-users",request);
        assertFalse(result);

    }

}
