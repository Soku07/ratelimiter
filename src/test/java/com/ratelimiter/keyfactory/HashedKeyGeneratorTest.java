package com.ratelimiter.keyfactory;

import com.ratelimiter.model.AbstractRule;
import com.ratelimiter.model.AntPathRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HashedKeyGeneratorTest {
    private HashedKeyGenerator hashedKeyGenerator;
    private final String path = "/api/v1/get-user";
    private final String identity = "user-123";
    private final AbstractRule rule = new AntPathRule(path,5,null);;
    @BeforeEach
    void init() {
        hashedKeyGenerator = new HashedKeyGenerator();
    }

    @Test
    void shouldGenerateConsistentHashKey(){
        AbstractRule rule = new AntPathRule(path,5,null);

        String k1 = hashedKeyGenerator.getKey(rule, identity);
        String k2 = hashedKeyGenerator.getKey(rule, identity);
        assertEquals(k1,k2);
        assertEquals(67, k1.length());
    };

    @Test
    void shouldThrowExceptionOnNullInputs (){
        assertThrows(IllegalArgumentException.class, () -> hashedKeyGenerator.getKey(null, identity));
        assertThrows(IllegalArgumentException.class, () -> hashedKeyGenerator.getKey(rule, null));
        assertThrows(IllegalArgumentException.class,()->hashedKeyGenerator.getKey(rule, ""));

    }

}

