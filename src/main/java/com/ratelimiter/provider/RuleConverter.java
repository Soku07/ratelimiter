package com.ratelimiter.provider;

import com.ratelimiter.model.AbstractRule;
import com.ratelimiter.model.RateLimitPolicy;
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
        RateLimitPolicy policy = buildPolicy(ruleDTO);
        int priority = calculateEffectivePriority(ruleDTO);

        return ruleFactory.createRule(ruleDTO.getPathPattern(), priority,policy);

    }
    private RateLimitPolicy buildPolicy(RuleDTO ruleDTO) {
        PolicyDTO policy = ruleDTO.getPolicy();
        if(policy.getLimit() < 0){
            throw new IllegalArgumentException("Limit must be greater than or equal to 0 for policy : " + ruleDTO.getPathPattern());
        }
        Duration window = Duration.parse(policy.getWindow());

        //Add validation for Algorithm key and identity strategy also
        return new RateLimitPolicy(
                policy.getLimit(),
                window,
                policy.getAlgorithmKey(),
                policy.getIdentityStrategy()
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
