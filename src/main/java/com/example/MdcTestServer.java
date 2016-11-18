package com.example;

import io.vertx.core.VertxOptions;
import io.vertx.ext.dropwizard.DropwizardMetricsOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpServer;
import io.vertx.rxjava.ext.web.Router;
import org.slf4j.MDC;
import rx.Observable;
import rx.plugins.RxJavaHooks;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MdcTestServer {
    public static void main(String[] args) {
        new MdcTestServer().run(8082).toBlocking().subscribe();
    }

    public Observable<HttpServer> run(int port) {
        RxJavaHooks.setOnScheduleAction((orig) -> {
            Map<String, String> context = MDC.getCopyOfContextMap();
            return () -> {
                final Map<String, String> originalMdc = MDC.getCopyOfContextMap();

                if (context != null) {
                    MDC.setContextMap(context);
                }

                try {
                    orig.call();
                } finally {
                    if (originalMdc != null) {
                        MDC.setContextMap(originalMdc);
                    }
                }
            };
        });

        Vertx vertx = Vertx.vertx(new VertxOptions().setMetricsOptions(
                new DropwizardMetricsOptions().setJmxEnabled(true)
        ));
        HttpServer httpServer = vertx.createHttpServer();
        Router router = Router.router(vertx);
        router.route().handler(context -> {
            context.next();
            context.addBodyEndHandler(handler -> {
                MDC.clear();
            });
        });
        router.route("/vertx/mdc/:id").handler(
                routingContext -> {
                    String id = routingContext.pathParam("id");
                    MDC.put("id", id);
                    vertx.setTimer(1, t -> {
                        // This case fails because RxJavaHooks never work on Vertx#setTimer.
                        routingContext.response()
                                .end(MDC.get("id"));
                    });
                }
        );
        router.route("/rx/mdc/:id").handler(
                routingContext -> {
                    String id = routingContext.pathParam("id");
                    MDC.put("id", id);
                    routingContext.response()
                            .putHeader("X-ID", id);
                    Observable.timer(Long.parseLong(id), TimeUnit.MILLISECONDS)
                            .doOnCompleted(() -> {
                                routingContext.response()
                                        .end(MDC.get("id"));
                            })
                            .subscribe();
                }
        );
        return httpServer.requestHandler(router::accept)
                .listenObservable(port);
    }
}
