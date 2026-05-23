package com.ratelimiter.gatling.accuracy.algorithm;

import com.ratelimiter.gatling.accuracy.BaseAccuracyTest;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import static io.gatling.javaapi.core.CoreDsl.rampUsers;

import java.time.Duration;
import java.util.List;

public class FixedWindowTest extends BaseAccuracyTest {

    public final int baseLimit;
    public final long windowSeconds;
    public final int totalRequestsExpected;

    public FixedWindowTest(){
        List<EndpointRule> rules = loadEnpointAndItsLimitFromCSV();
        EndpointRule target = rules.getFirst();

        this.baseLimit = target.limit();
        this.windowSeconds = target.window().toSeconds();

        // Testing the classic request burst allowed at window edges
        this.totalRequestsExpected = baseLimit*2;

        ScenarioBuilder window1RampScenario = createAccuracyScenario(
                "Window1-GradualDrain",
                target.method(),
                target.endpoint(),
                singleIdentityFeeder
        );
        ScenarioBuilder window2BurstScenario = createAccuracyScenario(
                "Window2-ResetExploit",
                target.method(),
                target.endpoint(),
                singleIdentityFeeder
        );

        setUp(
                window1RampScenario.injectOpen(
                        rampUsers(baseLimit).during(Duration.ofSeconds(windowSeconds - 1))
                ),
                // Phase 2: Wait for Window 1 to cross the boundary, then slam the reset immediately
                window2BurstScenario.injectOpen(
                        OpenInjectionStep.nothingFor(Duration.ofMillis((windowSeconds * 1000) + 100)),
                        OpenInjectionStep.atOnceUsers(baseLimit) // Fire another 10 users instantly
                )
        ).protocols(httpProtocol);
    }
    @Override
    protected void validateAssertions(int allowed, int throttled) {
        if (allowed != totalRequestsExpected) {
            throw new AssertionError(String.format(
                    "FIXED WINDOW BOUNDARY VIOLATION FAULT!\n" +
                            "For a pure Fixed Window algorithm, exactly double the limit (%d) should be allowed at the edge.\n" +
                            "Actual metrics -> Allowed: %d, Throttled: %d.\n" +
                            "If allowed is lower, your backend might be running a Sliding Window or Token Bucket instead!",
                    totalRequestsExpected, allowed, throttled
            ));
        }

        System.out.println(String.format(
                ">> SUCCESS: Fixed Window boundary edge-case validated perfectly!\n" +
                        "   Allowed exactly double the capacity limit: %d requests.\n" +
                        "   Throttled: %d requests. (Vulnerability to burst boundary verified).",
                allowed, throttled
        ));
    }
}
