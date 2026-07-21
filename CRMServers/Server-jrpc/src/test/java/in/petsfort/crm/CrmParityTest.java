package in.petsfort.crm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jay.rpc.RpcException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

final class CrmParityTest {
    @TempDir Path temporaryDirectory;
    CrmDatabase database;InMemoryFirebaseService firebase;CrmHandler handler;
    @BeforeEach void setup()throws Exception{database=new CrmDatabase(temporaryDirectory.resolve("products.db"));database.initialize();firebase=new InMemoryFirebaseService();handler=new CrmHandler(database,firebase);}
    @AfterEach void close(){database.close();}

    @ParameterizedTest
    @CsvSource({"100,10,18,2,212.400","99.99,12.5,18,3,309.719","0.99,0,5,7,7.277","1250.75,33.33,28,4,4269.440"})
    void pythonCheckoutUsesExactDecimalTotals(String mrp,String discount,String gst,int quantity,String expected)throws Exception{
        createProduct("SKU",mrp,discount,gst,100);createUser(false,"100000");JsonObject result=call(CrmRpc.CHECKOUT_ORDER,pythonCart("SKU",quantity));
        assertEquals(0,new java.math.BigDecimal(expected).compareTo(result.get("total").getAsBigDecimal()));
        JsonObject bill=call(CrmRpc.GET_BILL,id("order_id",result.get("order_id").getAsString()));
        assertEquals(0,result.get("total").getAsBigDecimal().setScale(2,Money.ROUNDING).compareTo(bill.getAsJsonObject("totals").get("total").getAsBigDecimal()));
    }

    @Test void checkoutMatchesPythonSideEffects()throws Exception{createProduct("SKU","100","10","18",10);createUser(false,"1000");JsonObject result=call(CrmRpc.CHECKOUT_ORDER,pythonCart("SKU",2));assertEquals("Order created successfully",result.get("message").getAsString());
        assertEquals(0,new java.math.BigDecimal("787.60").compareTo(call(CrmRpc.GET_USER,id("user_id","SHOP")).get("credits").getAsBigDecimal().setScale(2,Money.ROUNDING)));
        JsonObject order=result.getAsJsonObject("order");assertEquals(2,order.getAsJsonObject("items").getAsJsonObject("SKU").get("paid_count").getAsInt());assertEquals(1,database.query("SELECT * FROM bills",List.of()).size());assertFalse(firebase.notifications.isEmpty());}

    @Test void blockedUserCannotSpendOrChangeStock()throws Exception{createProduct("SKU","100","0","0",5);createUser(true,"1000");RpcException error=assertThrows(RpcException.class,()->call(CrmRpc.CHECKOUT_ORDER,pythonCart("SKU",1)));assertEquals("USER_BLOCKED",error.getCode());assertEquals(5,call(CrmRpc.GET_PRODUCT,id("product_id","SKU")).get("stock").getAsInt());}

    @Test void bulkDetailsAcceptsPythonCartAndCalculatesCost()throws Exception{createProduct("SKU","100","10","18",5);JsonObject cart=new JsonObject();JsonObject q=new JsonObject();q.addProperty("count",2);cart.add("SKU",q);JsonObject result=call(CrmRpc.GET_PRODUCTS_BULK,cart);assertEquals(1,result.getAsJsonArray("product_details").size());assertEquals("212.40",result.getAsJsonObject("cost").get("total").getAsBigDecimal().setScale(2).toPlainString());}

    @Test void firebaseUserPasswordAndDeleteAreManaged()throws Exception{JsonObject user=user(false,"100");user.remove("uid");user.addProperty("pwd","secret123");JsonObject created=call(CrmRpc.PUT_USER,user);String uid=created.get("uid").getAsString();assertTrue(firebase.users.containsKey(uid));JsonObject update=user(false,"200");update.addProperty("user_id","SHOP");update.addProperty("pwd","newpass123");call(CrmRpc.PUT_USER,update);assertTrue(firebase.users.get(uid).endsWith(":newpass123"));call(CrmRpc.DELETE_USER,id("user_id","SHOP"));assertFalse(firebase.users.containsKey(uid));}

