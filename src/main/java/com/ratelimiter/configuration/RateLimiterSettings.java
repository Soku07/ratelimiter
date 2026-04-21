package com.ratelimiter.configuration;

import com.ratelimiter.ConstEnum;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class RateLimiterSettings {
    //volatile always reads from main ram thus a latest value is read.
    //volatile also adds a latency as a thread has to read from main ram .
    //if the enabled is non volatile a change in setting is not guaranteed to apply
    //Removing volatile now because the api to change this settings is not exposed yet
    private  boolean enabled = true;
    private boolean allowRequestOnMatchingRuleNotFound = true;
    private  boolean byPassOnException = false;
    private double burstFactor = ConstEnum.BURST_FACTOR;

}
