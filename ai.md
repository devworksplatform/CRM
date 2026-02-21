# AI CONTEXT: PetsFort CRM

> Structured reference for AI agents. Minimal prose, maximum signal.

---

## IDENTITY

- **Package**: `crmapp.petsfort`
- **App Name**: "Petsfort"
- **Type**: Android B2B CRM (pet supplies wholesale)
- **Language**: Java 17
- **compileSdk**: 35, **minSdk**: 26, **targetSdk**: 35
- **versionCode**: 17, **versionName**: "1.17"
- **Build**: Gradle (AGP 8.7.3, Kotlin 2.0.21)

---

## DOMAINS & SERVERS

| Domain | Purpose |
|--------|---------|
| `ec2-13-203-205-116.ap-south-1.compute.amazonaws.com` | API server (FastAPI + SQLite) |
| `server.petsfort.in` | API alias (same server) |
| `petsfort.in` | Landing page (same server) |
| `admin.petsfort.in` | Reverse-proxied to `pets-fort.web.app` |
| `pets-fort.web.app` | Firebase-hosted admin panel + bill viewer |
| `pets-fort-default-rtdb.asia-southeast1.firebasedatabase.app` | Firebase RTDB |
| `pets-fort.firebasestorage.app` | Firebase Storage bucket |

---

## FILE MAP

### Android App (`app/src/main/java/crmapp/petsfort/`)

| File | Lines | Role | Key Responsibility |
|------|-------|------|-------------------|
| `MainActivity.java` | 157 | Activity | Splash → auth check → route by role |
| `LoginActivity.java` | 658 | Activity | Firebase email/password auth, FCM subscribe, app update check |
| `ChooseUserActivity.java` | 482 | Activity | Agent/Admin picks client to proxy as |
| `PrincipalActivity.java` | 566 | Activity | Main hub: DrawerLayout + BottomNav(Home,Cart) + ViewPager |
| `HomeFragmentActivity.java` | 612 | Fragment | Announcements slider, category grid, credits display, shortcuts |
| `Frag1FragmentActivity.java` | 197 | Fragment | 4-col category RecyclerView (embedded in Home) |
| `CartFragmentActivity.java` | 573 | Fragment | Cart list, live pricing via bulk-details, cost breakdown, confirm |
| `SearchActivity.java` | 902 | Activity | Product browse/search with subcategory sidebar filter |
| `ProductviewActivity.java` | 516 | Activity | Product detail, add-to-cart, image gallery launch |
| `ImagesViewActivity.java` | 230 | Activity | Fullscreen zoomable image gallery (ViewPager2 + ZoomableImageView) |
| `OrderreviewActivity.java` | 435 | Activity | Pre-checkout: fresh pricing, credit validation, address, confirm |
| `OrderActivity.java` | 362 | Activity | Order history list, status badges |
| `OrderCartViewActivity.java` | 481 | Activity | Order detail: products read-only, cost breakdown, bill link |
| `ProfileActivity.java` | 160 | Activity | User info display (name, email, address, credits) |
| `CategoryActivity.java` | 187 | Activity | Standalone category list (unused/alternate) |
| `FragmentWrapper.java` | 78 | Activity | Hosts CartFragmentActivity standalone (from SearchActivity cart icon) |
| `FileUtil.java` | 596 | Utility | Sketchware-generated file ops + bitmap ops (mostly unused) |
| `SketchwareUtil.java` | 208 | Utility | Sketchware-generated: custom toasts, sort, connectivity check |
| `RequestNetwork.java` | 56 | Utility | Sketchware HTTP wrapper (unused — app uses Business.java OkHttp) |
| `RequestNetworkController.java` | 220 | Utility | Sketchware HTTP executor with trust-all SSL (unused directly) |

### Layouts (`app/src/main/java/crmapp/petsfort/layouts/`)

| File | Lines | Role |
|------|-------|------|
| `ZoomableImageView.java` | 240 | Custom ImageView with pinch-zoom + pan via Matrix transforms |
| `LinearBg.java` | 142 | Custom LinearLayout with animated gradient background (30s cycle through 10 white-gray gradients) |

### Business Logic (`app/src/main/java/crmapp/petsfort/JLogics/`)

