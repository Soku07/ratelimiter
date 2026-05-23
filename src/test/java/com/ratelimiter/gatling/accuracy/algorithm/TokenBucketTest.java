package com.ratelimiter.gatling.accuracy.algorithm;

import com.ratelimiter.gatling.accuracy.BaseAccuracyTest;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.time.Duration;
import java.util.List;

public class TokenBucketTest extends BaseAccuracyTest {
    public final double burstFactor;
    public final int expectedMaxCapacity;
    public final int totalRequestsToFire;
    public TokenBucketTest(){
        this.burstFactor = Double.parseDouble(System.getProperty("BURST_FACTOR", "0.0"));
        List<EndpointRule> rules = loadEnpointAndItsLimitFromCSV();
        EndpointRule target = rules.getFirst();
        this.expectedMaxCapacity = (int) ((1.0 + burstFactor) * target.limit());
        this.totalRequestsToFire = (int) Math.ceil(expectedMaxCapacity * 0.1) + expectedMaxCapacity;

        double timePerTokenSeconds = (double) target.window().toSeconds() / target.limit();
        //we will wait for some delta time so that it is gurateed that atleast 1 token is generated
        double waitTimeSeconds = timePerTokenSeconds * 1.3;
        ScenarioBuilder scenario = createAccuracyScenario("Token Bucket Accuracy Test", target.method(), target.endpoint(),singleIdentityFeeder);
        ScenarioBuilder refillTokenScenario = createAccuracyScenario("Refil",target.method(),target.endpoint(),singleIdentityFeeder);



        setUp(scenario.injectOpen(OpenInjectionStep.atOnceUsers(totalRequestsToFire)),
                refillTokenScenario.injectOpen(
                        OpenInjectionStep.nothingFor(Duration.ofMillis((long) waitTimeSeconds * 1000)),
                        OpenInjectionStep.atOnceUsers(1))


        ).protocols(httpProtocol);



    }

    @Override
    protected void validateAssertions(int allowed, int throttled) {
        int minAllowed = expectedMaxCapacity;
        int maxAllowed = expectedMaxCapacity + 1;
        int totalVolumeSent = totalRequestsToFire + 1;
        if (allowed < minAllowed || allowed > maxAllowed || (allowed + throttled) != totalVolumeSent) {
            throw new AssertionError(String.format(
                    "CRITICAL: Token Bucket Math Violation!\n" +
                            "Expected total allowed requests to be within the jitter-safe range [%d - %d].\n" +
                            "Actual System Metrics -> Allowed: %d, Throttled: %d (Total Sent: %d)",
                    minAllowed, maxAllowed, allowed, throttled, (allowed + throttled)
            ));
        }

        System.out.println(String.format(
                ">> SUCCESS: Micro-accuracy validated perfectly within clock tolerances!\n" +
                        "   Actual Allowed: %d (Legal Range: [%d - %d]) | Actual Throttled: %d",
                allowed, minAllowed, maxAllowed, throttled
        ));
    }
}
