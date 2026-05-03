package com.ratelimiter.exceptions.applicationexceptions;

public class InvalidPolicyException extends RateLimitConfigException {
    public InvalidPolicyException(String ruleName, String reason) {
        super(String.format("Invalid rule configuration for [%s]: %s", ruleName, reason));
    }
    public InvalidPolicyException(String message, Throwable cause) {
        super(message, cause);
    }

}
