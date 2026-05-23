package com.ratelimiter.gatling.accuracy.algorithm;

import com.ratelimiter.gatling.accuracy.BaseAccuracyTest;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.time.Duration;
import java.util.List;

public class ProbabilisticSlidingWindowTest extends BaseAccuracyTest {

    public final int baseLimit;
    public final long windowMillis;

    public final long wave2DelayMillis;
    public final long wave3DelayMillis;
    public final long wave4DelayMillis;

    public final int wave1TotalSent;

    public ProbabilisticSlidingWindowTest(){
        List<EndpointRule> rules = loadEnpointAndItsLimitFromCSV();
        EndpointRule target = rules.getFirst();

        this.baseLimit = target.limit();
        this.windowMillis = target.window().toMillis();

        //Sending some additional delta 10% reqs
        this.wave1TotalSent = baseLimit + (int) Math.ceil(baseLimit * 0.1);

        // Wave 2: Move 1.1x past Window 1 to force the baseline into 'prev' (1100ms)
        this.wave2DelayMillis = (long) (windowMillis * 1.1);
        // Wave 3: Wait another 40% of the window. Both prev and curr exist simultaneously. (1500ms total)
        this.wave3DelayMillis = wave2DelayMillis + (long) (windowMillis * 0.4);
        // Wave 4: Wait 2.5x full windows past Wave 3 to guarantee a clean slate (4000ms total)
        this.wave4DelayMillis = wave3DelayMillis + (long) (windowMillis * 2.5);

        ScenarioBuilder wave1Baseline = createAccuracyScenario("Wave1-NormalBaseline", target.method(), target.endpoint(), singleIdentityFeeder);
        ScenarioBuilder wave2Rollover = createAccuracyScenario("Wave2-RolloverTrigger", target.method(), target.endpoint(), singleIdentityFeeder);
        ScenarioBuilder wave3WeightedEdge = createAccuracyScenario("Wave3-WeightedEdgeCheck", target.method(), target.endpoint(), singleIdentityFeeder);
        ScenarioBuilder wave4Reset = createAccuracyScenario("Wave4-CleanSlateReset", target.method(), target.endpoint(), singleIdentityFeeder);

        setUp(
                wave1Baseline.injectOpen(
                        OpenInjectionStep.atOnceUsers(wave1TotalSent)
                ),
                wave2Rollover.injectOpen(
                        OpenInjectionStep.nothingFor(Duration.ofMillis(wave2DelayMillis)),
                        OpenInjectionStep.atOnceUsers(1)
                ),
                wave3WeightedEdge.injectOpen(
                        OpenInjectionStep.nothingFor(Duration.ofMillis(wave3DelayMillis)),
                        OpenInjectionStep.atOnceUsers(1)
                ),
                wave4Reset.injectOpen(
                        OpenInjectionStep.nothingFor(Duration.ofMillis(wave4DelayMillis)),
                        OpenInjectionStep.atOnceUsers(1)
                )
        ).protocols(httpProtocol);
    }
    @Override
    protected void validateAssertions(int allowed, int throttled) {
        int expectedTotalVolume = wave1TotalSent + 3; // 6 + 1 + 1 + 1 = 9 total requests fired
        int minAllowed = baseLimit + 2; // Guard for heavy machine jitter environments (7)
        int maxAllowed = baseLimit + 3; // Perfect runtime tracking execution (8)
        if (allowed < minAllowed || allowed > maxAllowed || (allowed + throttled) != expectedTotalVolume) {
            throw new AssertionError(String.format(
                    "CRITICAL: Probabilistic Sliding Window Math Desynchronization!\n" +
                            "Timeline execution compromised or rate limiter metrics leaked.\n" +
                            "Expected allowed requests inside range [%d - %d]. Actual system allowed: %d.",
                    minAllowed, maxAllowed, allowed
            ));
        }

        System.out.println(String.format(
                ">> SUCCESS: High-precision sliding window simulation complete!\n" +
                        "   Allowed: %d (Bounded) | Throttled: %d | Real clock metrics matched logic rules precisely.",
                allowed, throttled
        ));

    }
}
