package com.petsfort.jrpc;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

final class ApiFailure extends Exception {
    final int statusCode;
    final JsonElement detail;

    ApiFailure(int statusCode, String detail) {
        this(statusCode, new JsonPrimitive(detail));
    }

    ApiFailure(int statusCode, JsonElement detail) {
        super(detail == null ? "" : detail.toString());
        this.statusCode = statusCode;
        this.detail = detail;
    }
}

