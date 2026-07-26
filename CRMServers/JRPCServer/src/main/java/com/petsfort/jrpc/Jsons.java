package com.petsfort.jrpc;

import com.google.gson.*;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.regex.Pattern;

final class Jsons {
    private static final Gson GSON = new Gson();
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private Jsons() {}

    static String requiredString(JsonObject object, String key) throws ApiFailure {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull() || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isString() || value.getAsString().isEmpty()) {
            throw new ApiFailure(422, key + " is required");
        }
        return value.getAsString();
    }

    static String optionalString(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }

    static int optionalInt(JsonObject object, String key, int fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsInt();
    }

    static double optionalDouble(JsonObject object, String key, double fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsDouble();
    }

    static boolean optionalBoolean(JsonObject object, String key, boolean fallback) {
        JsonElement value = object.get(key);
        return value == null || value.isJsonNull() ? fallback : value.getAsBoolean();
    }

    static JsonObject row(ResultSet rs) throws SQLException {
        JsonObject result = new JsonObject();
        ResultSetMetaData metadata = rs.getMetaData();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            String key = metadata.getColumnLabel(i);
            Object value = rs.getObject(i);
            if (value == null) result.add(key, JsonNull.INSTANCE);
            else if (value instanceof Boolean) result.addProperty(key, (Boolean) value);
            else if (value instanceof Number) result.addProperty(key, (Number) value);
            else result.addProperty(key, String.valueOf(value));
        }
        return result;
    }

    static JsonElement parseOr(JsonElement value, JsonElement fallback) {
        if (value == null || value.isJsonNull()) return fallback;
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) return value;
        try {
            return JsonParser.parseString(value.getAsString());
        } catch (JsonParseException ignored) {
            return fallback;
        }
    }

    static String json(JsonElement value) {
        return GSON.toJson(value == null ? JsonNull.INSTANCE : value);
    }

    static String identifier(String value) throws ApiFailure {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw new ApiFailure(400, "Invalid SQL identifier: " + value);
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    static void copy(JsonObject source, JsonObject destination) {
        for (java.util.Map.Entry<String, JsonElement> entry : source.entrySet()) {
            destination.add(entry.getKey(), entry.getValue());
        }
    }

    static double round2(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }
}
