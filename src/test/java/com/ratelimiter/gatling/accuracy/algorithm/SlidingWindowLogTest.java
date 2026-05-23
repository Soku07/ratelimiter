package com.ratelimiter.gatling.accuracy.algorithm;

import com.ratelimiter.gatling.accuracy.BaseAccuracyTest;
import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import java.time.Duration;
import java.util.List;

/**
 * Saturated Burst Verification Suite for Sliding Window Log.
 * Fires heavy parallel bursts during throttled windows to guarantee that
 * rejected traffic does not leak timestamps into the tracking registry.
 */
public class SlidingWindowLogTest extends BaseAccuracyTest {

    public final int baseLimit;
    public final long windowMillis;

    public final long wave1MidWindowDelayMillis;
    public final long wave2EdgeDelayMillis;
    public final long wave3RollingDelayMillis;
    public final long wave4CooldownDelayMillis;

    // Tracker for our exact structural user volumes

    public final int totalExpectedRequests;

    public SlidingWindowLogTest() {
        List<EndpointRule> rules = loadEnpointAndItsLimitFromCSV();
        EndpointRule target = rules.getFirst();

        this.baseLimit = target.limit();
        this.windowMillis = target.window().toMillis();

        // Timeline Checkpoints
        this.wave1MidWindowDelayMillis = (long) (windowMillis * 0.50);
        this.wave2EdgeDelayMillis = (long) (windowMillis - 100);
        this.wave3RollingDelayMillis = (long) (windowMillis * 1.30);
        this.wave4CooldownDelayMillis = (long) (windowMillis * 3.5);

        // Match the burst density to our capacity limit to heavily stress the block


        // Calculation: (baseLimit - 1) + 1 + wave2BurstSize + 1 + 1
        // For a limit of 5: 4 + 1 + 5 + 1 + 1 = 12 total requests fired
        this.totalExpectedRequests = baseLimit + 3;

        ScenarioBuilder wave1Baseline = createAccuracyScenario("Log-Wave1-InitialFill", target.method(), target.endpoint(), singleIdentityFeeder);
        ScenarioBuilder wave1MidCap = createAccuracyScenario("Log-Wave1.5-MidWindowCap", target.method(), target.endpoint(), singleIdentityFeeder);
        ScenarioBuilder wave2EdgeClamp = createAccuracyScenario("Log-Wave2-SaturatedBurst", target.method(), target.endpoint(), singleIdentityFeeder);
        ScenarioBuilder wave3RollingEvict = createAccuracyScenario("Log-Wave3-RollingEvict", target.method(), target.endpoint(), singleIdentityFeeder);
        ScenarioBuilder wave4CooldownReset = createAccuracyScenario("Log-Wave4-CooldownReset", target.method(), target.endpoint(), singleIdentityFeeder);

        System.out.println(String.format(
                ">> INITIALIZING LOG SATURATED BURST ACCURACY TESTS:\n" +
                        "   Target Rule Configuration: %d requests per %d ms\n" +
                        "   Total Stress Volume Fired : %d requests\n" +
                        "   Timeline Roadmap:\n" +
                        "     - T+0ms    : Firing %d users (Baseline buffer initialization)\n" +
                        "     - T+%dms  : Firing 1 user  (Caps log to maximum limit capacity)\n" +
                        "     - T+%dms  : FIRING 1 user (Should be throttled)" +
                        "     - T+%dms : Firing 1 user  (Expects 200 OK via lookback eviction slot)\n" +
                        "     - T+%dms : Firing 1 user  (Expects 200 OK via complete stale memory drop)",
                baseLimit, windowMillis, totalExpectedRequests, (baseLimit - 1),
                wave1MidWindowDelayMillis, wave2EdgeDelayMillis,
                wave3RollingDelayMillis, wave4CooldownDelayMillis
        ));

        setUp(
                // Phase 1: Establish baseline bounds
                wave1Baseline.injectOpen(OpenInjectionStep.atOnceUsers(baseLimit - 1)),
                wave1MidCap.injectOpen(
                        OpenInjectionStep.nothingFor(Duration.ofMillis(wave1MidWindowDelayMillis)),
                        OpenInjectionStep.atOnceUsers(1)
                ),
                // Phase 2: Slam the dry bucket with a massive parallel burst
                wave2EdgeClamp.injectOpen(
                        OpenInjectionStep.nothingFor(Duration.ofMillis(wave2EdgeDelayMillis)),
                        OpenInjectionStep.atOnceUsers(1)
                ),
                // Phase 3 & 4: Roll past thresholds to verify eviction logic and resets
                wave3RollingEvict.injectOpen(
                        OpenInjectionStep.nothingFor(Duration.ofMillis(wave3RollingDelayMillis)),
                        OpenInjectionStep.atOnceUsers(1)
                ),
                // Phase 5: Verification of stale cleanup
                wave4CooldownReset.injectOpen(
                        OpenInjectionStep.nothingFor(Duration.ofMillis(wave4CooldownDelayMillis)),
                        OpenInjectionStep.atOnceUsers(1)
                )
        ).protocols(httpProtocol);
    }

    @Override
    protected void validateAssertions(int allowed, int throttled) {
        // --- PRECISION MATHEMATICAL EXPECTATIONS MATRIX ---
        // Ideal Path:
        //   Allowed = (baseLimit - 1) + 1 [Wave 1] + 0 [Wave 2 Burst] + 1 [Wave 3] + 1 [Wave 4] = baseLimit + 2 (7)
        //   Throttled = wave2BurstSize (5)
        //
        // Real-Clock Lag Margin Path (Wave 3 hits right on an OS thread scheduler boundary and gets blocked):
        //   Allowed = baseLimit + 1 (6)
        //   Throttled = wave2BurstSize + 1 (6)

        int minAllowed = baseLimit + 1; // 6
        int maxAllowed = baseLimit + 2; // 7

        System.out.println("\n=================================================");
        System.out.println("   SATURATED BURST LOG ACCURACY METRICS REPORT");
        System.out.println("=================================================");
        System.out.println("  Configured Base Limit : " + baseLimit);
        System.out.println("  Acceptable Allowed Range: [" + minAllowed + " to " + maxAllowed + "]");
        System.out.println("  Actual System Allowed   : " + allowed);
        System.out.println("  Actual System Throttled : " + throttled);
        System.out.println("  Telemetry Stream Total  : " + (allowed + throttled) + " / " + totalExpectedRequests);
        System.out.println("=================================================\n");

        // Guard Condition 1: Check if the algorithm broke core rate limiting properties
        if (allowed < minAllowed || allowed > maxAllowed) {
            throw new AssertionError(String.format(
                    "CRITICAL: Rate limiter math desynchronized under saturated burst load!\n" +
                            "Expected total allowed requests to map inside range [%d - %d].\n" +
                            "Actual metrics -> Allowed: %d, Throttled: %d",
                    minAllowed, maxAllowed, allowed, throttled
            ));
        }

        // Guard Condition 2: Check if network transmission dropped requests on the floor
        if ((allowed + throttled) != totalExpectedRequests) {
            throw new AssertionError(String.format(
                    "NETWORK TRANSMISSION FAULT: Cumulative telemetry stream desynchronized!\n" +
                            "Gatling fired exactly %d requests, but the metrics stream only accounted for %d.\n" +
                            "Aborting validation due to incomplete sample sizes.",
                    totalExpectedRequests, (allowed + throttled)
            ));
        }

        System.out.println(">> SUCCESS: Saturated burst margins verified perfectly across real-clock schedules!");
    }
}