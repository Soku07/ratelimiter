package com.ratelimiter.aspects;

import com.ratelimiter.exceptions.InfrastructureException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ProviderExceptionAspect {
    @AfterThrowing(pointcut = "execution(* com.ratelimiter.provider.*.*(..))", throwing = "ex")
    public void handleProviderException(Exception ex) {
        log.error(ex.getMessage(), ex);
        throw new InfrastructureException("Failed to initialise provider", ex);
    }
}
