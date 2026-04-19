package com.ratelimiter.orchestrator;

import com.ratelimiter.algorithm.RateLimitAlgorithm;
import com.ratelimiter.configuration.RateLimiterSettings;
import com.ratelimiter.exceptions.InfrastructureException;
import com.ratelimiter.exceptions.MissingIdentityException;
import com.ratelimiter.keyfactory.KeyFactory;
import com.ratelimiter.model.AbstractRule;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitSpecs;
import com.ratelimiter.provider.PolicyRegistry;
import com.ratelimiter.resolver.IdentityResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class RestAPIOrchestrator implements RateLimitOrchestrator {

    private final PolicyRegistry policyRegistry;
    private final KeyFactory keyFactory;
    private final Map<RateLimitSpecs.Algorithm, RateLimitAlgorithm> rateLimitAlgorithms;
    private final Map<RateLimitSpecs.Identity, IdentityResolver> identityResolvers;
    private final RateLimiterSettings rateLimiterSettings;


    public RestAPIOrchestrator(PolicyRegistry policyRegistry, KeyFactory keyFactory , List<RateLimitAlgorithm> rateLimitAlgorithms, List<IdentityResolver> identityResolvers, RateLimiterSettings rateLimiterSettings) {

        this.policyRegistry = policyRegistry;
        this.keyFactory = keyFactory;

        //using .collect because we have list injected to the constructor which has all the implementations of the said interface.
        //We need to keep them all but in a Map.

        this.rateLimitAlgorithms = rateLimitAlgorithms.stream()
                .collect(Collectors.toMap(RateLimitAlgorithm::getAlgorithmType, a->a));
        this.identityResolvers = identityResolvers.stream()
                .collect(Collectors.toMap(IdentityResolver::getIdentityType, a->a));
        this.rateLimiterSettings = rateLimiterSettings;
    }

    private boolean processRule(HttpServletRequest request, AbstractRule rule) {
        try {
            RateLimitPolicy policy = rule.getPolicy();
            String identity = identityResolvers.get(policy.identityStrategy())
                    .resolve(request)
                    .orElseThrow(()-> new MissingIdentityException(policy.identityStrategy().toString(), rule.getPathPattern(),request.getRequestURI()));

            String key = keyFactory.getKey(rule, identity);
            return rateLimitAlgorithms.get(policy.algorithm()).isAllowed(key,policy);
        }
        catch (IllegalArgumentException | MissingIdentityException e) {
            log.error("Rate limit blocked due to invalid request/identity: {}", e.getMessage());
            return false;
        }
        catch (InfrastructureException e){
            log.error("Storage/Infra failure. Applying bypass [{}]: {}",
                    rateLimiterSettings.isByPassOnException(), e.getMessage(), e);
            return rateLimiterSettings.isByPassOnException();
        }
        catch (Exception e) {
            log.error("Critical unexpected error in rate limiter logic", e);
            return false;
        }

    }

    //Code improvised with Modern Java syntax
    @Override
    public boolean isAllowed(String resourcePath, Object requestSource) {
        HttpServletRequest request = (HttpServletRequest) requestSource;
        return policyRegistry.getBestMatch(resourcePath)
                .map(rule -> processRule(request,rule) )
                .orElseGet(rateLimiterSettings::isAllowRequestOnMatchingRuleNotFound);
    }

    // Code written by me
//    @Override
//    public boolean isAllowed(String resourcePath, Object requestSource) {
//        try {
//            HttpServletRequest  httpServletRequest = (HttpServletRequest) requestSource;
//            Optional<AbstractRule> matchingRule = policyRegistry.getBestMatch(httpServletRequest.getRequestURI());
//
//            AbstractRule rule = matchingRule.orElse(null);
//            if(rule == null){
//                return rateLimiterSettings.isAllowRequestOnMatchingRuleNotFound();
//            }
//            RateLimitPolicy policy = rule.getPolicy();
//
//            Optional<String> identity = identityResolvers.get(policy.identityStrategy()).resolve(httpServletRequest);
//            // Null input of identity thorws IllegalArgExp
//            String key = keyFactory.getKey(rule,identity.get());
//            // Key is always avalible after this line and so the implementation isAllowed does not handle case when key or policy is null and it just calls the storage and applies algorithm
//
//            boolean isAllowed = false;
//            isAllowed = rateLimitAlgorithms.get(policy.algorithm()).isAllowed(key,policy);
//            return isAllowed;
//
//        }
//        catch (IllegalArgumentException e) {
//            return false;
//        }
//        catch (StorageException  ste ){
//            log.error("Storage unavailable. Bypass setting [{}]: {}",
//                    rateLimiterSettings.isByPassOnException(), ste.getMessage(), ste);
//            return rateLimiterSettings.isByPassOnException();
//        }
//        catch (Exception e) {
//            log.error("Unknown error");
//            return false;
//        }
//    }
}
