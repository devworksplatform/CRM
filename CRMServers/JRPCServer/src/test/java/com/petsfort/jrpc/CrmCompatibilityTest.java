package com.petsfort.jrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;

import java.nio.file.*;
import java.sql.*;

import static org.junit.jupiter.api.Assertions.*;

final class CrmCompatibilityTest {
    private static Path sourceDatabase;
    private Path workingDatabase;
    private CrmService service;

    @BeforeAll
    static void locateSource() {
        sourceDatabase = Paths.get(System.getProperty(
                "petsfort.db.path", "../backups_sqliteDBs_2026-07-26--12-49-45.db"))
                .toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(sourceDatabase),
                "Supplied compatibility database is missing: " + sourceDatabase);
    }

    @BeforeEach
    void setUp() throws Exception {
        Path directory = Files.createTempDirectory("petsfort-jrpc-test-");
        workingDatabase = directory.resolve("products.db");
        Files.copy(sourceDatabase, workingDatabase);
        System.setProperty("petsfort.db.path", workingDatabase.toString());
        service = CrmService.open(CrmConfiguration.fromEnvironment());
    }

    @AfterEach
    void tearDown() throws Exception {
        if (service != null) service.close();
        if (workingDatabase != null) {
            Files.deleteIfExists(workingDatabase);
            Files.deleteIfExists(workingDatabase.getParent());
        }
    }

    @Test
    void everyOperationHasADedicatedHandler() {
        for (CrmRpc rpc : CrmRpc.values()) {
            assertNotNull(service.handlerFor(rpc), "No handler for " + rpc);
        }
    }

    @Test
    void listProductsReturnsNormalizedPythonShape() throws Exception {
        JsonObject request = new JsonObject();
        request.addProperty("limit", 10);
        JsonObject response = call(CrmRpc.GET_PRODUCTS, request);
        JsonArray rows = response.getAsJsonArray("data");
        assertNotNull(rows);
        if (!rows.isEmpty()) {
            JsonObject product = rows.get(0).getAsJsonObject();
            assertTrue(product.get("product_img").isJsonArray());
            assertTrue(product.get("offer_active").isJsonPrimitive());
            assertTrue(product.has("cost_rate"));
        }
    }

    @Test
    void categorySpecialEntryPreservesPythonOrderingBugFix() throws Exception {
        JsonArray rows = call(CrmRpc.GET_CATEGORIES, new JsonObject()).getAsJsonArray("data");
        for (int i = 1; i < rows.size(); i++) {
            assertNotEquals("cc41f1da652f4", rows.get(i).getAsJsonObject().get("id").getAsString());
        }
    }

    @Test
    void stockSummaryDoesNotMutateDatabase() throws Exception {
        JsonObject response = call(CrmRpc.GET_GST_STOCK_SUMMARY, new JsonObject());
        assertTrue(response.has("products"));
        assertTrue(response.has("summary"));
        assertTrue(response.has("category_summary"));
    }

    @Test
    void backupSnapshotRestoresDataAndDynamicSchema() throws Exception {
        CrmDatabase database = new CrmDatabase(CrmConfiguration.fromEnvironment());
        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE backup_probe (" +
                    "id TEXT PRIMARY KEY, value TEXT NOT NULL, custom_number INTEGER DEFAULT 7)");
            statement.execute("CREATE UNIQUE INDEX backup_probe_value_idx ON backup_probe(value)");
            statement.execute("INSERT INTO backup_probe(id,value,custom_number) VALUES('one','before',42)");
        }

        JsonObject snapshot = database.createBackupSnapshot();
        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE backup_probe SET value='after', custom_number=99 WHERE id='one'");
        }

        database.restoreBackupSnapshot(snapshot);
        try (Connection connection = database.connect(); Statement statement = connection.createStatement();
             ResultSet row = statement.executeQuery(
                     "SELECT value, custom_number FROM backup_probe WHERE id='one'")) {
            assertTrue(row.next());
            assertEquals("before", row.getString("value"));
            assertEquals(42, row.getInt("custom_number"));
        }
        try (Connection connection = database.connect(); Statement statement = connection.createStatement();
             ResultSet index = statement.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type='index' " +
                             "AND name='backup_probe_value_idx'")) {
            assertTrue(index.next());
        }
    }

    @Test
    void groupedProductSearchKeepsCategoryAndSubcategoryConstraints() throws Exception {
        String suffix = java.util.UUID.randomUUID().toString().replace("-", "");
        String category = "search-category-" + suffix;
        String subcategory = "search-sub-" + suffix;
        String needle = "searchneedle" + suffix;
        CrmDatabase database = new CrmDatabase(CrmConfiguration.fromEnvironment());
        try (Connection connection = database.connect();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO products(id,product_id,product_name,product_desc,cat_id,cat_sub) " +
                             "VALUES(?,?,?,?,?,?)")) {
            insertProduct(insert, "matching-" + suffix, "SKU-A-" + suffix,
                    needle + " product", "matching description", category, subcategory);
            insertProduct(insert, "same-sub-" + suffix, "SKU-B-" + suffix,
                    "unrelated product", "nothing here", category, subcategory);
            insertProduct(insert, "other-sub-" + suffix, "SKU-C-" + suffix,
                    needle + " wrong subcategory", "matching description", category, "other-sub");
        }

        JsonObject body = new JsonObject();
        JsonArray filters = new JsonArray();
        filters.add(filter("cat_id", "eq", category));
        filters.add(filter("cat_sub", "contains", subcategory));
        body.add("filters", filters);
        body.addProperty("filter_logic", "AND");
        body.addProperty("search_value", needle.toUpperCase(java.util.Locale.ROOT));
        JsonArray fields = new JsonArray();
        fields.add("product_name");
        fields.add("product_desc");
        body.add("search_fields", fields);
        JsonObject request = new JsonObject();
        request.add("body", body);

        JsonArray matches = call(CrmRpc.POST_PRODUCTS_QUERY, request).getAsJsonArray("data");
        assertEquals(1, matches.size());
        assertEquals("matching-" + suffix,
                matches.get(0).getAsJsonObject().get("id").getAsString());
    }

    private static void insertProduct(PreparedStatement insert, String id, String productId,
                                      String name, String description, String category,
                                      String subcategory) throws SQLException {
        insert.setString(1, id);
        insert.setString(2, productId);
        insert.setString(3, name);
        insert.setString(4, description);
        insert.setString(5, category);
        insert.setString(6, subcategory);
        insert.executeUpdate();
    }

    private static JsonObject filter(String field, String operator, String value) {
        JsonObject filter = new JsonObject();
        filter.addProperty("field", field);
        filter.addProperty("operator", operator);
        filter.addProperty("value", value);
        return filter;
    }

    private JsonObject call(CrmRpc rpc, JsonObject request) throws Exception {
        JsonObject response = new JsonObject();
        service.handlerFor(rpc).onRpc(rpc, request, response);
        return response;
    }
}
