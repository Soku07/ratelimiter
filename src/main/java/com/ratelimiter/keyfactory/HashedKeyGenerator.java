package com.ratelimiter.keyfactory;

import com.ratelimiter.model.AbstractRule;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class HashedKeyGenerator implements KeyFactory{
    private static final String PREFIX = "rl";
    @Override
    public String getKey(AbstractRule rule, String identity) {
        Assert.isTrue(rule != null && StringUtils.hasText(rule.getPathPattern()) && StringUtils.hasText(identity), "Invalid rate limit request: Rule and Identity are required");
        String pathAndIdentityCombinedText = rule.getPathPattern() + ":" + identity;
        String hashedKey = hash(pathAndIdentityCombinedText);
        return String.format("%s:%s", PREFIX, hashedKey);
    }

    String hash(String input){
        return DigestUtils.sha256Hex(input);
    }
}
