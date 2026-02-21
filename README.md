# PetsFort CRM - Complete Application Documentation

## Table of Contents
1. [Overview](#overview)
2. [Tech Stack](#tech-stack)
3. [Architecture](#architecture)
4. [User Roles](#user-roles)
5. [Application Flow](#application-flow)
6. [Screen-by-Screen Breakdown](#screen-by-screen-breakdown)
7. [Data Models](#data-models)
8. [API Layer (Business.java)](#api-layer)
9. [Backend Server](#backend-server)
10. [Local Storage](#local-storage)
11. [Push Notifications (FCM)](#push-notifications)
12. [Key Utilities](#key-utilities)

---

## Overview

**PetsFort CRM** (`crmapp.petsfort`) is an Android B2B CRM application for a pet supplies business ("PetsFort"). It allows **clients** (retailers/buyers) to browse products, add them to a cart, and place orders using a **prepaid credit system**. **Admins/Agents** can act on behalf of any client via a proxy-user system and view/generate bills.

- **Package**: `crmapp.petsfort`
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35
- **Version**: 1.17 (versionCode 17)

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Java 17 |
| **UI** | Android Views (XML layouts), Material Design, Lottie animations, Shimmer loading |
| **Auth** | Firebase Authentication (Email/Password) |
| **Realtime Data** | Firebase Realtime Database (announcements only) |
| **Networking** | OkHttp3 (REST API calls to custom backend) |
| **Images** | Glide |
| **JSON** | Gson + org.json |
| **Push Notifications** | Firebase Cloud Messaging (FCM) with topic-based subscriptions |
| **In-App Updates** | Google Play In-App Updates (IMMEDIATE type) |
| **Backend Server** | Python FastAPI + SQLite (hosted on AWS EC2 / Fly.io) |
| **Admin Panel** | Firebase hosted web app (`pets-fort.web.app`) |

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                    Android App (Client)                  │
│                                                          │
│  Activities/Fragments ──► Business.java (API Layer)      │
│        │                        │                        │
│        ▼                        ▼                        │
│  SharedPreferences         OkHttp3 REST calls            │
│  (Cart, Login, Proxy)      (to FastAPI backend)          │
│        │                        │                        │
│        ▼                        ▼                        │
│  Local DB (localDB)      AWS EC2 Server                  │
│                          (FastAPI + SQLite)               │
│                                                          │
│  Firebase Auth ◄──────► Firebase Auth                    │
│  Firebase RTDB ◄──────► Announcements (datas/announcement)│
│  Firebase FCM  ◄──────► Topic-based notifications        │
└──────────────────────────────────────────────────────────┘
```

---

## User Roles

Roles are stored as string numbers in the `role` field of the User model:

| Role Code | Role Name | Capabilities |
|-----------|-----------|-------------|
| `"1"` | **Client** | Browse products, manage cart, place orders, view own orders & profile |
| `"2"` | **Agent** | All Client capabilities + proxy into any Client account, view bills |
| `"3"` | **Viewer** | Same flow as Client (view-only implied) |
| `"4"` | **Admin** | Same as Agent — proxy into clients, view bills, manage orders |

---

## Application Flow

### Startup Flow (MainActivity)
```
App Launch
    │
    ▼
MainActivity (Splash Screen)
    │
    ├── FirebaseAuth user exists AND role saved in SharedPreferences?
    │       │
    │       ├── role == "2" or "4" (Agent/Admin)
    │       │       └── ChooseUserActivity (select client to proxy as)
    │       │
    │       └── role == "1" or "3" (Client/Viewer)
    │               └── PrincipalActivity (main app)
    │
    └── No user logged in
            └── LoginActivity
```

### Login Flow (LoginActivity)
```
LoginActivity
    │
    ├── Email + Password sign-in via Firebase Auth
    │
    ├── On success:
    │       ├── Call GET /user/{uid} to fetch user data from server
    │       ├── Check if user is blocked (isBlocked == 1) → sign out + reject
    │       ├── Subscribe to FCM topics:
    │       │       ├── "all_users"
    │       │       ├── "role_{roleName}"
    │       │       ├── "user_{uid}"
    │       │       └── "order_checkout" (Agents/Admins only)
    │       ├── Save name & role to SharedPreferences ("logindata")
    │       └── Navigate to MainActivity → restart flow
    │
    ├── App update check (Google Play In-App Updates — IMMEDIATE)
    │       └── If update available → freeze UI with non-cancelable dialog
    │
    └── Notification permission request (Android 13+)
```

### Agent/Admin Proxy Flow (ChooseUserActivity)
```
ChooseUserActivity
    │
    ├── Fetch ALL users from GET /userdata/
    ├── Filter to show only role "1" (Client) users
    ├── Searchable list (by name, email, uid)
    │
    └── On user select:
            ├── Save proxy UID to SharedPreferences ("logindata" → "Proxy" key)
            ├── Clear local cart
            └── Navigate to PrincipalActivity (operating as that client)
```

### Main App Flow (PrincipalActivity)
```
PrincipalActivity
    │
    ├── Toolbar with drawer (right-side) + bottom navigation (2 tabs)
    │
    ├── Tab 0: HomeFragmentActivity
    │       ├── Image slider (announcements from Firebase RTDB)
    │       ├── Category grid (from GET /categories API)
    │       │       └── Tap category → SearchActivity (filtered)
    │       ├── Credit balance display (animated counter)
    │       │       └── Fetched from GET /user/{uid}
    │       ├── View Orders card → OrderActivity
    │       └── View Profile → ProfileActivity
    │
    ├── Tab 1: CartFragmentActivity
    │       ├── Cart items from local SharedPreferences
    │       ├── Calls POST /products/bulk-details to get live pricing
    │       ├── Shows: Subtotal, GST, MRP, Discount, Grand Total
    │       ├── +/- quantity controls per item
    │       ├── Confirm Order → OrderreviewActivity
    │       └── Expandable bottom cost panel (drag animation)
    │
    ├── Search icon → SearchActivity (global)
    │
    └── Drawer Menu:
            ├── User name display
            ├── Home
            ├── Orders → OrderActivity
            ├── Profile → ProfileActivity
            ├── App version display
            └── Logout:
                    ├── Proxy mode → clear cart + finish (back to ChooseUserActivity)
                    └── Normal mode → unsubscribe FCM + clear cart + sign out + MainActivity
```

### Search & Product Flow
```
SearchActivity
    │
    ├── If opened with "category" extra:
    │       ├── Left sidebar: SubCategory list (from GET /subcats/{cat_id})
    │       └── Products filtered by category + optional subcategory
    │
    ├── If opened without category:
    │       └── Shows ALL products (global search)
    │
    ├── Search bar: filters by product_name, product_hsn, product_cid
    │
    ├── API: POST /products/query with filters, limit=1000, order_by=product_name ASC
    │
    ├── Product grid (2 columns):
    │       ├── Shows: image, name, code, MRP, rate, GST%, discount%
    │       ├── "Add to Cart" button → reveals +/- counter
    │       └── Tap product → ProductviewActivity
    │
    └── Cart icon → FragmentWrapper (CartFragmentActivity in standalone mode)


ProductviewActivity
    │
    ├── Full product details: image, name, MRP, rate, GST, discount, description
    ├── Stock status ("Hurry up! Last few left" if < 10)
    ├── Quantity input with +/- buttons
    ├── Tap image → ImagesViewActivity (full-screen zoomable gallery)
    ├── Cart persisted to local SharedPreferences
    └── If opened in "view-only" mode (from order detail): controls disabled
```

### Order Flow
```
CartFragmentActivity → "Confirm Order"
    │
    ▼
OrderreviewActivity
    │
    ├── Fetches fresh pricing via POST /products/bulk-details
    ├── Fetches user credits via GET /user/{uid}
    ├── Shows cost breakdown: Subtotal, GST, Total (no discount), Discount, Grand Total
    ├── Shows credit balance
    ├── Address field (pre-filled from user profile, editable)
    ├── Notes field
    │
    ├── Validation:
    │       ├── Credits must be >= order total
    │       ├── Credits must not be expired (checks creditse date field)
    │       └── If insufficient → alert with admin contact (7092552211)
    │
    └── "Confirm Order" button:
            ├── POST /orders/checkout/{userId} with cart + address + notes
            │
            ├── Success (200):
            │       ├── Clear local cart
            │       ├── Alert: "Order Confirmed"
            │       ├── Agent/Admin: "View Bill" button → OrderActivity with orderid
            │       └── "OK" → MainActivity (restart fresh)
            │
            ├── OutOfStock error:
            │       ├── Update local cart with available stock
            │       └── Alert with remaining stock info
            │
            └── Other error:
                    └── Alert with error message


OrderActivity (Order History)
    │
    ├── POST /orders/query with filter: user_id == current userId
    ├── List of orders: OrderId, Status (badge), Date, Items count, Total cost
    ├── If opened with "orderid" extra → auto-navigate to that order detail
    │
    └── Tap order → OrderCartViewActivity


OrderCartViewActivity (Order Detail)
    │
    ├── Order info header: OrderId, Status badge, Date, Items/Cost summary
    ├── Product list with quantities (read-only)
    ├── Cost breakdown panel (expandable)
    ├── Agent/Admin: "View Bill" button → opens web browser to
    │       https://pets-fort.web.app/bill.html?orderid={orderId}
    └── Back button
```

### Order Statuses
| Status | Color | Display Text |
|--------|-------|-------------|
| `ORDER_PENDING` | Yellow | Pending |
| `ORDER_IN_PROGRESS` | Blue | In Progress |
| `ORDER_DELIVERED` | Green | Delivered |
| `ORDER_CANCELLED` | Red | Cancelled |

---

## Screen-by-Screen Breakdown

| Screen | Class | Layout | Purpose |
|--------|-------|--------|---------|
| Splash | `MainActivity` | `main.xml` | App entry, auth check, routing |
| Login | `LoginActivity` | `login.xml` | Email/password auth, signup toggle, password reset |
| Choose Client | `ChooseUserActivity` | `choose_user.xml` | Agent/Admin selects client to proxy |
| Main Hub | `PrincipalActivity` | `principal.xml` | Drawer + BottomNav + ViewPager host |
| Home | `HomeFragmentActivity` | `home_fragment.xml` | Announcements, categories, credits, shortcuts |
| Categories Grid | `Frag1FragmentActivity` | `frag1_fragment.xml` | 4-column category grid (embedded in Home) |
| Cart | `CartFragmentActivity` | `cart_fragment.xml` | Cart items + cost breakdown + confirm |
| Search/Browse | `SearchActivity` | `search.xml` | Product search/filter with subcategory sidebar |
| Product Detail | `ProductviewActivity` | `productview.xml` | Full product view with add-to-cart |
| Image Gallery | `ImagesViewActivity` | `imagesviewactivity.xml` | Full-screen zoomable product images |
| Order Review | `OrderreviewActivity` | `orderreview.xml` | Pre-checkout: costs, address, confirm |
| Order History | `OrderActivity` | `order.xml` | List of user's orders |
| Order Detail | `OrderCartViewActivity` | `order_cart_view_activity.xml` | Single order products + costs |
| Profile | `ProfileActivity` | `profile.xml` | User info: name, email, address, credits |
| Fragment Host | `FragmentWrapper` | `fragment_wrapper_activity.xml` | Standalone host for CartFragment |
| Category List | `CategoryActivity` | `categories.xml` | Unused/standalone category list |

---

## Data Models

### User (`JLogics/Models/User.java`)
| Field | Type | Description |
|-------|------|-------------|
| `uid` | String | Firebase Auth UID |
| `id` | String | Server-side DB ID |
| `name` | String | Display name |
| `email` | String | Email address |
| `role` | String | "1"=Client, "2"=Agent, "3"=Viewer, "4"=Admin |
| `address` | String | Delivery address |
| `credits` | double | Prepaid balance (₹) |
| `creditse` | String | Credit expiry date ("dd-MM-yyyy") |
| `isBlocked` | int | 0=active, 1=blocked |

### Product (`JLogics/Models/Product.java`)
| Field | Type | Description |
|-------|------|-------------|
| `productId` | String | Unique product identifier |
| `productName` | String | Product name |
| `productCid` | String | Product code (custom ID) |
| `productHsn` | String | HSN code |
| `productDesc` | String | Description |
| `productImg` | List\<String\> | Image URLs |
| `catId` | String | Parent category ID |
| `catSub` | String | Subcategory (comma-separated) |
| `costRate` | double | Selling rate (calculated: MRP - discount) |
| `costMrp` | double | Maximum retail price |
| `costGst` | double | GST percentage |
| `costDis` | double | Discount percentage |
| `stock` | int | Available stock quantity |
| `id` | String | Server-side DB ID |

### CartProduct (`JLogics/Models/CartProduct.java`)
Extends `Product` with:
| Field | Type | Description |
|-------|------|-------------|
| `productCount` | Long | Quantity in cart |
| `actualCost` | Double | (Unused currently) |

### Category / SubCategory
| Field | Type |
|-------|------|
| `id` | String |
| `name` | String |
| `image` | String (URL) |
| `parentid` | String (SubCategory only) |

### Order (Inner class in `Business.OrderQueryApiClient`)
| Field | Type | Description |
|-------|------|-------------|
| `orderId` | String | Auto-generated order ID (e.g., "250121AB42") |
| `userId` | String | Firebase UID |
| `items` | Map | `{productId: {count: N}}` |
| `itemsDetail` | List\<Product\> | Full product details per item |
| `orderStatus` | String | ORDER_PENDING / IN_PROGRESS / DELIVERED / CANCELLED |
| `totalRate` | double | Sum of rates |
| `totalGst` | double | Sum of GST |
| `totalDiscount` | double | Sum of discounts |
| `total` | double | Final total |
| `createdAt` | String | UTC timestamp (converted to IST for display) |

---

## API Layer

All API calls are centralized in `Business.java` (`JLogics/Business.java`). The server URL is:
```
https://ec2-13-203-205-116.ap-south-1.compute.amazonaws.com
```

**SSL Note**: The app uses a trust-all SSL configuration (accepts all certificates) via a custom `OkHttpClient`.

### API Endpoints Used by the App

| Method | Endpoint | Purpose | Client Class |
|--------|----------|---------|-------------|
| GET | `/user/{uid}` | Get single user data | `UserDataApiClient` |
| GET | `/userdata/` | Get all users | `UserDataApiClient` |
| PUT | `/userdata/{uid}` | Update user data | `UserDataApiClient` |
| GET | `/categories` | Get all categories | `CategoriesApiClient` |
| GET | `/subcats/{category_id}` | Get subcategories | `SubCategoriesApiClient` |
| POST | `/products/query` | Search/filter products | `QueryApiClient` |
| POST | `/products/bulk-details` | Get details + cost for cart items | `BulkDetailsApiClient` |
| POST | `/orders/checkout/{userId}` | Place an order | `OrderCheckoutApiClient` |
| POST | `/orders/query` | Query order history | `OrderQueryApiClient` |

### Query API Request Format
```json
{
  "filters": [
    {"field": "cat_id", "operator": "eq", "value": "cat_123"},
    {"field": "product_name", "operator": "contains", "value": "dog food"}
  ],
  "limit": 1000,
  "offset": 0,
  "order_by": "product_name",
  "order_direction": "ASC"
}
```

Filter operators: `eq`, `neq`, `gt`, `lt`, `gte`, `lte`, `contains`, `startswith`, `endswith`, `in`

**Note**: The first filter is used as the AND condition, remaining filters are OR'd together.

### Bulk Details Request Format (Cart)
```json
{
  "product_id_1": {"count": 2},
  "product_id_2": {"count": 5}
}
```

### Order Checkout Response
```json
{
  "order_id": "250121AB42",
  "order_status": "ORDER_PENDING",
  "message": "stored"
}
```
Or on out-of-stock:
```json
{
  "message": "OutOfStock",
  "product_available_stock": 3,
  "product_id": "...",
  "product_name": "..."
}
```

---

## Backend Server

Located at `CRMServers/Server/fly/server/main.py` — a **Python FastAPI** application with **async SQLite** (aiosqlite).

### Database Tables
| Table | Purpose |
|-------|---------|
| `products` | Product catalog |
| `orders` | Order records |
| `category` | Product categories |
| `subcategory` | Product subcategories |
| `userdata` | User profiles & credits |
| `bills` | Generated bill HTML |
| `credit_notes` | Credit note records |
| `debit_notes` | Debit note records |

### Key Server Logic
- **cost_rate** is computed server-side: `cost_rate = cost_mrp - (cost_mrp * cost_dis / 100)`
- **Order checkout** deducts credits from user, validates stock, creates order atomically
- **Bill generation** available at `https://pets-fort.web.app/bill.html?orderid={id}`
- Server runs behind CORS allowing specific origins
- Deployed on **AWS EC2** with Docker (Fly.io config also present)

---

## Local Storage

### SharedPreferences Keys

| Pref Name | Key | Value | Used By |
|-----------|-----|-------|---------|
| `logindata` | `email` | User's email | Login |
| `logindata` | `uid` | Firebase UID | Login |
| `logindata` | `name` | Display name | Login |
| `logindata` | `role` | Role code string | Login, routing |
| `logindata` | `Proxy` | Proxy user's UID (Agent/Admin mode) | Proxy system |
| `localDB` | `localDB` | JSON HashMap (contains `carts` sub-map) | Cart storage |
| `app_prefs` | `{version}` | Login marker per version | AppVersionManager |
| `app_prefs` | `{version}_alertFreeze` | Update alert marker | AppVersionManager |

### Cart Structure (in `localDB`)
```json
{
  "carts": {
    "product_id_1": {"count": 3},
    "product_id_2": {"count": 1}
  }
}
```

Cart is stored **entirely in local SharedPreferences** (not on server). The cart is cleared on:
- Successful order checkout
- Logout
- Switching proxy user (Agent/Admin)

---

## Push Notifications

### FCM Topic Subscriptions (on login)
| Topic | Who Subscribes | Purpose |
|-------|---------------|---------|
| `all_users` | Everyone | Broadcast notifications |
| `role_{roleName}` | Everyone | Role-specific notifications |
| `user_{uid}` | Everyone | User-specific notifications |
| `order_checkout` | Agents & Admins only | Order placement notifications |

### Notification Handling
- `MyFirebaseMessagingService` receives FCM messages
- Shows a simple notification via `NotificationHelper` (channel: "Action Infos", HIGH importance)
- No deep-link handling — notifications are display-only

---

## Key Utilities

### JHelpers.java
- `capitalize(String)` — Title-cases each word
- `convertUtcToIstAndFormat(String)` — UTC → IST datetime string ("dd-MMM-yyyy hh:mm a")
- `formatDoubleToRupeesString(double)` — Indian locale number formatting (e.g., "1,23,456.78")
- `runAfterDelay(Activity, int, Callback)` — Schedule UI callback after delay
- `TransitionManager(View, duration)` — AutoTransition animation on LinearLayout
- `JValueAnimator.animate(start, end, duration, callback)` — ValueAnimator wrapper

### AppVersionManager.java
- Tracks login state per app version (forces re-login on updates)
- Manages "alert freeze" flag for pending Play Store updates
- `getAppVersion()` — Gets current versionName from PackageManager

### Proxy System (Business.localDB_SharedPref)
- Agents/Admins set a "Proxy" UID in SharedPreferences
- `getProxyUID()` returns the proxy UID if set, otherwise the real user's UID
- All data operations (cart, orders, profile) use the proxy UID
- Proxy is cleared on logout from Agent screen

---

## Important Notes

1. **Credit-Based Ordering**: Users must have sufficient prepaid credits to place orders. Credits are managed by admins.
2. **Credit Expiry**: Credits have an expiry date (`creditse`). Expired credits block order placement.
3. **SSL Trust-All**: The app disables SSL certificate validation — this is a security concern for production.
4. **Cart Polling**: `CartFragmentActivity` polls local cart every 100ms to detect changes — this is inefficient.
5. **No Signup Flow**: The signup button exists in the UI but the create-account logic is not wired up (accounts are created server-side/by admin).
6. **App Update Enforcement**: If a Play Store update is available, a non-cancelable dialog is shown, forcing the user to update.
7. **Sketchware Legacy**: The codebase contains deprecated Sketchware-generated methods (showMessage, getLocationX/Y, etc.) that are no longer actively used.

---

I've fully analyzed every flow. Let me know what you'd like to fix!
