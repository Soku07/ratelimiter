package com.ratelimiter.provider;

import com.ratelimiter.model.AbstractRule;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class OrderedListRuleStore implements  RuleStore{
    private final List<AbstractRule> sortedRules;

    public OrderedListRuleStore(List<AbstractRule> rules) {
        this.sortedRules = rules.stream().sorted(
                Comparator.comparingInt(AbstractRule::getPriority).reversed()

        ).toList();
    }
    @Override
    public Optional<AbstractRule> findBestMatch(String requestPath) {
       return sortedRules.stream().filter(rule -> rule.matches(requestPath)).findFirst();
    }
}
