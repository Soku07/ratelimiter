package com.ratelimiter.exceptions.applicationexceptions;

public class RuleDataSourceException extends RateLimitConfigException{
    public RuleDataSourceException(String dataSourceType,String message,Throwable cause){
        super(String.format("[%s] : %s", dataSourceType, message), cause);
    }
    public RuleDataSourceException(String message, Throwable cause){
        super(message,cause);
    }
}
