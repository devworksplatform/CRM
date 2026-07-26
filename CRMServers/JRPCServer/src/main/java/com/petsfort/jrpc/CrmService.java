package com.petsfort.jrpc;

import com.google.gson.*;
import com.jay.rpc.RpcException;
import com.jay.rpc.RpcHandler;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

final class CrmService implements AutoCloseable {
    private static final Set<String> HTML_OPERATIONS = Set.of(
            "GET_ROOT_HTML", "GET_PRIVACY_POLICY_HTML", "GET_DATABASE_HTML", "GET_ANALYTICS_HTML");
    private final CrmDatabase database;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Object checkoutLock = new Object();

    private CrmService(CrmDatabase database) {
        this.database = database;
    }

    static CrmService open(CrmConfiguration configuration) {
        return new CrmService(new CrmDatabase(configuration));
    }

    RpcHandler<CrmRpc> handlerFor(CrmRpc rpc) {
        switch (rpc) {
            case POST_PRODUCTS: return this::postProducts;
            case POST_PRODUCTS_QUERY: return this::postProductsQuery;
            case GET_PRODUCTS: return this::getProducts;
            case GET_PRODUCT: return this::getProduct;
            case PUT_PRODUCT: return this::putProduct;
            case DELETE_PRODUCT: return this::deleteProduct;
            case GET_OFFER_GROUPS: return this::getOfferGroups;
            case POST_OFFER_GROUP: return this::postOfferGroup;
            case PUT_OFFER_GROUP: return this::putOfferGroup;
            case POST_OFFER_GROUP_APPLY: return this::applyOfferGroup;
            case POST_OFFER_GROUP_CANCEL: return this::cancelOfferGroup;
            case DELETE_OFFER_GROUP: return this::deleteOfferGroup;
            case POST_SCHEMA_ADD_COLUMNS: return this::addSchemaColumns;
            case POST_SCHEMA_REMOVE_COLUMNS: return this::removeSchemaColumns;
            case GET_SCHEMA: return this::getSchema;
            case POST_PRODUCTS_BULK_DETAILS: return this::getBulkProducts;
            case POST_ORDERS_CHECKOUT: return this::checkout;
            case GET_BILL: return this::getBill;
            case POST_ORDERS_QUERY: return this::queryOrders;
            case PUT_ORDER: return this::putOrder;
            case DELETE_ORDER: return this::deleteOrder;
            case GET_CATEGORIES: return this::getCategories;
            case POST_CATEGORY: return this::postCategory;
            case DELETE_CATEGORY: return this::deleteCategory;
            case GET_SUBCATEGORIES: return this::getSubcategories;
            case GET_SUBCATEGORIES_V0_BY_CATEGORY: return this::getSubcategoriesV0;
            case GET_SUBCATEGORIES_BY_CATEGORY: return this::getSubcategoriesByCategory;
            case POST_SUBCATEGORY: return this::postSubcategory;
            case DELETE_SUBCATEGORY: return this::deleteSubcategory;
            case GET_USERDATA: return this::getUsers;
            case GET_USER: return this::getUser;
            case POST_USERDATA: return this::postUser;
            case PUT_USERDATA: return this::putUser;
            case DELETE_USERDATA: return this::deleteUser;
            case GET_DATABASE_TABLES: return this::getTables;
            case GET_DATABASE_TABLE_INFO: return this::getTableInfo;
            case GET_DATABASE_TABLE_DATA: return this::getTableData;
            case POST_DATABASE_TABLE_ROW: return this::postTableRow;
            case PUT_DATABASE_TABLE_ROW: return this::putTableRow;
            case DELETE_DATABASE_TABLE_ROW: return this::deleteTableRow;
            case GET_ANALYTICS_SUMMARY: return this::analyticsSummary;
            case GET_GST_DASHBOARD: return this::gstDashboard;
            case GET_GST_SALES_REGISTER: return this::gstSalesRegister;
            case POST_GST_CREDIT_NOTE: return this::postCreditNote;
            case GET_GST_CREDIT_NOTES: return this::getCreditNotes;
            case DELETE_GST_CREDIT_NOTE: return this::deleteCreditNote;
            case POST_GST_DEBIT_NOTE: return this::postDebitNote;
            case GET_GST_DEBIT_NOTES: return this::getDebitNotes;
            case DELETE_GST_DEBIT_NOTE: return this::deleteDebitNote;
            case GET_GST_PARTY_LEDGER: return this::gstPartyLedger;
            case GET_GST_DAY_BOOK: return this::gstDayBook;
            case GET_GST_PROFIT_LOSS: return this::gstProfitLoss;
            case GET_GST_STOCK_SUMMARY: return this::gstStockSummary;
            case GET_GST_OUTSTANDING: return this::gstOutstanding;
            case GET_GST_TAX_LEDGER: return this::gstTaxLedger;
            case GET_GST_DASHBOARD_EXTRAS: return this::gstDashboardExtras;
            default: return this::notImplementedHtml;
        }
    }

    private interface Action {
        void run(JsonObject request, JsonObject response) throws Exception;
    }

    private void execute(Action action, JsonObject request, JsonObject response) throws RpcException {
        if (closed.get()) throw new RpcException("SERVER_CLOSED", "CRM service is closed");
        try {
            action.run(request, response);
        } catch (ApiFailure failure) {
            response.addProperty("status_code", failure.statusCode);
            response.add("detail", failure.detail);
            throw new RpcException("HTTP_" + failure.statusCode, failure.detail.toString(), failure);
        } catch (SQLException failure) {
            throw new RpcException("DATABASE_ERROR", failure.getMessage(), failure);
        } catch (RpcException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new RpcException("INTERNAL_SERVER_ERROR", failure.getMessage(), failure);
        }
    }

    private void notImplementedHtml(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((request, response) -> {
            if (!HTML_OPERATIONS.contains(rpc.name())) throw new ApiFailure(404, "Unknown operation");
            throw new ApiFailure(501, "HTML responses are not implemented by the JRPC server");
        }, req, res);
    }

