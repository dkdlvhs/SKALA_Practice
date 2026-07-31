package com.skala.shopapi.exception;

import lombok.Getter;

@Getter
public class ResponseException extends RuntimeException {
    private final Error error;
    private final String message;

    public ResponseException(Error error, String message) {
        super(message);
        this.error = error;
        this.message = message;
    }
}
