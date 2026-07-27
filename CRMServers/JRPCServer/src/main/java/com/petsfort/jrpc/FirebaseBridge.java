package com.petsfort.jrpc;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/** Lazy Firebase Admin access, matching firebaseAuth.py without making init network-dependent. */
final class FirebaseBridge {
    private static volatile FirebaseBridge INSTANCE;
    private final FirebaseApp app;

    private FirebaseBridge(Path credentials) throws IOException {
        if (!Files.isRegularFile(credentials)) {
            throw new IOException("Firebase credential file not found: " + credentials);
        }
        FirebaseOptions options;
        try (java.io.InputStream stream = Files.newInputStream(credentials)) {
            options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(stream))
                    .setStorageBucket("pets-fort.firebasestorage.app")
                    .setDatabaseUrl("https://pets-fort-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .build();
        }
        String name = "petsfort-crm-" + Integer.toHexString(credentials.toString().hashCode());
        FirebaseApp existing = FirebaseApp.getApps().stream()
                .filter(candidate -> candidate.getName().equals(name)).findFirst().orElse(null);
        app = existing == null ? FirebaseApp.initializeApp(options, name) : existing;
    }

    static FirebaseBridge instance() {
        FirebaseBridge current = INSTANCE;
        if (current != null) return current;
        synchronized (FirebaseBridge.class) {
            if (INSTANCE == null) {
                try {
                    INSTANCE = new FirebaseBridge(CrmConfiguration.fromEnvironment().firebaseCredentials);
                } catch (IOException error) {
                    throw new IllegalStateException(error.getMessage(), error);
                }
            }
            return INSTANCE;
        }
    }

    String createUser(String email, String password) throws Exception {
        return FirebaseAuth.getInstance(app).createUser(
                new com.google.firebase.auth.UserRecord.CreateRequest()
                        .setEmail(email).setPassword(password)).getUid();
    }

    void changePassword(String uid, String password) throws Exception {
        FirebaseAuth.getInstance(app).updateUser(
                new com.google.firebase.auth.UserRecord.UpdateRequest(uid).setPassword(password));
    }

    void deleteUser(String uid) throws Exception {
        FirebaseAuth.getInstance(app).deleteUser(uid);
    }

    FirebaseDatabase database() {
        return FirebaseDatabase.getInstance(app);
    }

    FirebaseMessaging messaging() {
        return FirebaseMessaging.getInstance(app);
    }

    void setValue(String path, Object value) throws Exception {
        database().getReference(path).setValueAsync(value).get();
    }

    void deleteValue(String path) throws Exception {
        database().getReference(path).removeValueAsync().get();
    }

    Object getValue(String path) throws Exception {
        CompletableFuture<Object> result = new CompletableFuture<>();
        database().getReference(path).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                result.complete(snapshot.getValue());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                result.completeExceptionally(error.toException());
            }
        });
        return result.get(30, TimeUnit.SECONDS);
    }

    void requireBackupAdministrator(String idToken) throws Exception {
        if (idToken == null || idToken.trim().isEmpty()) {
            throw new ApiFailure(401, "A Firebase ID token is required.");
        }
        FirebaseToken token;
        try {
            token = FirebaseAuth.getInstance(app).verifyIdToken(idToken);
        } catch (Exception failure) {
            throw new ApiFailure(401, "Invalid or expired Firebase ID token.");
        }
        if (!"dev@petsfort.in".equalsIgnoreCase(token.getEmail())) {
            throw new ApiFailure(403, "Only dev@petsfort.in can manage backups.");
        }
    }

    void sendTopic(String topic, String title, String body) throws Exception {
        messaging().send(Message.builder()
                .setTopic(topic)
                .setNotification(Notification.builder().setTitle(title).setBody(body).build())
                .build());
    }
}
