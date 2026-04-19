package com.ratelimiter.resolver;

import com.ratelimiter.model.RateLimitSpecs;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuthTokenResolver implements IdentityResolver {

    private static  final int MAX_TOKEN_LENGTH = 2048;
    @Override
    public Optional<String> resolve(HttpServletRequest request) {
       //Auth header format (Standard): "<Scheme> <token>"

       if(request == null) return Optional.empty();
       return Optional.ofNullable(request.getHeader("Authorization"))
               .filter(auth -> !auth.isBlank() && auth.length() <= MAX_TOKEN_LENGTH)
               .map(String::trim)
               .map(token -> {
                   String[] parts = token.split("\\s+");
                   return parts.length >= 2 ?  parts[1] : parts[0];

               })
               .filter(token -> !token.isBlank());


    }

    @Override
    public RateLimitSpecs.Identity getIdentityType() {
        return RateLimitSpecs.Identity.AUTH_TOKEN;
    }
}
