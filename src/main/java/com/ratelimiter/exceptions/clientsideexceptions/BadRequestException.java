package com.ratelimiter.exceptions.clientsideexceptions;

import com.ratelimiter.exceptions.HTTPStatusProvider;
import com.ratelimiter.exceptions.RateLimitException;
import org.springframework.http.HttpStatus;

public class BadRequestException extends ClientSideException{
    public BadRequestException(String message) { super(message); }
    public BadRequestException(String message, Throwable cause) { super(message, cause); }
    @Override
    public int getStatus() { return HttpStatus.BAD_REQUEST.value(); }
}
