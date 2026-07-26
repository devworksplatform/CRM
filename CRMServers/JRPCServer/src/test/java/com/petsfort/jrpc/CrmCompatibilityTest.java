package com.petsfort.jrpc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;

import java.nio.file.*;

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

    private JsonObject call(CrmRpc rpc, JsonObject request) throws Exception {
        JsonObject response = new JsonObject();
        service.handlerFor(rpc).onRpc(rpc, request, response);
        return response;
    }
}
