package com.ratelimiter.exceptions;

public class InvalidPolicyException extends RuntimeException {
    public InvalidPolicyException(String message, Throwable cause) {
        super(message, cause);
    }
}
