package com.ratelimiter.provider;

import com.ratelimiter.model.AbstractRule;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;


//This interface is created to implement different strategies to find the best match.
// For version 1, the easiest thing is to store the rules in sorted order as per length and find the first match with higher priority
// Later we can use optimised data structures for path matching
public interface RuleStore {
   Optional<AbstractRule> findBestMatch(String requestPath);
}
