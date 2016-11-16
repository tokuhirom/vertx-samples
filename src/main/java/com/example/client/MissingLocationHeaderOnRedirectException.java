package com.example.client;

public class MissingLocationHeaderOnRedirectException extends Exception {
    public MissingLocationHeaderOnRedirectException(String originalUrl) {
        super("Missing location header on redirect: " + originalUrl);
    }
}
