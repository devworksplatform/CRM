package in.petsfort.crm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jay.rpc.RpcException;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class EmbeddedHttpServer implements AutoCloseable {
    private final CrmHandler rpc;
    private final CrmDatabase database;
    private final BackupService backups;
    private final Path logPath;
    private final ExecutorService streams = Executors.newCachedThreadPool();
    private final Set<Process> terminals = ConcurrentHashMap.newKeySet();
    private Undertow server;

    EmbeddedHttpServer(CrmHandler rpc, CrmDatabase database, BackupService backups) {
        this.rpc=rpc;this.database=database;this.backups=backups;
        this.logPath=Path.of(System.getProperty("crm.log.path",System.getenv().getOrDefault("CRM_LOG_PATH","serverLogs.txt"))).toAbsolutePath();
    }

    void start() {
        int port=Integer.getInteger("crm.http.port",Integer.parseInt(System.getenv().getOrDefault("CRM_HTTP_PORT","8080")));
        if(port<=0)return;String host=System.getenv().getOrDefault("CRM_HTTP_HOST","0.0.0.0");RoutingHandler routes=Handlers.routing();
        routes.get("/",this::root);routes.get("/sitemap.xml",x->asset(x,"sitemap.xml","application/xml"));routes.get("/privacy_policy",x->asset(x,"privacy_policy.html","text/html; charset=utf-8"));routes.get("/database",x->asset(x,"database.html","text/html; charset=utf-8"));routes.get("/analytics",x->asset(x,"analytics.html","text/html; charset=utf-8"));
        routes.get("/logs",this::logs);routes.delete("/logs",this::deleteLogs);routes.get("/ram",x->text(x,StatusCodes.OK,SystemMetrics.freeCommand(),"text/plain"));
        routes.get("/backup",x->json(x,StatusCodes.OK,backups.create()));routes.get("/restore/{restore_path}",x->json(x,StatusCodes.OK,backups.restore(param(x,"restore_path"))));
        routes.get("/backups/list",x->{if(!admin(x))return;JsonObject o=new JsonObject();o.add("backups",backups.list());json(x,200,o);});routes.post("/backups/create",x->{if(!admin(x))return;JsonObject o=new JsonObject();o.addProperty("detail","Backup created successfully.");o.add("result",backups.create());json(x,200,o);});
        routes.delete("/backups/older-than-days/{days}",x->{if(!admin(x))return;JsonObject o=new JsonObject();o.addProperty("detail","Old backups deleted.");o.add("deleted_ids",backups.deleteOlderThan(Integer.parseInt(param(x,"days"))));json(x,200,o);});routes.delete("/backups/{backup_id}",x->{if(!admin(x))return;String id=param(x,"backup_id");if(!backups.delete(id)){error(x,404,"Backup not found.");return;}JsonObject o=new JsonObject();o.addProperty("detail","Backup deleted.");o.addProperty("deleted_id",id);json(x,200,o);});
        routes.post("/backups/delete-selected",x->{if(!admin(x))return;JsonObject requestBody=body(x);JsonArray ids=requestBody.has("ids")&&requestBody.get("ids").isJsonArray()?requestBody.getAsJsonArray("ids"):null;if(ids==null||ids.isEmpty()){error(x,400,"A non-empty JSON list is required in the 'ids' field.");return;}List<String>v=new ArrayList<>();for(JsonElement id:ids){if(!id.isJsonPrimitive()||!id.getAsJsonPrimitive().isString()){error(x,400,"Every backup ID must be a string.");return;}v.add(id.getAsString());}JsonObject o=new JsonObject();o.addProperty("detail","Selected backups deleted.");o.add("deleted_ids",backups.deleteSelected(v));json(x,200,o);});routes.post("/backups/reset-current",x->{if(!admin(x))return;json(x,200,backups.reset());});

        routes.post("/products/",x->rpc(x,CrmRpc.CREATE_PRODUCT,null));routes.post("/products/query",x->rpc(x,CrmRpc.QUERY_PRODUCTS,"products"));routes.get("/products/",x->rpc(x,CrmRpc.LIST_PRODUCTS,"products"));routes.get("/products/{product_identifier}",x->rpc(x,CrmRpc.GET_PRODUCT,null));routes.put("/products/{product_identifier}",x->rpc(x,CrmRpc.UPDATE_PRODUCT,null));routes.delete("/products/{product_identifier}",x->rpc(x,CrmRpc.DELETE_PRODUCT,null));routes.post("/products/bulk-details",x->rpc(x,CrmRpc.GET_PRODUCTS_BULK,null));
        routes.get("/offer-groups",x->rpc(x,CrmRpc.LIST_OFFER_GROUPS,"offerGroups"));routes.post("/offer-groups",x->rpc(x,CrmRpc.CREATE_OFFER_GROUP,null));routes.put("/offer-groups/{group_id}",x->rpc(x,CrmRpc.UPDATE_OFFER_GROUP,null));routes.post("/offer-groups/{group_id}/apply",x->rpc(x,CrmRpc.APPLY_OFFER_GROUP,null));routes.post("/offer-groups/{group_id}/cancel",x->rpc(x,CrmRpc.CANCEL_OFFER_GROUP,null));routes.delete("/offer-groups/{group_id}",x->rpc(x,CrmRpc.DELETE_OFFER_GROUP,null));
        routes.post("/schema/add-columns",x->rpc(x,CrmRpc.ADD_COLUMNS,null));routes.post("/schema/remove-columns",x->rpc(x,CrmRpc.REMOVE_COLUMNS,null));routes.get("/schema",x->rpc(x,CrmRpc.GET_SCHEMA,null));
        routes.post("/orders/checkout/{user_id}",x->rpc(x,CrmRpc.CHECKOUT_ORDER,null));routes.get("/bills/{order_id}",x->rpc(x,CrmRpc.GET_BILL,null));routes.post("/orders/query",x->rpc(x,CrmRpc.QUERY_ORDERS,"orders"));routes.put("/orders/{order_id}",x->rpc(x,CrmRpc.UPDATE_ORDER,null));routes.delete("/orders/{order_id}",x->rpc(x,CrmRpc.DELETE_ORDER,null));
        routes.get("/categories",x->rpc(x,CrmRpc.LIST_CATEGORIES,"categories"));routes.post("/categories",x->rpc(x,CrmRpc.PUT_CATEGORY,null));routes.delete("/categories/{cat_id}",x->{x.addQueryParam("id",param(x,"cat_id"));rpc(x,CrmRpc.DELETE_CATEGORY,null);});routes.get("/subcategories",x->rpc(x,CrmRpc.LIST_SUBCATEGORIES,"subcategories"));routes.get("/subcats_v0/{category_id}",x->rpc(x,CrmRpc.LIST_AVAILABLE_SUBCATEGORIES,"subcategories"));routes.get("/subcats/{category_id}",x->rpc(x,CrmRpc.LIST_AVAILABLE_SUBCATEGORIES,"subcategories"));routes.post("/subcategories",x->rpc(x,CrmRpc.PUT_SUBCATEGORY,null));routes.delete("/subcategories/{cat_id}",x->{x.addQueryParam("id",param(x,"cat_id"));rpc(x,CrmRpc.DELETE_SUBCATEGORY,null);});
        routes.get("/userdata",x->rpc(x,CrmRpc.LIST_USERS,"users"));routes.get("/user/{user_id}",x->rpc(x,CrmRpc.GET_USER,null));routes.post("/userdata",x->rpc(x,CrmRpc.PUT_USER,null));routes.put("/userdata/{user_id}",x->rpc(x,CrmRpc.PUT_USER,null));routes.delete("/userdata/{user_id}",x->rpc(x,CrmRpc.DELETE_USER,null));
        routes.get("/api/tables",this::tables);routes.get("/api/table/{table_name}/info",x->rpc(x,CrmRpc.GET_TABLE_INFO,null));routes.get("/api/table/{table_name}/data",x->rpc(x,CrmRpc.GET_TABLE_DATA,null));routes.post("/api/table/{table_name}/row",x->rpc(x,CrmRpc.ADD_TABLE_ROW,null,201));routes.put("/api/table/{table_name}/row/{pk_value}",x->{x.addQueryParam("pk_column",primaryKey(param(x,"table_name")));rpc(x,CrmRpc.UPDATE_TABLE_ROW,null);});routes.delete("/api/table/{table_name}/row/{pk_value}",x->{x.addQueryParam("pk_column",primaryKey(param(x,"table_name")));rpc(x,CrmRpc.DELETE_TABLE_ROW,null);});
        routes.get("/analytics/summary",x->rpc(x,CrmRpc.ANALYTICS_SUMMARY,null));routes.get("/system-stats/snapshot",x->rpc(x,CrmRpc.SYSTEM_SNAPSHOT,null));routes.get("/system-stats/live",this::sse);
        routes.get("/gst/dashboard",x->rpc(x,CrmRpc.GST_DASHBOARD,null));routes.get("/gst/sales-register",x->rpc(x,CrmRpc.GST_SALES_REGISTER,"sales"));routes.post("/gst/credit-notes",x->rpc(x,CrmRpc.CREATE_CREDIT_NOTE,null));routes.get("/gst/credit-notes",x->rpc(x,CrmRpc.LIST_CREDIT_NOTES,"notes"));routes.delete("/gst/credit-notes/{cn_id}",x->rpc(x,CrmRpc.DELETE_CREDIT_NOTE,null));routes.post("/gst/debit-notes",x->rpc(x,CrmRpc.CREATE_DEBIT_NOTE,null));routes.get("/gst/debit-notes",x->rpc(x,CrmRpc.LIST_DEBIT_NOTES,"notes"));routes.delete("/gst/debit-notes/{dn_id}",x->rpc(x,CrmRpc.DELETE_DEBIT_NOTE,null));routes.get("/gst/party-ledger",x->rpc(x,CrmRpc.GST_PARTY_LEDGER,null));routes.get("/gst/day-book",x->rpc(x,CrmRpc.GST_DAY_BOOK,null));routes.get("/gst/profit-loss",x->rpc(x,CrmRpc.GST_PROFIT_LOSS,null));routes.get("/gst/stock-summary",x->rpc(x,CrmRpc.GST_STOCK_SUMMARY,null));routes.get("/gst/outstanding",x->rpc(x,CrmRpc.GST_OUTSTANDING,null));routes.get("/gst/tax-ledger",x->rpc(x,CrmRpc.GST_TAX_LEDGER,null));routes.get("/gst/dashboard-extras",x->rpc(x,CrmRpc.GST_DASHBOARD_EXTRAS,null));
        routes.add(Methods.GET,"/ws/terminal",Handlers.websocket(new TerminalSocket()));routes.setFallbackHandler(x->{if(x.getRequestMethod().equals(Methods.OPTIONS)){x.setStatusCode(204);x.endExchange();}else error(x,404,"Not found");});
        HttpHandler blockingRoutes=new BlockingHandler(routes);HttpHandler cors=exchange->{exchange.getResponseHeaders().put(new HttpString("Access-Control-Allow-Origin"),"*");exchange.getResponseHeaders().put(new HttpString("Access-Control-Allow-Methods"),"*");exchange.getResponseHeaders().put(new HttpString("Access-Control-Allow-Headers"),"*");String hostHeader=exchange.getRequestHeaders().getFirst(Headers.HOST);if(hostHeader!=null&&hostHeader.split(":")[0].equals("admin.petsfort.in")){exchange.setStatusCode(307);exchange.getResponseHeaders().put(Headers.LOCATION,"https://pets-fort.web.app"+exchange.getRequestURI()+(exchange.getQueryString().isBlank()?"":"?"+exchange.getQueryString()));exchange.endExchange();return;}blockingRoutes.handleRequest(exchange);};
        server=Undertow.builder().addHttpListener(port,host).setHandler(cors).build();server.start();System.out.println("Petsfort HTTP compatibility server listening on "+host+":"+port);
    }

    private void rpc(HttpServerExchange exchange,CrmRpc operation,String unwrap)throws Exception{rpc(exchange,operation,unwrap,200);}
    private void rpc(HttpServerExchange exchange,CrmRpc operation,String unwrap,int successStatus)throws Exception{JsonObject request=request(exchange);JsonObject response=new JsonObject();try{rpc.onRpc(operation,request,response);JsonElement payload=unwrap==null?response:response.get(unwrap);json(exchange,successStatus,payload==null?new JsonObject():payload);}catch(RpcException e){int status=Map.of("NOT_FOUND",404,"CONFLICT",409,"INVALID_REQUEST",400,"USER_BLOCKED",400,"INSUFFICIENT_STOCK",409,"FORBIDDEN",403).getOrDefault(e.getCode(),500);error(exchange,status,e.getMessage());}}
    private JsonObject request(HttpServerExchange exchange)throws IOException{JsonObject result=body(exchange);exchange.getQueryParameters().forEach((key,values)->{if(!values.isEmpty())result.addProperty(key,values.getFirst());});return result;}
    private JsonObject body(HttpServerExchange exchange)throws IOException{if(!exchange.isBlocking())exchange.startBlocking();String contentType=exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);if(contentType!=null&&(contentType.startsWith("application/x-www-form-urlencoded")||contentType.startsWith("multipart/form-data"))){FormDataParser parser=FormParserFactory.builder().build().createParser(exchange);JsonObject result=new JsonObject();if(parser==null)return result;try{FormData data=parser.parseBlocking();for(String name:data){FormData.FormValue value=data.getFirst(name);if(value==null)continue;if(value.isFileItem())result.addProperty(name,value.getFileName());else if(value.getValue().isEmpty())result.add(name,JsonNull.INSTANCE);else result.addProperty(name,value.getValue());}return result;}finally{parser.close();}}String text=new String(exchange.getInputStream().readAllBytes(),StandardCharsets.UTF_8);if(text.isBlank())return new JsonObject();JsonElement value=JsonParser.parseString(text);return value.isJsonObject()?value.getAsJsonObject():new JsonObject();}
    private void root(HttpServerExchange x)throws Exception{String host=x.getRequestHeaders().getFirst(Headers.HOST);host=host==null?"":host.split(":")[0];if(host.equals("petsfort.in")){asset(x,"index.html","text/html; charset=utf-8");return;}JsonObject o=new JsonObject();o.addProperty("message",host.equals("server.petsfort.in")?"API is running":"Hello From AWS Domain");json(x,200,o);}
    private void asset(HttpServerExchange x,String name,String type)throws Exception{try(InputStream in=getClass().getResourceAsStream("/crm-assets/"+name)){if(in==null){error(x,404,name+" not found");return;}text(x,200,new String(in.readAllBytes(),StandardCharsets.UTF_8),type);}}
    private void logs(HttpServerExchange x)throws Exception{if(!Files.exists(logPath)){error(x,500,"Could not find the serverLogs.txt");return;}text(x,200,Files.readString(logPath),"text/plain");}
    private void deleteLogs(HttpServerExchange x)throws Exception{Files.writeString(logPath,"");JsonObject o=new JsonObject();o.addProperty("detail","Log file deleted successfully.");json(x,200,o);}
    private void tables(HttpServerExchange x)throws Exception{JsonObject r=new JsonObject();rpc.onRpc(CrmRpc.LIST_TABLES,new JsonObject(),r);JsonArray names=new JsonArray();r.getAsJsonArray("tables").forEach(t->names.add(t.getAsJsonObject().get("name").getAsString()));json(x,200,names);}
    private String primaryKey(String table)throws SQLException{JsonArray columns=database.query("PRAGMA table_info("+table+")",List.of());for(JsonElement c:columns)if(c.getAsJsonObject().get("pk").getAsInt()==1)return c.getAsJsonObject().get("name").getAsString();return columns.get(0).getAsJsonObject().get("name").getAsString();}
    private boolean admin(HttpServerExchange x){String auth=x.getRequestHeaders().getFirst(Headers.AUTHORIZATION);if(auth==null||!auth.startsWith("Bearer ")||auth.substring(7).trim().isEmpty()){error(x,401,"Missing bearer token.");return false;}try{backups.verifyAdmin(auth.substring(7).trim());return true;}catch(SecurityException denied){error(x,403,"Only "+BackupService.ADMIN_EMAIL+" can manage backups.");return false;}catch(Exception invalid){error(x,401,"Invalid or expired bearer token.");return false;}}
    private void sse(HttpServerExchange x){x.getResponseHeaders().put(Headers.CONTENT_TYPE,"text/event-stream");x.getResponseHeaders().put(Headers.CACHE_CONTROL,"no-cache");x.setPersistent(true);x.dispatch(streams,()->{try{OutputStream out=x.getOutputStream();while(!Thread.currentThread().isInterrupted()){out.write(("data: "+SystemMetrics.snapshot()+"\n\n").getBytes(StandardCharsets.UTF_8));out.flush();Thread.sleep(1000);}}catch(Exception ignored){}finally{x.endExchange();}});}
    private static String param(HttpServerExchange x,String name){Deque<String>v=x.getQueryParameters().get(name);return v==null||v.isEmpty()?null:v.getFirst();}
    private static void json(HttpServerExchange x,int status,JsonElement value){text(x,status,value==null?"null":value.toString(),"application/json");}
    private static void text(HttpServerExchange x,int status,String value,String type){x.setStatusCode(status);x.getResponseHeaders().put(Headers.CONTENT_TYPE,type);x.getResponseSender().send(value);}
    private static void error(HttpServerExchange x,int status,String detail){JsonObject o=new JsonObject();o.addProperty("detail",detail);json(x,status,o);}

    private final class TerminalSocket implements WebSocketConnectionCallback{
        public void onConnect(WebSocketHttpExchange exchange,WebSocketChannel channel){try{String shell=System.getenv().getOrDefault("CRM_TERMINAL_SHELL","/bin/bash");Process process=new ProcessBuilder("script","-qfc",shell,"/dev/null").redirectErrorStream(true).start();terminals.add(process);streams.submit(()->{try(InputStream in=process.getInputStream()){byte[]buffer=new byte[4096];int read;while((read=in.read(buffer))>=0)WebSockets.sendText(new String(buffer,0,read,StandardCharsets.UTF_8),channel,null);}catch(Exception ignored){}finally{process.destroy();terminals.remove(process);try{channel.close();}catch(IOException ignored){}}});channel.getReceiveSetter().set(new AbstractReceiveListener(){protected void onFullTextMessage(WebSocketChannel ch,BufferedTextMessage message)throws IOException{String raw=message.getData();try{JsonObject command=JsonParser.parseString(raw).getAsJsonObject();String action=command.has("action")?command.get("action").getAsString():"input";if(action.equals("close")){process.destroy();ch.close();return;}if(action.equals("input")&&command.has("data")){process.getOutputStream().write(command.get("data").getAsString().getBytes(StandardCharsets.UTF_8));process.getOutputStream().flush();}}catch(RuntimeException e){process.getOutputStream().write(raw.getBytes(StandardCharsets.UTF_8));process.getOutputStream().flush();}}});channel.resumeReceives();}catch(Exception e){WebSockets.sendClose(1011,e.getMessage(),channel,null);}}
    }

    public void close(){if(server!=null){server.stop();server=null;}terminals.forEach(Process::destroy);terminals.clear();streams.shutdownNow();}
}
