package com.ratelimiter.interceptor;

import com.ratelimiter.model.RateLimitDecision;
import com.ratelimiter.orchestrator.RateLimitOrchestrator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component

@RequiredArgsConstructor
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimitOrchestrator rateLimitOrchestrator;

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler){
        String path = request.getRequestURI();
        RateLimitDecision decision = rateLimitOrchestrator.isAllowed(request.getRequestURI(), request);

        if (!decision.allowed()) {
            response.setStatus(decision.httpStatus());
            response.setContentType("application/json");
            try {
                // Using a simple JSON string or a Serializer if available
                String body = String.format("{\"error\": \"%s\", \"message\": \"%s\"}",
                        decision.error(), decision.message());
                response.getWriter().write(body);
            } catch (IOException e) {
                log.error("Failed to write rate limit response", e);
            }
            return false;
        }
        return true;
    }


}
