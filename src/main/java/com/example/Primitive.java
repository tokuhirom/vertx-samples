package com.example;

import io.vertx.core.Vertx;
import io.vertx.ext.web.Router;

public class Primitive {
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.createHttpClient()
                .get("http://example.com/", response -> {
                    System.out.println(response.statusCode());
                })
                .end();
        vertx.createHttpServer()
                .requestHandler(request -> {
                    request.response()
                            .end("OK");
                })
                .listen(8080);

        Router router = Router.router(vertx);
        router.route("/hello/:name")
                .handler(c -> {
                    c.response().end(
                            "Hello, " + c.pathParam("name"));
                });
        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(8081);
    }
}
