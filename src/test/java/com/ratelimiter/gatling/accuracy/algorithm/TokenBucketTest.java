package com.ratelimiter.gatling.accuracy.algorithm;

import com.ratelimiter.gatling.accuracy.BaseAccuracyTest;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.List;

public class TokenBucketTest extends BaseAccuracyTest {
    public final double burstFactor;
    public final int expectedMaxCapacity;
    public TokenBucketTest(){
        this.burstFactor = Double.parseDouble(System.getProperty("BURST_FACTOR", "0.0"));
        List<EndpointRule> rules = loadEnpointAndItsLimitFromCSV();
        EndpointRule target = rules.getFirst();
        this.expectedMaxCapacity = (int) ((1.0 + burstFactor) * target.limit());
        int totalRequestsToFire = (int) Math.ceil(expectedMaxCapacity * 0.1) + expectedMaxCapacity;

        ScenarioBuilder scenario = createAccuracyScenario("Token Bucket Accuracy Test", target.method(), target.endpoint(),singleIdentityFeeder);

        setUp(scenario.injectOpen(OpenInjectionStep.atOnceUsers(totalRequestsToFire))).protocols(httpProtocol);


    }

    @Override
    protected void validateAssertions(int allowed, int throttled) {
        if (allowed != expectedMaxCapacity) {
            throw new AssertionError(String.format(
                    "CRITICAL: Token Bucket Math Violation!\n" +
                            "Expected exactly %d allowed requests to pass through boundary check.\n" +
                            "Instead, the rate limiter allowed %d requests. Check for rounding errors.",
                    expectedMaxCapacity, allowed
            ));
        }
        System.out.println(String.format(
                ">> SUCCESS: Micro-accuracy validated perfectly!\n" +
                        "   Allowed exact capacity: %d | Cleanly blocked extra latency padding: %d",
                allowed, throttled
        ));
    }
}
