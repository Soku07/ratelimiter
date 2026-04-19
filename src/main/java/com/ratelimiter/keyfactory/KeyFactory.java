package com.ratelimiter.keyfactory;

import com.ratelimiter.model.AbstractRule;

public interface KeyFactory {
    String getKey(AbstractRule rule, String identity);
}
