package com.skala.shopapi.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.skala.shopapi.common.Response;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseException.class)
    public ResponseEntity<Response> handleResponseException(ResponseException ex) {
        Response response = Response.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .errors(Map.of("error", ex.getError().name()))
                .timestamp(LocalDateTime.now())
                .build();

        HttpStatus status = switch (ex.getError()) {
            case NOT_AUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case NOT_AUTHORIZED -> HttpStatus.FORBIDDEN;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(ParameterException.class)
    public ResponseEntity<Response> handleParameterException(ParameterException ex) {
        Map<String, Object> errors = new LinkedHashMap<>();
        errors.put("invalidFields", ex.getFields());

        Response response = Response.builder()
                .success(false)
                .message("입력값이 올바르지 않습니다.")
                .data(null)
                .errors(errors)
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Response> handleRuntimeException(RuntimeException ex) {
        Response response = Response.builder()
                .success(false)
                .message("서버 내부 오류가 발생했습니다.")
                .data(null)
                .errors(Map.of("exception", ex.getClass().getSimpleName()))
                .timestamp(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
