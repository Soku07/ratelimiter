package com.ratelimiter.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public abstract class AbstractRule {
    String pathPattern;
    int priority;
    RateLimitPolicy policy;

    public abstract boolean matches(String requestPath);
}