    @Test void offersPublishAndCleanAnnouncements()throws Exception{createProduct("SKU","100","0","0",5);JsonObject update=id("product_identifier","SKU");JsonObject body=new JsonObject();body.addProperty("offer_active",true);body.addProperty("offer_buy_qty",2);body.addProperty("offer_free_qty",1);update.add("body",body);call(CrmRpc.UPDATE_PRODUCT,update);assertNotNull(firebase.get("datas/announcement/all/offer_SKU"));body.addProperty("offer_active",false);call(CrmRpc.UPDATE_PRODUCT,update);assertNull(firebase.get("datas/announcement/all/offer_SKU"));}

    @Test void managedBackupsListRestoreDeleteAndReset()throws Exception{createProduct("SKU","10","0","0",1);JsonObject auth=id("token","admin-token");call(CrmRpc.CREATE_MANAGED_BACKUP,auth);JsonObject list=call(CrmRpc.LIST_BACKUPS,auth);assertTrue(list.getAsJsonArray("backups").size()>=2);String dated=list.getAsJsonArray("backups").get(1).getAsJsonObject().get("id").getAsString();JsonObject delete=id("token","admin-token");delete.addProperty("backup_id",dated);call(CrmRpc.DELETE_BACKUP,delete);assertNull(firebase.get("tables/"+dated));call(CrmRpc.RESET_BACKUPS,auth);assertEquals(2,call(CrmRpc.LIST_BACKUPS,auth).getAsJsonArray("backups").size());}

    @Test void creditDebitAndReportingUsePythonShapes()throws Exception{createProduct("SKU","100","10","18",10);createUser(false,"1000");JsonObject checkout=call(CrmRpc.CHECKOUT_ORDER,pythonCart("SKU",1));JsonObject update=id("order_id",checkout.get("order_id").getAsString());update.add("body",id("order_status","ORDER_DELIVERED"));call(CrmRpc.UPDATE_ORDER,update);JsonArray items=new JsonArray();JsonObject item=new JsonObject();item.addProperty("qty",2);item.addProperty("rate","10.25");item.addProperty("gst_rate","18");items.add(item);JsonObject note=new JsonObject();note.addProperty("user_id","UID");note.add("items",items);JsonObject cn=call(CrmRpc.CREATE_CREDIT_NOTE,note);assertEquals("24.19",cn.get("total").getAsBigDecimal().setScale(2).toPlainString());String year=String.valueOf(java.time.LocalDate.now().getYear());JsonObject range=new JsonObject();range.addProperty("from_date",year+"-01-01");range.addProperty("to_date",year+"-12-31");assertTrue(call(CrmRpc.GST_DAY_BOOK,range).has("summary"));assertTrue(call(CrmRpc.GST_PARTY_LEDGER,range).has("total_debit"));assertTrue(call(CrmRpc.GST_STOCK_SUMMARY,new JsonObject()).has("category_summary"));assertTrue(call(CrmRpc.ANALYTICS_SUMMARY,new JsonObject()).has("orders_trend_12_months"));}

    @Test void concurrentCheckoutNeverOversellsOrLosesPaise()throws Exception{createProduct("SKU","10","0","0",10);createUser(false,"1000");ExecutorService pool=Executors.newFixedThreadPool(8);try{List<Callable<JsonObject>>tasks=new java.util.ArrayList<>();for(int i=0;i<20;i++)tasks.add(()->call(CrmRpc.CHECKOUT_ORDER,pythonCart("SKU",1)));int created=0,out=0;for(Future<JsonObject>future:pool.invokeAll(tasks)){try{JsonObject result=future.get();if("OutOfStock".equals(result.has("message")?result.get("message").getAsString():""))out++;else created++;}catch(java.util.concurrent.ExecutionException error){Throwable cause=error.getCause();if(cause instanceof RpcException&&("DATABASE_BUSY".equals(((RpcException)cause).getCode())||"INSUFFICIENT_STOCK".equals(((RpcException)cause).getCode())))out++;else throw error;}}assertEquals(10,created);assertEquals(10,out);assertEquals(0,call(CrmRpc.GET_PRODUCT,id("product_id","SKU")).get("stock").getAsInt());assertEquals(10,database.query("SELECT * FROM orders",List.of()).size());assertEquals("900.00",call(CrmRpc.GET_USER,id("user_id","SHOP")).get("credits").getAsBigDecimal().setScale(2).toPlainString());}finally{pool.shutdownNow();}}

