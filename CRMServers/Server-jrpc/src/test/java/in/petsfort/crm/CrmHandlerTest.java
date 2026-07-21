package in.petsfort.crm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jay.rpc.RpcException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CrmHandlerTest {
    @TempDir Path temporaryDirectory;
    private CrmDatabase database;
    private CrmHandler handler;

    @BeforeEach
    void setUp() throws Exception {
        database = new CrmDatabase(temporaryDirectory.resolve("products.db"));
        database.initialize();
        handler = new CrmHandler(database);
    }

    @AfterEach void tearDown() { database.close(); }

    @Test
    void productCrudPreservesPythonJsonShape() throws Exception {
        JsonObject create = product("SKU-1", "Dog Food", 20);
        JsonObject created = call(CrmRpc.CREATE_PRODUCT, create);
        assertEquals("SKU-1", created.get("product_id").getAsString());
        assertTrue(created.get("product_img").isJsonArray());
        assertFalse(created.get("offer_active").getAsBoolean());
        assertEquals(90.0, created.get("cost_rate").getAsDouble());

        JsonObject get = new JsonObject(); get.addProperty("product_identifier", "SKU-1");
        assertEquals("Dog Food", call(CrmRpc.GET_PRODUCT, get).get("product_name").getAsString());

        JsonObject update = new JsonObject(); update.addProperty("product_identifier", "SKU-1");
        JsonObject body = new JsonObject(); body.addProperty("product_name", "Premium Dog Food"); body.addProperty("stock", 25);
        update.add("body", body);
        assertEquals(25, call(CrmRpc.UPDATE_PRODUCT, update).get("stock").getAsInt());

        JsonObject list = call(CrmRpc.LIST_PRODUCTS, new JsonObject());
        assertEquals(1, list.getAsJsonArray("products").size());
    }

    @Test
    void offerGroupApplyAndCancelUpdatesProducts() throws Exception {
        call(CrmRpc.CREATE_PRODUCT, product("SKU-1", "One", 20));
        call(CrmRpc.CREATE_PRODUCT, product("SKU-2", "Two", 20));
        JsonObject request = new JsonObject(); request.addProperty("name", "Buy two get one");
        request.addProperty("buy_qty", 2); request.addProperty("free_qty", 1);
        JsonArray ids = new JsonArray(); ids.add("SKU-1"); ids.add("SKU-2"); request.add("product_ids", ids);
        JsonObject group = call(CrmRpc.CREATE_OFFER_GROUP, request);

        JsonObject apply = new JsonObject(); apply.addProperty("group_id", group.get("id").getAsString());
        assertEquals("ACTIVE", call(CrmRpc.APPLY_OFFER_GROUP, apply).get("status").getAsString());
        JsonObject get = new JsonObject(); get.addProperty("product_id", "SKU-1");
        assertTrue(call(CrmRpc.GET_PRODUCT, get).get("offer_active").getAsBoolean());

        call(CrmRpc.CANCEL_OFFER_GROUP, apply);
        assertFalse(call(CrmRpc.GET_PRODUCT, get).get("offer_active").getAsBoolean());
    }

    @Test
    void checkoutCreatesOrderAndDeductsStock() throws Exception {
        call(CrmRpc.CREATE_PRODUCT, product("SKU-1", "Food", 20));
        JsonObject user = new JsonObject(); user.addProperty("id", "SHOP-1"); user.addProperty("uid", "firebase-1");
        user.addProperty("name", "Shop"); user.addProperty("email", "shop@example.com"); user.addProperty("role", "shop");
        user.addProperty("address", "Address"); user.addProperty("credits", 0); user.addProperty("creditse", "none");
        call(CrmRpc.PUT_USER, user);

        JsonObject checkout = new JsonObject(); checkout.addProperty("user_id", "SHOP-1");
        JsonObject items = new JsonObject(); items.addProperty("SKU-1", 2); checkout.add("items", items);
        JsonObject result = call(CrmRpc.CHECKOUT_ORDER, checkout);
        assertEquals("ORDER_PENDING", result.getAsJsonObject("order").get("order_status").getAsString());
        JsonObject get = new JsonObject(); get.addProperty("product_id", "SKU-1");
        assertEquals(18, call(CrmRpc.GET_PRODUCT, get).get("stock").getAsInt());
    }

    @Test
    void invalidInputReturnsStableRpcError() {
        RpcException error = assertThrows(RpcException.class, () -> call(CrmRpc.CREATE_PRODUCT, new JsonObject()));
        assertEquals("INVALID_REQUEST", error.getCode());
    }

    private JsonObject call(CrmRpc rpc, JsonObject request) throws RpcException {
        JsonObject response = new JsonObject(); handler.onRpc(rpc, request, response); return response;
    }

    private static JsonObject product(String id, String name, int stock) {
        JsonObject value = new JsonObject(); value.addProperty("product_id", id); value.addProperty("product_name", name);
        value.addProperty("product_desc", ""); value.addProperty("cat_id", "CAT"); value.addProperty("cat_sub", "SUB");
        value.addProperty("cost_mrp", 100); value.addProperty("cost_gst", 18); value.addProperty("cost_dis", 10); value.addProperty("stock", stock);
        JsonArray images = new JsonArray(); images.add("https://example.test/image.jpg"); value.add("product_img", images);
        return value;
    }
}