| File | Lines | Role |
|------|-------|------|
| `Business.java` | 1250 | **Central API + data layer** — all HTTP calls, cart/proxy management, SSL config |
| `JHelpers.java` | 184 | Utility: capitalize, UTC→IST, rupee formatting, delay runner, ValueAnimator, TransitionManager |
| `Callbacker.java` | 64 | Callback interfaces: `ApiResponseWaiters.*`, `Timer`, `onAnimateUpdate`, `Auth` |
| `AppVersionManager.java` | 61 | Per-version login tracker, app update alert freeze flag |

### Models (`app/src/main/java/crmapp/petsfort/JLogics/Models/`)

| File | Lines | Fields |
|------|-------|--------|
| `User.java` | ~80 | uid, id, name, email, role, address, credits, creditse, isBlocked, contact, gstin + `resolveRoleToString()` |
| `Product.java` | ~100 | productId, productName, productCid, productHsn, productDesc, productImg(List), catId, catSub, costRate, costMrp, costGst, costDis, stock, id |
| `CartProduct.java` | ~30 | extends Product + `productCount`(Long), `actualCost`(Double) |
| `Category.java` | ~30 | id, name, image |
| `SubCategory.java` | ~30 | id, name, image, parentid |

### FCM (`app/src/main/java/crmapp/petsfort/JFcm/`)

| File | Lines | Role |
|------|-------|------|
| `MyFirebaseMessagingService.java` | ~35 | `onMessageReceived` → delegates to `NotificationHelper` |
| `NotificationHelper.java` | ~40 | Channel "Action Infos" (HIGH importance), builds simple notification |

### Backend Server (`CRMServers/Server/fly/server/`)

| File | Lines | Role |
|------|-------|------|
| `main.py` | 4069 | **FastAPI app**: all API endpoints, DB ops, FCM calls, invoice gen, analytics, GST module, system stats, WebSocket terminal |
| `firebaseAuth.py` | 105 | Firebase Admin SDK: init, create/delete/update user, storage upload, FCM topic send |
| `dbbackup.py` | 206 | SQLite↔Firebase RTDB backup/restore |

---

## ROLE SYSTEM

| Code | Name | Routing | Capabilities |
|------|------|---------|-------------|
| `"1"` | Client | → `PrincipalActivity` | Browse, cart, order, profile |
| `"2"` | Agent | → `ChooseUserActivity` → picks client → `PrincipalActivity` | Everything Client + proxy into any Client account, view bills |
| `"3"` | Viewer | → `PrincipalActivity` | Same as Client |
| `"4"` | Admin | → `ChooseUserActivity` → picks client → `PrincipalActivity` | Same as Agent |

**Proxy system**: Agent/Admin selects a Client from user list. The selected Client's UID is stored in SharedPreferences key `"Proxy"`. All subsequent data ops (cart, orders, profile) use `Business.localDB_SharedPref.getProxyUID()` which returns proxy UID if set, else real user UID.

---

## NAVIGATION GRAPH

```
MainActivity (splash)
├── [no auth] → LoginActivity
├── [role 2/4] → ChooseUserActivity → PrincipalActivity
└── [role 1/3] → PrincipalActivity

PrincipalActivity
├── BottomNav Tab 0 → HomeFragmentActivity
│   ├── Announcement slider (Firebase RTDB: datas/announcement/all)
│   ├── Category grid (Frag1FragmentActivity) → tap → SearchActivity(category=X)
│   ├── Credits display (GET /user/{uid})
│   ├── "View Orders" → OrderActivity
│   └── "Profile" → ProfileActivity
├── BottomNav Tab 1 → CartFragmentActivity
│   └── "Confirm Order" → OrderreviewActivity
│       └── Success → OrderActivity(orderid=X) or MainActivity
├── Toolbar search icon → SearchActivity (no category filter)
│   ├── Product tap → ProductviewActivity
│   │   └── Image tap → ImagesViewActivity
│   └── Cart icon → FragmentWrapper(CartFragmentActivity)
└── Drawer menu
    ├── Orders → OrderActivity
    │   └── Order tap → OrderCartViewActivity
    │       └── "View Bill" (Agent/Admin) → browser: pets-fort.web.app/bill.html?orderid=X
    ├── Profile → ProfileActivity
    └── Logout → clear cart + FCM unsubscribe + Firebase signOut
```

---

## DATABASE SCHEMA (SQLite on server)

