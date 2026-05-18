package com.ratelimiter.stress;

import io.gatling.javaapi.core.OpenInjectionStep;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class RateLimitLoadTest extends Simulation {
    private static final int TOTAL_USERS = 10000;
    private static final AtomicInteger COUNT_200 = new AtomicInteger(0);
    private static final AtomicInteger COUNT_429 = new AtomicInteger(0);
    private static final List<String> IP_POOL = List.of(
            "192.168.1.10",
            "192.168.1.11",
            "192.168.1.12",
            "10.0.0.5"
    );
    private static final AtomicInteger USER_COUNTER = new AtomicInteger(1);

    private static final Iterator<Map<String, Object>> FEEDER =
            Stream.<Map<String, Object>>generate(() -> {
                int userId = USER_COUNTER.getAndIncrement();
                String token = "token-user-" + userId;
                String ip    = IP_POOL.get(ThreadLocalRandom.current().nextInt(IP_POOL.size()));
                return Map.of("token", token, "ip", ip);
            }).iterator();

    private static final HttpProtocolBuilder PROTOCOL = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .userAgentHeader("Gatling-LoadTest");
    private static final ScenarioBuilder SCENARIO = scenario("Rate Limit Load Test")
            .feed(FEEDER)
            .repeat(55).on(
                    pace(Duration.ofMillis(500))
                            .exec(
                            http("GET /api/v1/payments/execute")
                                    .get("/api/v1/payments/execute")
                                    .header("Authorization", "Bearer #{token}")
                                    .header("X-Forwarded-For", "#{ip}")
                                    .check(status().in(200,429).saveAs("httpStatus"))
                    )
                            .doIf(session -> session.getInt("httpStatus") == 200).then(
                                    exec(session -> {
                                        COUNT_200.incrementAndGet();
                                        return session;
                                    })
                            )
                            .doIf(session -> session.getInt("httpStatus") == 429).then(
                                    exec(session -> {
                                        COUNT_429.incrementAndGet();
                                        return session;
                                    })
                            )

            );

    private static final OpenInjectionStep INJECTION =
            rampUsers(TOTAL_USERS).during(Duration.ofSeconds(60));

    public RateLimitLoadTest(){
        setUp(
                SCENARIO.injectOpen(INJECTION)
        )

                .protocols(PROTOCOL);


//                .assertions(
//                        global().responseTime().max().lte(10000),
//                        global().successfulRequests().percent().gt(90d)
//                );
    }

    @Override
    public void after(){
        System.out.println("═══════════════════════════════");
        System.out.println("  RATE LIMITER RESULTS SUMMARY ");
        System.out.println("═══════════════════════════════");
        System.out.println("  Allowed  [200] : " + COUNT_200.get());
        System.out.println("  Throttled[429] : " + COUNT_429.get());
        System.out.println("  Total          : " + (COUNT_200.get() + COUNT_429.get()));
        System.out.printf ("  Throttle rate  : %.1f%%%n",
                COUNT_429.get() * 100.0 / (COUNT_200.get() + COUNT_429.get()));
        System.out.println("═══════════════════════════════");
    }
}