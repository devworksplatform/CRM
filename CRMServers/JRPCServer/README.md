# PetsFort CRM JRPC server

Java 11 JRPC application that ports the business operations from
`../Server/main.py`. The lifecycle class is:

```text
com.petsfort.jrpc.PetsFortApplication
```

## Runtime configuration

Set these environment variables on the Studio worker:

```text
PETS_FORT_DB_PATH=/absolute/path/to/products.db
PETS_FORT_FIREBASE_CREDENTIALS=/absolute/path/to/pets-fort-service-acc.json
```

The database defaults to `products.db` in the worker directory. Firebase Admin
is initialized lazily, so database-only RPCs remain available if Firebase is
temporarily unavailable. Operations that require Authentication, Realtime
Database, Storage, or Messaging fail rather than silently skipping that work.

## Request convention

Path parameters and query parameters are top-level request fields. JSON request
bodies are placed in `body`. For example:

```json
{
  "product_identifier": "SKU-123",
  "body": {
    "product_name": "Example",
    "cat_id": "dogs"
  }
}
```

Python endpoints whose successful response is a top-level array return that
array in `data`, because JRPC handlers populate a `JsonObject`.

## Build

Use Maven 3.9+:

```powershell
.\.tools\apache-maven-3.9.11\bin\mvn.cmd clean test package
```

The deployable shaded JAR is written to `dist/`. JRPC and Gson are excluded
because Studio supplies them.

## Lifecycle

`init(JServer)` opens and migrates the configured SQLite database, constructs
thread-safe services, and synchronously registers every `CrmRpc` value using a
dedicated Java method. It does not start or close the supplied `JServer`.

`close()` is idempotent and releases application-owned resources. SQLite
connections are operation-scoped, so in-flight transactions are committed or
rolled back by their handlers.

HTML, sitemap, system-statistics streaming, and terminal operations remain
outside the business migration scope. HTML enum operations return `HTTP_501`.

Backup administration is available through the appended `GET_BACKUPS` through
`POST_BACKUP_RESTORE` RPCs. These operations require a valid Firebase ID token
for `dev@petsfort.in`. Backups are stored under `tables/{backupId}` in Firebase
Realtime Database. New backups retain table schemas and rows; restore also
accepts the legacy Python row-map format and creates a safety backup before
changing the live database.

## Start Studio on Windows

Run this from PowerShell:

```powershell
.\start-studio.ps1
```

Alternatively, double-click `start-studio.cmd`. The launcher validates Java,
the SQLite database, Firebase credentials, and the Studio JAR before starting
Studio at `http://127.0.0.1:8080`. Keep its console window open.

The GitHub-ready application artifact is also copied to:

```text
resources/petsfort-crm-jrpc-1.0.1.jar
```

## Start and stop Studio on Ubuntu

The Ubuntu launchers use Linux paths for the same database, Firebase
credentials, and Studio JAR:

```bash
./start-studio.sh
```

Keep that terminal open. Press `Ctrl+C`, or stop Studio from another terminal:

```bash
./stop-studio.sh
```

Paths and the port can be overridden with command-line options:

```bash
./start-studio.sh \
  --database /path/to/products.db \
  --credentials /path/to/firebase-service-account.json \
  --studio-jar /path/to/jrpc-studio.jar \
  --port 8080
./stop-studio.sh --port 8080
```

The corresponding environment variables are `PETS_FORT_DB_PATH`,
`PETS_FORT_FIREBASE_CREDENTIALS`, `JRPC_STUDIO_JAR`, and
`JRPC_STUDIO_PORT`.
