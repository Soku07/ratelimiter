package com.ratelimiter.provider;

import com.ratelimiter.model.AbstractRule;

import java.util.List;

public interface RuleProvider {
    public List<AbstractRule> loadRuleStore();
}
