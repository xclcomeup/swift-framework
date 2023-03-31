package com.liepin.swift.framework.limit.rules.processor.fallback2s;

@SuppressWarnings("serial")
public class FallbackException extends RuntimeException {

    public FallbackException(String message, Throwable e) {
        super(message, e);
    }

}
