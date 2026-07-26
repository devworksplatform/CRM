package com.petsfort.jrpc;

import com.google.gson.*;
import com.jay.rpc.RpcException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/** Local-only HTTP adapter used to compare JRPC handlers with FastAPI. */
public final class TestGateway {
    private TestGateway() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: TestGateway <db> <port>");
        System.setProperty("petsfort.db.path", args[0]);
        CrmService service = CrmService.open(CrmConfiguration.fromEnvironment());
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1",
                Integer.parseInt(args[1])), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/health", exchange -> send(exchange, 200, "{\"ok\":true}"));
        server.createContext("/rpc", exchange -> {
            try {
                if (!"POST".equals(exchange.getRequestMethod())) {
                    send(exchange, 405, "{\"detail\":\"Method Not Allowed\"}");
                    return;
                }
                String[] parts = exchange.getRequestURI().getPath().split("/");
                CrmRpc rpc = CrmRpc.valueOf(parts[parts.length - 1]);
                JsonElement parsed = JsonParser.parseReader(new InputStreamReader(
                        exchange.getRequestBody(), StandardCharsets.UTF_8));
                JsonObject request = parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
                JsonObject response = new JsonObject();
                try {
                    service.handlerFor(rpc).onRpc(rpc, request, response);
                    send(exchange, 200, response.toString());
                } catch (RpcException error) {
                    if (!response.has("detail")) response.addProperty("detail", error.getMessage());
                    int status = response.has("status_code")
                            ? response.get("status_code").getAsInt() : 500;
                    send(exchange, status, response.toString());
                }
            } catch (Exception error) {
                JsonObject response = new JsonObject();
                response.addProperty("detail", error.toString());
                send(exchange, 500, response.toString());
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.stop(0);
            service.close();
        }));
        server.start();
        System.out.println("TEST_GATEWAY_READY " + server.getAddress().getPort());
    }

    private static void send(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }
}
