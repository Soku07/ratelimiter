package com.ratelimiter.algorithm;

import com.ratelimiter.BaseRateLimiterTest;
import com.ratelimiter.model.RateLimitPolicy;
import com.ratelimiter.model.RateLimitSpecs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class SlidingWindowLogTest extends BaseRateLimiterTest {

    private SlidingWindowLog testSlidingWindowLogWithCaffeine;
    private SlidingWindowLog testSlidingWindowLogWithRedis;

    @BeforeEach
    void initAlgorithms() {
        this.testSlidingWindowLogWithCaffeine = new SlidingWindowLog(caffieneStorageProvider);
        this.testSlidingWindowLogWithRedis = new SlidingWindowLog(redisProvider);
    }

    record TraceStep(int offsetMs, boolean shouldBeAllowed) {}
    record TraceTestData(RateLimitPolicy policy, TraceStep[] steps) {}

    @ParameterizedTest(name = "Running Trace Scenario {index}")
    @MethodSource("provideCustomTraceScenarios")
    void shouldValidateScenariosUsingSameFunction(TraceTestData testData) {
        String key = "trace-user-" + System.nanoTime();
        long startTime = System.currentTimeMillis();

        for (TraceStep step : testData.steps()) {
            sleepUntilOffset(startTime, step.offsetMs());

            String failureMessage = String.format("Failed at timeline marker: %d ms", step.offsetMs());

            assertEquals(step.shouldBeAllowed(), testSlidingWindowLogWithRedis.isAllowed(key, testData.policy()), "Redis: " + failureMessage);
            assertEquals(step.shouldBeAllowed(), testSlidingWindowLogWithCaffeine.isAllowed(key, testData.policy()), "Caffeine: " + failureMessage);
        }
    }

    private static Stream<TraceTestData> provideCustomTraceScenarios() {
        RateLimitPolicy sharedPolicy = new RateLimitPolicy(3, Duration.ofSeconds(1),
                RateLimitSpecs.Algorithm.SLIDING_WINDOW_LOG, RateLimitSpecs.Identity.AUTH_TOKEN);

        RateLimitPolicy shortPolicy = new RateLimitPolicy(1, Duration.ofSeconds(1),
                RateLimitSpecs.Algorithm.SLIDING_WINDOW_LOG, RateLimitSpecs.Identity.AUTH_TOKEN);

        return Stream.of(
                // Scenario 1: Standard capacity exhaust and complete eviction slide
                new TraceTestData(sharedPolicy, new TraceStep[]{
                        new TraceStep(100,  true),
                        new TraceStep(300,  true),
                        new TraceStep(400,  true), // Limit hit
                        new TraceStep(500,  false),
                        new TraceStep(800,  false),
                        new TraceStep(900,  false),
                        new TraceStep(1410, true)  // Logs older than 410ms cleared
                }),

                // Scenario 2: Large time gap with structural skip checking
                new TraceTestData(sharedPolicy, new TraceStep[]{
                        new TraceStep(100,  true),
                        new TraceStep(300,  true),
                        new TraceStep(400,  true),
                        new TraceStep(500,  false),
                        new TraceStep(1410, true)
                }),

                // Scenario 3: Single capacity policy boundary tracing (Fixed timeline logic)
                new TraceTestData(shortPolicy, new TraceStep[]{
                        new TraceStep(100,  true),
                        new TraceStep(1001, false), // Blocked by active 100ms item
                        new TraceStep(2100, true)   // Allowed: window is entirely clear now
                }),
                // --- SCENARIO 4: THE STAGGERED STEP-RECOVERY (The Ultimate Sliding Check) ---
// Policy: 3 req / 1000ms.
// We space requests out so they must expire completely independently, one by one.
                new TraceTestData(sharedPolicy, new TraceStep[]{
                        new TraceStep(100,  true),  // Allowed (1/3)
                        new TraceStep(500,  true),  // Allowed (2/3)
                        new TraceStep(900,  true),  // Allowed (3/3) -> Window full.
                        new TraceStep(1000, false), // Blocked at 1000ms (All three 100, 500, 900 are active)

                        // At 1105ms, the window looks back to 105ms.
                        // The 100ms request has slid out, but 500ms and 900ms remain. Exactly ONE slot opens.
                        new TraceStep(1105, true),  // Allowed (Re-occupies slot 3/3)
                        new TraceStep(1110, false), // Blocked immediately! (500, 900, 1105 are active)

                        // At 1505ms, the window looks back to 505ms.
                        // The 500ms request has slid out. Exactly ONE slot opens again.
                        new TraceStep(1505, true),  // Allowed (Re-occupies slot 3/3)
                        new TraceStep(1510, false)  // Blocked immediately! (900, 1105, 1505 are active)
                }),

// --- SCENARIO 5: THE EXACT MILLISECOND BOUNDARY (The Inclusive/Exclusive Edge) ---
// Policy: 1 req / 1000ms.
// Tests if your algorithm clears a log entry at the *exact* tick its window expires.
                new TraceTestData(shortPolicy, new TraceStep[]{
                        new TraceStep(100,  true),  // Allowed (1/1)
                        // At exactly 1100ms, the delta is exactly 1000ms (1100 - 100 = 1000).
                        // If your Lua script uses strict '>' instead of '>=' for eviction, this will fail.
                        new TraceStep(1100, true),  // Should be ALLOWED because the first request has officially aged out
                        new TraceStep(1150, false)  // Blocked! The 1100ms request is still fresh.
                }),

// --- SCENARIO 6: THE POISON PILL BURST (Testing Log Bloat & Rejection Cost) ---
// Policy: 3 req / 1000ms.
// What happens if a bad actor spams requests while blocked?
// A broken algorithm accidentally saves blocked requests to the ZSet/Cache, delaying future recovery.
                new TraceTestData(sharedPolicy, new TraceStep[]{
                        new TraceStep(100,  true),  // Allowed (1/3)
                        new TraceStep(150,  true),  // Allowed (2/3)
                        new TraceStep(200,  true),  // Allowed (3/3)
                        new TraceStep(300,  false), // Blocked!
                        new TraceStep(400,  false), // Blocked!
                        new TraceStep(500,  false), // Blocked!

                        // At 1150ms, window looks back to 150ms.
                        // The 100ms request is gone. If the blocked requests at 300, 400, 500 were
                        // incorrectly added to your log store, your count will be wrong and this will fail.
                        new TraceStep(1150, true)   // Allowed! (Only 150 and 200 remain active)
                }),

// --- SCENARIO 7: THE ZERO-OFFSET IMPACT BURST ---
// Policy: 3 req / 1000ms.
// Tests how the systems handle two hits occurring on the same relative CPU clock cycle offset.
                new TraceTestData(sharedPolicy, new TraceStep[]{
                        new TraceStep(100,  true),  // Allowed (1/3)
                        new TraceStep(100,  true),  // Allowed (2/3) - Same millisecond mark
                        new TraceStep(200,  true),  // Allowed (3/3) - Window full
                        new TraceStep(200,  false), // Blocked!
                        new TraceStep(1101, true),  // Allowed! (Both 100ms entries have slid out at once)
                        new TraceStep(1101, true),  // Allowed! (Second slot opened up from the twin 100ms entries)
                        new TraceStep(1101, false)  // Blocked! (The 200ms entry is still holding the final slot)
                })

        );
    }

    @Test
    void shouldBlockRequestAtExactLimit() {
        RateLimitPolicy policy = new RateLimitPolicy(3, Duration.ofMinutes(1),
                RateLimitSpecs.Algorithm.SLIDING_WINDOW_LOG, RateLimitSpecs.Identity.AUTH_TOKEN);
        String key = "test-user-123";

        for (int i = 1; i <= policy.limit(); i++) {
            assertTrue(testSlidingWindowLogWithRedis.isAllowed(key, policy), "Request " + i + " should be allowed");
            assertTrue(testSlidingWindowLogWithCaffeine.isAllowed(key, policy), "Request " + i + " should be allowed");
        }

        Boolean redisKeyExistenceCheck = redisTemplate.hasKey(key);
        assertTrue(redisKeyExistenceCheck, "Key must physically exist inside the Redis container infrastructure");

        assertFalse(testSlidingWindowLogWithCaffeine.isAllowed(key, policy), "Caffeine must reject the N+1 request");
        assertFalse(testSlidingWindowLogWithRedis.isAllowed(key, policy), "Redis must reject the N+1 request");
    }

    private void sleepUntilOffset(long startTime, long targetOffsetMs) {
        long elapsed = System.currentTimeMillis() - startTime;
        long timeToWait = targetOffsetMs - elapsed;
        if (timeToWait > 0) {
            try {
                Thread.sleep(timeToWait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}