    @Test void schemaMutationAndGenericTableResponsesMatchPython()throws Exception{JsonObject schema=call(CrmRpc.GET_SCHEMA,new JsonObject());assertFalse(schema.has("schema"));assertEquals("products",schema.getAsJsonObject("products_table").get("table_name").getAsString());JsonObject column=new JsonObject();column.addProperty("column_name","parity_label");column.addProperty("column_type","TEXT");column.addProperty("default_value","O'Reilly");JsonArray columns=new JsonArray();columns.add(column);JsonObject add=new JsonObject();add.add("columns",columns);JsonObject first=call(CrmRpc.ADD_COLUMNS,add);assertEquals(1,first.getAsJsonArray("added_columns").size());assertEquals(0,call(CrmRpc.ADD_COLUMNS,add).getAsJsonArray("added_columns").size());JsonObject row=new JsonObject();row.addProperty("id","GENERIC");row.addProperty("product_id","GEN-SKU");row.addProperty("product_name","Generic");JsonObject addRow=new JsonObject();addRow.addProperty("table_name","products");addRow.add("body",row);assertTrue(call(CrmRpc.ADD_TABLE_ROW,addRow).has("row_id"));JsonObject table=id("table_name","products");assertEquals("id",call(CrmRpc.GET_TABLE_INFO,table).get("pk_column").getAsString());assertTrue(call(CrmRpc.GET_TABLE_DATA,table).has("data"));JsonObject remove=new JsonObject();JsonArray removing=new JsonArray();removing.add("parity_label");remove.add("columns",removing);assertEquals("parity_label",call(CrmRpc.REMOVE_COLUMNS,remove).getAsJsonArray("removed_columns").get(0).getAsString());JsonObject bad=new JsonObject();JsonArray primary=new JsonArray();primary.add("id");bad.add("columns",primary);assertEquals("INVALID_REQUEST",assertThrows(RpcException.class,()->call(CrmRpc.REMOVE_COLUMNS,bad)).getCode());}

    @Test void missingBillReturnsPythonEmptyObject()throws Exception{assertEquals(0,call(CrmRpc.GET_BILL,id("order_id","does-not-exist")).size());}

    private JsonObject call(CrmRpc rpc,JsonObject request)throws RpcException{JsonObject response=new JsonObject();handler.onRpc(rpc,request,response);return response;}
    private void createProduct(String id,String mrp,String discount,String gst,int stock)throws Exception{JsonObject p=new JsonObject();p.addProperty("product_id",id);p.addProperty("product_name","Product "+id);p.addProperty("product_desc","");p.addProperty("product_hsn","HSN");p.addProperty("product_cid","CID");p.add("product_img",new JsonArray());p.addProperty("cat_id","CAT");p.addProperty("cat_sub","SUB");p.addProperty("cost_rate",new java.math.BigDecimal(mrp).multiply(java.math.BigDecimal.ONE.subtract(new java.math.BigDecimal(discount).movePointLeft(2))));p.addProperty("cost_mrp",new java.math.BigDecimal(mrp));p.addProperty("cost_gst",new java.math.BigDecimal(gst));p.addProperty("cost_dis",new java.math.BigDecimal(discount));p.addProperty("stock",stock);call(CrmRpc.CREATE_PRODUCT,p);}
    private void createUser(boolean blocked,String credits)throws Exception{call(CrmRpc.PUT_USER,user(blocked,credits));}
    private static JsonObject user(boolean blocked,String credits){JsonObject u=new JsonObject();u.addProperty("id","SHOP");u.addProperty("uid","UID");u.addProperty("name","Shop");u.addProperty("contact","999");u.addProperty("gstin","GST");u.addProperty("email","shop@test.invalid");u.addProperty("role","shop");u.addProperty("address","Address");u.addProperty("credits",new java.math.BigDecimal(credits));u.addProperty("creditse","2099");u.addProperty("isblocked",blocked?1:0);return u;}
    private static JsonObject pythonCart(String product,int count){JsonObject request=new JsonObject();request.addProperty("user_id","SHOP");JsonObject item=new JsonObject();item.addProperty("count",count);request.add(product,item);JsonObject other=new JsonObject();other.addProperty("address","Delivery");other.addProperty("notes","Test");request.add("otherData",other);return request;}
    private static JsonObject id(String key,String value){JsonObject o=new JsonObject();o.addProperty(key,value);return o;}
}
