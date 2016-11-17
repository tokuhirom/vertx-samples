package com.example;

import com.example.client.HighLevelHttpClient;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;

import java.util.concurrent.TimeUnit;

@Slf4j
public class SleepServer {
    public static void main(String[] args) {
        System.setProperty("vertx.logger-delegate-factory-class-name",
                "io.vertx.core.logging.SLF4JLogDelegateFactory");

        Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
                new DropwizardMetricsOptions().setJmxEnabled(true)
        ));
        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route("/vertx/sleep/:ms").handler(
                routingContext -> {
                    String ms = routingContext.pathParam("ms");
                    vertx.setTimer(Long.parseLong(ms), t -> {
                        routingContext.response()
                                .end("Slept " + ms + " ms");
                    });
                }
        );
        router.route("/rx/sleep/:ms").handler(
                routingContext -> {
                    long ms = Long.parseLong(routingContext.pathParam("ms"));
                    Observable.timer(ms, TimeUnit.MILLISECONDS)
                            .subscribe(
                                    t -> {
                                        routingContext.response()
                                                .end("Slept " + ms + " milliseconds");
                                    }
                            );
                }
        );
        httpServer.requestHandler(router::accept).listen(8081);
    }
}
