package com.example;

import com.example.client.HighLevelHttpClient;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.buffer.Buffer;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;

import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpClientServer {
    public static void main(String[] args) {
        System.setProperty("vertx.logger-delegate-factory-class-name",
                "io.vertx.core.logging.SLF4JLogDelegateFactory");

        Vertx vertx = Vertx.vertx();
        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);
        HighLevelHttpClient httpClient = new HighLevelHttpClient(vertx, new HttpClientOptions(), 7);
        router.route("/http").handler(
                routingContext -> {
                    String url = routingContext.request().getParam("url");
                    if (url == null) {
                        routingContext.response()
                                .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                                .end("Missing mandatory query parameter: url");
                        return;
                    }
                    httpClient.request(
                            routingContext.request().method(),
                            url,
                            new DefaultHttpHeaders())
                            .subscribe(response -> {
                                log.info("Response: {}",
                                        response.statusCode());
                                routingContext.response()
                                        .setStatusCode(response.statusCode())
                                        .setChunked(true);
                                response.toObservable()
                                        .map(Buffer::toString)
                                        .collect(StringBuilder::new, StringBuilder::append)
                                        .map(StringBuilder::toString)
                                        .subscribe(body -> {
                                            log.info("body: {}", body.substring(0, 30).replaceAll("\\s+", ""));
                                            routingContext.response()
                                                    .end(body.substring(0, 30).replaceAll("\\s+", ""));
                                        });

                            });
                }
        );
        httpServer.requestHandler(router::accept).listen(8080);
    }
}