### products
```sql
id TEXT PRIMARY KEY,
product_id TEXT UNIQUE,
product_name TEXT NOT NULL,
product_desc TEXT,
product_hsn TEXT DEFAULT '',
product_cid TEXT DEFAULT '',
product_img TEXT,           -- JSON array of URL strings
cat_id TEXT,
cat_sub TEXT,               -- subcategory id (single, not comma-separated despite comment)
cost_rate REAL,             -- COMPUTED on read: cost_mrp - (cost_mrp * cost_dis / 100)
cost_mrp REAL,
cost_gst REAL,              -- GST percentage (e.g., 18.0)
cost_dis REAL,              -- Discount percentage (e.g., 10.0)
stock INTEGER,
created_at TIMESTAMP,
updated_at TIMESTAMP
```

**CRITICAL**: `cost_rate` is NOT stored in DB. It's computed by `dict_to_product()`:
```python
row_dict['cost_rate'] = row_dict['cost_mrp'] - (row_dict['cost_mrp'] * row_dict['cost_dis'] / 100)
```

### orders
```sql
order_id TEXT PRIMARY KEY,   -- Generated: YYMMDD + 2 random letters + 2 random digits
user_id TEXT NOT NULL,       -- Firebase UID
items TEXT NOT NULL,         -- JSON: {"product_id": {"count": N}, ...}
items_detail TEXT NOT NULL,  -- JSON: full product snapshots with count at order time
order_status TEXT NOT NULL,  -- ORDER_PENDING | ORDER_IN_PROGRESS | ORDER_DELIVERED | ORDER_CANCELLED
total_rate REAL,
total_gst REAL,
total_discount REAL,
total REAL,                  -- Grand total (rate + GST)
created_at TEXT,             -- UTC ISO format + "Z"
address TEXT,
notes TEXT
```

### userdata
```sql
id TEXT PRIMARY KEY,
uid TEXT NOT NULL,           -- Firebase Auth UID
name TEXT NOT NULL,
contact TEXT DEFAULT 'N/A',
gstin TEXT DEFAULT 'N/A',
email TEXT NOT NULL,
role TEXT NOT NULL,           -- "1","2","3","4"
address TEXT NOT NULL,
credits REAL,                -- Prepaid balance (₹), deducted on order
creditse TEXT NOT NULL,      -- Credit expiry date (dd-MM-yyyy format)
isblocked INTEGER DEFAULT 0  -- 0=active, 1=blocked
```

### category
```sql
id TEXT PRIMARY KEY,
name TEXT NOT NULL,
image TEXT DEFAULT ''
```

### subcategory
```sql
id TEXT PRIMARY KEY,
parentid TEXT NOT NULL,      -- References category.id
name TEXT NOT NULL,
image TEXT DEFAULT ''
```

### bills
```sql
order_id TEXT PRIMARY KEY,
bill TEXT NOT NULL            -- JSON: full invoice data (company, details, items, totals, gstDetails, amountsInWords)
```

### credit_notes
```sql
cn_id TEXT PRIMARY KEY,
cn_number TEXT NOT NULL,     -- Format: CN/FY/NNN (e.g., CN/2025-26/001)
original_invoice TEXT DEFAULT '',
user_id TEXT NOT NULL,
user_name TEXT DEFAULT '',
user_gstin TEXT DEFAULT '',
reason TEXT DEFAULT '',
items TEXT NOT NULL,          -- JSON array
subtotal REAL DEFAULT 0,
cgst_total REAL DEFAULT 0,
sgst_total REAL DEFAULT 0,
total REAL DEFAULT 0,
created_at TEXT,
notes TEXT DEFAULT ''
```

### debit_notes
Same structure as credit_notes but with `dn_id`, `dn_number` (DN/FY/NNN).

---

## API ENDPOINTS (used by Android app)

### Product APIs
| Method | Path | Request Body | Response | Used By |
|--------|------|-------------|----------|---------|
| POST | `/products/query` | `QueryRequest` (filters, limit, offset, order_by, order_direction) | `List[ProductResponse]` | SearchActivity |
| POST | `/products/bulk-details` | `{"product_id": {"count": N}, ...}` | `{product_details: [...], cost: {total_mrp, total_rate, total_gst, total_discount, total}}` | CartFragmentActivity, OrderreviewActivity |

