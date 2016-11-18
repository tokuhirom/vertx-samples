package com.example;

import com.example.client.HighLevelHttpClient;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.ext.dropwizard.Match;
import io.vertx.ext.dropwizard.MatchType;
import io.vertx.rxjava.core.Vertx;
import lombok.extern.slf4j.Slf4j;

// Http Client sample is
@Slf4j
public class ClientPoolingSample {
    public static void main(String[] args) throws InterruptedException {
        Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
                new DropwizardMetricsOptions()
                        .addMonitoredHttpClientEndpoint(
                                new Match()
                                        .setType(MatchType.REGEX)
                                        .setValue(".*"))
                        .setJmxEnabled(true)
        ));
        HighLevelHttpClient highLevelHttpClient = new HighLevelHttpClient(vertx, new HttpClientOptions().setMaxPoolSize(1).setMetricsName("hello"), 7);
        highLevelHttpClient.get("http://localhost:8081/rx/sleep/100000").subscribe(httpClientResponse -> {
            log.info("Req1: {}", httpClientResponse.statusCode());
        });
        highLevelHttpClient.get("http://localhost:8081/rx/sleep/100").subscribe(httpClientResponse -> {
            log.info("Req2: {}", httpClientResponse.statusCode());
        });
        Thread.sleep(50000);
    }
}
