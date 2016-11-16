package com.example;

import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.LoggerHandler;

public class AccessLogServer {
    public static void main(String[] args) {
        System.setProperty("vertx.logger-delegate-factory-class-name",
                "io.vertx.core.logging.SLF4JLogDelegateFactory");

        Vertx vertx = Vertx.vertx();
        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT));
        httpServer.requestHandler(router::accept).listen(8080);
    }
}
