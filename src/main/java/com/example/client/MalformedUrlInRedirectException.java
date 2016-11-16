package com.example.client;

import sun.reflect.annotation.ExceptionProxy;

import java.net.MalformedURLException;

public class MalformedUrlInRedirectException extends Exception {
    public MalformedUrlInRedirectException(String originalUrl, MalformedURLException e) {
        super("Malformed url in redirect: " + originalUrl, e);
    }
}
