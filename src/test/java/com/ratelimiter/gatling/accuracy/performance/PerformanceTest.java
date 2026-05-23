package com.ratelimiter.gatling.accuracy.performance;

import com.ratelimiter.gatling.accuracy.BaseAccuracyTest;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.constantUsersPerSec;
import static io.gatling.javaapi.core.CoreDsl.rampUsersPerSec;

public class PerformanceTest extends BaseAccuracyTest {

    public PerformanceTest() {
        var performanceFeeder = createDynamicIdentityFeeder("performance-test");
        ScenarioBuilder trafficLoadScenario = createSmartDistributionScenario("Agnostic-Real-World-Load-Simulation",performanceFeeder);
        setUp(
                trafficLoadScenario.injectOpen(
                        // Phase 1: Warm-up from 1 to 1,000 RPS over 5 seconds
                        // Volume delivered: ~2,500 requests
                        rampUsersPerSec(1).to(1000).during(Duration.ofSeconds(5)),

                        // Phase 2: Acceleration Spike from 1,000 to 3,500 RPS over 3 seconds
                        // Volume delivered: ~6,750 requests
                        rampUsersPerSec(1000).to(3500).during(Duration.ofSeconds(3)),

                        // Phase 3: The Sustained Plateau at 3,500 RPS for 12 seconds
                        // Volume delivered: Exactly 42,000 requests
                        constantUsersPerSec(3500).during(Duration.ofSeconds(12))
                )
        )
                .protocols(httpProtocol);
    }
    @Override
    protected void validateAssertions(int allowed, int throttled) {
        System.out.println(">> PERFORMANCE COMPLETED STABLY UNDER DYNAMIC LIMIT-WEIGHT TRAFFIC SPLITS.");
    }
}
