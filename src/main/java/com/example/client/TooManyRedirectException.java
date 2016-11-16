package com.example.client;

public class TooManyRedirectException extends Exception {
    public TooManyRedirectException(String originalUrl) {
        super("Too many redirect: " + originalUrl);
    }
}
