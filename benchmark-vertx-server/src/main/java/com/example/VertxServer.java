package com.example;

import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;

public class VertxServer {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route("/").handler(c -> {
            c.response().end("OK");
        });
        httpServer.requestHandler(router::accept)
                .listen(8080);
    }
}
