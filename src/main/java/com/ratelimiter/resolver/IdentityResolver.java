package com.ratelimiter.resolver;

import com.ratelimiter.model.RateLimitSpecs;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

public interface IdentityResolver {
    Optional<String> resolve(HttpServletRequest request);
    RateLimitSpecs.Identity getIdentityType();

}
