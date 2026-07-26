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

HTML, sitemap, backup/restore, system-statistics streaming, and terminal
operations are intentionally outside the business migration scope. HTML enum
operations return `HTTP_501`.
