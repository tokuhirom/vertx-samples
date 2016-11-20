package com.example;

import com.google.common.math.Quantiles;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class BenchmarkClient {
    public static void main(String[] args) throws InterruptedException {
        // options
        int maxPoolSize = getPropInt("bc.maxPoolSize", "1000");
        int connectTimeout = getPropInt("bc.connectTimeout", "1000");
        int idleTimeout = getPropInt("bc.idleTimeout", "1000");
        int benchmarkTimeout = getPropInt("bc.benchmarkTimeout", "60");

        // parameters
        int requests = getPropInt("bc.requests", "100");
        CountDownLatch countDownLatch = new CountDownLatch(requests);
        ConcurrentHashMap<String, Integer> exceptionCounter = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, Integer> statusCodeCounter = new ConcurrentHashMap<>();
        ConcurrentHashSet<Long> elapsedMillisecs = new ConcurrentHashSet<>(requests);

        if (args.length != 1) {
            System.out.println("Usage: java -jar benchmark-client-*.jar <URL>");
            System.exit(1);
        }

        String url = args[0];

        Vertx vertx = Vertx.vertx();
        HttpClientOptions httpClientOptions = new HttpClientOptions()
                .setMaxPoolSize(maxPoolSize)
                .setConnectTimeout(connectTimeout)
                .setIdleTimeout(idleTimeout);
        HttpClient httpClient = vertx.createHttpClient(httpClientOptions);
        long totalStart = System.currentTimeMillis();
        for (int i = 0; i < requests; i++) {
            final int currentIndex = i;
            final long startedAt = System.currentTimeMillis();
            httpClient.getAbs(url, response -> {
                final long finishedAt = System.currentTimeMillis();
                long elapsed = finishedAt - startedAt;
                elapsedMillisecs.add(elapsed);
                countDownLatch.countDown();
                statusCodeCounter.compute(response.statusCode(),
                        (code, current) -> current == null ? 1 : current + 1);
            }).exceptionHandler(throwable -> {
                countDownLatch.countDown();
                String exc = throwable.getClass().getName() + " " + throwable.getMessage();
                System.out.println(currentIndex + " " + exc);
                exceptionCounter.compute(exc,
                        (key, current) -> current == null ? 1 : current + 1);
            })
                    .end();
        }

        countDownLatch.await(benchmarkTimeout, TimeUnit.SECONDS);
        long totalEnd = System.currentTimeMillis();
        vertx.close();

        System.out.println("\n\n\n========> Report <=======\n");
        System.out.println("maxPoolSize: " + maxPoolSize);
        System.out.println("connectTimeout: " + connectTimeout);
        System.out.println("idleTimeout: " + idleTimeout);
        System.out.println("benchmarkTimeout: " + benchmarkTimeout);
        System.out.println("requests: " + requests);
        System.out.println("\n========> Latency <=======\n");
        double[] elapsedSecs = elapsedMillisecs.stream()
                .mapToDouble(ms -> ms / 1000.0)
                .toArray();
        if (elapsedSecs.length > 0) {
            Quantiles.percentiles().indexes(50, 90, 99)
                    .compute(elapsedSecs)
                    .entrySet()
                    .stream()
                    .collect(Collectors.toList())
                    .stream()
                    .sorted(Comparator.comparingInt(Map.Entry::getKey))
                    .forEach(entry -> {
                        System.out.println(entry.getKey() + " percentile: " + entry.getValue());
                    });
            OptionalDouble average = Arrays.stream(elapsedSecs).average();
            if (average.isPresent()) {
                System.out.println("Average: " + average.getAsDouble());
            }
            System.out.println("Request/sec: " + (double) requests / ((totalEnd - totalStart) / 1000.0));
        }
        System.out.println("\n========> HTTP Status <=======\n");
        statusCodeCounter.forEach((status, count) -> {
            System.out.println(status + " " + count);
        });
        System.out.println("\n========> Exceptions <=======\n");
        exceptionCounter.forEach((message, count) -> {
            System.out.println(count + " " + message);
        });
    }

    private static int getPropInt(String propertyName, String defaultValue) {
        return Integer.parseInt(System.getProperty(propertyName, defaultValue));
    }
}
