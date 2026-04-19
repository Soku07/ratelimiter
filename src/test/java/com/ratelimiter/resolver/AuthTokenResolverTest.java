package com.ratelimiter.resolver;

import com.ratelimiter.model.RateLimitSpecs;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AuthTokenResolverTest {
    private AuthTokenResolver authTokenResolver;
    private HttpServletRequest request;

    @BeforeEach
    public void setup(){
        authTokenResolver = new AuthTokenResolver();
        request = mock(HttpServletRequest.class);
    }

    @Test
    public void shouldHandleNULLRequest(){
        assertTrue(authTokenResolver.resolve(null).isEmpty());
        when(request.getHeader("Authorization")).thenReturn("");
        assertTrue(authTokenResolver.resolve(request).isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
            "abcdefg, abcdefg",         // Simple token
            "' abcdefg ', abcdefg",     // Trimming
            "'Bearer xyz123', xyz123",  // Standard format
            "'Token only-token', only-token" // Single part
    })
    void shouldResolveCorrectToken(String authToken, String expected){
        when(request.getHeader("Authorization")).thenReturn(authToken);
        Optional<String> token = authTokenResolver.resolve(request);
        assertEquals(Optional.of(expected), token);
    }
    @Test
    void shouldReturnCorrectType() {
        assertEquals(RateLimitSpecs.Identity.AUTH_TOKEN,authTokenResolver.getIdentityType());

    }
}
