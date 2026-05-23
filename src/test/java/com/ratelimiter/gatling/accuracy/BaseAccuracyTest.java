package com.ratelimiter.gatling.accuracy;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.HttpProtocolBuilder;
import io.gatling.javaapi.http.HttpRequestActionBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.gatling.javaapi.core.CoreDsl.exec;
import static io.gatling.javaapi.core.CoreDsl.scenario;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;


public abstract class BaseAccuracyTest extends Simulation {
    public record EndpointRule(String method, String endpoint, int limit, Duration window) {

    }
    protected static final AtomicInteger COUNT_200 = new AtomicInteger(0);
    protected static final AtomicInteger COUNT_429 = new AtomicInteger(0);
    protected final String baseUrl = System.getProperty("BASE_URL", "http://localhost:8080");

    protected final HttpProtocolBuilder httpProtocol = http
            .baseUrl(baseUrl)
            .acceptHeader("application/json")
            .maxConnectionsPerHost(60000)
            .contentTypeHeader("application/json")
            .shareConnections();


    // We have to pass same token and ip so that a particular bucket is tested
    protected final Iterator<Map<String, Object>> singleIdentityFeeder =
            Stream.generate(() -> Map.<String, Object>of(
                    "token", System.getProperty("TEST_TOKEN", "accuracy-spec-user"),
                    "ip", System.getProperty("TEST_IP", "192.168.1.100")
            )).iterator();

    protected List<EndpointRule> loadEnpointAndItsLimitFromCSV(){
        String csvFilePath = System.getProperty("ENDPOINT_CSV_PATH","");
        List<EndpointRule> rules = new ArrayList<>();
        String line;
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            br.readLine(); // Skip CSV column headers (method,endpoint,limit)
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] data = line.split(",");
                if (data.length >= 3) {
                    String method = data[0].trim();
                    String endpoint = data[1].trim();
                    int limit = Integer.parseInt(data[2].trim());
                    Duration window = (data.length >= 4 && !data[3].trim().isBlank())
                            ? Duration.parse(data[3].trim())
                            : null;
                    rules.add(new EndpointRule(method, endpoint, limit,window));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read manifest file from path: " + csvFilePath, e);
        }
        if (rules.isEmpty()) {
            throw new IllegalStateException("Your test case input file is empty! Cannot initialize test.");
        }
        return Collections.unmodifiableList(rules);
    }

    protected Iterator<Map<String, Object>> createDynamicIdentityFeeder(String prefix) {
        Random random = new Random();
        return Stream.generate(() -> {
            // Randomize the 2nd, 3rd, and 4th octets for maximum scale
            int octet2 = 16 + random.nextInt(16);   // 16 to 31
            int octet3 = random.nextInt(256);       // 0 to 255
            int octet4 = 1 + random.nextInt(254);   // 1 to 254 (avoiding network/broadcast boundaries)

            String randomIp = String.format("172.%d.%d.%d", octet2, octet3, octet4);

            return Map.<String, Object>of(
                    "token", "token-" + prefix + "-" + UUID.randomUUID().toString().substring(0, 8),
                    "ip", randomIp
            );
        }).iterator();
    }


    protected ScenarioBuilder createAccuracyScenario(String name, String method, String endpoint, Iterator<Map<String, Object>> feeder) {
        return buildAccuracyScenario(name, feeder,
                exec(buildRequest(http(session -> requestName(session)).httpRequest(method, endpoint)))
        );
    }

    protected ScenarioBuilder createAccuracyScenario(String name, Iterator<Map<String, Object>> feeder) {
        return buildAccuracyScenario(name, feeder,
                exec(session -> session.set("resolvedMethod", session.getString("method").toUpperCase()))
                        .doSwitch("#{resolvedMethod}").on(
                                Choice.withKey("GET",    exec(buildRequest(http("GET-REQ").get("#{endpoint}")))),
                                Choice.withKey("POST",   exec(buildRequest(http("POST-REQ").post("#{endpoint}")))),
                                Choice.withKey("PUT",    exec(buildRequest(http("PUT-REQ").put("#{endpoint}")))),
                                Choice.withKey("DELETE", exec(buildRequest(http("DELETE-REQ").delete("#{endpoint}"))))
                        )
        );
    }

    private ScenarioBuilder buildAccuracyScenario(String name, Iterator<Map<String, Object>> feeder, ChainBuilder requestChain) {
        return scenario(name)
                .feed(feeder)
                .exec(requestChain)
                .doIf(session -> session.contains("httpStatus") && session.getInt("httpStatus") == 200).then(
                        exec(session -> { COUNT_200.incrementAndGet(); return session; })
                )
                .doIf(session -> session.contains("httpStatus") && session.getInt("httpStatus") == 429).then(
                        exec(session -> { COUNT_429.incrementAndGet(); return session; })
                );
    }

    private HttpRequestActionBuilder buildRequest(HttpRequestActionBuilder builder) {
        return builder
                .header("Authorization", "Bearer #{token}")
                .header("X-Forwarded-For", "#{ip}")
                .check(status().in(200, 429).saveAs("httpStatus"));
    }

    private static String requestName(Session session) {
        return "STATUS-" + (session.contains("httpStatus") ? session.getInt("httpStatus") : "UNKNOWN");
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