    private void postProducts(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute(this::createProduct, req, res);
    }
    private void postProductsQuery(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> r.add("data", queryRows("products", q, true)), req, res);
    }
    private void getProducts(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> r.add("data", listProducts(q)), req, res);
    }
    private void getProduct(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute(this::readProduct, req, res);
    }
    private void putProduct(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute(this::updateProduct, req, res);
    }
    private void deleteProduct(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute(this::removeProduct, req, res);
    }

    private void createProduct(JsonObject request, JsonObject response) throws Exception {
        JsonObject product = payload(request);
        requireProduct(product);
        String productId = Jsons.optionalString(product, "product_id", UUID.randomUUID().toString());
        String id = UUID.randomUUID().toString();
        String now = LocalDateTime.now().toString();
        boolean hasGroup=product.has("offer_group_id");
        String sql = "INSERT INTO products(id,product_id,product_name,product_desc,product_hsn," +
                "product_cid,product_img,cat_id,cat_sub,cost_rate,cost_mrp,cost_gst,cost_dis," +
                "offer_buy_qty,offer_free_qty,offer_active,"+(hasGroup?"offer_group_id,":"")+
                "stock,created_at,updated_at) VALUES("+(hasGroup?
                "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?":"?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?")+")";
        try (Connection connection = database.connect(); PreparedStatement statement = connection.prepareStatement(sql)) {
            int i = 1;
            statement.setString(i++, id);
            statement.setString(i++, productId);
            statement.setString(i++, Jsons.requiredString(product, "product_name"));
            statement.setString(i++, Jsons.optionalString(product, "product_desc", ""));
            statement.setString(i++, Jsons.optionalString(product, "product_hsn", ""));
            statement.setString(i++, Jsons.optionalString(product, "product_cid", ""));
            statement.setString(i++, Jsons.json(product.has("product_img") ? product.get("product_img") : new JsonArray()));
            statement.setString(i++, Jsons.requiredString(product, "cat_id"));
            statement.setString(i++, Jsons.requiredString(product, "cat_sub"));
            statement.setDouble(i++, Jsons.optionalDouble(product, "cost_rate", 0));
            statement.setDouble(i++, requiredDouble(product, "cost_mrp"));
            statement.setDouble(i++, requiredDouble(product, "cost_gst"));
            statement.setDouble(i++, requiredDouble(product, "cost_dis"));
            statement.setInt(i++, Jsons.optionalInt(product, "offer_buy_qty", 0));
            statement.setInt(i++, Jsons.optionalInt(product, "offer_free_qty", 0));
            statement.setInt(i++, Jsons.optionalBoolean(product, "offer_active", false) ? 1 : 0);
            if(hasGroup)setNullableString(statement, i++, product.get("offer_group_id"));
            statement.setInt(i++, requiredInt(product, "stock"));
            statement.setString(i++, now);
            statement.setString(i, now);
            try {
                statement.executeUpdate();
            } catch (SQLException error) {
                if (error.getMessage().contains("UNIQUE constraint failed: products.product_id")) {
                    throw new ApiFailure(409, "Product ID '" + productId + "' already exists.");
                }
                throw error;
            }
            JsonObject created=findProduct(connection, id);
            Jsons.copy(created, response);
            if(created.get("offer_active").getAsBoolean())publishOffer(created,true);
        }
    }

    private JsonArray listProducts(JsonObject request) throws SQLException {
        int limit = Jsons.optionalInt(request, "limit", 100);
        int offset = Jsons.optionalInt(request, "offset", 0);
        JsonArray result = new JsonArray();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM products LIMIT ? OFFSET ?")) {
            statement.setInt(1, Math.max(1, Math.min(1000, limit)));
            statement.setInt(2, Math.max(0, offset));
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(productRow(rows));
            }
        }
        return result;
    }

    private void readProduct(JsonObject request, JsonObject response) throws Exception {
        String identifier = Jsons.requiredString(request, "product_identifier");
        try (Connection connection = database.connect()) {
            JsonObject product = findProduct(connection, identifier);
            if (product == null) throw new ApiFailure(404, "Product not found");
            Jsons.copy(product, response);
        }
    }

    private void updateProduct(JsonObject request, JsonObject response) throws Exception {
        String identifier = Jsons.requiredString(request, "product_identifier");
        JsonObject updates = payload(request);
        requireProduct(updates);
        try (Connection connection = database.connect()) {
            JsonObject current = findProduct(connection, identifier);
            if (current == null) throw new ApiFailure(404, "Product not found");
            JsonObject detachedGroup=null;
            boolean offerWasChanged=Jsons.optionalBoolean(updates,"offer_active",false)!=current.get("offer_active").getAsBoolean()
                    ||Jsons.optionalInt(updates,"offer_buy_qty",0)!=nullableInt(current,"offer_buy_qty")
                    ||Jsons.optionalInt(updates,"offer_free_qty",0)!=nullableInt(current,"offer_free_qty");
            String originalGroup=current.has("offer_group_id")&&!current.get("offer_group_id").isJsonNull()
                    ?current.get("offer_group_id").getAsString():null;
            if(originalGroup!=null&&!originalGroup.isEmpty()){
                if(offerWasChanged){
                    updates.add("offer_group_id",JsonNull.INSTANCE);
                    JsonArray groups=selectArrayOn(connection,"SELECT * FROM offer_groups WHERE id=?",List.of(originalGroup));
                    if(groups.size()>0){detachedGroup=groups.get(0).getAsJsonObject();
                        JsonArray ids=Jsons.parseOr(detachedGroup.get("product_ids"),new JsonArray()).getAsJsonArray(),remaining=new JsonArray();
                        String oldId=current.get("product_id").getAsString();ids.forEach(v->{if(!v.getAsString().equals(oldId))remaining.add(v);});
                        String now=Instant.now().toString();detachedGroup.add("product_ids",remaining);detachedGroup.addProperty("updated_at",now);
                        if(remaining.size()>0)executeOn(connection,"UPDATE offer_groups SET product_ids=?,updated_at=? WHERE id=?",
                                Jsons.json(remaining),now,originalGroup);
                        else{detachedGroup.addProperty("status","CANCELED");detachedGroup.addProperty("canceled_at",now);
                            executeOn(connection,"UPDATE offer_groups SET product_ids=?,status='CANCELED',updated_at=?,canceled_at=? WHERE id=?",
                                    "[]",now,now,originalGroup);}
                    }
                }else updates.addProperty("offer_group_id",originalGroup);
            }
            String sql = "UPDATE products SET product_id=?,product_name=?,product_desc=?,product_hsn=?," +
                    "product_cid=?,product_img=?,cat_id=?,cat_sub=?,cost_rate=?,cost_mrp=?,cost_gst=?," +
                    "cost_dis=?,offer_buy_qty=?,offer_free_qty=?,offer_active=?,offer_group_id=?,stock=?," +
                    "updated_at=? WHERE id=?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                int i = 1;
                statement.setString(i++, Jsons.optionalString(updates, "product_id", current.get("product_id").getAsString()));
                statement.setString(i++, Jsons.requiredString(updates, "product_name"));
                statement.setString(i++, Jsons.optionalString(updates, "product_desc", ""));
                statement.setString(i++, Jsons.optionalString(updates, "product_hsn", ""));
                statement.setString(i++, Jsons.optionalString(updates, "product_cid", ""));
                statement.setString(i++, Jsons.json(updates.get("product_img")));
                statement.setString(i++, Jsons.requiredString(updates, "cat_id"));
                statement.setString(i++, Jsons.requiredString(updates, "cat_sub"));
                statement.setDouble(i++, Jsons.optionalDouble(updates, "cost_rate", 0));
                statement.setDouble(i++, requiredDouble(updates, "cost_mrp"));
                statement.setDouble(i++, requiredDouble(updates, "cost_gst"));
                statement.setDouble(i++, requiredDouble(updates, "cost_dis"));
                statement.setInt(i++, Jsons.optionalInt(updates, "offer_buy_qty", 0));
                statement.setInt(i++, Jsons.optionalInt(updates, "offer_free_qty", 0));
                statement.setInt(i++, Jsons.optionalBoolean(updates, "offer_active", false) ? 1 : 0);
                setNullableString(statement, i++, updates.get("offer_group_id"));
                statement.setInt(i++, requiredInt(updates, "stock"));
                statement.setString(i++, LocalDateTime.now().toString());
                statement.setString(i, current.get("id").getAsString());
                statement.executeUpdate();
            }
            JsonObject changed=findProduct(connection,current.get("id").getAsString());
            Jsons.copy(changed,response);
            if(detachedGroup!=null)publishOfferGroup(detachedGroup,false);
            if(changed.has("offer_group_id")&&!changed.get("offer_group_id").isJsonNull()){
                try{FirebaseBridge.instance().deleteValue("datas/announcement/all/"+offerKey(changed));}catch(Exception ignored){}
            }else publishOffer(changed,changed.get("offer_active").getAsBoolean()&&offerWasChanged);
        }
    }

    private void removeProduct(JsonObject request, JsonObject response) throws Exception {
        String identifier = Jsons.requiredString(request, "product_identifier");
        try (Connection connection = database.connect()) {
            JsonObject product = findProduct(connection, identifier);
            if (product == null) throw new ApiFailure(404, "Product not found");
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM products WHERE id=?")) {
                statement.setString(1, product.get("id").getAsString());
                statement.executeUpdate();
            }
            response.addProperty("message", "Product deleted successfully");
            response.add("deleted_product", product);
            product.addProperty("offer_active",false);
            publishOffer(product,false);
        }
    }

    private void publishOffer(JsonObject product,boolean notify){
        try{
            String productId=Jsons.optionalString(product,"product_id",Jsons.optionalString(product,"id",""));
            String key=offerKey(product);
            int buy=nullableInt(product,"offer_buy_qty"),free=nullableInt(product,"offer_free_qty");
            boolean active=Jsons.optionalBoolean(product,"offer_active",false)&&buy>0&&free>0;
            if(!active){FirebaseBridge.instance().deleteValue("datas/announcement/all/"+key);return;}
            JsonObject value=new JsonObject();String name=Jsons.optionalString(product,"product_name","selected product");
            String title="Buy "+buy+", get "+free+" FREE";
            String body=name+": every "+buy+" paid items includes "+free+" extra free.";
            value.addProperty("type","offer");value.addProperty("product_id",productId);value.addProperty("title",title);
            value.addProperty("subtitle",body);JsonElement images=product.get("product_img");
            value.addProperty("img",images!=null&&images.isJsonArray()&&images.getAsJsonArray().size()>0?
                    images.getAsJsonArray().get(0).getAsString():"");
            FirebaseBridge.instance().setValue("datas/announcement/all/"+key,new Gson().fromJson(value,Object.class));
            if(notify)FirebaseBridge.instance().sendTopic("all_users","New Petsfort offer",body);
        }catch(Exception ignored){/* Python logs and keeps the committed database mutation. */}
    }
    private String offerKey(JsonObject product){
        String productId=Jsons.optionalString(product,"product_id",Jsons.optionalString(product,"id",""));
        return "offer_"+productId.replaceAll("[^A-Za-z0-9_-]","_");
    }

    private JsonObject findProduct(Connection connection, String identifier) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT * FROM products WHERE id=? OR product_id=? LIMIT 1")) {
            statement.setString(1, identifier);
            statement.setString(2, identifier);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? productRow(rows) : null;
            }
        }
    }

    private JsonObject productRow(ResultSet rows) throws SQLException {
        JsonObject product = Jsons.row(rows);
        product.add("product_img", Jsons.parseOr(product.get("product_img"), new JsonArray()));
        product.addProperty("offer_buy_qty", nullableInt(product, "offer_buy_qty"));
        product.addProperty("offer_free_qty", nullableInt(product, "offer_free_qty"));
        product.addProperty("offer_active", nullableInt(product, "offer_active") != 0);
        if (!product.has("offer_group_id") || product.get("offer_group_id").isJsonNull()
                || product.get("offer_group_id").getAsString().isEmpty()) {
            product.add("offer_group_id", JsonNull.INSTANCE);
        }
        double mrp = nullableDouble(product, "cost_mrp");
        double discount = nullableDouble(product, "cost_dis");
        product.addProperty("cost_rate", mrp - mrp * discount / 100.0);
        return product;
    }

    private JsonObject payload(JsonObject request) throws ApiFailure {
        JsonElement payload = request.get("body");
        if (payload == null) payload = request.get("payload");
        if (payload == null) return request;
        if (!payload.isJsonObject()) throw new ApiFailure(400, "Invalid request body: Expected a JSON object.");
        return payload.getAsJsonObject();
    }

    private void requireProduct(JsonObject product) throws ApiFailure {
        Jsons.requiredString(product, "product_name");
        Jsons.requiredString(product, "cat_id");
        Jsons.requiredString(product, "cat_sub");
        requiredDouble(product, "cost_mrp");
        requiredDouble(product, "cost_gst");
        requiredDouble(product, "cost_dis");
        requiredInt(product, "stock");
    }

    private double requiredDouble(JsonObject object, String key) throws ApiFailure {
        if (!object.has(key) || object.get(key).isJsonNull()) throw new ApiFailure(422, key + " is required");
        try { return object.get(key).getAsDouble(); }
        catch (Exception error) { throw new ApiFailure(422, key + " must be a number"); }
    }

    private int requiredInt(JsonObject object, String key) throws ApiFailure {
        if (!object.has(key) || object.get(key).isJsonNull()) throw new ApiFailure(422, key + " is required");
        try { return object.get(key).getAsInt(); }
        catch (Exception error) { throw new ApiFailure(422, key + " must be an integer"); }
    }

    private int nullableInt(JsonObject object, String key) {
        return !object.has(key) || object.get(key).isJsonNull() ? 0 : object.get(key).getAsInt();
    }

    private double nullableDouble(JsonObject object, String key) {
        return !object.has(key) || object.get(key).isJsonNull() ? 0 : object.get(key).getAsDouble();
    }

    private void setNullableString(PreparedStatement statement, int index, JsonElement value)
            throws SQLException {
        if (value == null || value.isJsonNull()) statement.setNull(index, Types.VARCHAR);
        else statement.setString(index, value.getAsString());
    }

    // Remaining endpoint implementations are kept below in endpoint order.

    private JsonArray queryRows(String table, JsonObject request, boolean normalizeProducts)
            throws Exception {
        JsonObject query = payload(request);
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(Jsons.identifier(table));
        List<Object> parameters = new ArrayList<>();
        appendFilters(sql, parameters, query);
        appendOrderLimit(sql, parameters, query);
        JsonArray result = new JsonArray();
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            bind(statement, parameters);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(normalizeProducts ? productRow(rows) : orderRow(rows));
            }
        }
        return result;
    }

    private void appendFilters(StringBuilder sql, List<Object> parameters, JsonObject query)
            throws ApiFailure {
        JsonArray filters = query.has("filters") && query.get("filters").isJsonArray()
                ? query.getAsJsonArray("filters") : new JsonArray();
        if (filters.size() == 0) {
            sql.append(" WHERE 1=1");
            return;
        }
        List<String> parts = new ArrayList<>();
        for (JsonElement item : filters) {
            JsonObject filter = item.getAsJsonObject();
            String field = Jsons.identifier(Jsons.requiredString(filter, "field"));
            String operator = Jsons.requiredString(filter, "operator");
            JsonElement value = filter.get("value");
            switch (operator) {
                case "eq": parts.add(field + " = ?"); parameters.add(toJdbc(value)); break;
                case "neq": parts.add(field + " != ?"); parameters.add(toJdbc(value)); break;
                case "gt": parts.add(field + " > ?"); parameters.add(toJdbc(value)); break;
                case "lt": parts.add(field + " < ?"); parameters.add(toJdbc(value)); break;
                case "gte": parts.add(field + " >= ?"); parameters.add(toJdbc(value)); break;
                case "lte": parts.add(field + " <= ?"); parameters.add(toJdbc(value)); break;
                case "contains": parts.add(field + " LIKE ?"); parameters.add("%" + value.getAsString() + "%"); break;
                case "startswith": parts.add(field + " LIKE ?"); parameters.add(value.getAsString() + "%"); break;
                case "endswith": parts.add(field + " LIKE ?"); parameters.add("%" + value.getAsString()); break;
                case "in":
                    if (value == null || !value.isJsonArray() || value.getAsJsonArray().size() == 0) {
                        parts.add("1=0");
                    } else {
                        StringJoiner placeholders = new StringJoiner(",");
                        for (JsonElement element : value.getAsJsonArray()) {
                            placeholders.add("?");
                            parameters.add(toJdbc(element));
                        }
                        parts.add(field + " IN (" + placeholders + ")");
                    }
                    break;
                default: throw new ApiFailure(422, "Invalid operator: " + operator);
            }
        }
        // Deliberately preserves the unusual Python rule: first AND (all remaining ORed).
        sql.append(" WHERE ").append(parts.get(0));
        if (parts.size() > 1) {
            sql.append(" AND (").append(String.join(" OR ", parts.subList(1, parts.size()))).append(')');
        }
    }

    private void appendOrderLimit(StringBuilder sql, List<Object> parameters, JsonObject query)
            throws ApiFailure {
        if (query.has("order_by") && !query.get("order_by").isJsonNull()) {
            String direction = Jsons.optionalString(query, "order_direction", "ASC").toUpperCase(Locale.ROOT);
            if (!direction.equals("ASC") && !direction.equals("DESC")) direction = "ASC";
            sql.append(" ORDER BY ").append(Jsons.identifier(query.get("order_by").getAsString()))
                    .append(' ').append(direction);
        }
        if (query.has("limit") && !query.get("limit").isJsonNull()) {
            sql.append(" LIMIT ?");
            parameters.add(query.get("limit").getAsInt());
            int offset = Jsons.optionalInt(query, "offset", 0);
            if (offset != 0) {
                sql.append(" OFFSET ?");
                parameters.add(offset);
            }
        }
    }

    private Object toJdbc(JsonElement value) {
        if (value == null || value.isJsonNull()) return null;
        JsonPrimitive primitive = value.getAsJsonPrimitive();
        if (primitive.isBoolean()) return primitive.getAsBoolean();
        if (primitive.isNumber()) return primitive.getAsNumber();
        return primitive.getAsString();
    }

    private void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int i = 0; i < parameters.size(); i++) statement.setObject(i + 1, parameters.get(i));
    }

    private JsonObject orderRow(ResultSet rows) throws SQLException {
        JsonObject order = Jsons.row(rows);
        order.add("items", Jsons.parseOr(order.get("items"), new JsonObject()));
        order.add("items_detail", Jsons.parseOr(order.get("items_detail"), new JsonArray()));
        return order;
    }

    private void getBill(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            String id = Jsons.requiredString(q, "order_id");
            try (Connection c = database.connect();
                 PreparedStatement s = c.prepareStatement("SELECT bill FROM bills WHERE order_id=?")) {
                s.setString(1, id);
                try (ResultSet rows = s.executeQuery()) {
                    if (rows.next()) {
                        JsonElement bill = Jsons.parseOr(new JsonPrimitive(rows.getString(1)), new JsonObject());
                        if (bill.isJsonObject()) Jsons.copy(bill.getAsJsonObject(), r);
                        else r.add("data", bill);
                    } else {
                        // The Python implementation returns null rather than a 404.
                        r.add("data", JsonNull.INSTANCE);
                    }
                }
            }
        }, req, res);
    }

    private void queryOrders(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> r.add("data", queryOrdersInternal(q)), req, res);
    }

    private JsonArray queryOrdersInternal(JsonObject request) throws Exception {
        JsonObject query = payload(request);
        StringBuilder sql = new StringBuilder("SELECT orders.*, userdata.name AS user_name " +
                "FROM orders JOIN userdata ON orders.user_id=userdata.uid");
        List<Object> parameters = new ArrayList<>();
        appendFilters(sql, parameters, query);
        appendOrderLimit(sql, parameters, query);
        JsonArray result = new JsonArray();
        try (Connection c = database.connect(); PreparedStatement s = c.prepareStatement(sql.toString())) {
            bind(s, parameters);
            try (ResultSet rows = s.executeQuery()) {
                while (rows.next()) result.add(orderRow(rows));
            }
        }
        return result;
    }

    private void putOrder(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((request, response) -> {
            String id = Jsons.requiredString(request, "order_id");
            JsonObject body = payload(request);
            LinkedHashMap<String, Object> values = new LinkedHashMap<>();
            for (String field : List.of("order_status", "items", "items_detail",
                    "total_rate", "total_gst", "total_discount", "total")) {
                if (!body.has(field) || body.get(field).isJsonNull()) continue;
                JsonElement value = body.get(field);
                if (field.equals("items") || field.equals("items_detail")) values.put(field, Jsons.json(value));
                else if (field.startsWith("total")) values.put(field,
                        Math.round(value.getAsDouble() * 1000.0) / 1000.0);
                else values.put(field, value.getAsString());
            }
            if (values.isEmpty()) throw new ApiFailure(400, "No valid fields provided for update.");
            StringJoiner set = new StringJoiner(",");
            values.keySet().forEach(key -> set.add(key + "=?"));
            try (Connection c = database.connect();
                 PreparedStatement s = c.prepareStatement("UPDATE orders SET " + set + " WHERE order_id=?")) {
                int index = 1;
                for (Object value : values.values()) s.setObject(index++, value);
                s.setString(index, id);
                if (s.executeUpdate() == 0) throw new ApiFailure(404, "Order not found or no changes made.");
                try (PreparedStatement read = c.prepareStatement("SELECT * FROM orders WHERE order_id=?")) {
                    read.setString(1, id);
                    try (ResultSet rows = read.executeQuery()) {
                        if (!rows.next()) throw new ApiFailure(404, "Order not found after update attempt.");
                        JsonObject changed=orderRow(rows);Jsons.copy(changed,response);
                        String status=Jsons.optionalString(changed,"order_status","");
                        if(status.equals("ORDER_PENDING"))status="pending";
                        else if(status.equals("ORDER_IN_PROGRESS"))status="in progress";
                        else if(status.equals("ORDER_DELIVERED"))status="delivered";
                        else if(status.equals("ORDER_CANCELLED"))status="cancelled";
                        try{FirebaseBridge.instance().sendTopic("user_"+changed.get("user_id").getAsString(),
                                "Order Status Updated","Hi, your order is "+status+"!");}catch(Exception ignored){}
                    }
                }
            }
        }, req, res);
    }

    private void deleteOrder(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        simpleDelete(req, res, "orders", "order_id", "order_id", "Order not found.",
                "Order deleted successfully");
    }

    private void getCategories(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            JsonArray categories = selectArray("SELECT id,name,image FROM category", List.of());
            for (int i = 0; i < categories.size(); i++) {
                if ("cc41f1da652f4".equals(categories.get(i).getAsJsonObject().get("id").getAsString())) {
                    JsonElement special = categories.remove(i);
                    JsonArray reordered = new JsonArray();
                    reordered.add(special);
                    categories.forEach(reordered::add);
                    categories = reordered;
                    break;
                }
            }
            r.add("data", categories);
        }, req, res);
    }

    private void postCategory(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            JsonObject body = payload(q);
            upsert("INSERT OR REPLACE INTO category(id,name,image) VALUES(?,?,?)",
                    Jsons.requiredString(body, "id"), Jsons.requiredString(body, "name"),
                    Jsons.optionalString(body, "image", null));
            r.addProperty("message", "Category added successfully");
        }, req, res);
    }

    private void deleteCategory(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        simpleDelete(req, res, "category", "id", "cat_id", "Order not found.",
                "Category deleted successfully");
    }

    private void getSubcategories(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> r.add("data",
                selectArray("SELECT id,parentid,name,image FROM subcategory", List.of())), req, res);
    }

    private void getSubcategoriesV0(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> r.add("data", selectArray(
                "SELECT DISTINCT s.id,s.parentid,s.name,s.image FROM subcategory s " +
                        "JOIN products p ON s.id=p.cat_sub WHERE s.parentid=?",
                List.of(Jsons.requiredString(q, "category_id")))), req, res);
    }

    private void getSubcategoriesByCategory(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            JsonArray rows = selectArray("SELECT DISTINCT s.id,s.parentid,s.name,s.image," +
                    "(SELECT p1.product_img FROM products p1 WHERE p1.cat_sub=s.id LIMIT 1) fallback_img " +
                    "FROM subcategory s JOIN products p ON s.id=p.cat_sub WHERE s.parentid=?",
                    List.of(Jsons.requiredString(q, "category_id")));
            for (JsonElement element : rows) {
                JsonObject row = element.getAsJsonObject();
                String image = Jsons.optionalString(row, "image", "");
                if (image.trim().isEmpty()) {
                    JsonElement fallback = Jsons.parseOr(row.get("fallback_img"), new JsonArray());
                    if (fallback.isJsonArray() && fallback.getAsJsonArray().size() > 0) {
                        row.add("image", fallback.getAsJsonArray().get(0));
                    }
                }
                row.remove("fallback_img");
            }
            r.add("data", rows);
        }, req, res);
    }

    private void postSubcategory(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            JsonObject body = payload(q);
            upsert("INSERT OR REPLACE INTO subcategory(id,parentid,name,image) VALUES(?,?,?,?)",
                    Jsons.requiredString(body, "id"), Jsons.requiredString(body, "parentid"),
                    Jsons.requiredString(body, "name"), Jsons.optionalString(body, "image", null));
            r.addProperty("message", "Category added successfully");
        }, req, res);
    }

    private void deleteSubcategory(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        simpleDelete(req, res, "subcategory", "id", "cat_id", "Order not found.",
                "Category deleted successfully");
    }

    private void getUsers(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> r.add("data", selectArray(
                "SELECT uid,id,name,contact,gstin,email,role,address,credits,creditse,isblocked FROM userdata",
                List.of())), req, res);
    }

    private void getUser(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            String id = Jsons.requiredString(q, "user_id");
            JsonArray users = selectArray("SELECT uid,id,name,contact,gstin,email,role,address,credits," +
                    "creditse,isblocked FROM userdata WHERE id=? OR uid=?", List.of(id, id));
            if (users.size() == 0) throw new ApiFailure(404, "User with ID '" + id + "' not found");
            Jsons.copy(users.get(0).getAsJsonObject(), r);
        }, req, res);
    }

    private void postUser(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            JsonObject body = payload(q);
            // Firebase account creation is intentionally performed by FirebaseBridge once credentials exist.
            String uid = FirebaseBridge.instance().createUser(
                    Jsons.requiredString(body, "email"), Jsons.requiredString(body, "pwd"));
            upsert("INSERT OR REPLACE INTO userdata(id,uid,name,contact,gstin,email,role,address," +
                            "credits,creditse,isblocked) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                    Jsons.requiredString(body, "id"), uid, Jsons.requiredString(body, "name"),
                    Jsons.requiredString(body, "contact"), Jsons.requiredString(body, "gstin"),
                    Jsons.requiredString(body, "email"), Jsons.requiredString(body, "role"),
                    Jsons.requiredString(body, "address"), requiredDouble(body, "credits"),
                    Jsons.requiredString(body, "creditse"), Jsons.optionalInt(body, "isblocked", 0));
            r.addProperty("message", "User added successfully");
            r.addProperty("uid", uid);
        }, req, res);
    }

    private void putUser(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            String id = Jsons.requiredString(q, "user_id");
            JsonObject body = payload(q);
            String password = Jsons.optionalString(body, "pwd", "");
            if (!password.isEmpty()) FirebaseBridge.instance().changePassword(id, password);
            upsert("UPDATE userdata SET name=?,email=?,contact=?,gstin=?,role=?,address=?,credits=?," +
                            "creditse=?,isblocked=? WHERE uid=? OR id=?",
                    Jsons.requiredString(body, "name"), Jsons.requiredString(body, "email"),
                    Jsons.requiredString(body, "contact"), Jsons.requiredString(body, "gstin"),
                    Jsons.requiredString(body, "role"), Jsons.requiredString(body, "address"),
                    requiredDouble(body, "credits"), Jsons.requiredString(body, "creditse"),
                    Jsons.optionalInt(body, "isblocked", 0), id, id);
            r.addProperty("message", "User updated successfully");
            r.addProperty("uid", "0");
        }, req, res);
    }

    private void deleteUser(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            String id = Jsons.requiredString(q, "user_id");
            FirebaseBridge.instance().deleteUser(id);
            int changed = update("DELETE FROM userdata WHERE id=? OR uid=?", id, id);
            if (changed == 0) throw new ApiFailure(404, "Order not found.");
            r.addProperty("message", "User deleted successfully");
        }, req, res);
    }

    private void simpleDelete(JsonObject request, JsonObject response, String table, String column,
                              String requestKey, String notFound, String message) throws RpcException {
        execute((q, r) -> {
            String id = Jsons.requiredString(q, requestKey);
            int changed = update("DELETE FROM " + Jsons.identifier(table) + " WHERE " +
                    Jsons.identifier(column) + "=?", id);
            if (changed == 0) throw new ApiFailure(404, notFound);
            r.addProperty("message", message);
        }, request, response);
    }

    JsonArray selectArray(String sql, List<?> parameters) throws SQLException {
        JsonArray result = new JsonArray();
        try (Connection c = database.connect(); PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < parameters.size(); i++) s.setObject(i + 1, parameters.get(i));
            try (ResultSet rows = s.executeQuery()) {
                while (rows.next()) result.add(Jsons.row(rows));
            }
        }
        return result;
    }

    private int update(String sql, Object... parameters) throws SQLException {
        try (Connection c = database.connect(); PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) s.setObject(i + 1, parameters[i]);
            return s.executeUpdate();
        }
    }

    private void upsert(String sql, Object... parameters) throws SQLException {
        update(sql, parameters);
    }

    private void getOfferGroups(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            JsonArray groups = selectArray("SELECT * FROM offer_groups ORDER BY created_at DESC", List.of());
            normalizeJsonColumn(groups, "product_ids", new JsonArray());
            r.add("data", groups);
        }, req, res);
    }

    private void postOfferGroup(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> createOrUpdateOfferGroup(q, r, false), req, res);
    }

    private void putOfferGroup(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> createOrUpdateOfferGroup(q, r, true), req, res);
    }

    private void createOrUpdateOfferGroup(JsonObject request, JsonObject response, boolean updating)
            throws Exception {
        JsonObject body = payload(request);
        String id = updating ? Jsons.requiredString(request, "group_id") : UUID.randomUUID().toString();
        String name = Jsons.requiredString(body, "name");
        String description = Jsons.optionalString(body, "description", "");
        int buy = requiredInt(body, "buy_qty");
        int free = requiredInt(body, "free_qty");
        if (buy < 1 || free < 1) throw new ApiFailure(422, "buy_qty and free_qty must be at least 1");
        JsonArray ids = body.has("product_ids") && body.get("product_ids").isJsonArray()
                ? body.getAsJsonArray("product_ids") : new JsonArray();
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        ids.forEach(value -> unique.add(value.getAsString()));
        if (unique.isEmpty()) throw new ApiFailure(422, "product_ids must not be empty");
        try (Connection c = database.connect()) {
            c.setAutoCommit(false);
            try {
                JsonObject current = null;
                if (updating) {
                    JsonArray found = selectArrayOn(c, "SELECT * FROM offer_groups WHERE id=?", List.of(id));
                    if (found.size() == 0) throw new ApiFailure(404, "Offer group not found");
                    current = found.get(0).getAsJsonObject();
                    if ("ACTIVE".equals(current.get("status").getAsString())) {
                        throw new ApiFailure(409, "Cancel the active offer before editing its group");
                    }
                }
                validateOfferProducts(c, unique, updating ? id : null);
                String now = Instant.now().toString();
                if (updating) {
                    executeOn(c, "UPDATE offer_groups SET name=?,description=?,buy_qty=?,free_qty=?," +
                                    "product_ids=?,updated_at=? WHERE id=?",
                            name, description, buy, free, Jsons.json(toArray(unique)), now, id);
                } else {
                    executeOn(c, "INSERT INTO offer_groups(id,name,description,buy_qty,free_qty,product_ids," +
                                    "status,created_at,updated_at,canceled_at) VALUES(?,?,?,?,?,?,'DRAFT',?,?,NULL)",
                            id, name, description, buy, free, Jsons.json(toArray(unique)), now, now);
                }
                c.commit();
                JsonObject group = selectArrayOn(c, "SELECT * FROM offer_groups WHERE id=?", List.of(id))
                        .get(0).getAsJsonObject();
                group.add("product_ids", Jsons.parseOr(group.get("product_ids"), new JsonArray()));
                Jsons.copy(group, response);
            } catch (Exception error) {
                c.rollback();
                throw error;
            }
        }
    }

    private void validateOfferProducts(Connection c, Set<String> ids, String ownGroup) throws Exception {
        JsonArray missing = new JsonArray();
        JsonArray conflicts = new JsonArray();
        for (String id : ids) {
            JsonArray rows = selectArrayOn(c,
                    "SELECT product_id,offer_group_id FROM products WHERE product_id=?", List.of(id));
            if (rows.size() == 0) missing.add(id);
            else {
                JsonElement group = rows.get(0).getAsJsonObject().get("offer_group_id");
                if (group != null && !group.isJsonNull() && !group.getAsString().isEmpty()
                        && !group.getAsString().equals(ownGroup)) conflicts.add(id);
            }
        }
        if (missing.size() > 0) {
            JsonObject detail = new JsonObject();
            detail.addProperty("message", "Products not found");
            detail.add("product_ids", missing);
            throw new ApiFailure(404, detail);
        }
        if (conflicts.size() > 0) {
            JsonObject detail = new JsonObject();
            detail.addProperty("message", "Products already belong to another offer group");
            detail.add("product_ids", conflicts);
            throw new ApiFailure(409, detail);
        }
    }

    private void applyOfferGroup(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> setOfferGroupStatus(q, r, true), req, res);
    }

    private void cancelOfferGroup(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> setOfferGroupStatus(q, r, false), req, res);
    }

    private void setOfferGroupStatus(JsonObject request, JsonObject response, boolean active)
            throws Exception {
        String id = Jsons.requiredString(request, "group_id");
        try (Connection c = database.connect()) {
            c.setAutoCommit(false);
            try {
                JsonArray rows = selectArrayOn(c, "SELECT * FROM offer_groups WHERE id=?", List.of(id));
                if (rows.size() == 0) throw new ApiFailure(404, "Offer group not found");
                JsonObject group = rows.get(0).getAsJsonObject();
                JsonArray productIds = Jsons.parseOr(group.get("product_ids"), new JsonArray()).getAsJsonArray();
                if (active) {
                    LinkedHashSet<String> ids = new LinkedHashSet<>();
                    productIds.forEach(v -> ids.add(v.getAsString()));
                    validateOfferProducts(c, ids, id);
                    for (String productId : ids) executeOn(c,
                            "UPDATE products SET offer_buy_qty=?,offer_free_qty=?,offer_active=1," +
                                    "offer_group_id=?,updated_at=? WHERE product_id=?",
                            group.get("buy_qty").getAsInt(), group.get("free_qty").getAsInt(),
                            id, LocalDateTime.now().toString(), productId);
                    executeOn(c, "UPDATE offer_groups SET status='ACTIVE',updated_at=?,canceled_at=NULL WHERE id=?",
                            Instant.now().toString(), id);
                } else {
                    executeOn(c, "UPDATE products SET offer_buy_qty=0,offer_free_qty=0,offer_active=0," +
                            "offer_group_id=NULL,updated_at=? WHERE offer_group_id=?",
                            LocalDateTime.now().toString(), id);
                    String now = Instant.now().toString();
                    executeOn(c, "UPDATE offer_groups SET status='CANCELED',updated_at=?,canceled_at=? WHERE id=?",
                            now, now, id);
                }
                c.commit();
                JsonObject changed = selectArrayOn(c, "SELECT * FROM offer_groups WHERE id=?", List.of(id))
                        .get(0).getAsJsonObject();
                changed.add("product_ids", Jsons.parseOr(changed.get("product_ids"), new JsonArray()));
                Jsons.copy(changed, response);
                publishOfferGroup(changed,active);
            } catch (Exception error) {
                c.rollback();
                throw error;
            }
        }
    }

    private void deleteOfferGroup(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            String id = Jsons.requiredString(q, "group_id");
            JsonArray rows = selectArray("SELECT status FROM offer_groups WHERE id=?", List.of(id));
            if (rows.size() == 0) throw new ApiFailure(404, "Offer group not found");
            String status = rows.get(0).getAsJsonObject().get("status").getAsString();
            if (!status.equals("DRAFT") && !status.equals("CANCELED")) {
                throw new ApiFailure(409, "Only draft or canceled groups can be deleted");
            }
            update("DELETE FROM offer_groups WHERE id=?", id);
            r.addProperty("message", "Offer group deleted");
        }, req, res);
    }

    private JsonArray toArray(Collection<String> values) {
        JsonArray result = new JsonArray();
        values.forEach(result::add);
        return result;
    }

    private void publishOfferGroup(JsonObject group,boolean notify){
        try{
            String id=group.get("id").getAsString(),key="offer_group_"+id;
            if(!"ACTIVE".equals(Jsons.optionalString(group,"status",""))){
                FirebaseBridge.instance().deleteValue("datas/announcement/all/"+key);return;}
            JsonArray ids=group.getAsJsonArray("product_ids");JsonObject first=null;
            if(ids!=null&&ids.size()>0)try(Connection c=database.connect()){first=findProduct(c,ids.get(0).getAsString());}
            JsonObject value=new JsonObject();String title=group.get("name").getAsString()+" · Buy "+
                    group.get("buy_qty").getAsInt()+", get "+group.get("free_qty").getAsInt()+" FREE";
            String body=Jsons.optionalString(group,"description","");
            if(body.isEmpty())body="Available on "+(ids==null?0:ids.size())+" selected products.";
            value.addProperty("type","offer_group");value.addProperty("offer_group_id",id);
            value.addProperty("title",title);value.addProperty("subtitle",body);value.add("product_ids",ids);
            String image="";if(first!=null&&first.get("product_img").isJsonArray()&&first.getAsJsonArray("product_img").size()>0)
                image=first.getAsJsonArray("product_img").get(0).getAsString();value.addProperty("img",image);
            FirebaseBridge.instance().setValue("datas/announcement/all/"+key,new Gson().fromJson(value,Object.class));
            if(notify)FirebaseBridge.instance().sendTopic("all_users","New Petsfort offer",body);
        }catch(Exception ignored){}
    }

    private void normalizeJsonColumn(JsonArray rows, String column, JsonElement fallback) {
        rows.forEach(row -> {
            JsonObject object = row.getAsJsonObject();
            object.add(column, Jsons.parseOr(object.get(column), fallback.deepCopy()));
        });
    }

    private JsonArray selectArrayOn(Connection c, String sql, List<?> parameters) throws SQLException {
        JsonArray result = new JsonArray();
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < parameters.size(); i++) s.setObject(i + 1, parameters.get(i));
            try (ResultSet rows = s.executeQuery()) {
                while (rows.next()) result.add(Jsons.row(rows));
            }
        }
        return result;
    }

    private int executeOn(Connection c, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) s.setObject(i + 1, parameters[i]);
            return s.executeUpdate();
        }
    }

    private void addSchemaColumns(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            JsonObject body = payload(q);
            JsonArray columns = body.has("columns") ? body.getAsJsonArray("columns") : new JsonArray();
            JsonArray added = new JsonArray();
            JsonArray existing = new JsonArray();
            Set<String> current = tableColumns("products");
            for (JsonElement element : columns) {
                JsonObject column = element.getAsJsonObject();
                String name = Jsons.requiredString(column, "column_name");
                if (!name.matches("^[A-Za-z_][A-Za-z0-9_]*$")) {
                    throw new ApiFailure(400, "Invalid column name: " + name);
                }
                if (current.contains(name)) { existing.add(name); continue; }
                String type = Jsons.requiredString(column, "column_type").toUpperCase(Locale.ROOT);
                if (!Set.of("TEXT", "INTEGER", "REAL", "BLOB", "NUMERIC").contains(type)) {
                    throw new ApiFailure(400, "Invalid column type: " + type);
                }
                StringBuilder alter = new StringBuilder("ALTER TABLE products ADD COLUMN ")
                        .append(Jsons.identifier(name)).append(' ').append(type);
                if (column.has("default_value") && !column.get("default_value").isJsonNull()) {
                    alter.append(" DEFAULT ").append(sqlLiteral(column.get("default_value")));
                }
                update(alter.toString());
                added.add(name);
                current.add(name);
            }
            r.addProperty("message", "Columns added successfully (or skipped if existing)");
            r.add("added_columns", added);
        }, req, res);
    }

    private String sqlLiteral(JsonElement value) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) return value.getAsString();
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) return value.getAsBoolean() ? "1" : "0";
        return "'" + value.getAsString().replace("'", "''") + "'";
    }

    private void removeSchemaColumns(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            JsonArray requested = payload(q).getAsJsonArray("columns");
            Set<String> current = tableColumns("products");
            List<String> remove = new ArrayList<>();
            for (JsonElement value : requested) remove.add(value.getAsString());
            List<String> absent = new ArrayList<>();
            remove.stream().filter(name -> !current.contains(name)).forEach(absent::add);
            if (!absent.isEmpty()) throw new ApiFailure(400, "Columns don't exist: " + String.join(", ", absent));
            if (remove.contains("id")) throw new ApiFailure(400, "Cannot remove the primary key 'id' column");
            if (remove.size() >= current.size()) throw new ApiFailure(400, "Cannot remove all columns.");
            JsonArray info=selectArray("PRAGMA table_info(products)",List.of());
            List<String> kept=new ArrayList<>(),definitions=new ArrayList<>();
            for(JsonElement value:info){JsonObject column=value.getAsJsonObject();String name=column.get("name").getAsString();
                if(remove.contains(name))continue;kept.add(name);StringBuilder definition=new StringBuilder(name)
                        .append(' ').append(column.get("type").getAsString());
                if(column.get("pk").getAsInt()!=0)definition.append(" PRIMARY KEY");
                if(column.get("notnull").getAsInt()!=0)definition.append(" NOT NULL");
                JsonElement defaultValue=column.get("dflt_value");
                if(defaultValue!=null&&!defaultValue.isJsonNull()){String raw=defaultValue.getAsString();
                    if(raw.startsWith("'")&&raw.endsWith("'"))definition.append(" DEFAULT ").append(raw);
                    else definition.append(" DEFAULT '").append(raw.replace("'","''")).append('\'');}
                definitions.add(definition.toString());
            }
            try (Connection c = database.connect(); Statement s = c.createStatement()) {
                c.setAutoCommit(false);
                try{
                    s.execute("CREATE TABLE products_new ("+String.join(", ",definitions)+")");
                    s.execute("INSERT INTO products_new ("+String.join(", ",kept)+") SELECT "+
                            String.join(", ",kept)+" FROM products");
                    s.execute("DROP TABLE products");s.execute("ALTER TABLE products_new RENAME TO products");c.commit();
                }catch(Exception error){c.rollback();throw error;}
            }
            r.addProperty("message", "Columns removed successfully by recreating the table");
            r.add("removed_columns", toArray(remove));
        }, req, res);
    }

    private void getSchema(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            JsonArray products = schemaColumns("products");
            if (products.size() == 0) throw new ApiFailure(404, "Products table schema not found or table is empty.");
            JsonObject productTable=new JsonObject();productTable.addProperty("table_name","products");
            productTable.add("columns",products);r.add("products_table",productTable);
            JsonObject orderTable=new JsonObject();orderTable.addProperty("table_name","orders");
            orderTable.add("columns",schemaColumns("orders"));r.add("orders_table",orderTable);
        }, req, res);
    }

    private JsonArray schemaColumns(String table)throws Exception{
        JsonArray raw=selectArray("PRAGMA table_info("+Jsons.identifier(table)+")",List.of()),result=new JsonArray();
        for(JsonElement value:raw){JsonObject source=value.getAsJsonObject(),column=new JsonObject();
            column.add("name",source.get("name"));column.add("type",source.get("type"));
            column.addProperty("not_null",source.get("notnull").getAsInt()!=0);
            column.add("default_value",source.get("dflt_value"));
            column.addProperty("primary_key",source.get("pk").getAsInt()!=0);result.add(column);}
        return result;
    }

    private Set<String> tableColumns(String table) throws Exception {
        JsonArray rows = selectArray("PRAGMA table_info(" + Jsons.identifier(table) + ")", List.of());
        LinkedHashSet<String> result = new LinkedHashSet<>();
        rows.forEach(row -> result.add(row.getAsJsonObject().get("name").getAsString()));
        return result;
    }

    private void getBulkProducts(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            JsonObject body = payload(q);
            JsonArray products = new JsonArray(),warnings=new JsonArray();double mrpTotal=0,rateTotal=0,gstTotal=0,discountTotal=0,total=0;
            try (Connection c = database.connect()) {
                for (Map.Entry<String,JsonElement> entry:body.entrySet()) {
                    String id=entry.getKey();int count=0;
                    if(entry.getValue().isJsonObject()&&entry.getValue().getAsJsonObject().has("count")){
                        JsonElement countValue=entry.getValue().getAsJsonObject().get("count");
                        if(countValue.isJsonPrimitive()&&countValue.getAsJsonPrimitive().isNumber())count=Math.max(0,countValue.getAsInt());}
                    JsonObject product=findProduct(c,id);
                    if(product==null){warnings.add("Product ID '"+id+"' not found.");continue;}
                    int buy=nullableInt(product,"offer_buy_qty"),freeQty=nullableInt(product,"offer_free_qty");
                    int free=product.get("offer_active").getAsBoolean()&&buy>0&&freeQty>0?(count/buy)*freeQty:0;
                    product.addProperty("requested_count",count);product.addProperty("paid_count",count);
                    product.addProperty("free_count",free);product.addProperty("fulfilled_count",count+free);products.add(product);
                    double mrp=nullableDouble(product,"cost_mrp"),rate=nullableDouble(product,"cost_rate"),
                            gst=rate*nullableDouble(product,"cost_gst")/100,discount=mrp*nullableDouble(product,"cost_dis")/100;
                    mrpTotal+=mrp*count;rateTotal+=rate*count;gstTotal+=gst*count;discountTotal+=discount*count;total+=(rate+gst)*count;
                }
            }
            List<JsonElement> sorted=new ArrayList<>();products.forEach(sorted::add);
            sorted.sort((a,b)->pickKey(a.getAsJsonObject()).compareTo(pickKey(b.getAsJsonObject())));
            while(products.size()>0)products.remove(0);sorted.forEach(products::add);
            JsonObject cost=new JsonObject();cost.addProperty("total_mrp",Jsons.round2(mrpTotal));
            cost.addProperty("total_rate",Jsons.round2(rateTotal));cost.addProperty("total_gst",Jsons.round2(gstTotal));
            cost.addProperty("total_discount",Jsons.round2(discountTotal));cost.addProperty("total",Jsons.round2(total));
            r.add("product_details",products);r.add("cost",cost);if(warnings.size()>0)r.add("warnings",warnings);
        }, req, res);
    }

    // Implemented in the accounting/checkout section.
    private void checkout(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->{
            synchronized(checkoutLock){checkoutInternal(q,r);}
        },req,res);
    }

    private void checkoutInternal(JsonObject request,JsonObject response)throws Exception{
        String userId=Jsons.requiredString(request,"user_id");
        JsonObject data=payload(request).deepCopy();
        JsonElement otherElement=data.remove("otherData");
        if(otherElement==null||!otherElement.isJsonObject())throw new ApiFailure(422,"otherData is required");
        JsonObject other=otherElement.getAsJsonObject();
        JsonObject orderItems=new JsonObject();JsonArray details=new JsonArray();
        List<String> notFound=new ArrayList<>();Map<String,Integer> newStocks=new LinkedHashMap<>();
        double totalRate=0,totalGst=0,totalDiscount=0,total=0;
        try(Connection c=database.connect()){
            c.setAutoCommit(false);
            try{
                for(Map.Entry<String,JsonElement> entry:data.entrySet()){
                    String pid=entry.getKey();
                    if(!entry.getValue().isJsonObject()||!entry.getValue().getAsJsonObject().has("count"))
                        throw new ApiFailure(400,"Invalid item data for product ID '"+pid+"'. Expected {'count': number}.");
                    JsonElement countValue=entry.getValue().getAsJsonObject().get("count");
                    if(!countValue.isJsonPrimitive()||!countValue.getAsJsonPrimitive().isNumber())continue;
                    int paid=countValue.getAsInt();if(paid<=0)continue;
                    JsonObject product=findRawProduct(c,pid);if(product==null){notFound.add(pid);continue;}
                    int buy=nullableInt(product,"offer_buy_qty"),freeQty=nullableInt(product,"offer_free_qty");
                    boolean active=product.get("offer_active").getAsBoolean()&&buy>0&&freeQty>0;
                    int free=active?(paid/buy)*freeQty:0,fulfilled=paid+free;
                    int stock=nullableInt(product,"stock");
                    if(stock<fulfilled){
                        response.addProperty("message","OutOfStock");response.addProperty("product_available_stock",stock);
                        response.addProperty("product_id",pid);response.addProperty("product_name",
                                Jsons.optionalString(product,"product_name","product"));
                        response.addProperty("requested_paid_count",paid);response.addProperty("requested_free_count",free);
                        c.rollback();return;
                    }
                    newStocks.put(pid,stock-fulfilled);
                    JsonObject item=new JsonObject();
                    for(String field:List.of("id","product_id","product_name","product_desc","product_hsn",
                            "product_cid","product_img","cat_id","cat_sub","created_at","updated_at","cost_mrp",
                            "cost_gst","stock","cost_dis")){
                        JsonElement fieldValue=product.get(field);
                        item.add(field,fieldValue==null?JsonNull.INSTANCE:fieldValue.deepCopy());
                    }
                    double itemMrp=nullableDouble(item,"cost_mrp"),itemDiscount=nullableDouble(item,"cost_dis");
                    item.addProperty("cost_rate",itemMrp-(itemMrp*itemDiscount/100));
                    item.addProperty("count",fulfilled);item.addProperty("paid_count",paid);item.addProperty("free_count",free);
                    item.addProperty("offer_buy_qty",buy);item.addProperty("offer_free_qty",freeQty);
                    details.add(item);
                    JsonObject count=new JsonObject();count.addProperty("count",fulfilled);
                    count.addProperty("paid_count",paid);count.addProperty("free_count",free);orderItems.add(pid,count);
                    double mrp=nullableDouble(product,"cost_mrp"),discount=nullableDouble(product,"cost_dis");
                    double rate=mrp-(mrp*discount/100),gst=rate*nullableDouble(product,"cost_gst")/100;
                    totalRate+=rate*paid;totalGst+=gst*paid;totalDiscount+=(mrp*discount/100)*paid;total+=(rate+gst)*paid;
                }
                if(!notFound.isEmpty())throw new ApiFailure(404,"Products not found: "+String.join(", ",notFound));
                if(orderItems.size()==0)throw new ApiFailure(400,"No valid items found in the request to create an order.");
                List<JsonElement> sortedDetails=new ArrayList<>();details.forEach(sortedDetails::add);
                sortedDetails.sort((left,right)->pickKey(left.getAsJsonObject()).compareTo(pickKey(right.getAsJsonObject())));
                while(details.size()>0)details.remove(0);sortedDetails.forEach(details::add);
                JsonArray users=selectArrayOn(c,"SELECT * FROM userdata WHERE id=? OR uid=?",List.of(userId,userId));
                if(users.size()==0)throw new ApiFailure(400,"User Not found.");
                JsonObject user=users.get(0).getAsJsonObject();
                if(nullableInt(user,"isblocked")!=0)throw new ApiFailure(400,"User is Blocked");
                for(Map.Entry<String,Integer> stock:newStocks.entrySet())executeOn(c,
                        "UPDATE products SET stock=? WHERE id=? OR product_id=?",stock.getValue(),stock.getKey(),stock.getKey());
                totalRate=round3(totalRate);totalGst=round3(totalGst);totalDiscount=round3(totalDiscount);total=round3(total);
                executeOn(c,"UPDATE userdata SET credits=credits-? WHERE id=? OR uid=?",total,userId,userId);
                String orderId=shortId(),now=Instant.now().toString();
                executeOn(c,"INSERT INTO orders(order_id,user_id,items,items_detail,order_status,total_rate,"+
                                "total_gst,total_discount,total,created_at,address,notes) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                        orderId,userId,Jsons.json(orderItems),Jsons.json(details),"ORDER_PENDING",totalRate,totalGst,
                        totalDiscount,total,now,Jsons.requiredString(other,"address"),Jsons.optionalString(other,"notes",""));
                JsonObject invoice=InvoiceBuilder.create(orderId,now,userId,user,details,other,totalRate,totalGst,totalDiscount,total);
                executeOn(c,"INSERT INTO bills(order_id,bill) VALUES(?,?)",orderId,Jsons.json(invoice));
                c.commit();
                response.addProperty("message","Order created successfully");response.addProperty("order_id",orderId);
                response.addProperty("user_id",userId);response.addProperty("order_status","ORDER_PENDING");
                response.addProperty("total",total);
                try{
                    FirebaseBridge.instance().sendTopic("user_"+userId,"Order Made",
                            "Thank you for making order, your order is in pending, we will update you!");
                    FirebaseBridge.instance().sendTopic("order_checkout","New Order",
                            Jsons.optionalString(user,"name","Not Found")+" is made a new order of Rs."+total);
                }catch(Exception ignored){}
            }catch(Exception error){c.rollback();throw error;}
        }
    }
    private double round3(double value){return Math.round(value*1000d)/1000d;}
    private String shortId(){
        LocalDate now=LocalDate.now();String letters="";
        Random random=new Random();for(int i=0;i<2;i++)letters+=(char)('A'+random.nextInt(26));
        return now.format(DateTimeFormatter.ofPattern("yyMMdd"))+letters+String.format("%02d",10+random.nextInt(90));
    }
    private JsonObject findRawProduct(Connection connection,String identifier)throws SQLException{
        try(PreparedStatement statement=connection.prepareStatement(
                "SELECT * FROM products WHERE id=? OR product_id=? LIMIT 1")){
            statement.setString(1,identifier);statement.setString(2,identifier);
            try(ResultSet rows=statement.executeQuery()){return rows.next()?Jsons.row(rows):null;}
        }
    }
    private String pickKey(JsonObject product){
        String cat=Jsons.optionalString(product,"cat_id","").toLowerCase(Locale.ROOT);
        List<String> subs=new ArrayList<>(Arrays.asList(Jsons.optionalString(product,"cat_sub","").toLowerCase(Locale.ROOT).split(",")));
        subs.replaceAll(String::trim);subs.removeIf(String::isEmpty);Collections.sort(subs);
        return cat+"\u0000"+String.join(",",subs)+"\u0000"+naturalKey(Jsons.optionalString(product,"product_name",""))+
                "\u0000"+Jsons.optionalString(product,"product_id",Jsons.optionalString(product,"id","")).toLowerCase(Locale.ROOT);
    }
    private String naturalKey(String value){
        StringBuilder key=new StringBuilder();java.util.regex.Matcher matcher=java.util.regex.Pattern.compile("(\\d+)|(\\D+)").matcher(value.toLowerCase(Locale.ROOT));
        while(matcher.find()){if(matcher.group(1)!=null)key.append('\u0001').append(String.format("%020d",Long.parseLong(matcher.group(1))));
            else key.append('\u0000').append(matcher.group(2));}return key.toString();
    }
    private void getTables(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {JsonArray rows=selectArray(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'",List.of());
            JsonArray names=new JsonArray();rows.forEach(v->names.add(v.getAsJsonObject().get("name")));r.add("data",names);
        }, req, res);
    }
    private void getTableInfo(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            String table = checkedTable(q);
            JsonArray columns = selectArray("PRAGMA table_info(" + Jsons.identifier(table) + ")", List.of());
            r.add("columns", columns);
            String pk = null;
            int count = 0;
            for (JsonElement value : columns) if (value.getAsJsonObject().get("pk").getAsInt() != 0) {
                pk = value.getAsJsonObject().get("name").getAsString(); count++;
            }
            if (count == 1) r.addProperty("pk_column", pk); else r.add("pk_column", JsonNull.INSTANCE);
        }, req, res);
    }
    private void getTableData(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> r.add("data", selectArray(
                "SELECT * FROM " + Jsons.identifier(checkedTable(q)), List.of())), req, res);
    }
    private void postTableRow(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            String table = checkedTable(q);
            JsonObject body = payload(q);
            if (body.entrySet().isEmpty()) throw new ApiFailure(400, "No columns provided.");
            StringJoiner columns = new StringJoiner(",");
            StringJoiner marks = new StringJoiner(",");
            List<Object> values = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
                columns.add(Jsons.identifier(entry.getKey())); marks.add("?");
                values.add(entry.getValue().isJsonNull() || (entry.getValue().isJsonPrimitive()
                        && entry.getValue().getAsJsonPrimitive().isString()
                        && entry.getValue().getAsString().isEmpty()) ? null : toJdbc(entry.getValue()));
            }
            try (Connection c = database.connect(); PreparedStatement s = c.prepareStatement(
                    "INSERT INTO " + Jsons.identifier(table) + "(" + columns + ") VALUES(" + marks + ")",
                    Statement.RETURN_GENERATED_KEYS)) {
                bind(s, values); s.executeUpdate();
                long id = 0;
                try (ResultSet keys = s.getGeneratedKeys()) { if (keys.next()) id = keys.getLong(1); }
                r.addProperty("message", "Row added successfully"); r.addProperty("row_id", id);
            } catch (SQLException error) {
                if (error.getMessage().toLowerCase(Locale.ROOT).contains("constraint")) {
                    throw new ApiFailure(400, "Failed to add row (Integrity Error): " + error.getMessage()
                            + ". Check unique constraints or non-null fields.");
                }
                throw error;
            }
        }, req, res);
    }
    private void putTableRow(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> mutateTableRow(q, r, false), req, res);
    }
    private void deleteTableRow(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> mutateTableRow(q, r, true), req, res);
    }

    private String checkedTable(JsonObject request) throws Exception {
        String table = Jsons.requiredString(request, "table_name");
        JsonArray found = selectArray("SELECT name FROM sqlite_master WHERE type='table' AND name=?", List.of(table));
        if (found.size() == 0) throw new ApiFailure(404, "Table '" + table + "' not found.");
        return table;
    }

    private String primaryKey(String table) throws Exception {
        JsonArray info = selectArray("PRAGMA table_info(" + Jsons.identifier(table) + ")", List.of());
        String pk = null;
        int count = 0;
        for (JsonElement value : info) if (value.getAsJsonObject().get("pk").getAsInt() != 0) {
            pk = value.getAsJsonObject().get("name").getAsString(); count++;
        }
        return count == 1 ? pk : null;
    }

    private void mutateTableRow(JsonObject request, JsonObject response, boolean deleting) throws Exception {
        String table = checkedTable(request);
        String pk = primaryKey(table);
        if (pk == null) throw new ApiFailure(400, "Cannot " + (deleting ? "delete" : "update")
                + ": Table '" + table + "' does not have a single primary key defined.");
        String value = Jsons.requiredString(request, "pk_value");
        int changed;
        if (deleting) {
            changed = update("DELETE FROM " + Jsons.identifier(table) + " WHERE " +
                    Jsons.identifier(pk) + "=?", value);
        } else {
            JsonObject body = payload(request);
            StringJoiner set = new StringJoiner(",");
            List<Object> values = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : body.entrySet()) {
                if (entry.getKey().equals(pk)) continue;
                set.add(Jsons.identifier(entry.getKey()) + "=?");
                values.add(entry.getValue().isJsonNull() ? null : toJdbc(entry.getValue()));
            }
            if (values.isEmpty()) throw new ApiFailure(400, "No columns provided to update.");
            values.add(value);
            try (Connection c = database.connect(); PreparedStatement s = c.prepareStatement(
                    "UPDATE " + Jsons.identifier(table) + " SET " + set + " WHERE " +
                            Jsons.identifier(pk) + "=?")) {
                bind(s, values); changed = s.executeUpdate();
            }
        }
        if (changed == 0) throw new ApiFailure(404, "Row with " + pk + "='" + value
                + "' not found in table '" + table + "'.");
        response.addProperty("message", "Row with " + pk + "='" + value + "' "
                + (deleting ? "deleted" : "updated") + " successfully.");
    }

    private void analyticsSummary(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q, r) -> {
            r.addProperty("total_earnings_overall", Jsons.round2(scalarDouble(
                    "SELECT COALESCE(SUM(total),0) FROM orders WHERE order_status='ORDER_DELIVERED'")));
            r.addProperty("total_orders_overall", scalarLong("SELECT COUNT(order_id) FROM orders"));
            r.addProperty("total_orders_this_month", scalarLong("SELECT COUNT(order_id) FROM orders " +
                    "WHERE strftime('%Y-%m',created_at)=strftime('%Y-%m','now')"));
            r.addProperty("total_orders_last_month", scalarLong("SELECT COUNT(order_id) FROM orders " +
                    "WHERE strftime('%Y-%m',created_at)=strftime('%Y-%m','now','-1 month')"));
            r.addProperty("total_earnings_this_month", Jsons.round2(scalarDouble("SELECT COALESCE(SUM(total),0) " +
                    "FROM orders WHERE order_status='ORDER_DELIVERED' AND " +
                    "strftime('%Y-%m',created_at)=strftime('%Y-%m','now')")));
            r.addProperty("total_earnings_last_month", Jsons.round2(scalarDouble("SELECT COALESCE(SUM(total),0) " +
                    "FROM orders WHERE order_status='ORDER_DELIVERED' AND " +
                    "strftime('%Y-%m',created_at)=strftime('%Y-%m','now','-1 month')")));
            r.addProperty("low_stock_products_count", scalarLong("SELECT COUNT(id) FROM products WHERE stock<5"));
            r.addProperty("low_stock_threshold", 5);
            JsonArray low = selectArray("SELECT id,product_name AS name,stock FROM products WHERE stock<5", List.of());
            r.add("low_stock_products_list", low);
            JsonObject distribution = new JsonObject();
            JsonArray statuses = selectArray("SELECT order_status,COUNT(order_id) value FROM orders GROUP BY order_status", List.of());
            statuses.forEach(v -> distribution.addProperty(v.getAsJsonObject().get("order_status").getAsString(),
                    v.getAsJsonObject().get("value").getAsLong()));
            r.add("order_status_distribution", distribution);
            JsonArray products = selectArray("SELECT p.product_name name," +
                    "SUM(CAST(json_extract(item.value,'$.count') AS INTEGER)*" +
                    "CAST(json_extract(item.value,'$.cost_mrp') AS REAL)) revenue FROM orders o," +
                    "json_each(o.items_detail) item JOIN products p ON json_extract(item.value,'$.product_id')=p.product_id " +
                    "WHERE o.order_status='ORDER_DELIVERED' GROUP BY p.product_name ORDER BY revenue DESC LIMIT 10", List.of());
            if (products.size() == 0) { JsonObject note = new JsonObject(); note.addProperty("note", "No product sales data found."); products.add(note); }
            r.add("top_selling_products_revenue", products);
            JsonArray users = selectArray("SELECT u.name username,SUM(o.total) total_value FROM orders o " +
                    "JOIN userdata u ON o.user_id=u.uid WHERE o.order_status='ORDER_DELIVERED' " +
                    "GROUP BY u.name ORDER BY total_value DESC LIMIT 5", List.of());
            if (users.size() == 0) { JsonObject note = new JsonObject(); note.addProperty("note", "No user order data found."); users.add(note); }
            r.add("top_order_taking_users", users);
            JsonObject trend = new JsonObject(); JsonArray labels = new JsonArray(); JsonArray data = new JsonArray();
            Map<String, Long> counts = new HashMap<>();
            selectArray("SELECT strftime('%Y-%m',created_at) month,COUNT(order_id) count FROM orders " +
                    "WHERE created_at>=DATE('now','-12 months') GROUP BY month ORDER BY month", List.of())
                    .forEach(v -> counts.put(v.getAsJsonObject().get("month").getAsString(),
                            v.getAsJsonObject().get("count").getAsLong()));
            LocalDate first = LocalDate.now().withDayOfMonth(1).minusMonths(12);
            for (int i = 0; i < 12; i++) {
                String label = first.plusMonths(i).format(DateTimeFormatter.ofPattern("yyyy-MM"));
                labels.add(label); data.add(counts.getOrDefault(label, 0L));
            }
            trend.add("labels", labels); trend.add("data", data);
            if (counts.isEmpty()) trend.addProperty("note", "No order data for the last 12 months.");
            r.add("orders_trend_12_months", trend);
        }, req, res);
    }

    long scalarLong(String sql, Object... parameters) throws SQLException {
        try (Connection c = database.connect(); PreparedStatement s = c.prepareStatement(sql)) {
            for (int i=0;i<parameters.length;i++) s.setObject(i+1, parameters[i]);
            try(ResultSet row=s.executeQuery()){ return row.next() ? row.getLong(1) : 0; }
        }
    }
    double scalarDouble(String sql, Object... parameters) throws SQLException {
        try (Connection c = database.connect(); PreparedStatement s = c.prepareStatement(sql)) {
            for (int i=0;i<parameters.length;i++) s.setObject(i+1, parameters[i]);
            try(ResultSet row=s.executeQuery()){ return row.next() ? row.getDouble(1) : 0; }
        }
    }
    private void gstDashboard(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->AccountingReports.dashboard(this,q,r),req,res);
    }
    private void gstSalesRegister(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->r.add("data",AccountingReports.salesRegister(this,q)),req,res);
    }
    private void postCreditNote(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->createNote(q,r,true),req,res);
    }
    private void getCreditNotes(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->r.add("data",listNotes(q,true)),req,res);
    }
    private void deleteCreditNote(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->deleteNote(q,r,true),req,res);
    }
    private void postDebitNote(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->createNote(q,r,false),req,res);
    }
    private void getDebitNotes(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->r.add("data",listNotes(q,false)),req,res);
    }
    private void deleteDebitNote(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->deleteNote(q,r,false),req,res);
    }

    private void createNote(JsonObject request, JsonObject response, boolean credit) throws Exception {
        JsonObject note=payload(request); JsonArray items=note.getAsJsonArray("items");
        if(items==null) throw new ApiFailure(422,"items is required");
        LocalDateTime now=LocalDateTime.now(ZoneOffset.UTC);
        int start=now.getMonthValue()>=4?now.getYear():now.getYear()-1;
        String kind=credit?"cn":"dn", upper=credit?"CN":"DN";
        String table=credit?"credit_notes":"debit_notes";
        String prefix=upper+"/"+start+"-"+String.format("%02d",(start+1)%100)+"/";
        JsonArray last=selectArray("SELECT "+kind+"_number FROM "+table+" WHERE "+kind+
                "_number LIKE ? ORDER BY "+kind+"_number DESC LIMIT 1",List.of(prefix+"%"));
        int sequence=1;
        if(last.size()>0){String number=last.get(0).getAsJsonObject().get(kind+"_number").getAsString();
            sequence=Integer.parseInt(number.substring(number.lastIndexOf('/')+1))+1;}
        String number=prefix+String.format("%03d",sequence);
        double subtotal=0,cgst=0,sgst=0;
        for(JsonElement e:items){JsonObject item=e.getAsJsonObject();
            double taxable=Jsons.optionalDouble(item,"qty",1)*Jsons.optionalDouble(item,"rate",0);
            double rate=Jsons.optionalDouble(item,"gst_rate",0);subtotal+=taxable;
            cgst+=taxable*(rate/2/100);sgst+=taxable*(rate/2/100);}
        subtotal=Jsons.round2(subtotal);cgst=Jsons.round2(cgst);sgst=Jsons.round2(sgst);
        double total=Jsons.round2(subtotal+cgst+sgst);
        String id=UUID.randomUUID().toString().substring(0,12);
        update("INSERT INTO "+table+"("+kind+"_id,"+kind+"_number,original_invoice,user_id,"+
                        "user_name,user_gstin,reason,items,subtotal,cgst_total,sgst_total,total,created_at,notes)"+
                        " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id,number,Jsons.optionalString(note,"original_invoice",""),Jsons.requiredString(note,"user_id"),
                Jsons.optionalString(note,"user_name",""),Jsons.optionalString(note,"user_gstin",""),
                Jsons.optionalString(note,"reason",""),Jsons.json(items),subtotal,cgst,sgst,total,
                now.toString(),Jsons.optionalString(note,"notes",""));
        response.addProperty(kind+"_id",id);response.addProperty(kind+"_number",number);response.addProperty("total",total);
    }
    private JsonArray listNotes(JsonObject request,boolean credit)throws Exception{
        String table=credit?"credit_notes":"debit_notes";
        String from=Jsons.optionalString(request,"from_date",null),to=Jsons.optionalString(request,"to_date",null);
        JsonArray rows=(from!=null&&to!=null)?selectArray("SELECT * FROM "+table+
                " WHERE created_at>=? AND created_at<=? ORDER BY created_at DESC",
                List.of(from+"T00:00:00",to+"T23:59:59")):
                selectArray("SELECT * FROM "+table+" ORDER BY created_at DESC",List.of());
        normalizeJsonColumn(rows,"items",new JsonArray());return rows;
    }
    private void deleteNote(JsonObject request,JsonObject response,boolean credit)throws Exception{
        String kind=credit?"cn":"dn",table=credit?"credit_notes":"debit_notes";
        int changed=update("DELETE FROM "+table+" WHERE "+kind+"_id=?",Jsons.requiredString(request,kind+"_id"));
        if(changed==0)throw new ApiFailure(404,credit?"Credit note not found":"Debit note not found");
        response.addProperty("message","Deleted");
    }
    private void gstPartyLedger(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->AccountingReports.partyLedger(this,q,r),req,res);
    }
    private void gstDayBook(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->AccountingReports.dayBook(this,q,r),req,res);
    }
    private void gstProfitLoss(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->AccountingReports.profitLoss(this,q,r),req,res);
    }
    private void gstStockSummary(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->AccountingReports.stockSummary(this,r),req,res);
    }
    private void gstOutstanding(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->AccountingReports.outstanding(this,r),req,res);
    }
    private void gstTaxLedger(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->AccountingReports.taxLedger(this,q,r),req,res);
    }
    private void gstDashboardExtras(CrmRpc rpc, JsonObject req, JsonObject res) throws RpcException {
        execute((q,r)->AccountingReports.dashboardExtras(this,q,r),req,res);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) database.close();
    }
}
