package com.ratelimiter.exceptions;

public class InfrastructureException extends RuntimeException{
    public InfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
