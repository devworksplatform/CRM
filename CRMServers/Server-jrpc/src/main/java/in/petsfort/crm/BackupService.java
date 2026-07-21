package in.petsfort.crm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class BackupService {
    static final String ADMIN_EMAIL = "dev@petsfort.in";
    private static final DateTimeFormatter ID_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd--HH-mm-ss");
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
    private final CrmDatabase database;
    private final FirebaseService firebase;
    private final Gson gson = new Gson();

    BackupService(CrmDatabase database, FirebaseService firebase) { this.database = database; this.firebase = firebase; }

    JsonObject create() throws Exception {
        requireFirebase();
        String id = ZonedDateTime.now(IST).format(ID_FORMAT);
        Path local = database.path().resolveSibling("backups").resolve(id + ".db");
        database.backup(local);
        JsonObject storage;
        try { storage = firebase.upload(local, "backups/sqliteDBs/" + id + ".db"); }
        catch (Exception error) { storage = new JsonObject(); storage.add("path", null); storage.add("url", null); storage.addProperty("err", error.getMessage()); }
        Map<String, Object> data = exportDatabase();
        firebase.set("tables/" + id, data); firebase.set("tables/latest", data);
        JsonObject realtime = new JsonObject(); realtime.addProperty("path", "tables/" + id); realtime.add("err", null);
        JsonObject result = new JsonObject(); result.add("realtimeDb", realtime); result.add("storage", storage);
        JsonObject log = new JsonObject(); log.addProperty("1", "Firebase backup completed successfully."); result.add("log", log); return result;
    }

    JsonObject restore(String id) throws Exception {
        validateId(id); requireFirebase(); Object value = firebase.get("tables/" + id);
        if (!(value instanceof Map)) throw new IllegalArgumentException("Backup not found: " + id);
        @SuppressWarnings("unchecked") Map<String, Object> tables = (Map<String, Object>) value;
        restoreDatabase(tables);
        JsonObject response = new JsonObject(); response.addProperty("path", "tables/" + id); response.add("err", null);
        JsonObject log = new JsonObject(); log.addProperty("1", "Restore completed successfully."); response.add("log", log); return response;
    }

    JsonArray list() throws Exception {
        requireFirebase(); Object root = firebase.get("tables"); JsonArray result = new JsonArray();
        if (!(root instanceof Map)) return result;
        List<String> ids = new ArrayList<>(); ((Map<?, ?>) root).keySet().forEach(key -> ids.add(String.valueOf(key)));
        ids.sort(Comparator.comparing((String id) -> id.equals("latest") ? "9999" : id).reversed());
        for (String id : ids) { JsonObject item = new JsonObject(); item.addProperty("id", id); item.addProperty("path", "tables/" + id); item.addProperty("is_latest", id.equals("latest"));
            if (id.equals("latest")) item.add("created_at", null); else { try { item.addProperty("created_at", ZonedDateTime.of(java.time.LocalDateTime.parse(id, ID_FORMAT), IST).toOffsetDateTime().toString()); } catch (RuntimeException e) { item.add("created_at", null); } }
            result.add(item); }
        return result;
    }

    boolean delete(String id) throws Exception { validateId(id); requireFirebase(); if (firebase.get("tables/" + id) == null) return false; firebase.delete("tables/" + id); return true; }

    JsonArray deleteSelected(Collection<String> input) throws Exception {
        JsonArray deleted = new JsonArray(); for (String id : new LinkedHashSet<>(input)) if (delete(id)) deleted.add(id); return deleted;
    }

    JsonArray deleteOlderThan(int days) throws Exception {
        if (days < 0) throw new IllegalArgumentException("Days must be zero or greater"); JsonArray deleted = new JsonArray(); Instant cutoff = Instant.now().minus(Duration.ofDays(days));
        for (JsonElement item : list()) { String id = item.getAsJsonObject().get("id").getAsString(); if (id.equals("latest")) continue;
            try { if (ZonedDateTime.of(java.time.LocalDateTime.parse(id, ID_FORMAT), IST).toInstant().isBefore(cutoff) && delete(id)) deleted.add(id); } catch (RuntimeException ignored) {} }
        return deleted;
    }

    JsonObject reset() throws Exception {
        JsonObject fresh = create(); String path = fresh.getAsJsonObject("realtimeDb").get("path").getAsString(); String created = path.substring(path.lastIndexOf('/') + 1);
        JsonArray deleted = new JsonArray(); for (JsonElement item : list()) { String id = item.getAsJsonObject().get("id").getAsString(); if (!id.equals("latest") && !id.equals(created) && delete(id)) deleted.add(id); }
        JsonObject result = new JsonObject(); result.addProperty("detail", "Backups reset and fresh backup created."); result.addProperty("created_id", created); result.add("deleted_ids", deleted); result.add("result", fresh); return result;
    }

    void verifyAdmin(String token) throws Exception {
        requireFirebase(); if (token == null || token.isBlank()) throw new SecurityException("Missing bearer token");
        String email = firebase.verifyEmail(token); if (!ADMIN_EMAIL.equalsIgnoreCase(email)) throw new SecurityException("Only " + ADMIN_EMAIL + " can manage backups");
    }

    private Map<String, Object> exportDatabase() throws Exception {
        Map<String, Object> tables = new LinkedHashMap<>();
        try (Connection connection = database.open(); Statement statement = connection.createStatement(); ResultSet names = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
            List<String> tableNames = new ArrayList<>(); while (names.next()) tableNames.add(names.getString(1));
            for (String table : tableNames) {
                String pk = primaryKey(connection, table); Map<String, Object> records = new LinkedHashMap<>();
                try (Statement rowsStatement = connection.createStatement(); ResultSet rows = rowsStatement.executeQuery("SELECT * FROM " + table)) {
                    ResultSetMetaData meta = rows.getMetaData(); while (rows.next()) { Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= meta.getColumnCount(); i++) row.put(meta.getColumnLabel(i), rows.getObject(i)); Object key = row.get(pk); if (key != null) records.put(String.valueOf(key), row); }
                }
                if (!records.isEmpty()) tables.put(table, records);
            }
        }
        return tables;
    }

    private void restoreDatabase(Map<String, Object> tables) throws Exception {
        Path safety = database.path().resolveSibling(database.path().getFileName() + ".before-firebase-restore-" + Instant.now().toEpochMilli()); database.backup(safety);
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                for (Map.Entry<String, Object> entry : tables.entrySet()) {
                    String table = Jsons.identifier(entry.getKey(), "table"); if (!(entry.getValue() instanceof Map)) continue;
                    database.update(connection, "DELETE FROM " + table, List.of());
                    for (Object rowValue : ((Map<?, ?>) entry.getValue()).values()) {
                        JsonObject row = gson.toJsonTree(rowValue).getAsJsonObject(); String pk = primaryKey(connection, table);
                        database.insert(connection, table, row, pk, row.get(pk).getAsString());
                    }
                }
                connection.commit();
            } catch (Exception error) { connection.rollback(); throw error; }
        }
    }

    private static String primaryKey(Connection connection, String table) throws Exception {
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            String first = null; while (result.next()) { if (first == null) first = result.getString("name"); if (result.getInt("pk") == 1) return result.getString("name"); } return first;
        }
    }

    private void requireFirebase() { if (!firebase.enabled()) throw new IllegalStateException("Firebase is not configured"); }
    private static void validateId(String id) { if (id == null || id.isBlank() || id.matches(".*[.#$\\[\\]/\\x00-\\x1f\\x7f].*")) throw new IllegalArgumentException("Invalid backup ID"); }
}
