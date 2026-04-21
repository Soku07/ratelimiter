package com.ratelimiter.provider;


import com.ratelimiter.exceptions.InfrastructureException;
import com.ratelimiter.model.AbstractRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
public class YAMLRuleProvider implements RuleProvider{

    private final Resource yamlRuleResourceFile;
    private final RuleConverter ruleConverter;
    private final ObjectMapper objectMapper;

    public YAMLRuleProvider(
            @Value("${ratelimiter.config.path}") Resource yamlRuleResourceFile,
            RuleConverter ruleConverter,

            @Qualifier("yamlObjectMapper") ObjectMapper yamlMapper
    ){
        this.yamlRuleResourceFile = yamlRuleResourceFile;
        this.ruleConverter = ruleConverter;
        this.objectMapper = yamlMapper;
    }

    @Override
    public List<AbstractRule> loadRuleStore() {
        try(InputStream inputStream = yamlRuleResourceFile.getInputStream()){
            PolicyInputDTO policyInputDTO = objectMapper.readValue(inputStream, PolicyInputDTO.class);
            if(policyInputDTO.getClients() == null || policyInputDTO.getClients().isEmpty()) return Collections.emptyList();
            List<AbstractRule> rules = policyInputDTO.getClients().stream()
                    .flatMap(client -> client.getRules().stream() )
                    .map(this::safeConvertRule)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            log.info("Successfully loaded {} rules", rules.size());

            return rules;
        }
        catch (IOException e){
            throw new InfrastructureException("Unable to load YAML file : ",e);
        }



    }

    private AbstractRule safeConvertRule(RuleDTO ruleDTO){
        try {
            return ruleConverter.convertAndValidateRule(ruleDTO);
        }
        catch (Exception e){
            log.error("Skipping malformed rule for path [{}] : {}",ruleDTO.getPathPattern(),e.getMessage());
        }
        return null;
    }


}
