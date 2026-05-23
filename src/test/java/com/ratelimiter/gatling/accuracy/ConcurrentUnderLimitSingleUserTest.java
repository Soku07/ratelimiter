package com.ratelimiter.gatling.accuracy;

import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.List;

import static io.gatling.javaapi.core.OpenInjectionStep.atOnceUsers;

public class ConcurrentUnderLimitSingleUserTest extends BaseAccuracyTest {

    private final int configuredLimit;
    private final int requestsToTrigger;

    public ConcurrentUnderLimitSingleUserTest(){
        List<EndpointRule> rules = loadEnpointAndItsLimitFromCSV();
        EndpointRule targetRule =  rules.getFirst();
        this.configuredLimit = targetRule.limit();
        String userOverride = System.getProperty("CONCURRENT_REQUESTS");
        if (userOverride != null && !userOverride.isBlank()) {
            this.requestsToTrigger = Integer.parseInt(userOverride);
            if(requestsToTrigger > configuredLimit) throw new IllegalArgumentException(String.format(
                    "CRITICAL SETUP ERROR: Trigger volume (%d) must be strictly LESS than the configured limit (%d) " +
                            "for an Under-Limit Accuracy test scenario.",
                    requestsToTrigger, configuredLimit
            ));

        } else {
            this.requestsToTrigger = (int) (configuredLimit * 0.8);
        }
        ScenarioBuilder scenario = createAccuracyScenario(
                "Single User Single Endpoint Under-Limit Burst",
                targetRule.method(),
                targetRule.endpoint(),
                singleIdentityFeeder
        );
        //atOnceUsers provides a thread for each virtual user making the test concurrent.
        //repeatPerUser is set to 1 because atOnceUsers(requestsToTrigger) is already creating "requestsToTrigger" concurrent requests
        setUp(scenario.injectOpen(atOnceUsers(requestsToTrigger))).protocols(httpProtocol);
    }
    @Override
    protected void validateAssertions(int allowed, int throttled) {
        if (throttled > 0) {
            throw new AssertionError(String.format(
                    "CRITICAL ACCURACY FAULT: False-positive throttling! A burst of %d concurrent requests " +
                            "was fired against a limit of %d, but the rate limiter dropped %d requests with HTTP 429.",
                    requestsToTrigger, configuredLimit, throttled
            ));
        }

        System.out.println(String.format(
                ">> SUCCESS: Concurrent Under-Limit single-user test passed flawlessly. " +
                        "Fired %d requests simultaneously against a limit of %d. Throttled: 0.",
                requestsToTrigger, configuredLimit
        ));
    }
}
