package com.ratelimiter.provider;

import com.ratelimiter.model.AbstractRule;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public record OrderedListRuleStore(List<AbstractRule> sortedRules) implements RuleStore {
    public OrderedListRuleStore(List<AbstractRule> sortedRules) {
        if(sortedRules == null || sortedRules.isEmpty()){
            throw new IllegalArgumentException("Cannot create OrderedListRuleStore with empty list");
        }
        this.sortedRules = sortedRules.stream().sorted(
                Comparator.comparingInt(AbstractRule::getPriority).reversed()

        ).toList();
    }

    @Override
    public Optional<AbstractRule> findBestMatch(String requestPath) {
        return sortedRules.stream().filter(rule -> rule.matches(requestPath)).findFirst();
    }
}
