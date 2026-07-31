package com.skala.shopapi.exception;

import java.util.ArrayList;
import java.util.List;

public class ParameterException extends RuntimeException {
    private final List<String> fields = new ArrayList<>();

    public ParameterException(List<String> fields) {
        this.fields.addAll(fields);
    }

    public List<String> getFields() {
        return fields;
    }
}
