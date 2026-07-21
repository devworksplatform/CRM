package in.petsfort.crm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jay.rpc.RpcException;
import com.jay.rpc.RpcHandler;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

/** Thread-safe handler: every call obtains its own SQLite connection. */
public final class CrmHandler implements RpcHandler<CrmRpc> {
    private static final Set<String> PRODUCT_FIELDS = Set.of(
            "product_id", "product_name", "product_desc", "product_hsn", "product_cid",
            "product_img", "cat_id", "cat_sub", "cost_rate", "cost_mrp", "cost_gst",
            "cost_dis", "offer_buy_qty", "offer_free_qty", "offer_active", "offer_group_id", "stock");
    private static final Set<String> USER_FIELDS = Set.of(
            "uid", "name", "contact", "gstin", "email", "role", "address", "credits", "creditse", "isblocked");
    private static final Set<String> ORDER_UPDATE_FIELDS = Set.of(
            "order_status", "items", "items_detail", "total_rate", "total_gst", "total_discount", "total", "address", "notes");
    private final CrmDatabase database;
    private final FirebaseService firebase;
    private final BackupService backups;
    private final InvoiceService invoices = new InvoiceService();
    private final ReportingService reports;
    private final ReentrantLock checkoutLock = new ReentrantLock(true);
    private final Instant startedAt = Instant.now();

    CrmHandler(CrmDatabase database) { this(database, FirebaseService.disabled()); }
    CrmHandler(CrmDatabase database, FirebaseService firebase) {
        this.database = database;
        this.firebase = firebase;
        this.backups = new BackupService(database, firebase);
        this.reports = new ReportingService(database);
    }

    @Override
    public void onRpc(CrmRpc rpc, JsonObject request, JsonObject response) throws RpcException {
        try {
            switch (rpc) {
                case HEALTH: health(response); return;
                case CREATE_PRODUCT: createProduct(request, response); return;
                case QUERY_PRODUCTS: queryProducts(request, response); return;
                case LIST_PRODUCTS: listProducts(request, response); return;
                case GET_PRODUCT: getProduct(request, response); return;
                case UPDATE_PRODUCT: updateProduct(request, response); return;
                case DELETE_PRODUCT: deleteProduct(request, response); return;
                case GET_PRODUCTS_BULK: bulkProducts(request, response); return;
                case LIST_OFFER_GROUPS: response.add("offerGroups", database.query("SELECT * FROM offer_groups ORDER BY created_at DESC", List.of())); return;
                case CREATE_OFFER_GROUP: createOfferGroup(request, response); return;
                case UPDATE_OFFER_GROUP: updateOfferGroup(request, response); return;
                case APPLY_OFFER_GROUP: setOfferGroupStatus(request, response, true); return;
                case CANCEL_OFFER_GROUP: setOfferGroupStatus(request, response, false); return;
                case DELETE_OFFER_GROUP: deleteOfferGroup(request, response); return;
                case CHECKOUT_ORDER: checkout(request, response); return;
                case QUERY_ORDERS: queryOrders(request, response); return;
                case UPDATE_ORDER: updateOrder(request, response); return;
                case DELETE_ORDER: deleteById("orders", "order_id", request, response, "Order"); return;
                case GET_BILL: getBill(request, response); return;
                case LIST_CATEGORIES: listCategories(response); return;
                case PUT_CATEGORY: putCategory(request, response); return;
                case DELETE_CATEGORY: deleteById("category", "id", request, response, "Category"); return;
                case LIST_SUBCATEGORIES: response.add("subcategories", database.query("SELECT id,parentid,name,image FROM subcategory", List.of())); return;
                case LIST_AVAILABLE_SUBCATEGORIES: availableSubcategories(request, response); return;
                case PUT_SUBCATEGORY: putSubcategory(request, response); return;
                case DELETE_SUBCATEGORY: deleteById("subcategory", "id", request, response, "Subcategory"); return;
                case LIST_USERS: response.add("users", database.query("SELECT uid,id,name,contact,gstin,email,role,address,credits,creditse,isblocked FROM userdata", List.of())); return;
                case GET_USER: getUser(request, response); return;
                case PUT_USER: putUser(request, response); return;
                case DELETE_USER: deleteUser(request, response); return;
                case GET_SCHEMA: getSchema(response); return;
                case ADD_COLUMNS: addColumns(request, response); return;
                case REMOVE_COLUMNS: removeColumns(request, response); return;
                case LIST_TABLES: listTables(response); return;
                case GET_TABLE_INFO: tableInfo(request, response); return;
                case GET_TABLE_DATA: tableData(request, response); return;
                case ADD_TABLE_ROW: addTableRow(request, response); return;
                case UPDATE_TABLE_ROW: updateTableRow(request, response); return;
                case DELETE_TABLE_ROW: deleteTableRow(request, response); return;
                case CREATE_BACKUP: createBackup(request, response); return;
                case RESTORE_BACKUP: restoreBackup(request, response); return;
                case SYSTEM_SNAPSHOT: systemSnapshot(response); return;
                case ANALYTICS_SUMMARY: analytics(response); return;
                case GST_DASHBOARD: copyInto(reports.gstDashboard(Jsons.optionalString(request,"fy",null)),response); return;
                case GST_SALES_REGISTER: response.add("sales",reports.salesRegister(Jsons.optionalString(request,"from_date",null),Jsons.optionalString(request,"to_date",null))); return;
                case CREATE_CREDIT_NOTE: createNote("credit_notes", "cn", request, response); return;
                case LIST_CREDIT_NOTES: listNotes("credit_notes", request, response); return;
                case DELETE_CREDIT_NOTE: deleteById("credit_notes", "cn_id", request, response, "Credit note"); return;
                case CREATE_DEBIT_NOTE: createNote("debit_notes", "dn", request, response); return;
                case LIST_DEBIT_NOTES: listNotes("debit_notes", request, response); return;
                case DELETE_DEBIT_NOTE: deleteById("debit_notes", "dn_id", request, response, "Debit note"); return;
                case GST_PARTY_LEDGER: copyInto(reports.partyLedger(Jsons.optionalString(request,"from_date",null),Jsons.optionalString(request,"to_date",null),Jsons.optionalString(request,"user_id",null)),response); return;
                case GST_DAY_BOOK: copyInto(reports.dayBook(Jsons.optionalString(request,"from_date",null),Jsons.optionalString(request,"to_date",null)),response); return;
                case GST_PROFIT_LOSS: copyInto(reports.profitLoss(Jsons.optionalString(request,"fy",null)),response); return;
                case GST_STOCK_SUMMARY: copyInto(reports.stockSummary(),response); return;
                case GST_OUTSTANDING: copyInto(reports.outstanding(),response); return;
                case GST_TAX_LEDGER: copyInto(reports.taxLedger(Jsons.optionalString(request,"from_date",null),Jsons.optionalString(request,"to_date",null)),response); return;
                case GST_DASHBOARD_EXTRAS: copyInto(reports.dashboardExtras(Jsons.optionalString(request,"fy",null)),response); return;
                case LIST_BACKUPS: verifyBackupAdmin(request); response.add("backups", backups.list()); return;
                case CREATE_MANAGED_BACKUP: verifyBackupAdmin(request); response.add("result", backups.create()); response.addProperty("detail", "Backup created successfully."); return;
                case DELETE_OLD_BACKUPS: verifyBackupAdmin(request); response.add("deleted_ids", backups.deleteOlderThan(Jsons.optionalInt(request, "days", -1))); response.addProperty("detail", "Old backups deleted."); return;
                case DELETE_BACKUP: verifyBackupAdmin(request); deleteManagedBackup(request, response); return;
                case DELETE_SELECTED_BACKUPS: verifyBackupAdmin(request); deleteSelectedBackups(request, response); return;
                case RESET_BACKUPS: verifyBackupAdmin(request); copyInto(backups.reset(), response); return;
                default: throw new RpcException("RPC_NOT_SUPPORTED", "Unsupported CRM RPC: " + rpc);
            }
        } catch (RpcException error) {
            throw error;
        } catch (SQLException error) {
            throw new RpcException(Jsons.errorCode(error), safeSqlMessage(error), error);
        } catch (IOException error) {
            throw new RpcException("FILE_ERROR", error.getMessage(), error);
        } catch (SecurityException error) {
            throw new RpcException("FORBIDDEN", error.getMessage(), error);
        } catch (RuntimeException error) {
            throw new RpcException("INVALID_REQUEST", "Invalid request: " + error.getMessage(), error);
        } catch (Exception error) {
            throw new RpcException("INTEGRATION_ERROR", error.getMessage(), error);
        }
    }

    private void health(JsonObject response) {
        response.addProperty("status", "ok");
        response.addProperty("service", "petsfort-crm-jrpc");
        response.addProperty("startedAt", startedAt.toString());
        response.addProperty("database", database.path().toString());
    }

