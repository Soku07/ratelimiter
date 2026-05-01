package com.ratelimiter.provider;

import com.ratelimiter.exceptions.InvalidPolicyException;
import com.ratelimiter.model.AbstractRule;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitSpecs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleConverter {
    private final RuleFactory ruleFactory;
    public AbstractRule convertAndValidateRule(RuleDTO ruleDTO) {
        try {
            RateLimitPolicy policy = buildPolicy(ruleDTO);
            int priority = calculateEffectivePriority(ruleDTO);

            return ruleFactory.createRule(ruleDTO.getPathPattern(), priority, policy);
        }
        catch (Exception e) {
            throw new InvalidPolicyException(e.getMessage(), e);
        }
    }
    private RateLimitPolicy buildPolicy(RuleDTO ruleDTO) {
        PolicyDTO policy = ruleDTO.getPolicy();
        if(policy.getLimit() < 1){
            throw new IllegalArgumentException("Limit must be greater than or equal to 1 for policy : " + ruleDTO.getPathPattern());
        }
        Duration window = Duration.parse(policy.getWindow());

        RateLimitSpecs.Algorithm algorithm = RateLimitSpecs.Algorithm.valueOf(policy.getAlgorithmKey());
        RateLimitSpecs.Identity identity = RateLimitSpecs.Identity.valueOf(policy.getIdentityStrategy());
        //Add validation for Algorithm key and identity strategy also
        return new RateLimitPolicy(
                policy.getLimit(),
                window,
                algorithm,
                identity
        );
    }

    private int calculateEffectivePriority(RuleDTO ruleDTO) {
        if(ruleDTO.getPolicy() != null && ruleDTO.getPriority() != null  && !ruleDTO.getPriority().isBlank()){
            try{
                return Integer.parseInt(ruleDTO.getPriority().trim());
            }
            catch (NumberFormatException e){
                log.warn("Invalid priority provided for policy : {}", ruleDTO.getPathPattern());
            }

        }
        return ruleDTO.getPathPattern().length();
    }
}