### Order APIs
| Method | Path | Request Body | Response | Used By |
|--------|------|-------------|----------|---------|
| POST | `/orders/checkout/{user_id}` | `{"product_id": {"count": N}, ..., "otherData": {"address": "...", "notes": "..."}}` | `{order_id, order_status, total}` or `{message: "OutOfStock", ...}` | OrderreviewActivity |
| POST | `/orders/query` | `QueryRequest` with filter `user_id eq uid` | `List[OrderQueryResponse]` (includes `user_name` JOIN) | OrderActivity |

### User APIs
| Method | Path | Response | Used By |
|--------|------|----------|---------|
| GET | `/user/{uid}` | `UserData` | LoginActivity, HomeFragment, OrderreviewActivity, ProfileActivity |
| GET | `/userdata` | `List[UserData]` | ChooseUserActivity |

### Category APIs
| Method | Path | Response | Used By |
|--------|------|----------|---------|
| GET | `/categories` | `List[Category]` | Frag1FragmentActivity, ProductviewActivity |
| GET | `/subcats/{category_id}` | `List[Subcategory]` (filtered: only subcats with products, with image fallback) | SearchActivity |

### Bill API
| Method | Path | Response | Used By |
|--------|------|----------|---------|
| GET | `/bills/{order_id}` | JSON invoice data | Admin panel (bill.html) |

---

