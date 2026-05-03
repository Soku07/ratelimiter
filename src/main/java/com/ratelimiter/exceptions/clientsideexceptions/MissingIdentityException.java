package com.ratelimiter.exceptions.clientsideexceptions;

public class MissingIdentityException extends BadRequestException{
    public MissingIdentityException(String detail) {
        super("Required identity missing: " + detail);
    }
    public MissingIdentityException(String detail, Throwable cause) {
        super("Required identity missing: " + detail, cause);
    }
    public MissingIdentityException(String strategy, String rulePattern, String requestUri) {
        super(String.format("Identity missing for request [%s]. Strategy [%s] required by rule [%s]",
                requestUri, strategy, rulePattern));

    }
}
