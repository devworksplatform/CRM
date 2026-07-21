# Petsfort CRM JRPC server

This is the Java/JRPC replacement for `../Server/main.py`. It is an application
JAR loaded by JRPC Studio. The lifecycle also starts an embedded Undertow server
so the original FastAPI HTTP, HTML, SSE, and WebSocket clients continue working.

## Studio configuration

- Lifecycle class: `in.petsfort.crm.CrmApplication`
- RPC enum: `in.petsfort.crm.CrmRpc`
- Deployment artifact: `dist/crm-jrpc-server-1.0.0.jar`
- Java compatibility: Java 11

Set `CRM_DB_PATH` in the Studio worker environment to the absolute path of the
persistent SQLite database. If it is not set, the application uses
`products.db` in the worker's current directory. The application initializes
missing tables and performs the same offer-column migration as the Python app.

Do not copy `jrpc-studio.jar` into this project. Studio supplies JRPC, Gson,
Firebase Admin, and logging to the worker. This application's shaded JAR contains
SQLite JDBC and Undertow, but excludes the Studio-provided libraries.

## Runtime configuration

- `CRM_HTTP_HOST` defaults to `0.0.0.0`.
- `CRM_HTTP_PORT` defaults to `8080`; use `0` to disable HTTP compatibility.
- `CRM_LOG_PATH` defaults to `serverLogs.txt`.
- `CRM_FIREBASE_CREDENTIALS` defaults to `pets-fort-service-acc.json`.
- `CRM_FIREBASE_ENABLED=true` makes missing/invalid credentials a startup error.
- `CRM_FIREBASE_DATABASE_URL` and `CRM_FIREBASE_STORAGE_BUCKET` default to the
  same Petsfort Firebase resources used by the Python server.

When Firebase is configured, user creation/password changes/deletion, FCM topic
notifications, offer announcements, managed RTDB backups, and Storage uploads
are handled by Firebase Admin. Passwords are never stored in SQLite or returned.

## Build

```bash
cd /home/jay/works/CRM/CRMServers/Server-jrpc
./build.sh
```

The script runs the complete test suite, creates the shaded deployment JAR,
checks its required classes/assets and manifest, and verifies that libraries
supplied by Studio were not accidentally bundled. Set `MAVEN_BIN` if Maven is
installed at a nonstandard location.

## Request convention

Path and query values from FastAPI become top-level JSON fields. A former HTTP
request body can be sent directly or under `body`. List results are wrapped in a
named property because a JRPC response is a JSON object.

Examples:

```json
// GET_PRODUCT
{"product_identifier":"SKU-100"}

// LIST_PRODUCTS
{"limit":100,"offset":0}

// UPDATE_PRODUCT
{
  "product_identifier":"SKU-100",
  "body":{"product_name":"New name","stock":40}
}

// QUERY_PRODUCTS
{
  "filters":[{"field":"cat_id","operator":"eq","value":"DOG"}],
  "order_by":"product_name",
  "order_direction":"ASC",
  "limit":100,
  "offset":0
}

// CHECKOUT_ORDER
{
  "user_id":"shop-id-or-firebase-uid",
  "items":{"SKU-100":2,"SKU-200":1},
  "address":"Delivery address",
  "notes":"Call before delivery"
}
```

Query operators are `eq`, `neq`, `gt`, `lt`, `gte`, `lte`, `contains`,
`startswith`, `endswith`, and `in`.

## FastAPI to JRPC mapping

The enum names directly describe the migrated routes: products, offer groups,
orders, bills, categories, subcategories, users, schema/table administration,
backup/restore, analytics, system snapshot, credit/debit notes, and GST reports.
Open Studio's **Mappings** view after deployment for the complete ordered list.

All 75 routes declared by `main.py` are also registered by the embedded server,
including HTML/assets, CORS and host redirects, log access, live SSE metrics, and
the interactive terminal WebSocket. The same business operations are exposed as
64 JRPC operations where an object request/response contract is applicable.

## Safe migration

1. Stop writes to the Python server and create a database backup.
2. Set `CRM_DB_PATH` to the existing database's absolute path.
3. Deploy the JAR and test `HEALTH`, list/get operations, then writes in Studio.
4. Update clients from HTTP routes to the corresponding `CrmRpc` enum values.
5. Never reorder `CrmRpc`; append future operations because ordinals are the wire protocol.

Do not run Python and JRPC write traffic against the database during the cutover.
