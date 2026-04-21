package com.ratelimiter.interceptor;

import com.ratelimiter.orchestrator.RateLimitOrchestrator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

@Component

@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {
    private final RateLimitOrchestrator rateLimitOrchestrator;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        String path = request.getRequestURI();
        boolean isAllowed = rateLimitOrchestrator.isAllowed(path,request);
        if(!isAllowed){
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            try
            {
                response.getWriter().write("{\"error\": \"Quota Exceeded\", \"message\": \"Please slow down.\"}");

            }
            catch (IOException e){
                return false;
            }
            return false;

        }
        return true;
    }


}