## API ENDPOINTS (admin/server only — not used by app)

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/products/` | Create product |
| GET | `/products/` | List all products |
| GET | `/products/{id}` | Get single product |
| PUT | `/products/{id}` | Update product |
| DELETE | `/products/{id}` | Delete product |
| POST | `/schema/add-columns` | Add columns to products table |
| POST | `/schema/remove-columns` | Remove columns (table recreation) |
| GET | `/schema` | Get table schemas |
| PUT | `/orders/{order_id}` | Update order status (triggers FCM) |
| DELETE | `/orders/{order_id}` | Delete order |
| POST | `/userdata` | Create user (Firebase Auth + DB) |
| PUT | `/userdata/{uid}` | Update user (+ optional password change) |
| DELETE | `/userdata/{uid}` | Delete user (Firebase Auth + DB) |
| POST | `/categories` | Add/replace category |
| DELETE | `/categories/{id}` | Delete category |
| POST | `/subcategories` | Add/replace subcategory |
| DELETE | `/subcategories/{id}` | Delete subcategory |
| GET | `/backup` | Backup DB to Firebase Storage + RTDB |
| GET | `/restore/{path}` | Restore DB from Firebase RTDB |
| GET | `/database` | DB admin HTML page |
| GET | `/api/tables` | List all tables |
| GET | `/api/table/{name}/info` | Table schema info |
| GET | `/api/table/{name}/data` | All rows from table |
| POST | `/api/table/{name}/row` | Insert row |
| PUT | `/api/table/{name}/row/{pk}` | Update row |
| DELETE | `/api/table/{name}/row/{pk}` | Delete row |
| GET | `/analytics` | Analytics HTML dashboard |
| GET | `/analytics/summary` | Analytics data JSON |
| GET | `/system-stats/live` | SSE stream of system stats |
| GET | `/system-stats/snapshot` | One-time system stats |
| GET/POST/DELETE | `/gst/*` | GST module: dashboard, sales-register, credit-notes, debit-notes, party-ledger, day-book, profit-loss, stock-summary, outstanding, tax-ledger, dashboard-extras |
| WebSocket | `/ws/terminal` | Remote terminal (PTY-based) |

---

## QUERY FILTER SYSTEM

Used by both `/products/query` and `/orders/query`:

```json
{
  "filters": [
    {"field": "cat_id", "operator": "eq", "value": "abc123"},
    {"field": "product_name", "operator": "contains", "value": "dog"}
  ],
  "limit": 1000,
  "offset": 0,
  "order_by": "product_name",
  "order_direction": "ASC"
}
```

**Operators**: `eq`, `neq`, `gt`, `lt`, `gte`, `lte`, `contains`, `startswith`, `endswith`, `in`

**IMPORTANT filter logic**: First filter = WHERE. Remaining filters = OR'd within AND:
```sql
WHERE filter[0] AND (filter[1] OR filter[2] OR ...)
```

---

## PRICING/COST CALCULATION

### Server-side formula
```
cost_rate = cost_mrp - (cost_mrp * cost_dis / 100)
gst_amount = (cost_rate * cost_gst) / 100
actual_rate = cost_rate + gst_amount       -- per unit with GST
disc_amount = cost_mrp * cost_dis / 100    -- discount per unit
total = actual_rate * count                -- line total
```

### Totals in order/cart
```
total_mrp = Σ(cost_mrp * count)
total_rate = Σ(cost_rate * count)
total_gst = Σ(gst_amount * count)
total_discount = Σ(disc_amount * count)
total = Σ(actual_rate * count)             -- this is what gets deducted from credits
```

### Client-side display (CartFragmentActivity)
```
Subtotal = total_rate    (labeled "Subtotal")
GST      = total_gst     (labeled "GST Total")
MRP      = total_mrp     (labeled "Total (No Discount)")
Discount = total_discount (labeled "Discount")
Grand    = total          (labeled "Grand Total")
```

---

## ORDER CHECKOUT FLOW (server-side)

1. Receive `{product_id: {count: N}, ..., otherData: {address, notes}}`
2. **Acquire `order_lock`** (asyncio.Lock — serialized checkout)
3. For each product:
   a. Fetch from DB by id or product_id
   b. Check `stock >= requested_count`, if not → return `{message: "OutOfStock", ...}`
   c. Compute cost_rate from cost_mrp and cost_dis (NOT from stored cost_rate)
   d. Accumulate totals
4. Validate user exists and not blocked
5. **Deduct stock** for all products
6. **Deduct credits**: `UPDATE userdata SET credits=credits-{total}`
7. **Insert order** with status `ORDER_PENDING`
8. **Generate invoice** → insert into `bills` table
9. **Send FCM**: notify user + notify `order_checkout` topic (agents/admins)
10. Return `{order_id, order_status, total}`

---

## LOCAL STORAGE (SharedPreferences)

### `logindata` preferences
| Key | Value | Set By |
|-----|-------|--------|
| `email` | User email | LoginActivity |
| `uid` | Firebase UID | LoginActivity |
| `name` | Display name | LoginActivity |
| `role` | Role code ("1"/"2"/"3"/"4") | LoginActivity |
| `Proxy` | Proxy client UID | ChooseUserActivity |

### `localDB` preferences
| Key | Value |
|-----|-------|
| `localDB` | JSON: `{"carts": {"product_id": {"count": N}, ...}}` |

### `app_prefs` preferences
| Key | Value |
|-----|-------|
| `{versionName}` | `"logged"` (marks login for this version) |
| `{versionName}_alertFreeze` | `"true"` (update alert shown) |

### Cart operations (`Business.localDB_SharedPref`)
- `getCart()` → returns `HashMap<String,Object>` of carts sub-map
- `updateCartProduct(localDB, productId, map)` → sets `carts.{productId}` = map
- `deleteCartProduct(localDB, productId)` → removes `carts.{productId}`
- `clearCart(localDB)` → removes entire `carts` key
- `setProxyUID(localDB, uid)` → sets `Proxy` in logindata
- `getProxyUID()` → returns Proxy if set, else real UID

### Cart cleared on:
- Successful order checkout
- Logout (any mode)
- Switching proxy user (Agent/Admin)

---

## FCM TOPICS

| Topic | Subscribed By | Trigger |
|-------|--------------|---------|
| `all_users` | All users on login | Broadcast notifications |
| `role_{roleName}` | All users on login | Role-specific notifications |
| `user_{uid}` | All users on login | Order status changes, credit updates |
| `order_checkout` | Agent/Admin only | New order placed by any user |

### FCM notifications sent by server:
| Function | Topic | Title | Body |
|----------|-------|-------|------|
| `FCM_notify_order_checkout` | `user_{uid}` | "Order Made" | "Thank you for making order..." |
| `FCM_notify_order_checkout` | `order_checkout` | "New Order" | "{user_name} made order of Rs.{total}" |
| `FCM_notify_order_status_change` | `user_{uid}` | "Order Status Updated" | "your order is {status}!" |
| `FCM_notify_user_credits_change` | `user_{uid}` | "Credits Updated" | "Hi, {msg}!" |

---

## FIREBASE RTDB STRUCTURE

```
datas/
  announcement/
    all/           -- Array of announcement objects
      [0]/
        image: "url"
        ...
```

Only used for: Home screen image slider in `HomeFragmentActivity`.

---

## SSL CONFIGURATION

**Both `Business.java` and `RequestNetworkController.java` use trust-all SSL:**
```java
final TrustManager[] trustAllCerts = new TrustManager[]{
    new X509TrustManager() {
        public void checkClientTrusted(...) {} // no-op
        public void checkServerTrusted(...) {} // no-op
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[]{}; }
    }
};
SSLContext sslContext = SSLContext.getInstance("TLS");
sslContext.init(null, trustAllCerts, new SecureRandom());
builder.hostnameVerifier((hostname, session) -> true);
```

---

## BUSINESS.JAVA INNER CLASSES

| Inner Class | Methods | Purpose |
|-------------|---------|---------|
| `JFCM` | `subscribeTopic(topic)`, `unsubscribeTopic(topic)` | Firebase Messaging topic management |
| `localDB_SharedPref` | `getCart()`, `updateCartProduct()`, `deleteCartProduct()`, `clearCart()`, `setProxyUID()`, `getProxyUID()`, `readLocalDB()` | SharedPreferences operations |
| `UserDataApiClient` | `getUserDataCallApi(uid, cb)`, `getAllUsersCallApi(cb)`, `updateUserDataCallApi(uid, body, cb)` | `/user/*` and `/userdata/*` endpoints |
| `CategoriesApiClient` | `getCategoriesCallApi(cb)` | `GET /categories` |
| `SubCategoriesApiClient` | `getSubCategoriesCallApi(catId, cb)` | `GET /subcats/{catId}` |
| `QueryApiClient` | `getQueryCallApi(body, cb)` | `POST /products/query` |
| `BulkDetailsApiClient` | `getBulkDetailsCallApi(body, cb)` + inner `CostDetails` class | `POST /products/bulk-details` |
| `OrderCheckoutApiClient` | `checkoutCallApi(userId, body, cb)` | `POST /orders/checkout/{userId}` |
| `OrderQueryApiClient` | `getOrderQueryCallApi(body, cb)` + inner `Order` class | `POST /orders/query` |

**Server URL constant**: `public static String serverURL = "https://ec2-13-203-205-116.ap-south-1.compute.amazonaws.com";`

**OkHttp config**: 60s connect/read/write timeout, trust-all SSL, runs callbacks on `Activity.runOnUiThread`.

---

## ORDER STATUS ENUM (client-side)

```java
public enum JOrderStatus {
    PENDING("ORDER_PENDING"),
    IN_PROGRESS("ORDER_IN_PROGRESS"),
    DELIVERED("ORDER_DELIVERED"),
    CANCELLED("ORDER_CANCELLED");
}
```

Status badge colors: PENDING=yellow, IN_PROGRESS=blue, DELIVERED=green, CANCELLED=red.

---

## INVOICE GENERATION (server-side)

On checkout, `generate_invoice_data()` creates a structured JSON bill stored in `bills` table:

```json
{
  "company": { "name": "Petsfort", "address": "...", "gstNo": "...", "email": "petsfort.in@gamil.com" },
  "details": { "invoiceNo": "INV-{order_id}", "dated": "YYYY-MM-DD", ... },
  "consignee": { "name": "...", "address": "..." },
  "buyer": { "name": "...", "address": "...", "contactNo": "...", "gstin": "..." },
  "items": [{ "sNo": 1, "description": "...", "hsnSac": "...", "rate": 0.0, "amount": 0.0, ... }],
  "totals": { "subTotal": 0.0, "cgstAmount": 0.0, "sgstAmount": 0.0, "total": 0.0 },
  "gstDetails": [{ "hsnSac": "...", "taxableValue": 0.0, "cgstRate": "9%", ... }],
  "amountsInWords": { "amountChargeable": "INR ... Rupees Only", "taxAmount": "INR ... Only" }
}
```

GST split: CGST = cost_gst/2, SGST = cost_gst/2 (assumed intra-state).

---

## DEPLOYMENT

### Fly.io config (`fly.toml`)
- **App**: `jay-fastapi`
- **Region**: `sin` (Singapore)
- **VM**: `shared-cpu-1x`
- **DB mount**: `/app/sqlite` (persistent volume)
- **Port**: 8080, force HTTPS
- **Auto-stop/start**: enabled

### Server startup
1. If `products.db` doesn't exist → restore from Firebase RTDB (`tables/latest`)
2. `init_db()` creates all tables if not exist
3. Uvicorn serves on port 8080

---

## CORS ORIGINS
```python
["https://pets-fort.web.app", "https://petsfort.in", "https://server.petsfort.in",
 "http://localhost:5500", "https://ec2-13-203-205-116.ap-south-1.compute.amazonaws.com"]
```

---

## KNOWN QUIRKS / TECHNICAL DEBT

1. **Trust-all SSL**: Both app and server accept any certificate — security risk
2. **Cart polling**: CartFragmentActivity polls local cart every 100ms via `Timer` — wasteful
3. **No signup in app**: Create-account UI exists in LoginActivity but account creation is server/admin only via `POST /userdata`
4. **Sketchware legacy**: FileUtil.java, SketchwareUtil.java, RequestNetwork.java, RequestNetworkController.java are Sketchware-generated boilerplate; most methods are unused or deprecated
5. **Category hardcode**: Server hardcodes category `cc41f1da652f4` to always be first in list
6. **Concurrent checkout**: Server uses `asyncio.Lock()` to serialize all checkouts — potential bottleneck
7. **cost_rate computed not stored**: `dict_to_product()` always recomputes cost_rate; the DB column exists but the stored value is overwritten on read
8. **Credit expiry format**: `creditse` field uses `dd-MM-yyyy` string, parsed with `SimpleDateFormat` on client
9. **Order query filter logic**: First filter is AND'd, rest are OR'd — unintuitive SQL building
10. **WebSocket terminal**: Server exposes a full PTY shell via `/ws/terminal` — major security concern
11. **Admin contact hardcoded**: Phone number `7092552211` hardcoded in OrderreviewActivity for support
12. **In-app update**: Uses IMMEDIATE type — forces user to update before using app
13. **App version login**: AppVersionManager forces re-login on every app version change
14. **Firebase secrets in XML**: API keys and project IDs are in `res/values/secrets.xml` (committed to repo)
15. **Company email typo**: Invoice company email is `"petsfort.in@gamil.com"` (typo: "gamil" instead of "gmail")
16. **Bill viewer**: Agent/Admin "View Bill" opens `https://pets-fort.web.app/bill.html?orderid={id}` in browser

---

## ADMIN PANEL (`CRMServers/PetsFortAdminPanel/`)

Firebase-hosted at `pets-fort.web.app`. Contains:
- `bill.html` — Bill/invoice viewer (fetches from `/bills/{order_id}`)
- `index.html` — Admin panel entry
- `modules/mod0-mod9/` — 10 admin modules (each with `run.html` + `script.js`)
- `functions/main.py` — Firebase Functions backend

---

## RESOURCE FILES

### Fonts (in assets)
- `fonts/salesbold.ttf` — Bold text (product names, discounts)
- `fonts/sailes.ttf` — Regular text (prices, descriptions)

### Lottie animations (in res/raw)
- `lottie_cat_loading.json` — Loading spinner
- `lottie_cat_visit.json` — Home screen decoration
- `lottie_no_data.json` — Empty state
- `lottie_add_to_cart.json` — Cart animation
- `lottie_thug_dog.json` — Decorative
- `lottie2.json`, `lottie3.json` — Other animations

### Color scheme
- Primary: `#E8E8E8` (light gray)
- PrimaryDark: `#F0B493` (peach)
- Accent: `#008DCD` (blue)
- Theme: `AppCompat.Light.NoActionBar`

---

## KEY CONSTANTS

```java
// Business.java
static String serverURL = "https://ec2-13-203-205-116.ap-south-1.compute.amazonaws.com";
static int CONNECT_TIMEOUT = 60; // seconds
static int READ_TIMEOUT = 60;
static int WRITE_TIMEOUT = 60;

// ImagesViewActivity.java
static String EXTRA_IMAGE_URLS = "image_urls";
static String EXTRA_INITIAL_POSITION = "initial_position";

// AppVersionManager.java
// Uses PackageManager to get versionName at runtime
```

```python
# main.py
DB_PATH = "/app/sqlite/products.db"
LOW_STOCK_THRESHOLD = 5
# CORS origins listed above
```
