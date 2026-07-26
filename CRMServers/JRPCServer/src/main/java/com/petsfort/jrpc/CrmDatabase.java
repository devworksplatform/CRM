package com.petsfort.jrpc;

import java.sql.*;

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

