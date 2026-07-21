package in.petsfort.crm;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.StorageClient;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.auth.oauth2.GoogleCredentials;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

interface FirebaseService extends AutoCloseable {
    boolean enabled();
    String createUser(String email, String password) throws Exception;
    void updatePassword(String uid, String password) throws Exception;
    void deleteUser(String uid) throws Exception;
    void notifyTopic(String topic, String title, String body, Map<String, String> data) throws Exception;
    void set(String path, Object value) throws Exception;
    Object get(String path) throws Exception;
    void delete(String path) throws Exception;
    String verifyEmail(String bearerToken) throws Exception;
    JsonObject upload(Path file, String objectName) throws Exception;
    @Override void close();

    static FirebaseService fromEnvironment() throws Exception {
        String enabled = System.getenv("CRM_FIREBASE_ENABLED");
        if ("false".equalsIgnoreCase(enabled) || "0".equals(enabled)) return disabled();
        String credential = System.getenv().getOrDefault("CRM_FIREBASE_CREDENTIALS", "pets-fort-service-acc.json");
        Path path = Path.of(credential);
        if (!Files.isRegularFile(path)) {
            if ("true".equalsIgnoreCase(enabled) || "1".equals(enabled)) throw new IllegalStateException("Firebase credential not found: " + path.toAbsolutePath());
            return disabled();
        }
        String databaseUrl = System.getenv().getOrDefault("CRM_FIREBASE_DATABASE_URL", "https://pets-fort-default-rtdb.asia-southeast1.firebasedatabase.app");
        String storageBucket = System.getenv().getOrDefault("CRM_FIREBASE_STORAGE_BUCKET", "pets-fort.firebasestorage.app");
        return new AdminFirebaseService(path, databaseUrl, storageBucket);
    }

    static FirebaseService disabled() { return DisabledFirebaseService.INSTANCE; }
}

final class DisabledFirebaseService implements FirebaseService {
    static final DisabledFirebaseService INSTANCE = new DisabledFirebaseService();
    private DisabledFirebaseService() {}
    public boolean enabled() { return false; }
    private Exception unavailable() { return new IllegalStateException("Firebase is not configured; set CRM_FIREBASE_ENABLED=true and CRM_FIREBASE_CREDENTIALS"); }
    public String createUser(String email, String password) throws Exception { throw unavailable(); }
    public void updatePassword(String uid, String password) throws Exception { throw unavailable(); }
    public void deleteUser(String uid) throws Exception { throw unavailable(); }
    public void notifyTopic(String topic, String title, String body, Map<String, String> data) throws Exception { throw unavailable(); }
    public void set(String path, Object value) throws Exception { throw unavailable(); }
    public Object get(String path) throws Exception { throw unavailable(); }
    public void delete(String path) throws Exception { throw unavailable(); }
    public String verifyEmail(String bearerToken) throws Exception { throw unavailable(); }
    public JsonObject upload(Path file, String objectName) throws Exception { throw unavailable(); }
    public void close() {}
}

final class AdminFirebaseService implements FirebaseService {
    private final FirebaseApp app;
    private final FirebaseAuth auth;
    private final FirebaseDatabase database;
    private final FirebaseMessaging messaging;

    AdminFirebaseService(Path credential, String databaseUrl, String storageBucket) throws Exception {
        try (FileInputStream stream = new FileInputStream(credential.toFile())) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .setDatabaseUrl(databaseUrl).setStorageBucket(storageBucket).build();
            app = FirebaseApp.initializeApp(options, "petsfort-crm-" + UUID.randomUUID());
        }
        auth = FirebaseAuth.getInstance(app); database = FirebaseDatabase.getInstance(app); messaging = FirebaseMessaging.getInstance(app);
    }

    public boolean enabled() { return true; }
    public String createUser(String email, String password) throws Exception {
        return auth.createUserAsync(new UserRecord.CreateRequest().setEmail(email).setPassword(password)).get(30, TimeUnit.SECONDS).getUid();
    }
    public void updatePassword(String uid, String password) throws Exception {
        auth.updateUserAsync(new UserRecord.UpdateRequest(uid).setPassword(password)).get(30, TimeUnit.SECONDS);
    }
    public void deleteUser(String uid) throws Exception { auth.deleteUserAsync(uid).get(30, TimeUnit.SECONDS); }
    public void notifyTopic(String topic, String title, String body, Map<String, String> data) throws Exception {
        Message message = Message.builder().setTopic(topic).setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .putAllData(data == null ? Collections.emptyMap() : data).build();
        messaging.sendAsync(message).get(30, TimeUnit.SECONDS);
    }
    public void set(String path, Object value) throws Exception { database.getReference(path).setValueAsync(value).get(30, TimeUnit.SECONDS); }
    public void delete(String path) throws Exception { database.getReference(path).removeValueAsync().get(30, TimeUnit.SECONDS); }
    public Object get(String path) throws Exception {
        CompletableFuture<Object> future = new CompletableFuture<>();
        database.getReference(path).addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(DataSnapshot snapshot) { future.complete(snapshot.getValue()); }
            public void onCancelled(DatabaseError error) { future.completeExceptionally(error.toException()); }
        });
        return future.get(30, TimeUnit.SECONDS);
    }
    public String verifyEmail(String bearerToken) throws Exception {
        FirebaseToken token = auth.verifyIdTokenAsync(bearerToken).get(30, TimeUnit.SECONDS); return token.getEmail();
    }
    public JsonObject upload(Path file, String objectName) throws Exception {
        com.google.cloud.storage.Blob blob = StorageClient.getInstance(app).bucket().create(objectName, Files.readAllBytes(file), "application/x-sqlite3");
        JsonObject result = new JsonObject(); result.addProperty("path", blob.getName());
        result.addProperty("url", "https://storage.googleapis.com/" + blob.getBucket() + "/" + blob.getName()); result.add("err", null); return result;
    }
    public void close() { app.delete(); }
}
