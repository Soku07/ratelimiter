package com.ratelimiter.provider;

import com.ratelimiter.model.AbstractRule;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PolicyRegistry {
    private final RuleProvider ruleProvider;
    private final  RuleStoreFactory ruleStoreFactory;

    private RuleStore activeRuleStore;
    public PolicyRegistry(RuleProvider ruleProvider, RuleStoreFactory ruleStoreFactory) {
        this.ruleProvider = ruleProvider;
        this.ruleStoreFactory = ruleStoreFactory;
    }
    @PostConstruct
    public void init() {
        refreshRules();
    }
    public synchronized void refreshRules(){
        List<AbstractRule> rules = ruleProvider.loadRuleStore();
        this.activeRuleStore =  ruleStoreFactory.createRuleStore(rules);
    }

    public Optional<AbstractRule> getBestMatch(String requestPath){
        return activeRuleStore.findBestMatch(requestPath);
    }
}
