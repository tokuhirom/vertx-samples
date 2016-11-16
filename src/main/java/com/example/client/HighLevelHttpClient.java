package com.example.client;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.subjects.ReplaySubject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;

@Slf4j
public class HighLevelHttpClient {
    private final HttpClient httpClient;
    private final HttpClient httpsClient;
    private final int maxRedirects;

    public HighLevelHttpClient(Vertx vertx, HttpClientOptions httpClientOptions, int maxRedirects) {
        this.maxRedirects = maxRedirects;
        this.httpClient = vertx.createHttpClient(
                new HttpClientOptions(httpClientOptions).setSsl(false));
        this.httpsClient = vertx.createHttpClient(
                new HttpClientOptions(httpClientOptions).setSsl(true));
    }

    public Observable<HttpClientResponse> request(
            HttpMethod method,
            String url,
            io.netty.handler.codec.http.HttpHeaders headers) {
        return request(method, url, headers,
                HttpClientRequest::end);
    }

    public Observable<HttpClientResponse> request(
            HttpMethod method,
            String url,
            io.netty.handler.codec.http.HttpHeaders headers,
            String content) {
        return request(method, url, headers, httpClientRequest -> {
            httpClientRequest.end(content);
        });
    }

    private Observable<HttpClientResponse> request(
            HttpMethod method,
            String url,
            io.netty.handler.codec.http.HttpHeaders headers,
            Consumer<HttpClientRequest> finalizer) {
        ReplaySubject<HttpClientResponse> subject = ReplaySubject.create();
        return doRequest(method, url, url, headers, finalizer, subject, maxRedirects);
    }

    private Observable<HttpClientResponse> doRequest(
            HttpMethod method,
            String originalUrl,
            String url,
            io.netty.handler.codec.http.HttpHeaders headers,
            Consumer<HttpClientRequest> finalizer,
            ReplaySubject<HttpClientResponse> subject,
            int remainRedirects) {
        log.debug("Requesting {}(original: {})",
                url, originalUrl);
        HttpClient client = url.startsWith("https") ? httpsClient : httpClient;
        HttpClientRequest request = client.requestAbs(HttpMethod.GET, url, httpClientResponse -> {
            // TODO 302 igai mo care
            if (httpClientResponse.statusCode() == 302) {
                if (remainRedirects == 0) {
                    subject.onError(new TooManyRedirectException(originalUrl));
                    return;
                }
                String location = httpClientResponse.getHeader("Location");
                if (location == null) {
                    subject.onError(new MissingLocationHeaderOnRedirectException(originalUrl));
                    return;
                }

                try {
                    URL newUrl = new URL(new URL(url), location);
                    doRequest(method, originalUrl, newUrl.toExternalForm(), headers, finalizer, subject, remainRedirects - 1);
                } catch (MalformedURLException e) {
                    subject.onError(new MalformedUrlInRedirectException(originalUrl, e));
                }
            } else {
                subject.onNext(httpClientResponse);
                subject.onCompleted();
            }
        }).exceptionHandler(subject::onError);
        finalizer.accept(request);
        return subject;
    }
}
