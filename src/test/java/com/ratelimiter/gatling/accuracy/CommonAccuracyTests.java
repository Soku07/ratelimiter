package com.ratelimiter.gatling.accuracy;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;

public class CommonAccuracyTests extends BaseAccuracyTest{
    private static final String ENDPOINT = System.getProperty("TEST_ENDPOINT", "/api/v1/payments/execute");
    private static final String METHOD = System.getProperty("TEST_METHOD", "POST");
    private static final int CONFIG_LIMIT = Integer.parseInt(System.getProperty("LIMIT", "50"));
    private final int safeUnderLimitCount = (int) (CONFIG_LIMIT * 0.9);
    public CommonAccuracyTests() {
        ScenarioBuilder underLimitScenario = createAccuracyScenario(
                "Common - Under Limit Sanity", METHOD, ENDPOINT, "{}"
        );
        int userCount = Math.max(1, safeUnderLimitCount);
        setUp(
                underLimitScenario.injectOpen(
                    OpenInjectionStep.atOnceUsers(userCount)
                )

        ).protocols(httpProtocol);


    }

    @Override
    protected void validateAssertions(int allowed, int throttled) {
        if (throttled > 0) {
            throw new AssertionError("CRITICAL FAULT: Common baseline scenario leaked a 429!");
        }
        System.out.println(">> SUCCESS: All common accuracy scenarios passed cleanly.");
    }
}
