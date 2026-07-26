package com.petsfort.jrpc;

import java.nio.file.Path;
import java.nio.file.Paths;

final class CrmConfiguration {
    final Path database;
    final Path firebaseCredentials;

    private CrmConfiguration(Path database, Path firebaseCredentials) {
        this.database = database;
        this.firebaseCredentials = firebaseCredentials;
    }

    static CrmConfiguration fromEnvironment() {
        String db = setting("petsfort.db.path", "PETS_FORT_DB_PATH", "products.db");
        String credentials = setting(
                "petsfort.firebase.credentials",
                "PETS_FORT_FIREBASE_CREDENTIALS",
                "pets-fort-service-acc.json");
        return new CrmConfiguration(
                Paths.get(db).toAbsolutePath().normalize(),
                Paths.get(credentials).toAbsolutePath().normalize());
    }

    private static String setting(String property, String environment, String fallback) {
        String value = System.getProperty(property);
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(environment);
        }
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}

