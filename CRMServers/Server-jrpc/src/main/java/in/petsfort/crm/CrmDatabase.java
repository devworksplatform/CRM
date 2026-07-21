package in.petsfort.crm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jay.rpc.RpcException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class CrmDatabase implements AutoCloseable {
    private static final Map<String, String> SCHEMAS = Map.ofEntries(
            Map.entry("products", "CREATE TABLE IF NOT EXISTS products (id TEXT PRIMARY KEY, product_id TEXT UNIQUE, product_name TEXT NOT NULL, product_desc TEXT, product_hsn TEXT DEFAULT '', product_cid TEXT DEFAULT '', product_img TEXT, cat_id TEXT, cat_sub TEXT, cost_rate REAL, cost_mrp REAL, cost_gst REAL, cost_dis REAL, offer_buy_qty INTEGER NOT NULL DEFAULT 0, offer_free_qty INTEGER NOT NULL DEFAULT 0, offer_active INTEGER NOT NULL DEFAULT 0, offer_group_id TEXT DEFAULT NULL, stock INTEGER, created_at TEXT, updated_at TEXT)"),
            Map.entry("offer_groups", "CREATE TABLE IF NOT EXISTS offer_groups (id TEXT PRIMARY KEY, name TEXT NOT NULL, description TEXT DEFAULT '', buy_qty INTEGER NOT NULL, free_qty INTEGER NOT NULL, product_ids TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'DRAFT', created_at TEXT NOT NULL, updated_at TEXT NOT NULL, canceled_at TEXT)"),
            Map.entry("orders", "CREATE TABLE IF NOT EXISTS orders (order_id TEXT PRIMARY KEY, user_id TEXT NOT NULL, items TEXT NOT NULL, items_detail TEXT NOT NULL, order_status TEXT NOT NULL, total_rate REAL, total_gst REAL, total_discount REAL, total REAL, created_at TEXT, address TEXT, notes TEXT)"),
            Map.entry("category", "CREATE TABLE IF NOT EXISTS category (id TEXT PRIMARY KEY, name TEXT NOT NULL, image TEXT DEFAULT '')"),
            Map.entry("subcategory", "CREATE TABLE IF NOT EXISTS subcategory (id TEXT PRIMARY KEY, parentid TEXT NOT NULL, name TEXT NOT NULL, image TEXT DEFAULT '')"),
            Map.entry("userdata", "CREATE TABLE IF NOT EXISTS userdata (id TEXT PRIMARY KEY, uid TEXT NOT NULL, name TEXT NOT NULL, contact TEXT DEFAULT 'N/A', gstin TEXT DEFAULT 'N/A', email TEXT NOT NULL, role TEXT NOT NULL, address TEXT NOT NULL, credits REAL, creditse TEXT NOT NULL, isblocked INTEGER NOT NULL DEFAULT 0)"),
            Map.entry("bills", "CREATE TABLE IF NOT EXISTS bills (order_id TEXT PRIMARY KEY, bill TEXT NOT NULL)"),
            Map.entry("credit_notes", "CREATE TABLE IF NOT EXISTS credit_notes (cn_id TEXT PRIMARY KEY, cn_number TEXT NOT NULL, original_invoice TEXT DEFAULT '', user_id TEXT NOT NULL, user_name TEXT DEFAULT '', user_gstin TEXT DEFAULT '', reason TEXT DEFAULT '', items TEXT NOT NULL, subtotal REAL DEFAULT 0, cgst_total REAL DEFAULT 0, sgst_total REAL DEFAULT 0, total REAL DEFAULT 0, created_at TEXT, notes TEXT DEFAULT '')"),
            Map.entry("debit_notes", "CREATE TABLE IF NOT EXISTS debit_notes (dn_id TEXT PRIMARY KEY, dn_number TEXT NOT NULL, original_invoice TEXT DEFAULT '', user_id TEXT NOT NULL, user_name TEXT DEFAULT '', user_gstin TEXT DEFAULT '', reason TEXT DEFAULT '', items TEXT NOT NULL, subtotal REAL DEFAULT 0, cgst_total REAL DEFAULT 0, sgst_total REAL DEFAULT 0, total REAL DEFAULT 0, created_at TEXT, notes TEXT DEFAULT '')")
    );

    private final Path path;
    private volatile boolean closed;

    CrmDatabase(Path path) {
        this.path = path.toAbsolutePath().normalize();
    }

    Path path() { return path; }

    void initialize() throws SQLException, IOException {
        Path parent = path.getParent();
        if (parent != null) Files.createDirectories(parent);
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            for (String schema : SCHEMAS.values()) statement.execute(schema);
            migrateProductColumns(connection);
        }
    }

    Connection open() throws SQLException {
        if (closed) throw new SQLException("CRM database is closed");
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA busy_timeout=10000");
            statement.execute("PRAGMA journal_mode=WAL");
            statement.execute("PRAGMA foreign_keys=ON");
        }
        return connection;
    }

    JsonArray query(String sql, List<Object> parameters) throws SQLException {
        try (Connection connection = open()) { return query(connection, sql, parameters); }
    }

    JsonObject one(String sql, List<Object> parameters) throws SQLException {
        try (Connection connection = open()) { return one(connection, sql, parameters); }
    }

    JsonArray query(Connection connection, String sql, List<Object> parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                JsonArray rows = new JsonArray();
                while (result.next()) rows.add(Jsons.row(result));
                return rows;
            }
        }
    }

    JsonObject one(Connection connection, String sql, List<Object> parameters) throws SQLException {
        JsonArray rows = query(connection, sql, parameters);
        return rows.isEmpty() ? null : rows.get(0).getAsJsonObject();
    }

    int update(String sql, List<Object> parameters) throws SQLException {
        try (Connection connection = open()) { return update(connection, sql, parameters); }
    }

    int update(Connection connection, String sql, List<Object> parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            return statement.executeUpdate();
        }
    }

    JsonObject insert(String table, JsonObject values, String idColumn, String idValue) throws SQLException, RpcException {
        try (Connection connection = open()) { return insert(connection, table, values, idColumn, idValue); }
    }

    JsonObject insert(Connection connection, String table, JsonObject values, String idColumn, String idValue) throws SQLException, RpcException {
        Jsons.identifier(table, "table");
        List<String> columns = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
            columns.add(Jsons.identifier(entry.getKey(), "column"));
            parameters.add(Jsons.sqlValue(entry.getValue()));
        }
        String placeholders = String.join(",", columns.stream().map(ignored -> "?").toArray(String[]::new));
        update(connection, "INSERT INTO " + table + " (" + String.join(",", columns) + ") VALUES (" + placeholders + ")", parameters);
        return one(connection, "SELECT * FROM " + table + " WHERE " + Jsons.identifier(idColumn, "idColumn") + "=?", List.of(idValue));
    }

    Path backup(Path requested) throws SQLException, IOException {
        Path target = requested.toAbsolutePath().normalize();
        Path parent = target.getParent();
        if (parent != null) Files.createDirectories(parent);
        try (Connection connection = open(); Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA wal_checkpoint(FULL)");
        }
        Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    synchronized void restore(Path source) throws IOException, SQLException {
        if (!Files.isRegularFile(source)) throw new IOException("Backup does not exist: " + source);
        Path safety = path.resolveSibling(path.getFileName() + ".before-restore-" + Instant.now().toEpochMilli());
        if (Files.exists(path)) Files.copy(path, safety, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(source, path, StandardCopyOption.REPLACE_EXISTING);
        initialize();
    }

    private static void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) statement.setObject(i + 1, parameters.get(i));
    }

    private static void migrateProductColumns(Connection connection) throws SQLException {
        List<String> existing = new ArrayList<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("PRAGMA table_info(products)")) {
            while (result.next()) existing.add(result.getString("name"));
        }
        Map<String, String> migrations = Map.of(
                "offer_buy_qty", "INTEGER NOT NULL DEFAULT 0",
                "offer_free_qty", "INTEGER NOT NULL DEFAULT 0",
                "offer_active", "INTEGER NOT NULL DEFAULT 0",
                "offer_group_id", "TEXT DEFAULT NULL");
        try (Statement statement = connection.createStatement()) {
            for (Map.Entry<String, String> migration : migrations.entrySet()) {
                if (!existing.contains(migration.getKey())) {
                    statement.execute("ALTER TABLE products ADD COLUMN " + migration.getKey() + " " + migration.getValue());
                }
            }
        }
    }

    static String uuid() { return UUID.randomUUID().toString(); }

    @Override public void close() { closed = true; }
}
