package com.ratelimiter.gatling.accuracy;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;


public abstract class BaseAccuracyTest extends Simulation {
    protected static final AtomicInteger COUNT_200 = new AtomicInteger(0);
    protected static final AtomicInteger COUNT_429 = new AtomicInteger(0);
    protected final String baseUrl = System.getProperty("BASE_URL", "http://localhost:8080");
    protected final String testToken = System.getProperty("TEST_TOKEN", "accuracy-spec-user");
    protected final String testIp = System.getProperty("TEST_IP", "192.168.1.100");

    protected final HttpProtocolBuilder httpProtocol = http
            .baseUrl(baseUrl)
            .acceptHeader("application/json")
            .contentTypeHeader("application/json")
            .shareConnections();

    // We have to pass same token and ip so that a particular bucket is tested
    protected final Iterator<Map<String, Object>> singleIdentityFeeder =
            Stream.generate(() -> Map.<String, Object>of(
                    "token", testToken,
                    "ip", testIp
            )).iterator();

    protected ScenarioBuilder createAccuracyScenario(String scenarioName, String method, String endPoint,String payload) {
        var requestBuilder = http(session -> "STATUS-" + session.getString("httpStatus"))
                .httpRequest(method, endPoint)
                .header("Authorization", "Bearer #{token}")
                .header("X-Forwarded-For", "#{ip}");

        if (payload != null && !payload.isBlank()) {
            requestBuilder = requestBuilder.body(StringBody(payload));
        }
        return scenario(scenarioName)
                .feed(singleIdentityFeeder)
                .exec(
                        // Extract the status code natively and save it to the user session
                        requestBuilder.check(status().in(200, 429).saveAs("httpStatus"))
                )
                .doIf(session -> session.getInt("httpStatus") == 200).then(
                        exec(session -> { COUNT_200.incrementAndGet(); return session; })
                )
                .doIf(session -> session.getInt("httpStatus") == 429).then(
                        exec(session -> { COUNT_429.incrementAndGet(); return session; })
                );
    }
    @Override
    public void after() {
        int allowed = COUNT_200.get();
        int throttled = COUNT_429.get();

        System.out.println("\n=================================================");
        System.out.println("   ACCURACY REPORT: " + this.getClass().getSimpleName());
        System.out.println("=================================================");
        System.out.println("  Actual Allowed   (200): " + allowed);
        System.out.println("  Actual Throttled (429): " + throttled);
        System.out.println("  Total Volume Sent     : " + (allowed + throttled));
        System.out.println("=================================================");

        validateAssertions(allowed, throttled);
    }

    protected abstract void validateAssertions(int allowed, int throttled);

}
