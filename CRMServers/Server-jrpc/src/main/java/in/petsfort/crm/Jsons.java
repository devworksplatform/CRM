package in.petsfort.crm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jay.rpc.RpcException;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class Jsons {
    private static final Pattern IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private Jsons() {}

    static String requiredString(JsonObject request, String name) throws RpcException {
        String value = optionalString(request, name, null);
        if (value == null || value.isBlank()) {
            throw new RpcException("INVALID_REQUEST", name + " is required");
        }
        return value;
    }

    static String optionalString(JsonObject object, String name, String fallback) throws RpcException {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) return fallback;
        if (!value.isJsonPrimitive()) throw new RpcException("INVALID_REQUEST", name + " must be a scalar value");
        return value.getAsString();
    }

    static int optionalInt(JsonObject object, String name, int fallback) throws RpcException {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) return fallback;
        try { return value.getAsInt(); }
        catch (RuntimeException error) { throw new RpcException("INVALID_REQUEST", name + " must be an integer", error); }
    }

    static double optionalDouble(JsonObject object, String name, double fallback) throws RpcException {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) return fallback;
        try { return value.getAsDouble(); }
        catch (RuntimeException error) { throw new RpcException("INVALID_REQUEST", name + " must be a number", error); }
    }

    static boolean optionalBoolean(JsonObject object, String name, boolean fallback) throws RpcException {
        JsonElement value = object.get(name);
        if (value == null || value.isJsonNull()) return fallback;
        try { return value.getAsBoolean(); }
        catch (RuntimeException error) { throw new RpcException("INVALID_REQUEST", name + " must be a boolean", error); }
    }

    static JsonObject body(JsonObject request) throws RpcException {
        JsonElement body = request.get("body");
        if (body == null) return request;
        if (!body.isJsonObject()) throw new RpcException("INVALID_REQUEST", "body must be a JSON object");
        return body.getAsJsonObject();
    }

    static JsonArray requiredArray(JsonObject object, String name) throws RpcException {
        JsonElement value = object.get(name);
        if (value == null || !value.isJsonArray()) throw new RpcException("INVALID_REQUEST", name + " must be an array");
        return value.getAsJsonArray();
    }

    static String identifier(String value, String field) throws RpcException {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw new RpcException("INVALID_REQUEST", field + " is not a valid SQL identifier");
        }
        return value;
    }

    static String direction(String value) {
        return "DESC".equalsIgnoreCase(value) ? "DESC" : "ASC";
    }

    static JsonObject row(ResultSet result) throws SQLException {
        JsonObject object = new JsonObject();
        ResultSetMetaData meta = result.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String name = meta.getColumnLabel(i);
            Object value = result.getObject(i);
            if (value == null) object.add(name, JsonNull.INSTANCE);
            else if (value instanceof Number) object.addProperty(name, (Number) value);
            else if (value instanceof Boolean) object.addProperty(name, (Boolean) value);
            else object.addProperty(name, value.toString());
        }
        normalizeRow(object);
        return object;
    }

    static void normalizeRow(JsonObject object) {
        for (String field : Set.of("product_img", "items", "items_detail", "product_ids")) {
            JsonElement value = object.get(field);
            if (value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                try { object.add(field, JsonParser.parseString(value.getAsString())); }
                catch (RuntimeException ignored) {
                    object.add(field, field.equals("items") ? new JsonObject() : new JsonArray());
                }
            }
        }
        if (object.has("offer_active") && !object.get("offer_active").isJsonNull()) {
            object.addProperty("offer_active", object.get("offer_active").getAsInt() != 0);
        }
        if (object.has("cost_mrp") && object.has("cost_dis")
                && !object.get("cost_mrp").isJsonNull() && !object.get("cost_dis").isJsonNull()) {
            double mrp = object.get("cost_mrp").getAsDouble();
            double discount = object.get("cost_dis").getAsDouble();
            object.addProperty("cost_rate", mrp - (mrp * discount / 100.0));
        }
    }

    static String jsonForDatabase(JsonElement value) {
        if (value == null || value.isJsonNull()) return null;
        return value.isJsonArray() || value.isJsonObject() ? value.toString() : value.getAsString();
    }

    static Object sqlValue(JsonElement value) {
        if (value == null || value.isJsonNull()) return null;
        if (value.isJsonArray() || value.isJsonObject()) return value.toString();
        if (value.getAsJsonPrimitive().isBoolean()) return value.getAsBoolean() ? 1 : 0;
        if (value.getAsJsonPrimitive().isNumber()) return value.getAsNumber();
        return value.getAsString();
    }

    static String errorCode(SQLException error) {
        String text = String.valueOf(error.getMessage()).toLowerCase(Locale.ROOT);
        if (text.contains("unique constraint")) return "CONFLICT";
        if (text.contains("database is locked") || text.contains("database busy")) return "DATABASE_BUSY";
        return "DATABASE_ERROR";
    }
}
