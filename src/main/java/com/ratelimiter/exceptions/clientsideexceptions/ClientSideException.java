package com.ratelimiter.exceptions.clientsideexceptions;

import com.ratelimiter.exceptions.HTTPStatusProvider;
import com.ratelimiter.exceptions.RateLimitException;

public abstract class ClientSideException extends RateLimitException implements HTTPStatusProvider {
    public ClientSideException(String message) { super(message); }
    public ClientSideException(String message, Throwable cause) { super(message, cause); }
}
