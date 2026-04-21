package com.ratelimiter.provider;

import lombok.Data;

import java.util.List;

@Data
public class PolicyInputDTO {
    private List<ClientConfigDTO> clients;
}

@Data
class ClientConfigDTO{
    private String clientID;
    private List<RuleDTO> rules;
}
@Data
class RuleDTO{
    private String pathPattern;
    private String priority;
    private PolicyDTO policy;
}

@Data
class PolicyDTO{
    private int limit;
    private String window;
    private String algorithmKey;
    private String identityStrategy;
}
