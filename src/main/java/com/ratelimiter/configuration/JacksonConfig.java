package com.ratelimiter.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

@Configuration
public class JacksonConfig {
    @Bean(name = "yamlObjectMapper")
    public ObjectMapper yamlObjectMapper(){
        return new ObjectMapper(new YAMLFactory());
    }
}
