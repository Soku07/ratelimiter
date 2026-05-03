package com.ratelimiter.exceptions.applicationexceptions;

import com.ratelimiter.exceptions.serversideexceptions.InfrastructureException;

public abstract class RateLimitConfigException extends InfrastructureException {
    public RateLimitConfigException(String message) { super(message); }
    public RateLimitConfigException(String message, Throwable cause) { super(message, cause); }
}
