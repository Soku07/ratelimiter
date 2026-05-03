package com.ratelimiter.exceptions.serversideexceptions;

import com.ratelimiter.exceptions.HTTPStatusProvider;
import com.ratelimiter.exceptions.RateLimitException;
import org.springframework.http.HttpStatus;

public abstract class InfrastructureException extends RateLimitException implements HTTPStatusProvider {
    public InfrastructureException(String message) { super(message); }
    public InfrastructureException(String message, Throwable cause) { super(message, cause); }

    @Override
    public int getStatus() { return HttpStatus.INTERNAL_SERVER_ERROR.value(); }
}
