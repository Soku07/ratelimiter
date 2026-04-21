package com.ratelimiter.resolver;

import com.ratelimiter.model.RateLimitSpecs;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class IPResolver implements IdentityResolver {

    private static final int MAX_IP_LENGTH = 100;
    @Override
    public Optional<String> resolve(HttpServletRequest request) {
        if (request == null) return Optional.empty();
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() > MAX_IP_LENGTH || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return Optional.ofNullable(ip)
                .map(s -> s.contains(",") ? s.split(",")[0] : s)
                .map(String::trim)
                .filter(s -> s.length() <= MAX_IP_LENGTH)
                .filter(s -> !s.isBlank());
    }

    @Override
    public RateLimitSpecs.Identity getIdentityType() {
        return RateLimitSpecs.Identity.IP_ADDRESS;
    }
}
