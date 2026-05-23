package com.ratelimiter.gatling.accuracy;

import io.gatling.javaapi.core.ScenarioBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.rampUsers;

public class ConcurrentUnderLimitMultiUserMultiEndpointTest extends BaseAccuracyTest{

    private final int totalConcurrentRequests;

    public ConcurrentUnderLimitMultiUserMultiEndpointTest(){
        List<EndpointRule> rules = loadEnpointAndItsLimitFromCSV();
        int endpointCountX = rules.size();
        int userCountN = Integer.parseInt(System.getProperty("USER_COUNT", "100"));
        int concurrentRequestsY = Integer.parseInt(System.getProperty("CONCURRENT_REQUESTS", "10"));

        this.totalConcurrentRequests = endpointCountX * userCountN * concurrentRequestsY;
        for (EndpointRule rule : rules) {
            if (concurrentRequestsY > rule.limit()) {
                throw new IllegalArgumentException(String.format(
                        "CRITICAL SETUP ERROR: Concurrent requests per user (Y: %d) cannot exceed " +
                                "the endpoint limit for '%s' (Limit: %d) for an Under-Limit Accuracy test scenario.",
                        concurrentRequestsY, rule.endpoint(), rule.limit()
                ));
            }
        }
        List<Map<String,String>> userPool = new ArrayList<>();

        Random random = new Random();
        for (int i = 0; i < userCountN; i++) {
            userPool.add(Map.of(
                    "token", "token-matrix-" + UUID.randomUUID().toString().substring(0, 8),
                    "ip", String.format("172.%d.%d.%d", 16 + random.nextInt(16), random.nextInt(256), 1 + random.nextInt(254))
            ));
        }
        AtomicInteger threadCounter = new AtomicInteger(0);


        Iterator<Map<String,Object>> matrixFeeder = Stream.generate(
                ()->{
                    int threadID = threadCounter.getAndIncrement();
                    int userIndex = (threadID / (endpointCountX * concurrentRequestsY)) % userCountN;
                    int endpointIndex = (threadID / concurrentRequestsY) % endpointCountX;

                    Map<String, String> identity = userPool.get(userIndex);
                    EndpointRule targetRule = rules.get(endpointIndex);

                    Map<String,Object> threadDataMatrix = new HashMap<>(identity);
                    threadDataMatrix.put("method", targetRule.method());
                    threadDataMatrix.put("endpoint",targetRule.endpoint());
                    return  threadDataMatrix;
                }
        ).iterator();

        ScenarioBuilder scenario = createAccuracyScenario("multi user multi endpoint",
                matrixFeeder);

        setUp(
                scenario.injectOpen(
                        rampUsers(totalConcurrentRequests).during(60)
                )
        ).protocols(httpProtocol);
    }
    @Override
    protected void validateAssertions(int allowed, int throttled) {
        if (throttled > 0) throw new AssertionError("Matrix lookup contention fault: " + throttled);

    }
}
