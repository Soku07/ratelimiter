package com.ratelimiter.exceptions;

public class StorageException extends InfrastructureException{

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
