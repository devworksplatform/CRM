package in.petsfort.crm;

import com.jay.config.JServer;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/** Lifecycle entry point loaded reflectively by JRPC Studio. */
public final class CrmApplication {
    private final AtomicBoolean initialized = new AtomicBoolean();
    private CrmDatabase database;
    private FirebaseService firebase;
    private EmbeddedHttpServer http;

    public CrmApplication() {}

    public void init(JServer server) {
        if (server == null) throw new IllegalArgumentException("server is required");
        if (!initialized.compareAndSet(false, true)) throw new IllegalStateException("CRM application is already initialized");
        try {
            String configured = System.getenv("CRM_DB_PATH");
            if (configured == null || configured.isBlank()) configured = System.getProperty("crm.db.path", "products.db");
            database = new CrmDatabase(Path.of(configured));
            database.initialize();
            firebase = FirebaseService.fromEnvironment();
            CrmHandler handler = new CrmHandler(database, firebase);
            server.registerRpc(Arrays.asList(CrmRpc.values()), handler);
            http = new EmbeddedHttpServer(handler, database, new BackupService(database, firebase));
            http.start();
            System.out.println("Petsfort CRM JRPC initialized with database " + database.path());
        } catch (Exception error) {
            initialized.set(false);
            if (database != null) database.close();
            if (firebase != null) firebase.close();
            if (http != null) http.close();
            database = null;
            firebase = null;
            http = null;
            throw new IllegalStateException("Could not initialize CRM JRPC application", error);
        }
    }

    public void close() {
        if (initialized.compareAndSet(true, false)) {
            CrmDatabase current = database;
            FirebaseService currentFirebase = firebase;
            EmbeddedHttpServer currentHttp = http;
            database = null;
            firebase = null;
            http = null;
            if (currentHttp != null) currentHttp.close();
            if (current != null) current.close();
            if (currentFirebase != null) currentFirebase.close();
            System.out.println("Petsfort CRM JRPC closed");
        }
    }
}
