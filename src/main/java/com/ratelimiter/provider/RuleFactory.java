package com.ratelimiter.provider;

import com.ratelimiter.model.AbstractRule;
import com.ratelimiter.model.AntPathRule;
import com.ratelimiter.model.RateLimitPolicy;
import org.springframework.stereotype.Component;

@Component
public class RuleFactory {
    //This is the only class that knows which concrete class of AbstractRule is in use
    public AbstractRule createRule(String path, int priority, RateLimitPolicy policy) {
        return new AntPathRule(path, priority, policy);
    }
}