    private void createProduct(JsonObject request, JsonObject response) throws RpcException, SQLException {
        JsonObject body = Jsons.body(request).deepCopy();
        requireFields(body, "product_name", "cat_id", "cat_sub", "cost_mrp", "cost_gst", "cost_dis", "stock");
        rejectUnknown(body, PRODUCT_FIELDS);
        String now = Instant.now().toString();
        body.addProperty("id", CrmDatabase.uuid());
        if (!body.has("product_id") || body.get("product_id").isJsonNull() || body.get("product_id").getAsString().isBlank()) {
            body.addProperty("product_id", CrmDatabase.uuid());
        }
        defaults(body, Map.of("product_desc", "", "product_hsn", "", "product_cid", "", "product_img", new JsonArray(),
                "offer_buy_qty", 0, "offer_free_qty", 0, "offer_active", false));
        body.addProperty("created_at", now);
        body.addProperty("updated_at", now);
        JsonObject product = database.insert("products", body, "id", body.get("id").getAsString());
        publishOfferQuietly(product, product.has("offer_active") && product.get("offer_active").getAsBoolean());
        copyInto(product, response);
    }

    private void listProducts(JsonObject request, JsonObject response) throws RpcException, SQLException {
        int limit = Math.min(1000, Math.max(1, Jsons.optionalInt(request, "limit", 100)));
        int offset = Math.max(0, Jsons.optionalInt(request, "offset", 0));
        response.add("products", database.query("SELECT * FROM products LIMIT ? OFFSET ?", List.of(limit, offset)));
    }

    private void queryProducts(JsonObject request, JsonObject response) throws RpcException, SQLException {
        response.add("products", queryWithFilters("products", request, false));
    }

    private void getProduct(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String id = requestId(request, "product_identifier", "product_id", "id");
        JsonObject product = database.one("SELECT * FROM products WHERE id=? OR product_id=?", List.of(id, id));
        if (product == null) throw new RpcException("NOT_FOUND", "Product not found");
        copyInto(product, response);
    }

    private void updateProduct(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String id = requestId(request, "product_identifier", "id");
        JsonObject existing = database.one("SELECT * FROM products WHERE id=? OR product_id=?", List.of(id, id));
        if (existing == null) throw new RpcException("NOT_FOUND", "Product not found");
        JsonObject body = Jsons.body(request).deepCopy();
        body.remove("product_identifier"); body.remove("id");
        rejectUnknown(body, PRODUCT_FIELDS);
        JsonObject detachedGroup = null;
        if (existing.has("offer_group_id") && !existing.get("offer_group_id").isJsonNull() && !existing.get("offer_group_id").getAsString().isBlank()
                && offerFieldsChanged(existing, body)) {
            String groupId = existing.get("offer_group_id").getAsString(); detachedGroup = database.one("SELECT * FROM offer_groups WHERE id=?", List.of(groupId)); body.add("offer_group_id", JsonNull.INSTANCE);
            if (detachedGroup != null) {
                JsonArray remaining = new JsonArray(); String productId = existing.get("product_id").getAsString();
                detachedGroup.getAsJsonArray("product_ids").forEach(idValue -> { if (!idValue.getAsString().equals(productId)) remaining.add(idValue.deepCopy()); });
                String now = Instant.now().toString(); detachedGroup.add("product_ids", remaining); detachedGroup.addProperty("updated_at", now);
                if (remaining.isEmpty()) { detachedGroup.addProperty("status", "CANCELED"); detachedGroup.addProperty("canceled_at", now); database.update("UPDATE offer_groups SET product_ids=?,status='CANCELED',updated_at=?,canceled_at=? WHERE id=?", List.of(remaining.toString(),now,now,groupId)); }
                else database.update("UPDATE offer_groups SET product_ids=?,updated_at=? WHERE id=?", List.of(remaining.toString(),now,groupId));
            }
        }
        body.addProperty("updated_at", Instant.now().toString());
        updateObject("products", body, "id", existing.get("id").getAsString());
        JsonObject updated = database.one("SELECT * FROM products WHERE id=?", List.of(existing.get("id").getAsString()));
        if (detachedGroup != null) publishOfferGroupQuietly(detachedGroup, false);
        publishOfferQuietly(updated, updated.has("offer_active") && updated.get("offer_active").getAsBoolean());
        copyInto(updated, response);
    }

    private void deleteProduct(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String id = requestId(request, "product_identifier", "id");
        JsonObject product = database.one("SELECT * FROM products WHERE id=? OR product_id=?", List.of(id, id));
        if (product == null) throw new RpcException("NOT_FOUND", "Product not found");
        database.update("DELETE FROM products WHERE id=?", List.of(product.get("id").getAsString()));
        product.addProperty("offer_active", false); publishOfferQuietly(product, false);
        response.addProperty("message", "Product deleted successfully");
        response.add("deleted_product", product);
    }

