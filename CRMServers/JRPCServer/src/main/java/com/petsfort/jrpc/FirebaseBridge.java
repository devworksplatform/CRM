package com.petsfort.jrpc;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
}
