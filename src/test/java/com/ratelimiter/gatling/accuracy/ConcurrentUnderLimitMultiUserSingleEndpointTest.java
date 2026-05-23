package com.ratelimiter.gatling.accuracy;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class ConcurrentUnderLimitMultiUserSingleEndpointTest extends BaseAccuracyTest{

    private final int configuredLimit;
    private  int requestsToTriggerPerUser = 10;
    private  int userCount = 10;

    public ConcurrentUnderLimitMultiUserSingleEndpointTest(){
        List<EndpointRule> rules = loadEnpointAndItsLimitFromCSV();
        EndpointRule targetRule =  rules.getFirst();
        this.configuredLimit = targetRule.limit();
        String USER_COUNT = System.getProperty("USER_COUNT", "10");
        String CONCURRENT_REQUESTS = System.getProperty("CONCURRENT_REQUESTS","10");
        if(USER_COUNT != null && !USER_COUNT.isBlank()){
            this.userCount = Integer.parseInt(USER_COUNT);
        }
        if(CONCURRENT_REQUESTS != null && !CONCURRENT_REQUESTS.isBlank()){
            this.requestsToTriggerPerUser = Integer.parseInt(CONCURRENT_REQUESTS);
        }

        if(requestsToTriggerPerUser > configuredLimit){
            throw new IllegalArgumentException(String.format(
                    "CRITICAL SETUP ERROR: Trigger volume (%d) must be strictly LESS than the configured limit (%d) " +
                            "for an Under-Limit Accuracy test scenario.",
                    requestsToTriggerPerUser, configuredLimit
            ));
        }
        List<Map<String, Object>> identityPool = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < userCount; i++) {
            int octet2 = 16 + random.nextInt(16);
            int octet3 = random.nextInt(256);
            int octet4 = 1 + random.nextInt(254);
            identityPool.add(Map.of(
                    "token", "token-multi-" + UUID.randomUUID().toString().substring(0, 8),
                    "ip", String.format("172.%d.%d.%d", octet2, octet3, octet4)
            ));
        }
        AtomicInteger threadCounter = new AtomicInteger(0);
        Iterator<Map<String, Object>> concurrentFeeder = Stream.generate(() -> {
            int currentThreadIndex = threadCounter.getAndIncrement();
            int identityIndex = (currentThreadIndex / requestsToTriggerPerUser) % userCount;
            return identityPool.get(identityIndex);
        }).iterator();

        ScenarioBuilder scenario = createAccuracyScenario("Multi User Single Endpoint Concurrent Request Test",
                targetRule.method(), targetRule.endpoint(), concurrentFeeder);
        int totalParallelThreads = userCount * requestsToTriggerPerUser;
        setUp(scenario.injectOpen(OpenInjectionStep.atOnceUsers(totalParallelThreads))).protocols(httpProtocol);




    }
    @Override
    protected void validateAssertions(int allowed, int throttled) {
        if (throttled > 0) {
            throw new AssertionError(String.format(
                    "CRITICAL ACCURACY FAULT: False-positive throttling! A burst of %d concurrent requests " +
                            "was fired against a limit of %d, but the rate limiter dropped %d requests with HTTP 429.",
                    requestsToTriggerPerUser, configuredLimit, throttled
            ));
        }

        System.out.println(String.format(
                ">> SUCCESS: Concurrent Under-Limit multi-user test passed flawlessly. " +
                        "Fired %d requests simultaneously against a limit of %d. Throttled: 0.",
                requestsToTriggerPerUser, configuredLimit
        ));
    }
}