    private void bulkProducts(JsonObject request, JsonObject response) throws RpcException, SQLException {
        JsonObject counts = new JsonObject();
        if (request.has("product_ids") || request.has("ids")) {
            JsonArray ids = request.has("product_ids") ? Jsons.requiredArray(request, "product_ids") : Jsons.requiredArray(request, "ids");
            ids.forEach(id -> { JsonObject item = new JsonObject(); item.addProperty("count", 0); counts.add(id.getAsString(), item); });
        } else {
            JsonObject body = Jsons.body(request);
            for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
                if (!entry.getValue().isJsonObject()) throw new RpcException("INVALID_REQUEST", "Each product value must contain count");
                counts.add(entry.getKey(), entry.getValue().deepCopy());
            }
        }
        JsonArray details = new JsonArray(); JsonArray warnings = new JsonArray();
        BigDecimal totalMrp = Money.ZERO, totalRate = Money.ZERO, totalGst = Money.ZERO, totalDiscount = Money.ZERO, total = Money.ZERO;
        for (Map.Entry<String, JsonElement> entry : counts.entrySet()) {
            int count;
            try { count = Math.max(0, entry.getValue().getAsJsonObject().get("count").getAsInt()); } catch (RuntimeException error) { count = 0; }
            JsonObject product = database.one("SELECT * FROM products WHERE id=? OR product_id=?", List.of(entry.getKey(), entry.getKey()));
            if (product == null) { warnings.add("Product ID '" + entry.getKey() + "' not found."); continue; }
            int free = offerFree(product, count); product.addProperty("requested_count", count); product.addProperty("paid_count", count);
            product.addProperty("free_count", free); product.addProperty("fulfilled_count", count + free); details.add(product);
            BigDecimal mrp = Money.of(product, "cost_mrp"), rate = Money.rateAfterDiscount(mrp, Money.of(product, "cost_dis"));
            BigDecimal gst = Money.percent(rate, Money.of(product, "cost_gst"));
            totalMrp = totalMrp.add(mrp.multiply(BigDecimal.valueOf(count)));
            totalRate = totalRate.add(rate.multiply(BigDecimal.valueOf(count)));
            totalGst = totalGst.add(gst.multiply(BigDecimal.valueOf(count)));
            totalDiscount = totalDiscount.add(mrp.subtract(rate).multiply(BigDecimal.valueOf(count)));
            total = total.add(rate.add(gst).multiply(BigDecimal.valueOf(count)));
        }
        List<JsonElement> sorted = new ArrayList<>(); details.forEach(sorted::add); sorted.sort((a, b) -> comparePick(a.getAsJsonObject(), b.getAsJsonObject()));
        JsonArray sortedDetails = new JsonArray(); sorted.forEach(sortedDetails::add);
        JsonObject cost = new JsonObject(); Money.add(cost, "total_mrp", totalMrp); Money.add(cost, "total_rate", totalRate);
        Money.add(cost, "total_gst", totalGst); Money.add(cost, "total_discount", totalDiscount); Money.add(cost, "total", total);
        response.add("product_details", sortedDetails); response.add("cost", cost);
        if (!warnings.isEmpty()) response.add("warnings", warnings);
        response.add("products", sortedDetails.deepCopy());
    }

    private void createOfferGroup(JsonObject request, JsonObject response) throws RpcException, SQLException {
        JsonObject body = Jsons.body(request).deepCopy();
        validateOfferGroup(body);
        JsonArray ids = uniqueStrings(Jsons.requiredArray(body, "product_ids"));
        verifyOfferProducts(ids, null);
        String now = Instant.now().toString();
        JsonObject row = new JsonObject();
        row.addProperty("id", CrmDatabase.uuid());
        row.addProperty("name", Jsons.requiredString(body, "name").trim());
        row.addProperty("description", Jsons.optionalString(body, "description", "").trim());
        row.addProperty("buy_qty", Jsons.optionalInt(body, "buy_qty", 0));
        row.addProperty("free_qty", Jsons.optionalInt(body, "free_qty", 0));
        row.add("product_ids", ids);
        row.addProperty("status", "DRAFT"); row.addProperty("created_at", now); row.addProperty("updated_at", now);
        copyInto(database.insert("offer_groups", row, "id", row.get("id").getAsString()), response);
    }

    private void updateOfferGroup(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String groupId = requestId(request, "group_id", "id");
        JsonObject current = database.one("SELECT * FROM offer_groups WHERE id=?", List.of(groupId));
        if (current == null) throw new RpcException("NOT_FOUND", "Offer group not found");
        if ("ACTIVE".equals(current.get("status").getAsString())) throw new RpcException("CONFLICT", "Cancel the active offer before editing its group");
        JsonObject body = Jsons.body(request); validateOfferGroup(body);
        JsonArray ids = uniqueStrings(Jsons.requiredArray(body, "product_ids")); verifyOfferProducts(ids, groupId);
        JsonObject changes = new JsonObject();
        changes.addProperty("name", Jsons.requiredString(body, "name").trim());
        changes.addProperty("description", Jsons.optionalString(body, "description", "").trim());
        changes.addProperty("buy_qty", Jsons.optionalInt(body, "buy_qty", 0));
        changes.addProperty("free_qty", Jsons.optionalInt(body, "free_qty", 0));
        changes.add("product_ids", ids); changes.addProperty("status", "DRAFT");
        changes.addProperty("updated_at", Instant.now().toString()); changes.add("canceled_at", JsonNull.INSTANCE);
        updateObject("offer_groups", changes, "id", groupId);
        copyInto(database.one("SELECT * FROM offer_groups WHERE id=?", List.of(groupId)), response);
    }

    private void setOfferGroupStatus(JsonObject request, JsonObject response, boolean active) throws RpcException, SQLException {
        String groupId = requestId(request, "group_id", "id");
        JsonObject group = database.one("SELECT * FROM offer_groups WHERE id=?", List.of(groupId));
        if (group == null) throw new RpcException("NOT_FOUND", "Offer group not found");
        JsonArray ids = group.getAsJsonArray("product_ids");
        if (active) {
            verifyOfferProducts(ids, groupId);
            List<Object> parameters = new ArrayList<>();
            parameters.add(group.get("buy_qty").getAsInt()); parameters.add(group.get("free_qty").getAsInt()); parameters.add(groupId);
            ids.forEach(value -> parameters.add(value.getAsString()));
            database.update("UPDATE products SET offer_buy_qty=?,offer_free_qty=?,offer_active=1,offer_group_id=? WHERE product_id IN (" + placeholders(ids.size()) + ")", parameters);
            database.update("UPDATE offer_groups SET status='ACTIVE',updated_at=?,canceled_at=NULL WHERE id=?", List.of(Instant.now().toString(), groupId));
        } else {
            database.update("UPDATE products SET offer_buy_qty=0,offer_free_qty=0,offer_active=0,offer_group_id=NULL WHERE offer_group_id=?", List.of(groupId));
            database.update("UPDATE offer_groups SET status='CANCELED',updated_at=?,canceled_at=? WHERE id=?", List.of(Instant.now().toString(), Instant.now().toString(), groupId));
        }
        JsonObject updatedGroup = database.one("SELECT * FROM offer_groups WHERE id=?", List.of(groupId));
        if (active && firebase.enabled()) for (JsonElement productId : updatedGroup.getAsJsonArray("product_ids")) try { firebase.delete("datas/announcement/all/offer_" + productId.getAsString().replaceAll("[^A-Za-z0-9_-]", "_")); } catch (Exception warning) { System.err.println("Offer cleanup failed: " + warning.getMessage()); }
        publishOfferGroupQuietly(updatedGroup, active); copyInto(updatedGroup, response);
    }

    private void deleteOfferGroup(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String groupId = requestId(request, "group_id", "id");
        JsonObject group = database.one("SELECT * FROM offer_groups WHERE id=?", List.of(groupId));
        if (group == null) throw new RpcException("NOT_FOUND", "Offer group not found");
        if ("ACTIVE".equals(group.get("status").getAsString())) throw new RpcException("CONFLICT", "Cancel the active offer before deleting its group");
        database.update("DELETE FROM offer_groups WHERE id=?", List.of(groupId));
        try { if (firebase.enabled()) firebase.delete("datas/announcement/all/offer_group_" + groupId); } catch (Exception warning) { System.err.println("Offer group cleanup failed: " + warning.getMessage()); }
        response.addProperty("message", "Offer group deleted successfully");
    }

    private void checkout(JsonObject request, JsonObject response) throws Exception {
        checkoutLock.lock();
        try { checkoutTransaction(request, response); }
        finally { checkoutLock.unlock(); }
    }

    private void checkoutTransaction(JsonObject request, JsonObject response) throws Exception {
        JsonObject body = Jsons.body(request);
        String userId = requestId(request, "user_id");
        JsonObject items = checkoutItems(body);
        JsonObject other = body.has("otherData") && body.get("otherData").isJsonObject() ? body.getAsJsonObject("otherData").deepCopy() : new JsonObject();
        if (!other.has("address")) other.addProperty("address", Jsons.optionalString(body, "address", ""));
        if (!other.has("notes")) other.addProperty("notes", Jsons.optionalString(body, "notes", ""));
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                JsonObject user = database.one(connection, "SELECT * FROM userdata WHERE id=? OR uid=?", List.of(userId, userId));
                if (user == null) throw new RpcException("NOT_FOUND", "User not found");
                if (user.has("isblocked") && user.get("isblocked").getAsInt() != 0) throw new RpcException("USER_BLOCKED", "User is Blocked");
                JsonArray details = new JsonArray();
                JsonObject storedItems = new JsonObject(); JsonArray missing = new JsonArray();
                BigDecimal rate = Money.ZERO, gst = Money.ZERO, discount = Money.ZERO, total = Money.ZERO;
                for (Map.Entry<String, JsonElement> item : items.entrySet()) {
                    int quantity = item.getValue().isJsonObject() ? Jsons.optionalInt(item.getValue().getAsJsonObject(), "count", 0) : item.getValue().getAsInt();
                    if (quantity <= 0) continue;
                    JsonObject product = database.one(connection, "SELECT * FROM products WHERE product_id=? OR id=?", List.of(item.getKey(), item.getKey()));
                    if (product == null) { missing.add(item.getKey()); continue; }
                    int stock = product.get("stock").getAsInt();
                    int freeQuantity = offerFree(product, quantity);
                    int fulfilled = quantity + freeQuantity;
                    if (stock < fulfilled) {
                        connection.rollback(); response.addProperty("message", "OutOfStock"); response.addProperty("product_available_stock", stock);
                        response.addProperty("product_id", item.getKey()); response.addProperty("product_name", value(product, "product_name"));
                        response.addProperty("requested_paid_count", quantity); response.addProperty("requested_free_count", freeQuantity); return;
                    }
                    BigDecimal mrp = Money.of(product, "cost_mrp"), discountedUnit = Money.rateAfterDiscount(mrp, Money.of(product, "cost_dis"));
                    BigDecimal lineGst = Money.percent(discountedUnit, Money.of(product, "cost_gst")).multiply(BigDecimal.valueOf(quantity));
                    rate = rate.add(discountedUnit.multiply(BigDecimal.valueOf(quantity)));
                    discount = discount.add(mrp.subtract(discountedUnit).multiply(BigDecimal.valueOf(quantity)));
                    gst = gst.add(lineGst); total = total.add(discountedUnit.multiply(BigDecimal.valueOf(quantity)).add(lineGst));
                    JsonObject detail = product.deepCopy();
                    detail.addProperty("count", fulfilled); detail.addProperty("paid_count", quantity); detail.addProperty("free_count", freeQuantity);
                    details.add(detail);
                    JsonObject stored = new JsonObject(); stored.addProperty("count", fulfilled); stored.addProperty("paid_count", quantity); stored.addProperty("free_count", freeQuantity); storedItems.add(item.getKey(), stored);
                    if (database.update(connection, "UPDATE products SET stock=stock-? WHERE id=? AND stock>=?", List.of(fulfilled, product.get("id").getAsString(), fulfilled)) != 1) {
                        throw new RpcException("INSUFFICIENT_STOCK", "Stock changed while checking out " + item.getKey());
                    }
                }
                if (!missing.isEmpty()) throw new RpcException("NOT_FOUND", "Products not found: " + missing);
                if (details.isEmpty()) throw new RpcException("INVALID_REQUEST", "items must contain at least one positive quantity");
                List<JsonElement> sorted = new ArrayList<>(); details.forEach(sorted::add); sorted.sort((a, b) -> comparePick(a.getAsJsonObject(), b.getAsJsonObject())); details = new JsonArray(); sorted.forEach(details::add);
                String orderId = body.has("order_id") ? body.get("order_id").getAsString() : shortOrderId();
                Instant createdAt = Instant.now();
                JsonObject order = new JsonObject();
                order.addProperty("order_id", orderId); order.addProperty("user_id", userId);
                order.add("items", storedItems); order.add("items_detail", details); order.addProperty("order_status", "ORDER_PENDING");
                order.addProperty("total_rate", Money.database(rate)); order.addProperty("total_gst", Money.database(gst));
                order.addProperty("total_discount", Money.database(discount)); order.addProperty("total", Money.database(total));
                order.addProperty("created_at", createdAt.toString());
                order.addProperty("address", Jsons.optionalString(other, "address", "")); order.addProperty("notes", Jsons.optionalString(other, "notes", ""));
                JsonObject created = database.insert(connection, "orders", order, "order_id", orderId);
                database.update(connection, "UPDATE userdata SET credits=credits-? WHERE id=? OR uid=?", List.of(Money.database(total), userId, userId));
                JsonObject invoice = invoices.generate(orderId, createdAt, userId, details, user, other);
                database.update(connection, "INSERT INTO bills(order_id,bill) VALUES(?,?)", List.of(orderId, invoice.toString()));
                connection.commit();
                response.addProperty("message", "Order created successfully"); response.addProperty("order_id", orderId); response.addProperty("user_id", userId);
                response.addProperty("order_status", "ORDER_PENDING"); response.addProperty("total", Money.database(total)); response.add("order", created); response.add("bill", invoice);
                if (firebase.enabled()) try { firebase.notifyTopic("admin", "New Order", value(user, "name") + " placed an order worth ₹" + Money.output(total), Map.of("type", "order_checkout", "user_id", userId)); } catch (Exception warning) { System.err.println("FCM checkout notification failed: " + warning.getMessage()); }
            } catch (RpcException | SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            }
        }
    }

    private void queryOrders(JsonObject request, JsonObject response) throws RpcException, SQLException {
        response.add("orders", queryWithFilters("orders", request, true));
    }

    private void updateOrder(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String orderId = requestId(request, "order_id", "id");
        if (database.one("SELECT order_id FROM orders WHERE order_id=?", List.of(orderId)) == null) throw new RpcException("NOT_FOUND", "Order not found");
        JsonObject body = Jsons.body(request).deepCopy(); body.remove("order_id");
        rejectUnknown(body, ORDER_UPDATE_FIELDS);
        if (body.size() == 0) throw new RpcException("INVALID_REQUEST", "No valid fields provided for update");
        updateObject("orders", body, "order_id", orderId);
        JsonObject updated = database.one("SELECT * FROM orders WHERE order_id=?", List.of(orderId));
        if (body.has("order_status")) try { firebase.notifyTopic("user_" + updated.get("user_id").getAsString(), "Order status updated", body.get("order_status").getAsString(), Map.of("type", "order_status", "order_id", orderId)); } catch (Exception warning) { System.err.println("FCM status notification failed: " + warning.getMessage()); }
        copyInto(updated, response);
    }

    private void getBill(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String orderId = requestId(request, "order_id", "id");
        JsonObject row = database.one("SELECT bill FROM bills WHERE order_id=?", List.of(orderId));
        if (row == null || row.get("bill").isJsonNull()) return;
        try { copyInto(JsonParser.parseString(row.get("bill").getAsString()).getAsJsonObject(), response); }
        catch (RuntimeException error) { throw new RpcException("INVALID_BILL", "Stored bill JSON is invalid", error); }
    }

    private void listCategories(JsonObject response) throws SQLException {
        JsonArray categories = database.query("SELECT id,name,image FROM category ORDER BY CASE WHEN id='cc41f1da652f4' THEN 0 ELSE 1 END,name", List.of());
        response.add("categories", categories);
    }

    private void putCategory(JsonObject request, JsonObject response) throws RpcException, SQLException {
        JsonObject body = Jsons.body(request); requireFields(body, "id", "name");
        database.update("INSERT INTO category(id,name,image) VALUES(?,?,?) ON CONFLICT(id) DO UPDATE SET name=excluded.name,image=excluded.image",
                List.of(body.get("id").getAsString(), body.get("name").getAsString(), Jsons.optionalString(body, "image", "")));
        response.addProperty("message", "Category saved successfully");
    }

    private void availableSubcategories(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String categoryId = requestId(request, "category_id", "parentid");
        response.add("subcategories", database.query("SELECT DISTINCT s.id,s.parentid,s.name,COALESCE(NULLIF(s.image,''),(SELECT json_extract(p1.product_img,'$[0]') FROM products p1 WHERE p1.cat_sub=s.id LIMIT 1),'') image FROM subcategory s JOIN products p ON s.id=p.cat_sub WHERE s.parentid=?", List.of(categoryId)));
    }

    private void putSubcategory(JsonObject request, JsonObject response) throws RpcException, SQLException {
        JsonObject body = Jsons.body(request); requireFields(body, "id", "parentid", "name");
        database.update("INSERT INTO subcategory(id,parentid,name,image) VALUES(?,?,?,?) ON CONFLICT(id) DO UPDATE SET parentid=excluded.parentid,name=excluded.name,image=excluded.image",
                List.of(body.get("id").getAsString(), body.get("parentid").getAsString(), body.get("name").getAsString(), Jsons.optionalString(body, "image", "")));
        response.addProperty("message", "Subcategory saved successfully");
    }

    private void getUser(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String id = requestId(request, "user_id", "id", "uid");
        JsonObject user = database.one("SELECT uid,id,name,contact,gstin,email,role,address,credits,creditse,isblocked FROM userdata WHERE id=? OR uid=?", List.of(id, id));
        if (user == null) throw new RpcException("NOT_FOUND", "User not found");
        copyInto(user, response);
    }

    private void putUser(JsonObject request, JsonObject response) throws Exception {
        JsonObject body = Jsons.body(request).deepCopy();
        String id = request.has("user_id") ? request.get("user_id").getAsString() : Jsons.requiredString(body, "id");
        String password = Jsons.optionalString(body, "pwd", ""); body.remove("pwd"); body.remove("id"); body.remove("user_id"); rejectUnknown(body, USER_FIELDS);
        JsonObject existing = database.one("SELECT * FROM userdata WHERE id=? OR uid=?", List.of(id, id));
        if (existing == null) {
            requireFields(body, "name", "email", "role", "address", "credits", "creditse");
            String uid = body.has("uid") && !body.get("uid").getAsString().isBlank() ? body.get("uid").getAsString() : firebase.createUser(body.get("email").getAsString(), password);
            body.addProperty("uid", uid);
            body.addProperty("id", id); defaults(body, Map.of("contact", "N/A", "gstin", "N/A", "isblocked", 0));
            database.insert("userdata", body, "id", id);
            response.addProperty("message", "User added successfully"); response.addProperty("uid", uid);
        } else {
            if (!password.isBlank()) firebase.updatePassword(existing.get("uid").getAsString(), password);
            body.remove("uid"); updateObject("userdata", body, "id", existing.get("id").getAsString());
            response.addProperty("message", "User updated successfully"); response.addProperty("uid", existing.get("uid").getAsString());
        }
        response.addProperty("id", id);
        response.addProperty("authManaged", firebase.enabled());
    }

    private void deleteUser(JsonObject request, JsonObject response) throws Exception {
        String id = requestId(request, "user_id", "id", "uid");
        JsonObject existing = database.one("SELECT uid FROM userdata WHERE id=? OR uid=?", List.of(id, id));
        if (existing == null) throw new RpcException("NOT_FOUND", "User not found");
        if (firebase.enabled()) firebase.deleteUser(existing.get("uid").getAsString());
        int count = database.update("DELETE FROM userdata WHERE id=? OR uid=?", List.of(id, id));
        if (count == 0) throw new RpcException("NOT_FOUND", "User not found");
        response.addProperty("message", "User deleted successfully"); response.addProperty("authManaged", firebase.enabled());
    }

    private void getSchema(JsonObject response) throws SQLException {
        addTableSchema(response, "products_table", "products");
        addTableSchema(response, "orders_table", "orders");
    }

    private void addTableSchema(JsonObject response, String responseKey, String table) throws SQLException {
        JsonArray columns = new JsonArray();
        for (JsonElement element : database.query("PRAGMA table_info(" + table + ")", List.of())) {
            JsonObject source = element.getAsJsonObject();
            JsonObject column = new JsonObject();
            column.addProperty("name", source.get("name").getAsString());
            column.addProperty("type", source.get("type").getAsString());
            column.addProperty("not_null", source.get("notnull").getAsInt() != 0);
            column.add("default_value", source.get("dflt_value").deepCopy());
            column.addProperty("primary_key", source.get("pk").getAsInt() != 0);
            columns.add(column);
        }
        JsonObject schema = new JsonObject();
        schema.addProperty("table_name", table);
        schema.add("columns", columns);
        response.add(responseKey, schema);
    }

    private void addColumns(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String table = Jsons.identifier(Jsons.optionalString(request, "table", "products"), "table");
        JsonArray columns = Jsons.requiredArray(request, "columns");
        Set<String> existing = new HashSet<>();
        database.query("PRAGMA table_info(" + table + ")", List.of()).forEach(column -> existing.add(column.getAsJsonObject().get("name").getAsString()));
        JsonArray added = new JsonArray();
        for (JsonElement element : columns) {
            JsonObject column = element.getAsJsonObject();
            String name = Jsons.identifier(Jsons.requiredString(column, "column_name"), "column_name");
            if (existing.contains(name)) continue;
            String type = allowedColumnType(Jsons.requiredString(column, "column_type"));
            String sql = "ALTER TABLE " + table + " ADD COLUMN " + name + " " + type;
            if (column.has("default_value") && !column.get("default_value").isJsonNull()) {
                JsonElement defaultValue = column.get("default_value");
                if (defaultValue.isJsonPrimitive() && defaultValue.getAsJsonPrimitive().isString()) sql += " DEFAULT '" + defaultValue.getAsString().replace("'", "''") + "'";
                else if (defaultValue.isJsonPrimitive() && defaultValue.getAsJsonPrimitive().isBoolean()) sql += " DEFAULT " + (defaultValue.getAsBoolean() ? "1" : "0");
                else if (defaultValue.isJsonPrimitive() && defaultValue.getAsJsonPrimitive().isNumber()) sql += " DEFAULT " + defaultValue.getAsString();
                else throw new RpcException("INVALID_REQUEST", "Unsupported default value for column '" + name + "'");
            }
            database.update(sql, List.of()); added.add(name); existing.add(name);
        }
        response.addProperty("message", "Columns added successfully (or skipped if existing)"); response.add("added_columns", added);
    }

    private void removeColumns(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String table = Jsons.identifier(Jsons.optionalString(request, "table", "products"), "table");
        JsonArray columns = Jsons.requiredArray(request, "columns");
        Set<String> existing = new HashSet<>();
        database.query("PRAGMA table_info(" + table + ")", List.of()).forEach(column -> existing.add(column.getAsJsonObject().get("name").getAsString()));
        LinkedHashSet<String> removing = new LinkedHashSet<>();
        for (JsonElement column : columns) removing.add(Jsons.identifier(column.getAsString(), "column"));
        Set<String> missing = new LinkedHashSet<>(removing); missing.removeAll(existing);
        if (!missing.isEmpty()) throw new RpcException("INVALID_REQUEST", "Columns don't exist: " + String.join(", ", missing));
        if (removing.contains("id")) throw new RpcException("INVALID_REQUEST", "Cannot remove the primary key 'id' column");
        if (removing.size() == existing.size()) throw new RpcException("INVALID_REQUEST", "Cannot remove all columns.");
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                for (String column : removing) database.update(connection, "ALTER TABLE " + table + " DROP COLUMN " + column, List.of());
                connection.commit();
            } catch (SQLException error) {
                connection.rollback();
                throw error;
            }
        }
        JsonArray removed = new JsonArray(); removing.forEach(removed::add);
        response.addProperty("message", "Columns removed successfully by recreating the table"); response.add("removed_columns", removed);
    }

    private void listTables(JsonObject response) throws SQLException {
        response.add("tables", database.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name", List.of()));
    }

    private void tableInfo(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String table = Jsons.identifier(Jsons.requiredString(request, "table_name"), "table_name");
        JsonArray columns = database.query("PRAGMA table_info(" + table + ")", List.of());
        if (columns.isEmpty()) throw new RpcException("NOT_FOUND", "Table not found: " + table);
        response.add("columns", columns);
        for (JsonElement column : columns) if (column.getAsJsonObject().get("pk").getAsInt() != 0) {
            response.addProperty("pk_column", column.getAsJsonObject().get("name").getAsString());
            return;
        }
        response.add("pk_column", JsonNull.INSTANCE);
    }

    private void tableData(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String table = Jsons.identifier(Jsons.requiredString(request, "table_name"), "table_name");
        JsonArray columns = database.query("PRAGMA table_info(" + table + ")", List.of());
        if (columns.isEmpty()) throw new RpcException("NOT_FOUND", "Table not found: " + table);
        if (request.has("limit")) {
            int limit = Math.min(10000, Math.max(1, Jsons.optionalInt(request, "limit", 100)));
            int offset = Math.max(0, Jsons.optionalInt(request, "offset", 0));
            response.add("data", database.query("SELECT * FROM " + table + " LIMIT ? OFFSET ?", List.of(limit, offset)));
        } else response.add("data", database.query("SELECT * FROM " + table, List.of()));
    }

    private void addTableRow(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String table = Jsons.identifier(Jsons.requiredString(request, "table_name"), "table_name");
        JsonObject body = Jsons.body(request).deepCopy(); body.remove("table_name"); body.remove("limit"); body.remove("offset");
        List<String> columns = new ArrayList<>(); List<Object> values = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : body.entrySet()) { columns.add(Jsons.identifier(entry.getKey(), "column")); values.add(Jsons.sqlValue(entry.getValue())); }
        if (columns.isEmpty()) throw new RpcException("INVALID_REQUEST", "No columns provided.");
        try (Connection connection = database.open(); PreparedStatement statement = connection.prepareStatement("INSERT INTO " + table + "(" + String.join(",", columns) + ") VALUES(" + placeholders(columns.size()) + ")")) {
            for (int index=0; index<values.size(); index++) statement.setObject(index+1, values.get(index));
            statement.executeUpdate();
            long rowId=database.one(connection,"SELECT last_insert_rowid() AS row_id",List.of()).get("row_id").getAsLong();
            response.addProperty("message", "Row added successfully"); response.addProperty("row_id", rowId);
        }
    }

    private void updateTableRow(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String table = Jsons.identifier(Jsons.requiredString(request, "table_name"), "table_name");
        String pk = Jsons.identifier(Jsons.requiredString(request, "pk_column"), "pk_column");
        String value = Jsons.requiredString(request, "pk_value");
        JsonObject values=Jsons.body(request).deepCopy(); values.remove(pk); values.remove("table_name"); values.remove("pk_column"); values.remove("pk_value");
        if (values.size()==0) throw new RpcException("INVALID_REQUEST", "No columns provided to update.");
        List<String> sets=new ArrayList<>();List<Object>parameters=new ArrayList<>();for(Map.Entry<String,JsonElement>entry:values.entrySet()){sets.add(Jsons.identifier(entry.getKey(),"column")+"=?");parameters.add(Jsons.sqlValue(entry.getValue()));}parameters.add(value);
        int count=database.update("UPDATE "+table+" SET "+String.join(",",sets)+" WHERE "+pk+"=?",parameters);
        if(count==0)throw new RpcException("NOT_FOUND", "Row with "+pk+"='"+value+"' not found in table '"+table+"'.");
        response.addProperty("message", "Row with "+pk+"='"+value+"' updated successfully.");
    }

    private void deleteTableRow(JsonObject request, JsonObject response) throws RpcException, SQLException {
        String table = Jsons.identifier(Jsons.requiredString(request, "table_name"), "table_name");
        String pk = Jsons.identifier(Jsons.requiredString(request, "pk_column"), "pk_column");
        int count = database.update("DELETE FROM " + table + " WHERE " + pk + "=?", List.of(Jsons.requiredString(request, "pk_value")));
        String value=Jsons.requiredString(request, "pk_value");
        if (count == 0) throw new RpcException("NOT_FOUND", "Row with "+pk+"='"+value+"' not found in table '"+table+"'."); response.addProperty("message", "Row with "+pk+"='"+value+"' deleted successfully.");
    }

    private void createBackup(JsonObject request, JsonObject response) throws RpcException, SQLException, IOException {
        String configured = Jsons.optionalString(request, "path", null);
        Path path = configured == null ? database.path().resolveSibling("backups").resolve("products-" + Instant.now().toEpochMilli() + ".db") : Path.of(configured);
        response.addProperty("path", database.backup(path).toString()); response.addProperty("message", "Backup created successfully");
    }

    private void restoreBackup(JsonObject request, JsonObject response) throws RpcException, SQLException, IOException {
        database.restore(Path.of(Jsons.requiredString(request, "path")));
        response.addProperty("message", "Backup restored successfully");
    }

    private void deleteManagedBackup(JsonObject request, JsonObject response) throws Exception {
        String id = requestId(request, "backup_id", "id");
        if (!backups.delete(id)) throw new RpcException("NOT_FOUND", "Backup not found");
        response.addProperty("detail", "Backup deleted."); response.addProperty("deleted_id", id);
    }

    private void deleteSelectedBackups(JsonObject request, JsonObject response) throws Exception {
        JsonArray ids = Jsons.requiredArray(request, "ids");
        if (ids.isEmpty()) throw new RpcException("INVALID_REQUEST", "A non-empty ids array is required");
        List<String> values = new ArrayList<>(); ids.forEach(id -> values.add(id.getAsString()));
        response.addProperty("detail", "Selected backups deleted."); response.add("deleted_ids", backups.deleteSelected(values));
    }

    private void verifyBackupAdmin(JsonObject request) throws Exception {
        String token = Jsons.optionalString(request, "token", Jsons.optionalString(request, "bearer_token", null));
        backups.verifyAdmin(token);
    }

    private void systemSnapshot(JsonObject response) {
        copyInto(SystemMetrics.snapshot(), response);
    }

    private void analytics(JsonObject response) throws SQLException {
        JsonObject core = database.one("SELECT ROUND(COALESCE(SUM(CASE WHEN order_status='ORDER_DELIVERED' THEN total ELSE 0 END),0),2) total_earnings_overall,COUNT(*) total_orders_overall,COALESCE(SUM(CASE WHEN strftime('%Y-%m',created_at)=strftime('%Y-%m','now') THEN 1 ELSE 0 END),0) total_orders_this_month,COALESCE(SUM(CASE WHEN strftime('%Y-%m',created_at)=strftime('%Y-%m','now','-1 month') THEN 1 ELSE 0 END),0) total_orders_last_month,ROUND(COALESCE(SUM(CASE WHEN order_status='ORDER_DELIVERED' AND strftime('%Y-%m',created_at)=strftime('%Y-%m','now') THEN total ELSE 0 END),0),2) total_earnings_this_month,ROUND(COALESCE(SUM(CASE WHEN order_status='ORDER_DELIVERED' AND strftime('%Y-%m',created_at)=strftime('%Y-%m','now','-1 month') THEN total ELSE 0 END),0),2) total_earnings_last_month FROM orders", List.of());
        copyInto(core, response);
        JsonArray low = database.query("SELECT id,product_name name,stock FROM products WHERE stock<5", List.of()); response.addProperty("low_stock_products_count", low.size()); response.addProperty("low_stock_threshold", 5); response.add("low_stock_products_list", low);
        JsonObject distribution = new JsonObject(); for (JsonElement row : database.query("SELECT order_status,COUNT(*) count FROM orders GROUP BY order_status", List.of())) distribution.addProperty(row.getAsJsonObject().get("order_status").getAsString(), row.getAsJsonObject().get("count").getAsLong()); response.add("order_status_distribution", distribution);
        JsonArray topProducts = database.query("SELECT COALESCE(json_extract(item.value,'$.product_name'),p.product_name) name,ROUND(SUM(CAST(json_extract(item.value,'$.count') AS INTEGER)*CAST(json_extract(item.value,'$.cost_mrp') AS REAL)),2) revenue FROM orders o,json_each(o.items_detail) item LEFT JOIN products p ON json_extract(item.value,'$.product_id')=p.product_id WHERE o.order_status='ORDER_DELIVERED' GROUP BY name ORDER BY revenue DESC LIMIT 10", List.of());
        if (topProducts.isEmpty()) { JsonObject note = new JsonObject(); note.addProperty("note", "No product sales data found."); topProducts.add(note); } response.add("top_selling_products_revenue", topProducts);
        JsonArray topUsers = database.query("SELECT u.name username,ROUND(SUM(o.total),2) total_value FROM orders o JOIN userdata u ON o.user_id=u.uid OR o.user_id=u.id WHERE o.order_status='ORDER_DELIVERED' GROUP BY u.name ORDER BY total_value DESC LIMIT 5", List.of());
        if (topUsers.isEmpty()) { JsonObject note = new JsonObject(); note.addProperty("note", "No user order data found."); topUsers.add(note); } response.add("top_order_taking_users", topUsers);
        JsonObject trend = new JsonObject(); JsonArray labels = new JsonArray(), data = new JsonArray();
        JsonArray rows = database.query("SELECT strftime('%Y-%m',created_at) month,COUNT(*) count FROM orders WHERE created_at>=date('now','-12 months') GROUP BY month", List.of()); Map<String, Integer> counts = new LinkedHashMap<>(); rows.forEach(row -> counts.put(row.getAsJsonObject().get("month").getAsString(), row.getAsJsonObject().get("count").getAsInt()));
        YearMonth current = YearMonth.now(ZoneId.of("UTC")); for (int i = 12; i >= 1; i--) { String label = current.minusMonths(i).toString(); labels.add(label); data.add(counts.getOrDefault(label, 0)); }
        trend.add("labels", labels); trend.add("data", data); if (rows.isEmpty()) trend.addProperty("note", "No order data for the last 12 months."); response.add("orders_trend_12_months", trend);
    }

    private void gstDashboard(JsonObject request, JsonObject response) throws RpcException, SQLException {
        DateRange range = financialYear(request);
        JsonObject totals = database.one("SELECT COUNT(*) invoice_count,COALESCE(SUM(total_rate-total_discount),0) taxable_value,COALESCE(SUM(total_gst),0) output_tax,COALESCE(SUM(total),0) gross_sales FROM orders WHERE date(created_at) BETWEEN date(?) AND date(?)", List.of(range.from, range.to));
        response.add("period", range.json()); response.add("totals", totals);
        response.add("status", database.query("SELECT order_status,COUNT(*) count,COALESCE(SUM(total),0) total FROM orders WHERE date(created_at) BETWEEN date(?) AND date(?) GROUP BY order_status", List.of(range.from, range.to)));
    }

    private void salesRegister(JsonObject request, JsonObject response) throws RpcException, SQLException {
        DateRange range = dateRange(request);
        response.add("sales", database.query("SELECT o.*,u.name user_name,u.gstin user_gstin FROM orders o LEFT JOIN userdata u ON o.user_id=u.uid WHERE date(o.created_at) BETWEEN date(?) AND date(?) ORDER BY o.created_at", List.of(range.from, range.to)));
    }

    private void createNote(String table, String prefix, JsonObject request, JsonObject response) throws RpcException, SQLException {
        JsonObject input = Jsons.body(request).deepCopy(); requireFields(input, "user_id", "items");
        String idColumn = prefix + "_id", numberColumn = prefix + "_number";
        LocalDate nowDate = LocalDate.now(ZoneId.of("UTC")); int fyStart = nowDate.getMonthValue() >= 4 ? nowDate.getYear() : nowDate.getYear() - 1;
        String notePrefix = prefix.toUpperCase() + "/" + fyStart + "-" + String.valueOf(fyStart + 1).substring(2) + "/";
        JsonObject last = database.one("SELECT " + numberColumn + " FROM " + table + " WHERE " + numberColumn + " LIKE ? ORDER BY " + numberColumn + " DESC LIMIT 1", List.of(notePrefix + "%"));
        int sequence = last == null ? 1 : Integer.parseInt(last.get(numberColumn).getAsString().substring(last.get(numberColumn).getAsString().lastIndexOf('/') + 1)) + 1;
        String number = notePrefix + String.format("%03d", sequence);
        JsonArray items = Jsons.requiredArray(input, "items"); BigDecimal subtotal = Money.ZERO, cgst = Money.ZERO, sgst = Money.ZERO;
        for (JsonElement element : items) { JsonObject item = element.getAsJsonObject(); BigDecimal quantity = item.has("qty") ? Money.of(item, "qty") : BigDecimal.ONE; BigDecimal taxable = quantity.multiply(Money.of(item, "rate"));
            BigDecimal halfTax = Money.percent(taxable, Money.of(item, "gst_rate").divide(new BigDecimal("2"), 12, Money.ROUNDING)); subtotal = subtotal.add(taxable); cgst = cgst.add(halfTax); sgst = sgst.add(halfTax); }
        String id = input.has(idColumn) ? input.get(idColumn).getAsString() : CrmDatabase.uuid().substring(0, 12);
        JsonObject body = new JsonObject();
        body.addProperty(idColumn, id);
        body.addProperty(numberColumn, number); body.addProperty("original_invoice", Jsons.optionalString(input, "original_invoice", ""));
        body.addProperty("user_id", Jsons.requiredString(input, "user_id")); body.addProperty("user_name", Jsons.optionalString(input, "user_name", ""));
        body.addProperty("user_gstin", Jsons.optionalString(input, "user_gstin", "")); body.addProperty("reason", Jsons.optionalString(input, "reason", "")); body.add("items", items.deepCopy());
        body.addProperty("subtotal", Money.output(subtotal)); body.addProperty("cgst_total", Money.output(cgst)); body.addProperty("sgst_total", Money.output(sgst));
        body.addProperty("total", Money.output(subtotal.add(cgst).add(sgst))); body.addProperty("created_at", Instant.now().toString()); body.addProperty("notes", Jsons.optionalString(input, "notes", ""));
        database.insert(table, body, idColumn, id);
        response.addProperty(idColumn, id); response.addProperty(numberColumn, number); response.addProperty("total", Money.output(subtotal.add(cgst).add(sgst)));
    }

    private void listNotes(String table, JsonObject request, JsonObject response) throws RpcException, SQLException {
        JsonArray notes;
        if (request.has("from_date") && request.has("to_date")) { DateRange range = dateRange(request); notes = database.query("SELECT * FROM " + table + " WHERE date(created_at) BETWEEN date(?) AND date(?) ORDER BY created_at DESC", List.of(range.from, range.to)); }
        else notes = database.query("SELECT * FROM " + table + " ORDER BY created_at DESC", List.of());
        response.add("notes", notes);
    }

    private void partyLedger(JsonObject request, JsonObject response) throws RpcException, SQLException {
        DateRange range = dateRange(request); String userId = Jsons.optionalString(request, "user_id", null);
        String filter = userId == null ? "" : " AND o.user_id=?"; List<Object> values = new ArrayList<>(List.of(range.from, range.to)); if (userId != null) values.add(userId);
        response.add("entries", database.query("SELECT o.created_at date,o.order_id reference,o.user_id,'SALE' type,o.total debit,0 credit,o.order_status status FROM orders o WHERE date(o.created_at) BETWEEN date(?) AND date(?)" + filter + " ORDER BY o.created_at", values));
    }

    private void dayBook(JsonObject request, JsonObject response) throws RpcException, SQLException {
        DateRange range = dateRange(request);
        response.add("days", database.query("SELECT date(created_at) date,COUNT(*) transactions,COALESCE(SUM(total_rate-total_discount),0) taxable,COALESCE(SUM(total_gst),0) tax,COALESCE(SUM(total),0) total FROM orders WHERE date(created_at) BETWEEN date(?) AND date(?) GROUP BY date(created_at) ORDER BY date(created_at)", List.of(range.from, range.to)));
    }

    private void profitLoss(JsonObject request, JsonObject response) throws RpcException, SQLException {
        DateRange range = financialYear(request);
        response.add("summary", database.one("SELECT COALESCE(SUM(o.total),0) revenue,COALESCE(SUM((SELECT SUM(json_extract(j.value,'$.quantity')*json_extract(j.value,'$.cost_rate')) FROM json_each(o.items_detail) j)),0) estimated_cost,COALESCE(SUM(o.total_gst),0) tax FROM orders o WHERE date(created_at) BETWEEN date(?) AND date(?)", List.of(range.from, range.to)));
        response.add("period", range.json());
    }

    private void stockSummary(JsonObject response) throws SQLException {
        response.add("products", database.query("SELECT id,product_id,product_name,stock,cost_rate,cost_mrp,cost_gst,ROUND(stock*cost_rate,2) stock_value FROM products ORDER BY product_name", List.of()));
        response.add("totals", database.one("SELECT COUNT(*) products,COALESCE(SUM(stock),0) units,COALESCE(SUM(stock*cost_rate),0) stock_value FROM products", List.of()));
    }

    private void outstanding(JsonObject response) throws SQLException {
        response.add("parties", database.query("SELECT u.uid user_id,u.name,u.credits,COALESCE(SUM(CASE WHEN o.order_status NOT IN ('ORDER_DELIVERED','ORDER_CANCELLED') THEN o.total ELSE 0 END),0) outstanding FROM userdata u LEFT JOIN orders o ON o.user_id=u.uid GROUP BY u.uid,u.name,u.credits ORDER BY outstanding DESC", List.of()));
    }

    private void taxLedger(JsonObject request, JsonObject response) throws RpcException, SQLException {
        DateRange range = dateRange(request);
        response.add("output", database.query("SELECT date(created_at) date,order_id reference,total_rate-total_discount taxable,total_gst tax,total FROM orders WHERE date(created_at) BETWEEN date(?) AND date(?) ORDER BY created_at", List.of(range.from, range.to)));
        response.add("creditNotes", database.query("SELECT created_at date,cn_number reference,subtotal taxable,cgst_total+sgst_total tax,total FROM credit_notes WHERE date(created_at) BETWEEN date(?) AND date(?)", List.of(range.from, range.to)));
    }

    private void dashboardExtras(JsonObject request, JsonObject response) throws RpcException, SQLException {
        DateRange range = financialYear(request);
        response.add("monthlySales", database.query("SELECT strftime('%Y-%m',created_at) month,COUNT(*) invoices,COALESCE(SUM(total),0) sales,COALESCE(SUM(total_gst),0) tax FROM orders WHERE date(created_at) BETWEEN date(?) AND date(?) GROUP BY month ORDER BY month", List.of(range.from, range.to)));
        response.add("topParties", database.query("SELECT u.uid user_id,u.name,COUNT(o.order_id) orders,COALESCE(SUM(o.total),0) sales FROM userdata u JOIN orders o ON o.user_id=u.uid WHERE date(o.created_at) BETWEEN date(?) AND date(?) GROUP BY u.uid,u.name ORDER BY sales DESC LIMIT 10", List.of(range.from, range.to)));
    }

    private JsonArray queryWithFilters(String table, JsonObject request, boolean joinUsers) throws RpcException, SQLException {
        JsonArray filters = request.has("filters") ? Jsons.requiredArray(request, "filters") : new JsonArray();
        List<String> clauses = new ArrayList<>(); List<Object> parameters = new ArrayList<>();
        for (JsonElement element : filters) {
            JsonObject filter = element.getAsJsonObject(); String field = Jsons.identifier(Jsons.requiredString(filter, "field"), "filter.field");
            String operator = Jsons.requiredString(filter, "operator"); JsonElement value = filter.get("value");
            switch (operator) {
                case "eq": clauses.add(field + "=?"); parameters.add(Jsons.sqlValue(value)); break;
                case "neq": clauses.add(field + "!=?"); parameters.add(Jsons.sqlValue(value)); break;
                case "gt": clauses.add(field + ">?"); parameters.add(Jsons.sqlValue(value)); break;
                case "lt": clauses.add(field + "<?"); parameters.add(Jsons.sqlValue(value)); break;
                case "gte": clauses.add(field + ">=?"); parameters.add(Jsons.sqlValue(value)); break;
                case "lte": clauses.add(field + "<=?"); parameters.add(Jsons.sqlValue(value)); break;
                case "contains": clauses.add(field + " LIKE ?"); parameters.add("%" + value.getAsString() + "%"); break;
                case "startswith": clauses.add(field + " LIKE ?"); parameters.add(value.getAsString() + "%"); break;
                case "endswith": clauses.add(field + " LIKE ?"); parameters.add("%" + value.getAsString()); break;
                case "in":
                    if (value == null || !value.isJsonArray() || value.getAsJsonArray().isEmpty()) clauses.add("1=0");
                    else { clauses.add(field + " IN (" + placeholders(value.getAsJsonArray().size()) + ")"); value.getAsJsonArray().forEach(v -> parameters.add(Jsons.sqlValue(v))); }
                    break;
                default: throw new RpcException("INVALID_REQUEST", "Unsupported query operator: " + operator);
            }
        }
        String select = joinUsers ? "SELECT " + table + ".*,userdata.name user_name FROM " + table + " LEFT JOIN userdata ON " + table + ".user_id=userdata.uid" : "SELECT * FROM " + table;
        String where = "";
        if (clauses.size() == 1) where = " WHERE " + clauses.get(0);
        else if (clauses.size() > 1) where = " WHERE " + clauses.get(0) + " AND (" + String.join(" OR ", clauses.subList(1, clauses.size())) + ")";
        String sql = select + where;
        if (request.has("order_by")) sql += " ORDER BY " + Jsons.identifier(request.get("order_by").getAsString(), "order_by") + " " + Jsons.direction(Jsons.optionalString(request, "order_direction", "ASC"));
        if (request.has("limit") && !request.get("limit").isJsonNull()) { sql += " LIMIT ? OFFSET ?"; parameters.add(Math.min(1000, Math.max(1, request.get("limit").getAsInt()))); parameters.add(Math.max(0, Jsons.optionalInt(request, "offset", 0))); }
        return database.query(sql, parameters);
    }

    private void updateObject(String table, JsonObject body, String idColumn, String idValue) throws RpcException, SQLException {
        if (body.size() == 0) throw new RpcException("INVALID_REQUEST", "No fields to update");
        List<String> assignments = new ArrayList<>(); List<Object> values = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : body.entrySet()) { assignments.add(Jsons.identifier(entry.getKey(), "column") + "=?"); values.add(Jsons.sqlValue(entry.getValue())); }
        values.add(idValue); int count = database.update("UPDATE " + Jsons.identifier(table, "table") + " SET " + String.join(",", assignments) + " WHERE " + Jsons.identifier(idColumn, "idColumn") + "=?", values);
        if (count == 0) throw new RpcException("NOT_FOUND", "Record not found");
    }

    private void deleteById(String table, String column, JsonObject request, JsonObject response, String label) throws RpcException, SQLException {
        String id = requestId(request, column, "id");
        if (database.update("DELETE FROM " + table + " WHERE " + column + "=?", List.of(id)) == 0) throw new RpcException("NOT_FOUND", label + " not found");
        response.addProperty("message", label + " deleted successfully");
    }

    private void verifyOfferProducts(JsonArray ids, String allowedGroup) throws RpcException, SQLException {
        if (ids.isEmpty()) throw new RpcException("INVALID_REQUEST", "product_ids must not be empty");
        List<Object> parameters = new ArrayList<>(); ids.forEach(id -> parameters.add(id.getAsString()));
        JsonArray rows = database.query("SELECT product_id,offer_group_id FROM products WHERE product_id IN (" + placeholders(ids.size()) + ")", parameters);
        if (rows.size() != ids.size()) throw new RpcException("NOT_FOUND", "One or more products were not found");
        for (JsonElement row : rows) {
            JsonElement group = row.getAsJsonObject().get("offer_group_id");
            if (group != null && !group.isJsonNull() && !group.getAsString().isBlank() && !group.getAsString().equals(allowedGroup)) throw new RpcException("CONFLICT", "Product already belongs to another offer group: " + row.getAsJsonObject().get("product_id").getAsString());
        }
    }

    private static void validateOfferGroup(JsonObject body) throws RpcException {
        requireFields(body, "name", "buy_qty", "free_qty", "product_ids");
        if (Jsons.optionalInt(body, "buy_qty", 0) < 1 || Jsons.optionalInt(body, "free_qty", 0) < 1) throw new RpcException("INVALID_REQUEST", "buy_qty and free_qty must be at least 1");
    }

    private DateRange dateRange(JsonObject request) throws RpcException {
        String from = Jsons.optionalString(request, "from_date", LocalDate.now().withDayOfMonth(1).toString());
        String to = Jsons.optionalString(request, "to_date", LocalDate.now().toString());
        try { LocalDate.parse(from); LocalDate.parse(to); } catch (RuntimeException error) { throw new RpcException("INVALID_REQUEST", "Dates must use YYYY-MM-DD", error); }
        return new DateRange(from, to);
    }

    private DateRange financialYear(JsonObject request) throws RpcException {
        String fy = Jsons.optionalString(request, "fy", null); LocalDate now = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        int start = now.getMonthValue() >= 4 ? now.getYear() : now.getYear() - 1;
        if (fy != null) {
            try { start = Integer.parseInt(fy.substring(0, 4)); } catch (RuntimeException error) { throw new RpcException("INVALID_REQUEST", "fy must look like 2025-26", error); }
        }
        return new DateRange(LocalDate.of(start, 4, 1).toString(), LocalDate.of(start + 1, 3, 31).toString());
    }

    private static String requestId(JsonObject request, String... names) throws RpcException {
        for (String name : names) if (request.has(name) && !request.get(name).isJsonNull() && !request.get(name).getAsString().isBlank()) return request.get(name).getAsString();
        JsonElement body = request.get("body");
        if (body != null && body.isJsonObject()) for (String name : names) if (body.getAsJsonObject().has(name)) return body.getAsJsonObject().get(name).getAsString();
        throw new RpcException("INVALID_REQUEST", String.join(" or ", names) + " is required");
    }

    private static void requireFields(JsonObject object, String... names) throws RpcException {
        for (String name : names) if (!object.has(name) || object.get(name).isJsonNull()) throw new RpcException("INVALID_REQUEST", name + " is required");
    }

    private static void rejectUnknown(JsonObject object, Set<String> allowed) throws RpcException {
        for (String key : object.keySet()) if (!allowed.contains(key)) throw new RpcException("INVALID_REQUEST", "Unknown field: " + key);
    }

    private static void defaults(JsonObject object, Map<String, Object> defaults) {
        defaults.forEach((name, value) -> { if (!object.has(name) || object.get(name).isJsonNull()) {
            if (value instanceof String) object.addProperty(name, (String) value); else if (value instanceof Number) object.addProperty(name, (Number) value); else if (value instanceof Boolean) object.addProperty(name, (Boolean) value); else object.add(name, ((JsonElement) value).deepCopy());
        }});
    }

    private static void copyInto(JsonObject source, JsonObject target) { source.entrySet().forEach(entry -> target.add(entry.getKey(), entry.getValue().deepCopy())); }
    private static double number(JsonObject object, String name) { return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsDouble() : 0; }
    private static double round3(double value) { return Math.round(value * 1000.0) / 1000.0; }
    private static String placeholders(int count) { String[] values = new String[count]; Arrays.fill(values, "?"); return String.join(",", values); }
    private static List<Object> duplicate(List<Object> values) { List<Object> result = new ArrayList<>(values); result.addAll(values); return result; }
    private static JsonArray uniqueStrings(JsonArray input) { LinkedHashSet<String> values = new LinkedHashSet<>(); input.forEach(v -> values.add(v.getAsString())); JsonArray result = new JsonArray(); values.forEach(result::add); return result; }
    private static String shortOrderId() { return LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "-" + CrmDatabase.uuid().substring(0, 6).toUpperCase(); }
    private static String allowedColumnType(String input) throws RpcException { String value = input.trim().toUpperCase(); if (!Set.of("TEXT", "INTEGER", "REAL", "BLOB", "NUMERIC").contains(value)) throw new RpcException("INVALID_REQUEST", "Unsupported column type"); return value; }
    private static String safeSqlMessage(SQLException error) { String value = String.valueOf(error.getMessage()); return value.length() > 300 ? value.substring(0, 300) : value; }

    private static int offerFree(JsonObject product, int paid) {
        int buy = product.has("offer_buy_qty") && !product.get("offer_buy_qty").isJsonNull() ? product.get("offer_buy_qty").getAsInt() : 0;
        int free = product.has("offer_free_qty") && !product.get("offer_free_qty").isJsonNull() ? product.get("offer_free_qty").getAsInt() : 0;
        boolean active = product.has("offer_active") && product.get("offer_active").getAsBoolean() && buy > 0 && free > 0;
        return active ? (paid / buy) * free : 0;
    }

    private static boolean offerFieldsChanged(JsonObject existing, JsonObject changes) {
        for (String field : List.of("offer_active","offer_buy_qty","offer_free_qty")) {
            if (changes.has(field) && !String.valueOf(Jsons.sqlValue(changes.get(field))).equals(String.valueOf(Jsons.sqlValue(existing.get(field))))) return true;
        }
        return false;
    }

    private static JsonObject checkoutItems(JsonObject body) throws RpcException {
        if (body.has("items")) {
            if (!body.get("items").isJsonObject()) throw new RpcException("INVALID_REQUEST", "items must be an object of product_id to quantity");
            return body.getAsJsonObject("items");
        }
        JsonObject items = new JsonObject();
        for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
            if (Set.of("user_id", "otherData", "address", "notes", "order_id").contains(entry.getKey())) continue;
            items.add(entry.getKey(), entry.getValue().deepCopy());
        }
        if (items.isEmpty()) throw new RpcException("INVALID_REQUEST", "No cart products were supplied");
        return items;
    }

    private static int comparePick(JsonObject left, JsonObject right) {
        int category = value(left, "cat_id").compareToIgnoreCase(value(right, "cat_id")); if (category != 0) return category;
        int subcategory = normalizedSubcategory(value(left, "cat_sub")).compareTo(normalizedSubcategory(value(right, "cat_sub"))); if (subcategory != 0) return subcategory;
        int name = naturalCompare(value(left, "product_name"), value(right, "product_name")); if (name != 0) return name;
        return value(left, "product_id").compareToIgnoreCase(value(right, "product_id"));
    }

    private static String value(JsonObject object, String key) { JsonElement value = object.get(key); return value == null || value.isJsonNull() ? "" : value.getAsString(); }
    private static String normalizedSubcategory(String input) { String[] values = input.toLowerCase().split(","); Arrays.sort(values); return String.join(",", values).replace(" ", ""); }
    private static int naturalCompare(String a, String b) {
        int ai = 0, bi = 0;
        while (ai < a.length() && bi < b.length()) {
            if (Character.isDigit(a.charAt(ai)) && Character.isDigit(b.charAt(bi))) {
                int ae = ai, be = bi; while (ae < a.length() && Character.isDigit(a.charAt(ae))) ae++; while (be < b.length() && Character.isDigit(b.charAt(be))) be++;
                int cmp = new BigDecimal(a.substring(ai, ae)).compareTo(new BigDecimal(b.substring(bi, be))); if (cmp != 0) return cmp; ai = ae; bi = be;
            } else { int cmp = Character.compare(Character.toLowerCase(a.charAt(ai)), Character.toLowerCase(b.charAt(bi))); if (cmp != 0) return cmp; ai++; bi++; }
        }
        return Integer.compare(a.length(), b.length());
    }

    private void publishOfferQuietly(JsonObject product, boolean notify) {
        if (!firebase.enabled()) return;
        try {
            String productId = product.has("product_id") && !product.get("product_id").isJsonNull() ? product.get("product_id").getAsString() : product.get("id").getAsString();
            String key = "offer_" + productId.replaceAll("[^A-Za-z0-9_-]", "_");
            int buy = product.has("offer_buy_qty") ? product.get("offer_buy_qty").getAsInt() : 0;
            int free = product.has("offer_free_qty") ? product.get("offer_free_qty").getAsInt() : 0;
            boolean active = product.has("offer_active") && product.get("offer_active").getAsBoolean() && buy > 0 && free > 0;
            String path = "datas/announcement/all/" + key;
            if (!active) { firebase.delete(path); return; }
            String title = "Buy " + buy + ", get " + free + " FREE";
            String body = value(product, "product_name") + ": every " + buy + " paid items includes " + free + " extra free.";
            Map<String, Object> data = new LinkedHashMap<>(); data.put("type", "offer"); data.put("product_id", productId); data.put("title", title); data.put("subtitle", body);
            JsonArray images = product.has("product_img") && product.get("product_img").isJsonArray() ? product.getAsJsonArray("product_img") : new JsonArray(); data.put("img", images.isEmpty() ? "" : images.get(0).getAsString());
            firebase.set(path, data);
            if (notify) firebase.notifyTopic("all_users", "New Petsfort offer", body, Map.of("type", "offer", "product_id", productId));
        } catch (Exception warning) { System.err.println("Offer announcement failed: " + warning.getMessage()); }
    }

    private void publishOfferGroupQuietly(JsonObject group, boolean notify) {
        if (!firebase.enabled()) return;
        try {
            String id = group.get("id").getAsString(), path = "datas/announcement/all/offer_group_" + id;
            if (!"ACTIVE".equals(value(group, "status"))) { firebase.delete(path); return; }
            JsonArray ids = group.getAsJsonArray("product_ids"); String title = value(group, "name") + " · Buy " + group.get("buy_qty").getAsInt() + ", get " + group.get("free_qty").getAsInt() + " FREE";
            String body = value(group, "description"); if (body.isBlank()) body = "Available on " + ids.size() + " selected products. Tap to view them all.";
            Map<String, Object> data = new LinkedHashMap<>(); data.put("type", "offer_group"); data.put("group_id", id); data.put("product_count", ids.size()); data.put("title", title); data.put("subtitle", body); data.put("img", "");
            firebase.set(path, data); if (notify) firebase.notifyTopic("all_users", value(group, "name"), body, Map.of("type", "offer_group", "group_id", id));
        } catch (Exception warning) { System.err.println("Offer group announcement failed: " + warning.getMessage()); }
    }

    private static final class DateRange {
        final String from, to;
        DateRange(String from, String to) { this.from = from; this.to = to; }
        JsonObject json() { JsonObject result = new JsonObject(); result.addProperty("from", from); result.addProperty("to", to); return result; }
    }
}
