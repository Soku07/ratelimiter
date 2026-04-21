package com.ratelimiter.provider;

import com.ratelimiter.model.AbstractRule;
import com.ratelimiter.model.AntPathRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


class OrderedListRuleStoreTest {

    @Test
    void AreRulesSorted (){
        AbstractRule low = new AntPathRule(
            "/api/**",10,null
        );
        AbstractRule high = new AntPathRule(
                "/api/user/getUser",30,null
        );
        AbstractRule medium = new AntPathRule(
                "/api/user/saveUser",20,null
        );
        OrderedListRuleStore orderedListRuleStore = new OrderedListRuleStore(List.of(low,medium,high));
        List<AbstractRule> sortedRules = orderedListRuleStore.sortedRules();
        assertThat(orderedListRuleStore.sortedRules()).containsExactly(high,medium,low);
    }

    @Test
    void shouldReturnMatchingRuleWithHighestPriority(){
        AbstractRule r1 = new AntPathRule(
                "/api/**",10000,null
        );
        AbstractRule r2 = new AntPathRule(
                "/api/user/getUser",30,null
        );
        AbstractRule r3 = new AntPathRule(
                "/api/user/saveUser",20,null
        );
        AbstractRule r4 = new AntPathRule(
                "/api/user/saveUser",100,null
        );
        OrderedListRuleStore orderedListRuleStore = new OrderedListRuleStore(List.of(r1,r3,r2,r4));
        assertThat(orderedListRuleStore.findBestMatch("/api/user/getUser")).contains(r1);
        assertThat(orderedListRuleStore.findBestMatch("/api/user/deleteUser")).contains(r1);
        assertThat(orderedListRuleStore.findBestMatch("/api/user/saveUser")).contains(r1);
    }

    @Test
    void shouldReturnMatchingRule(){
        AbstractRule r1 = new AntPathRule(
                "/api/**",10,null
        );
        AbstractRule r2 = new AntPathRule(
                "/api/user/getUser",30,null
        );
        AbstractRule r3 = new AntPathRule(
                "/api/user/saveUser",30,null
        );

        OrderedListRuleStore orderedListRuleStore = new OrderedListRuleStore(List.of(r1,r3,r2));
        assertThat(orderedListRuleStore.findBestMatch("/api/user/getUser")).contains(r2);
        assertThat(orderedListRuleStore.findBestMatch("/api/user/deleteUser")).contains(r1);
        assertThat(orderedListRuleStore.findBestMatch("/api/user/saveUser")).contains(r3);

    }
}
