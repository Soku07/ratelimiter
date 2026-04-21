package com.ratelimiter.exceptions;

public class MissingIdentityException extends RuntimeException{
    public MissingIdentityException(String message) {
        super(message);
    }
    public MissingIdentityException( String strategy, String rulePath, String requestPath) {
        super(String.format("Could not resolve identity using [%s] | Rule Pattern: [%s] | Actual Request: [%s]",
                 strategy,rulePath, requestPath));
    }

    public MissingIdentityException(String strategy, String path) {
        super(String.format("Could not resolve identity using [%s] for resource [%s]", strategy, path));
    }
}
