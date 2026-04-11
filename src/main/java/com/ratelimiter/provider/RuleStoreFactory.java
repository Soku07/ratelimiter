package com.ratelimiter.provider;

import com.ratelimiter.model.AbstractRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleStoreFactory {
    public RuleStore createRuleStore(List<AbstractRule> rules) {
        return new OrderedListRuleStore(rules);
    }
}
