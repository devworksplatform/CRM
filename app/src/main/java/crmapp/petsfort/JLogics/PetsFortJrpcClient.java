package crmapp.petsfort.JLogics;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/** Application-wide, asynchronous RPC transport for the PetsFort server. */
public final class PetsFortJrpcClient {
    public static final String SERVER_ID = "PetsFort-CRM";
    private static final long TIMEOUT_MS = 45_000L;
    private static volatile PetsFortJrpcClient instance;

    private final FirebaseDatabase database;
    private final FirebaseAuth auth;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback {
        void onSuccess(JsonObject response);
        void onError(int statusCode, String message);
    }

    private PetsFortJrpcClient(Context context) {
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    public static PetsFortJrpcClient get(Context context) {
        PetsFortJrpcClient current = instance;
        if (current != null) return current;
        synchronized (PetsFortJrpcClient.class) {
            if (instance == null) instance = new PetsFortJrpcClient(context);
            return instance;
        }
    }

    public void call(CrmRpc rpc, JsonObject request, Callback callback) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            mainHandler.post(() -> callback.onError(401, "Please login first."));
            return;
        }

        AtomicBoolean completed = new AtomicBoolean(false);
        String requestId = UUID.randomUUID().toString().replace("-", "");
        String relativePath = SERVER_ID + "/" + user.getUid() + "/rpc/" + requestId;
        DatabaseReference requestRef = database.getReference("ServerReq").child(relativePath);
        DatabaseReference responseRef = database.getReference("ServerResp").child(relativePath);
        ValueEventListener[] listenerHolder = new ValueEventListener[1];

        Runnable timeout = () -> {
            if (completed.compareAndSet(false, true)) {
                if (listenerHolder[0] != null) {
                    responseRef.removeEventListener(listenerHolder[0]);
                }
                requestRef.removeValue();
                responseRef.removeValue();
                callback.onError(504, "JRPC server did not respond within 45 seconds.");
            }
        };
        mainHandler.postDelayed(timeout, TIMEOUT_MS);

        try {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) return;
                    if (!completed.compareAndSet(false, true)) return;
                    mainHandler.removeCallbacks(timeout);
                    responseRef.removeEventListener(this);
                    responseRef.removeValue();
                    Boolean success = snapshot.child("success").getValue(Boolean.class);
                    if (!Boolean.TRUE.equals(success)) {
                        String code = snapshot.child("errorCode").getValue(String.class);
                        String message = snapshot.child("errorMessage").getValue(String.class);
                        callback.onError(statusFromCode(code),
                                message == null ? "RPC request failed" : unquote(message));
                        return;
                    }
                    String json = snapshot.child("responseJson").getValue(String.class);
                    try {
                        callback.onSuccess(com.google.gson.JsonParser.parseString(
                                json == null ? "{}" : json).getAsJsonObject());
                    } catch (Exception error) {
                        callback.onError(502, "JRPC returned invalid JSON.");
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    if (!completed.compareAndSet(false, true)) return;
                    mainHandler.removeCallbacks(timeout);
                    callback.onError(503, error.getMessage());
                }
            };
            listenerHolder[0] = listener;
            responseRef.addValueEventListener(listener);

            List<Integer> ordinals = new ArrayList<>();
            ordinals.add(rpc.ordinal());
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("createdAtEpochMs", System.currentTimeMillis());
            envelope.put("rpcs", ordinals);
            envelope.put("requestJson", (request == null ? new JsonObject() : request).toString());
            requestRef.setValue(envelope).addOnFailureListener(error -> {
                if (!completed.compareAndSet(false, true)) return;
                mainHandler.removeCallbacks(timeout);
                responseRef.removeEventListener(listener);
                callback.onError(503, error.getMessage());
            });
        } catch (Exception error) {
            if (!completed.compareAndSet(false, true)) return;
            mainHandler.removeCallbacks(timeout);
            callback.onError(500,
                    error.getMessage() == null ? "Failed to start RPC request" : error.getMessage());
        }
    }

    public static JsonObject requestWithBody(Map<String, Object> body) {
        JsonObject request = new JsonObject();
        JsonElement value = new com.google.gson.Gson().toJsonTree(body);
        request.add("body", value);
        return request;
    }

    private static int statusFromCode(String code) {
        if (code != null && code.startsWith("HTTP_")) {
            try { return Integer.parseInt(code.substring(5)); }
            catch (NumberFormatException ignored) {}
        }
        return 500;
    }

    private static String unquote(String message) {
        try {
            JsonElement parsed = com.google.gson.JsonParser.parseString(message);
            return parsed.isJsonPrimitive() && parsed.getAsJsonPrimitive().isString()
                    ? parsed.getAsString() : message;
        } catch (Exception ignored) {
            return message;
        }
    }
}
