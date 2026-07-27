package com.petsfort.jrpc;

import com.google.gson.*;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/** Firebase Realtime Database backup administration and atomic SQLite restore. */
final class CrmBackupService {
    private static final Gson GSON = new Gson();
    private static final String ROOT = "tables";
    private static final String LATEST = "latest";
    private static final String ADMIN_TOKEN = "_auth_token";
    private static final ZoneId BACKUP_ZONE = ZoneOffset.ofHoursMinutes(5, 30);
    private static final DateTimeFormatter BACKUP_ID =
            DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm-ss");
    private static final Pattern SAFE_ID = Pattern.compile("(?:latest|\\d{4}-\\d{2}-\\d{2}--\\d{2}-\\d{2}-\\d{2})");

    private final CrmDatabase database;

    CrmBackupService(CrmDatabase database) {
        this.database = database;
    }

    void list(JsonObject request, JsonObject response) throws Exception {
        FirebaseBridge bridge = authorized(request);
        JsonObject root = objectValue(bridge.getValue(ROOT));
        List<JsonObject> backups = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            JsonObject item = describe(entry.getKey(), entry.getValue());
            backups.add(item);
        }
        backups.sort((left, right) -> {
            boolean leftLatest = left.get("is_latest").getAsBoolean();
            boolean rightLatest = right.get("is_latest").getAsBoolean();
            if (leftLatest != rightLatest) return leftLatest ? -1 : 1;
            return right.get("id").getAsString().compareTo(left.get("id").getAsString());
        });
        JsonArray result = new JsonArray();
        backups.forEach(result::add);
        response.add("backups", result);
    }

    synchronized void create(JsonObject request, JsonObject response) throws Exception {
        FirebaseBridge bridge = authorized(request);
        JsonObject item = createBackup(bridge);
        response.addProperty("detail", "Backup created successfully.");
        response.add("backup", item);
    }

    synchronized void delete(JsonObject request, JsonObject response) throws Exception {
        FirebaseBridge bridge = authorized(request);
        String id = backupId(request);
        if (bridge.getValue(path(id)) == null) throw new ApiFailure(404, "Backup not found.");
        bridge.deleteValue(path(id));
        response.addProperty("detail", "Backup deleted.");
        response.addProperty("deleted_id", id);
    }

    synchronized void deleteSelected(JsonObject request, JsonObject response) throws Exception {
        FirebaseBridge bridge = authorized(request);
        JsonObject body = body(request);
        JsonElement rawIds = body.get("ids");
        if (rawIds == null || !rawIds.isJsonArray() || rawIds.getAsJsonArray().size() == 0) {
            throw new ApiFailure(422, "A non-empty ids array is required.");
        }
        JsonArray deleted = new JsonArray();
        Set<String> unique = new LinkedHashSet<>();
        for (JsonElement rawId : rawIds.getAsJsonArray()) {
            if (!rawId.isJsonPrimitive() || !rawId.getAsJsonPrimitive().isString()) {
                throw new ApiFailure(422, "Every backup ID must be a string.");
            }
            unique.add(validateId(rawId.getAsString()));
        }
        for (String id : unique) {
            if (bridge.getValue(path(id)) != null) {
                bridge.deleteValue(path(id));
                deleted.add(id);
            }
        }
        response.addProperty("detail", "Selected backups deleted.");
        response.add("deleted_ids", deleted);
    }

    synchronized void deleteOlderThan(JsonObject request, JsonObject response) throws Exception {
        FirebaseBridge bridge = authorized(request);
        int days = Jsons.optionalInt(request, "days", -1);
        if (days < 0) throw new ApiFailure(422, "Days must be zero or greater.");
        ZonedDateTime cutoff = ZonedDateTime.now(BACKUP_ZONE).minusDays(days);
        JsonObject root = objectValue(bridge.getValue(ROOT));
        JsonArray deleted = new JsonArray();
        for (String id : new ArrayList<>(root.keySet())) {
            if (LATEST.equals(id)) continue;
            ZonedDateTime created = createdAt(id);
            if (created != null && created.isBefore(cutoff)) {
                bridge.deleteValue(path(validateId(id)));
                deleted.add(id);
            }
        }
        response.addProperty("detail", "Old backups deleted.");
        response.add("deleted_ids", deleted);
    }

    synchronized void reset(JsonObject request, JsonObject response) throws Exception {
        FirebaseBridge bridge = authorized(request);
        JsonObject created = createBackup(bridge);
        String createdId = created.get("id").getAsString();
        JsonObject root = objectValue(bridge.getValue(ROOT));
        JsonArray deleted = new JsonArray();
        for (String id : new ArrayList<>(root.keySet())) {
            if (LATEST.equals(id) || createdId.equals(id)) continue;
            bridge.deleteValue(path(validateId(id)));
            deleted.add(id);
        }
        response.addProperty("detail", "Backups reset and fresh backup created.");
        response.add("backup", created);
        response.add("deleted_ids", deleted);
    }

    synchronized void restore(JsonObject request, JsonObject response) throws Exception {
        FirebaseBridge bridge = authorized(request);
        String id = backupId(request);
        Object stored = bridge.getValue(path(id));
        if (stored == null) throw new ApiFailure(404, "Backup not found.");
        JsonObject selected = objectValue(stored);

        // Always preserve the current live state before applying a destructive restore.
        JsonObject safetyBackup = createBackup(bridge);
        database.restoreBackupSnapshot(selected);

        response.addProperty("detail", "Database restored successfully.");
        response.addProperty("restored_id", id);
        response.add("safety_backup", safetyBackup);
    }

    private FirebaseBridge authorized(JsonObject request) throws Exception {
        FirebaseBridge bridge = FirebaseBridge.instance();
        bridge.requireBackupAdministrator(Jsons.optionalString(request, ADMIN_TOKEN, null));
        return bridge;
    }

    private JsonObject createBackup(FirebaseBridge bridge) throws Exception {
        JsonObject snapshot = database.createBackupSnapshot();
        String id = nextBackupId(bridge);
        Object firebaseValue = GSON.fromJson(snapshot, Object.class);
        bridge.setValue(path(id), firebaseValue);
        bridge.setValue(path(LATEST), firebaseValue);
        return describe(id, snapshot);
    }

    private String nextBackupId(FirebaseBridge bridge) throws Exception {
        ZonedDateTime candidate = ZonedDateTime.now(BACKUP_ZONE);
        for (int attempt = 0; attempt < 60; attempt++) {
            String id = BACKUP_ID.format(candidate.plusSeconds(attempt));
            if (bridge.getValue(path(id)) == null) return id;
        }
        throw new ApiFailure(409, "Could not allocate a unique backup ID.");
    }

    private JsonObject describe(String id, JsonElement snapshot) {
        JsonObject item = new JsonObject();
        item.addProperty("id", id);
        item.addProperty("path", path(id));
        item.addProperty("is_latest", LATEST.equals(id));
        ZonedDateTime created = createdAt(id);
        if (created == null && snapshot != null && snapshot.isJsonObject()) {
            String raw = Jsons.optionalString(snapshot.getAsJsonObject(), "__created_at", null);
            if (raw != null) {
                try {
                    item.addProperty("created_at", OffsetDateTime.parse(raw).toString());
                } catch (DateTimeParseException ignored) {
                    item.add("created_at", JsonNull.INSTANCE);
                }
            } else {
                item.add("created_at", JsonNull.INSTANCE);
            }
        } else if (created == null) {
            item.add("created_at", JsonNull.INSTANCE);
        } else {
            item.addProperty("created_at", created.toOffsetDateTime().toString());
        }
        JsonObject counts = countSnapshot(snapshot);
        item.addProperty("table_count", counts.get("tables").getAsInt());
        item.addProperty("record_count", counts.get("records").getAsInt());
        int size = snapshot == null ? 0
                : snapshot.toString().getBytes(StandardCharsets.UTF_8).length;
        item.addProperty("size_bytes", size);
        return item;
    }

    private JsonObject countSnapshot(JsonElement snapshot) {
        int tableCount = 0;
        int recordCount = 0;
        if (snapshot != null && snapshot.isJsonObject()) {
            JsonObject root = snapshot.getAsJsonObject();
            JsonObject tables = root.has("__tables") && root.get("__tables").isJsonObject()
                    ? root.getAsJsonObject("__tables") : root;
            for (Map.Entry<String, JsonElement> entry : tables.entrySet()) {
                if (entry.getKey().startsWith("__") || !entry.getValue().isJsonObject()) continue;
                tableCount++;
                JsonElement rows = root.has("__tables")
                        ? entry.getValue().getAsJsonObject().get("rows") : entry.getValue();
                if (rows != null && rows.isJsonArray()) recordCount += rows.getAsJsonArray().size();
                else if (rows != null && rows.isJsonObject()) recordCount += rows.getAsJsonObject().size();
            }
        }
        JsonObject counts = new JsonObject();
        counts.addProperty("tables", tableCount);
        counts.addProperty("records", recordCount);
        return counts;
    }

    private ZonedDateTime createdAt(String id) {
        if (LATEST.equals(id)) return null;
        try {
            return LocalDateTime.parse(id, BACKUP_ID).atZone(BACKUP_ZONE);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private String backupId(JsonObject request) throws ApiFailure {
        return validateId(Jsons.requiredString(request, "backup_id"));
    }

    private String validateId(String id) throws ApiFailure {
        if (id == null || !SAFE_ID.matcher(id).matches()) {
            throw new ApiFailure(400, "Invalid backup ID.");
        }
        return id;
    }

    private String path(String id) {
        return ROOT + "/" + id;
    }

    private JsonObject body(JsonObject request) throws ApiFailure {
        JsonElement body = request.get("body");
        if (body == null) return request;
        if (!body.isJsonObject()) throw new ApiFailure(400, "A JSON object body is required.");
        return body.getAsJsonObject();
    }

    private JsonObject objectValue(Object value) throws ApiFailure {
        if (value == null) return new JsonObject();
        JsonElement converted = GSON.toJsonTree(value);
        if (!converted.isJsonObject()) throw new ApiFailure(502, "Backup data has an invalid format.");
        return converted.getAsJsonObject();
    }
}
