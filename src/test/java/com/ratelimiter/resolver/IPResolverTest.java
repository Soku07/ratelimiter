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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IPResolverTest {

    private IPResolver ipResolver;
    private HttpServletRequest request;
    @BeforeEach
    void setUp() {
        ipResolver = new IPResolver();
        request = mock(HttpServletRequest.class);
    }
    ///Test Cases :
    /// null request
    /// Request with no IP
    /// Request having IP
    @Test
    void shouldReturnEmptyWhenRequestIsNull() {
        assertTrue(ipResolver.resolve(null).isEmpty());
    }
    @Test
    void shouldReturnRemoteAddrWhenHeaderIsNULL() {
        String ip = "127.0.0.1";
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);
        assertTrue(ipResolver.resolve(request).isEmpty());
        when(request.getRemoteAddr()).thenReturn(ip);
        Optional<String> result = ipResolver.resolve(request);
        assertEquals(ipResolver.resolve(request),result);
    }

    @ParameterizedTest
    @CsvSource({
            "192.168.1.1, 192.168.1.1",
            "'192.168.1.1, 192.168.1.2, 1.1.1.1',192.168.1.1 ",
            "'', 1.1.1.1",
            "'unknown', '1.1.1.1"

    })
    void shouldResolveCorrectIP(String headerValue, String expected){
        String ip = "1.1.1.1";
        when(request.getHeader(argThat("x-forwarded-for"::equalsIgnoreCase)))
                .thenReturn(headerValue);
        when(request.getRemoteAddr()).thenReturn(ip);
        Optional<String> result = ipResolver.resolve(request);
        assertEquals(Optional.of(expected.trim()),result);

    }
    @Test
    void shouldIgnoreLongInputs(){
        String ip = "a".repeat(101);
        when(request.getHeader(argThat("x-forwarded-for"::equalsIgnoreCase))).thenReturn(ip);
        when(request.getRemoteAddr()).thenReturn(ip);
        Optional<String> result = ipResolver.resolve(request);
        assertEquals(Optional.empty(),result);
    }

    @Test
    void shouldReturnCorrectType(){
        assertEquals(RateLimitSpecs.Identity.IP_ADDRESS, ipResolver.getIdentityType());
    }


}
