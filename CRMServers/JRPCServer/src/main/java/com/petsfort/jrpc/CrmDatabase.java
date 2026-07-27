package com.petsfort.jrpc;

import com.google.gson.*;

import java.sql.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class CrmDatabase implements AutoCloseable {
    private final String jdbcUrl;

    CrmDatabase(CrmConfiguration configuration) {
        this.jdbcUrl = "jdbc:sqlite:" + configuration.database;
        try {
            Class.forName("org.sqlite.JDBC");
            migrate();
        } catch (ClassNotFoundException | SQLException error) {
            throw new IllegalStateException("Cannot initialize SQLite database " + configuration.database, error);
        }
    }

    Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=15000");
        }
        return connection;
    }

    JsonObject createBackupSnapshot() throws Exception {
        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("__format_version", 2);
        snapshot.addProperty("__created_at", OffsetDateTime.now().toString());
        JsonObject tables = new JsonObject();
        JsonArray schemaObjects = new JsonArray();

        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "SELECT name, sql FROM sqlite_master " +
                                "WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name");
                     ResultSet tableRows = statement.executeQuery()) {
                    while (tableRows.next()) {
                        String tableName = tableRows.getString("name");
                        JsonObject table = new JsonObject();
                        table.addProperty("schema", tableRows.getString("sql"));
                        JsonArray rows = new JsonArray();
                        try (Statement select = connection.createStatement();
                             ResultSet data = select.executeQuery(
                                     "SELECT * FROM " + Jsons.identifier(tableName))) {
                            while (data.next()) rows.add(Jsons.row(data));
                        }
                        table.add("rows", rows);
                        tables.add(tableName, table);
                    }
                }
                try (Statement statement = connection.createStatement();
                     ResultSet objects = statement.executeQuery(
                             "SELECT type, name, sql FROM sqlite_master " +
                                     "WHERE type IN ('index','trigger','view') AND sql IS NOT NULL " +
                                     "AND name NOT LIKE 'sqlite_%' ORDER BY type, name")) {
                    while (objects.next()) {
                        JsonObject object = new JsonObject();
                        object.addProperty("type", objects.getString("type"));
                        object.addProperty("name", objects.getString("name"));
                        object.addProperty("sql", objects.getString("sql"));
                        schemaObjects.add(object);
                    }
                }
                connection.commit();
            } catch (Exception failure) {
                connection.rollback();
                throw failure;
            }
        }
        snapshot.add("__tables", tables);
        snapshot.add("__schema_objects", schemaObjects);
        return snapshot;
    }

    void restoreBackupSnapshot(JsonObject snapshot) throws Exception {
        if (snapshot == null || snapshot.isEmpty()) {
            throw new ApiFailure(422, "Backup contains no database data.");
        }
        boolean versionTwo = snapshot.has("__format_version") && snapshot.has("__tables");
        JsonObject tables = versionTwo ? snapshot.getAsJsonObject("__tables") : snapshot;
        if (tables == null || tables.isEmpty()) {
            throw new ApiFailure(422, "Backup contains no tables.");
        }

        try (Connection connection = connect()) {
            try (Statement pragma = connection.createStatement()) {
                pragma.execute("PRAGMA foreign_keys=OFF");
            }
            connection.setAutoCommit(false);
            try {
                if (versionTwo) {
                    recreateTables(connection, tables);
                } else {
                    clearExistingTables(connection);
                }
                insertBackupRows(connection, tables, versionTwo);
                if (versionTwo) restoreSchemaObjects(connection, snapshot);
                connection.commit();
            } catch (Exception failure) {
                connection.rollback();
                throw failure;
            } finally {
                connection.setAutoCommit(true);
                try (Statement pragma = connection.createStatement()) {
                    pragma.execute("PRAGMA foreign_keys=ON");
                }
            }
        }
    }

    private void recreateTables(Connection connection, JsonObject tables) throws Exception {
        dropSchemaObjects(connection);
        for (String tableName : existingTableNames(connection)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP TABLE " + Jsons.identifier(tableName));
            }
        }
        for (Map.Entry<String, JsonElement> entry : tables.entrySet()) {
            Jsons.identifier(entry.getKey());
            JsonObject table = requireObject(entry.getValue(), "table " + entry.getKey());
            String schema = Jsons.requiredString(table, "schema");
            String normalized = schema.trim().toUpperCase(java.util.Locale.ROOT);
            if (!normalized.startsWith("CREATE TABLE")) {
                throw new ApiFailure(422, "Invalid schema in backup for table " + entry.getKey());
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(schema);
            }
        }
    }

    private void dropSchemaObjects(Connection connection) throws Exception {
        List<String[]> objects = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT type, name FROM sqlite_master " +
                             "WHERE type IN ('index','trigger','view') AND sql IS NOT NULL " +
                             "AND name NOT LIKE 'sqlite_%'")) {
            while (rows.next()) objects.add(new String[]{rows.getString("type"), rows.getString("name")});
        }
        for (String[] object : objects) {
            String keyword;
            switch (object[0]) {
                case "index": keyword = "INDEX"; break;
                case "trigger": keyword = "TRIGGER"; break;
                case "view": keyword = "VIEW"; break;
                default: throw new ApiFailure(422, "Invalid database object type.");
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute("DROP " + keyword + " " + Jsons.identifier(object[1]));
            }
        }
    }

    private void restoreSchemaObjects(Connection connection, JsonObject snapshot) throws Exception {
        JsonElement rawObjects = snapshot.get("__schema_objects");
        if (rawObjects == null || rawObjects.isJsonNull()) return;
        if (!rawObjects.isJsonArray()) {
            throw new ApiFailure(422, "Invalid schema objects in backup.");
        }
        for (JsonElement rawObject : rawObjects.getAsJsonArray()) {
            JsonObject object = requireObject(rawObject, "schema object");
            String type = Jsons.requiredString(object, "type");
            if (!Set.of("index", "trigger", "view").contains(type)) {
                throw new ApiFailure(422, "Invalid schema object type in backup.");
            }
            String sql = Jsons.requiredString(object, "sql").trim();
            String normalized = sql.toUpperCase(java.util.Locale.ROOT);
            String expected = "CREATE " + type.toUpperCase(java.util.Locale.ROOT);
            boolean valid = normalized.startsWith(expected)
                    || ("index".equals(type) && normalized.startsWith("CREATE UNIQUE INDEX"));
            if (!valid) {
                throw new ApiFailure(422, "Invalid " + type + " definition in backup.");
            }
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }
    }

    private void clearExistingTables(Connection connection) throws Exception {
        for (String tableName : existingTableNames(connection)) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("DELETE FROM " + Jsons.identifier(tableName));
            }
        }
    }

    private List<String> existingTableNames(Connection connection) throws SQLException {
        List<String> names = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
            while (rows.next()) names.add(rows.getString(1));
        }
        return names;
    }

    private void insertBackupRows(Connection connection, JsonObject tables, boolean versionTwo)
            throws Exception {
        for (Map.Entry<String, JsonElement> entry : tables.entrySet()) {
            String tableName = entry.getKey();
            Jsons.identifier(tableName);
            JsonElement rawRows = versionTwo
                    ? requireObject(entry.getValue(), "table " + tableName).get("rows")
                    : entry.getValue();
            if (rawRows == null || rawRows.isJsonNull()) continue;

            if (rawRows.isJsonArray()) {
                for (JsonElement row : rawRows.getAsJsonArray()) {
                    insertRow(connection, tableName, requireObject(row, "row in " + tableName));
                }
            } else if (rawRows.isJsonObject()) {
                for (Map.Entry<String, JsonElement> row : rawRows.getAsJsonObject().entrySet()) {
                    insertRow(connection, tableName,
                            requireObject(row.getValue(), "row " + row.getKey() + " in " + tableName));
                }
            } else {
                throw new ApiFailure(422, "Invalid rows in backup for table " + tableName);
            }
        }
    }

    private void insertRow(Connection connection, String tableName, JsonObject row) throws Exception {
        if (row.isEmpty()) return;
        List<String> columns = new ArrayList<>();
        StringBuilder placeholders = new StringBuilder();
        for (String column : row.keySet()) {
            columns.add(Jsons.identifier(column));
            if (placeholders.length() > 0) placeholders.append(',');
            placeholders.append('?');
        }
        String sql = "INSERT INTO " + Jsons.identifier(tableName) + " (" +
                String.join(",", columns) + ") VALUES (" + placeholders + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int index = 1;
            for (JsonElement value : row.asMap().values()) {
                bind(statement, index++, value);
            }
            statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, int index, JsonElement value) throws SQLException {
        if (value == null || value.isJsonNull()) {
            statement.setObject(index, null);
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
            statement.setInt(index, value.getAsBoolean() ? 1 : 0);
        } else if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            statement.setBigDecimal(index, value.getAsBigDecimal());
        } else if (value.isJsonPrimitive()) {
            statement.setString(index, value.getAsString());
        } else {
            statement.setString(index, value.toString());
        }
    }

    private JsonObject requireObject(JsonElement value, String description) throws ApiFailure {
        if (value == null || !value.isJsonObject()) {
            throw new ApiFailure(422, "Invalid " + description + " in backup.");
        }
        return value.getAsJsonObject();
    }

    private void migrate() throws SQLException {
        try (Connection connection = connect(); Statement sql = connection.createStatement()) {
            sql.execute("CREATE TABLE IF NOT EXISTS products (" +
                    "id TEXT PRIMARY KEY, product_id TEXT UNIQUE, product_name TEXT NOT NULL," +
                    "product_desc TEXT, product_hsn TEXT DEFAULT '', product_cid TEXT DEFAULT ''," +
                    "product_img TEXT, cat_id TEXT, cat_sub TEXT, cost_rate REAL, cost_mrp REAL," +
                    "cost_gst REAL, cost_dis REAL, offer_buy_qty INTEGER NOT NULL DEFAULT 0," +
                    "offer_free_qty INTEGER NOT NULL DEFAULT 0, offer_active INTEGER NOT NULL DEFAULT 0," +
                    "offer_group_id TEXT DEFAULT NULL, stock INTEGER, created_at TIMESTAMP, updated_at TIMESTAMP)");
            sql.execute("CREATE TABLE IF NOT EXISTS offer_groups (" +
                    "id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT DEFAULT ''," +
                    "buy_qty INTEGER NOT NULL, free_qty INTEGER NOT NULL, product_ids TEXT NOT NULL," +
                    "status TEXT NOT NULL DEFAULT 'DRAFT', created_at TEXT NOT NULL," +
                    "updated_at TEXT NOT NULL, canceled_at TEXT)");
            sql.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "order_id TEXT PRIMARY KEY, user_id TEXT NOT NULL, items TEXT NOT NULL," +
                    "items_detail TEXT NOT NULL, order_status TEXT NOT NULL, total_rate REAL," +
                    "total_gst REAL, total_discount REAL, total REAL, created_at TEXT," +
                    "address TEXT, notes TEXT)");
            sql.execute("CREATE TABLE IF NOT EXISTS category (" +
                    "id TEXT PRIMARY KEY, name TEXT NOT NULL, image TEXT DEFAULT '')");
            sql.execute("CREATE TABLE IF NOT EXISTS subcategory (" +
                    "id TEXT PRIMARY KEY, parentid TEXT NOT NULL, name TEXT NOT NULL, image TEXT DEFAULT '')");
            sql.execute("CREATE TABLE IF NOT EXISTS userdata (" +
                    "id TEXT PRIMARY KEY, uid TEXT NOT NULL, name TEXT NOT NULL, contact TEXT DEFAULT 'N/A'," +
                    "gstin TEXT DEFAULT 'N/A', email TEXT NOT NULL, role TEXT NOT NULL, address TEXT NOT NULL," +
                    "credits REAL, creditse TEXT NOT NULL, isblocked INTEGER NOT NULL DEFAULT 0)");
            sql.execute("CREATE TABLE IF NOT EXISTS bills (order_id TEXT PRIMARY KEY, bill TEXT NOT NULL)");
            sql.execute("CREATE TABLE IF NOT EXISTS credit_notes (" +
                    "cn_id TEXT PRIMARY KEY, cn_number TEXT NOT NULL, original_invoice TEXT DEFAULT ''," +
                    "user_id TEXT NOT NULL, user_name TEXT DEFAULT '', user_gstin TEXT DEFAULT ''," +
                    "reason TEXT DEFAULT '', items TEXT NOT NULL, subtotal REAL DEFAULT 0," +
                    "cgst_total REAL DEFAULT 0, sgst_total REAL DEFAULT 0, total REAL DEFAULT 0," +
                    "created_at TEXT, notes TEXT DEFAULT '')");
            sql.execute("CREATE TABLE IF NOT EXISTS debit_notes (" +
                    "dn_id TEXT PRIMARY KEY, dn_number TEXT NOT NULL, original_invoice TEXT DEFAULT ''," +
                    "user_id TEXT NOT NULL, user_name TEXT DEFAULT '', user_gstin TEXT DEFAULT ''," +
                    "reason TEXT DEFAULT '', items TEXT NOT NULL, subtotal REAL DEFAULT 0," +
                    "cgst_total REAL DEFAULT 0, sgst_total REAL DEFAULT 0, total REAL DEFAULT 0," +
                    "created_at TEXT, notes TEXT DEFAULT '')");
            ensureProductColumn(connection, "offer_buy_qty", "INTEGER NOT NULL DEFAULT 0");
            ensureProductColumn(connection, "offer_free_qty", "INTEGER NOT NULL DEFAULT 0");
            ensureProductColumn(connection, "offer_active", "INTEGER NOT NULL DEFAULT 0");
            ensureProductColumn(connection, "offer_group_id", "TEXT DEFAULT NULL");
        }
    }

    private void ensureProductColumn(Connection connection, String name, String definition)
            throws SQLException {
        boolean found = false;
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("PRAGMA table_info(products)")) {
            while (rows.next()) {
                if (name.equals(rows.getString("name"))) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("ALTER TABLE products ADD COLUMN " + name + " " + definition);
            }
        }
    }

    @Override
    public void close() {
        // Connections are operation-scoped; no persistent pool is owned here.
    }
}
