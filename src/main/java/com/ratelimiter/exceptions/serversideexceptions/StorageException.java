package com.ratelimiter.exceptions.serversideexceptions;

public class StorageException extends InfrastructureException{
    public StorageException(String storageType, String key, String message, Throwable cause) {
        super(String.format("[%s] Key: %s | %s", storageType, key, message), cause);

    }
    public StorageException(String message, Throwable cause) { super(message, cause); }
}
