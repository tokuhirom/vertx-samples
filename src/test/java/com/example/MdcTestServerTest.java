package com.example;

import com.example.client.HighLevelHttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClientResponse;
import io.vertx.rxjava.core.http.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import rx.Observable;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class MdcTestServerTest {
    @Test
    public void run() throws Exception {
        MdcTestServer mdcTestServer = new MdcTestServer();
        HttpServer server = mdcTestServer.run(0).toBlocking().first();
        int port = server.actualPort();

        Vertx vertx = Vertx.vertx();
        HighLevelHttpClient client = new HighLevelHttpClient(vertx, new HttpClientOptions().setMaxPoolSize(100), 7);
        Random random = new Random();
        List<Observable<HttpClientResponse>> list = IntStream.rangeClosed(1, 100)
                .mapToObj(i -> {
                    int id = random.nextInt(1000);
                    return client.get("http://127.0.0.1:" + port + "/rx/mdc/" + id);
                })
                .collect(Collectors.toList());
        Observable.merge(list)
                .toBlocking()
                .subscribe(response -> {
                    response.bodyHandler(buffer -> {
                        String header = response.getHeader("X-ID");
                        String got = buffer.toString();
                        Assert.assertEquals(header, got);
                        log.info("GOT: {}=={}", got, header);
                    });
                });
    }

}