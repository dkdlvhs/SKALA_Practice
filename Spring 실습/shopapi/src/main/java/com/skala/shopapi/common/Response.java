package com.skala.shopapi.common;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Response {
    private boolean success;
    private String message;
    private Object data;
    private Map<String, Object> errors;
    private LocalDateTime timestamp;
}
