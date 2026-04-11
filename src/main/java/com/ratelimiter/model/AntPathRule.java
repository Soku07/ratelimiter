package com.ratelimiter.model;

import org.springframework.util.AntPathMatcher;

public class AntPathRule extends AbstractRule{

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    public AntPathRule(String pathPattern,int priority, RateLimitPolicy policy ) {
        super(pathPattern, priority, policy);

    } 
    @Override
    public boolean matches(String requestPath) {
        if(requestPath == null){
            return false;
        }
        return MATCHER.match(this.pathPattern, requestPath);
    }
}
