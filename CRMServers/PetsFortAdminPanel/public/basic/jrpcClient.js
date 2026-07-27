(function (root) {
    "use strict";

    const RPC = Object.freeze({
        POST_PRODUCTS: 0, POST_PRODUCTS_QUERY: 1, GET_PRODUCTS: 2, GET_PRODUCT: 3,
        PUT_PRODUCT: 4, DELETE_PRODUCT: 5, GET_OFFER_GROUPS: 6, POST_OFFER_GROUP: 7,
        PUT_OFFER_GROUP: 8, POST_OFFER_GROUP_APPLY: 9, POST_OFFER_GROUP_CANCEL: 10,
        DELETE_OFFER_GROUP: 11, POST_SCHEMA_ADD_COLUMNS: 12, POST_SCHEMA_REMOVE_COLUMNS: 13,
        GET_SCHEMA: 14, POST_PRODUCTS_BULK_DETAILS: 15, POST_ORDERS_CHECKOUT: 16,
        GET_BILL: 17, POST_ORDERS_QUERY: 18, PUT_ORDER: 19, DELETE_ORDER: 20,
        GET_CATEGORIES: 21, POST_CATEGORY: 22, DELETE_CATEGORY: 23, GET_SUBCATEGORIES: 24,
        GET_SUBCATEGORIES_V0_BY_CATEGORY: 25, GET_SUBCATEGORIES_BY_CATEGORY: 26,
        POST_SUBCATEGORY: 27, DELETE_SUBCATEGORY: 28, GET_USERDATA: 29, GET_USER: 30,
        POST_USERDATA: 31, PUT_USERDATA: 32, DELETE_USERDATA: 33, GET_DATABASE_TABLES: 34,
        GET_DATABASE_TABLE_INFO: 35, GET_DATABASE_TABLE_DATA: 36, POST_DATABASE_TABLE_ROW: 37,
        PUT_DATABASE_TABLE_ROW: 38, DELETE_DATABASE_TABLE_ROW: 39, GET_ANALYTICS_SUMMARY: 40,
        GET_GST_DASHBOARD: 41, GET_GST_SALES_REGISTER: 42, POST_GST_CREDIT_NOTE: 43,
        GET_GST_CREDIT_NOTES: 44, DELETE_GST_CREDIT_NOTE: 45, POST_GST_DEBIT_NOTE: 46,
        GET_GST_DEBIT_NOTES: 47, DELETE_GST_DEBIT_NOTE: 48, GET_GST_PARTY_LEDGER: 49,
        GET_GST_DAY_BOOK: 50, GET_GST_PROFIT_LOSS: 51, GET_GST_STOCK_SUMMARY: 52,
        GET_GST_OUTSTANDING: 53, GET_GST_TAX_LEDGER: 54, GET_GST_DASHBOARD_EXTRAS: 55,
        GET_BACKUPS: 60, POST_BACKUP: 61, DELETE_BACKUPS_OLDER_THAN_DAYS: 62,
        DELETE_BACKUP: 63, POST_BACKUPS_DELETE_SELECTED: 64,
        POST_BACKUPS_RESET_CURRENT: 65, POST_BACKUP_RESTORE: 66
    });

    const LIST_RESULTS = new Set([
        RPC.POST_PRODUCTS_QUERY, RPC.GET_PRODUCTS, RPC.GET_OFFER_GROUPS, RPC.POST_ORDERS_QUERY,
        RPC.GET_CATEGORIES, RPC.GET_SUBCATEGORIES, RPC.GET_SUBCATEGORIES_V0_BY_CATEGORY,
        RPC.GET_SUBCATEGORIES_BY_CATEGORY, RPC.GET_USERDATA, RPC.GET_DATABASE_TABLES,
        RPC.GET_GST_SALES_REGISTER, RPC.GET_GST_CREDIT_NOTES, RPC.GET_GST_DEBIT_NOTES
    ]);

    class JrpcApiError extends Error {
        constructor(message, status, detail, code) {
            super(message);
            this.name = "JrpcApiError";
            this.status = status || 500;
            this.detail = detail == null ? message : detail;
            this.code = code || `HTTP_${this.status}`;
        }
    }

    function unsupported(method, path) {
        throw new JrpcApiError(`Not implemented in the JRPC server: ${method} ${path}`, 501);
    }

    function resolveEndpoint(method, rawUrl, body) {
        method = String(method || "GET").toUpperCase();
        const parsed = new URL(String(rawUrl || ""), "https://jrpc.local/");
        const path = parsed.pathname.replace(/^\/+|\/+$/g, "");
        const request = {};
        for (const [key, value] of parsed.searchParams.entries()) {
            request[key] = /^(limit|offset)$/.test(key) ? Number(value) : value;
        }
        if (body != null && ["POST", "PUT"].includes(method)) request.body = body;

        const staticRoutes = {
            "POST products": RPC.POST_PRODUCTS, "POST products/query": RPC.POST_PRODUCTS_QUERY,
            "GET products": RPC.GET_PRODUCTS, "GET offer-groups": RPC.GET_OFFER_GROUPS,
            "POST offer-groups": RPC.POST_OFFER_GROUP, "POST schema/add-columns": RPC.POST_SCHEMA_ADD_COLUMNS,
            "POST schema/remove-columns": RPC.POST_SCHEMA_REMOVE_COLUMNS, "GET schema": RPC.GET_SCHEMA,
            "POST products/bulk-details": RPC.POST_PRODUCTS_BULK_DETAILS,
            "POST orders/checkout": RPC.POST_ORDERS_CHECKOUT, "POST orders/query": RPC.POST_ORDERS_QUERY,
            "GET categories": RPC.GET_CATEGORIES, "POST categories": RPC.POST_CATEGORY,
            "GET subcategories": RPC.GET_SUBCATEGORIES, "POST subcategories": RPC.POST_SUBCATEGORY,
            "GET userdata": RPC.GET_USERDATA, "POST userdata": RPC.POST_USERDATA,
            "GET database/tables": RPC.GET_DATABASE_TABLES, "GET analytics/summary": RPC.GET_ANALYTICS_SUMMARY,
            "GET gst/dashboard": RPC.GET_GST_DASHBOARD, "GET gst/sales-register": RPC.GET_GST_SALES_REGISTER,
            "POST gst/credit-notes": RPC.POST_GST_CREDIT_NOTE, "GET gst/credit-notes": RPC.GET_GST_CREDIT_NOTES,
            "POST gst/debit-notes": RPC.POST_GST_DEBIT_NOTE, "GET gst/debit-notes": RPC.GET_GST_DEBIT_NOTES,
            "GET gst/party-ledger": RPC.GET_GST_PARTY_LEDGER, "GET gst/day-book": RPC.GET_GST_DAY_BOOK,
            "GET gst/profit-loss": RPC.GET_GST_PROFIT_LOSS, "GET gst/stock-summary": RPC.GET_GST_STOCK_SUMMARY,
            "GET gst/outstanding": RPC.GET_GST_OUTSTANDING, "GET gst/tax-ledger": RPC.GET_GST_TAX_LEDGER,
            "GET gst/dashboard-extras": RPC.GET_GST_DASHBOARD_EXTRAS,
            "GET backups/list": RPC.GET_BACKUPS, "POST backups/create": RPC.POST_BACKUP,
            "POST backups/delete-selected": RPC.POST_BACKUPS_DELETE_SELECTED,
            "POST backups/reset-current": RPC.POST_BACKUPS_RESET_CURRENT
        };
        let rpc = staticRoutes[`${method} ${path}`];
        const dynamic = [
            ["GET", /^products\/([^/]+)$/, RPC.GET_PRODUCT, "product_identifier"],
            ["PUT", /^products\/([^/]+)$/, RPC.PUT_PRODUCT, "product_identifier"],
            ["DELETE", /^products\/([^/]+)$/, RPC.DELETE_PRODUCT, "product_identifier"],
            ["PUT", /^offer-groups\/([^/]+)$/, RPC.PUT_OFFER_GROUP, "group_id"],
            ["POST", /^offer-groups\/([^/]+)\/apply$/, RPC.POST_OFFER_GROUP_APPLY, "group_id"],
            ["POST", /^offer-groups\/([^/]+)\/cancel$/, RPC.POST_OFFER_GROUP_CANCEL, "group_id"],
            ["DELETE", /^offer-groups\/([^/]+)$/, RPC.DELETE_OFFER_GROUP, "group_id"],
            ["GET", /^bills\/([^/]+)$/, RPC.GET_BILL, "order_id"],
            ["PUT", /^orders\/([^/]+)$/, RPC.PUT_ORDER, "order_id"],
            ["DELETE", /^orders\/([^/]+)$/, RPC.DELETE_ORDER, "order_id"],
            ["DELETE", /^categories\/([^/]+)$/, RPC.DELETE_CATEGORY, "cat_id"],
            ["GET", /^subcategories\/v0\/([^/]+)$/, RPC.GET_SUBCATEGORIES_V0_BY_CATEGORY, "category_id"],
            ["GET", /^subcategories\/([^/]+)$/, RPC.GET_SUBCATEGORIES_BY_CATEGORY, "category_id"],
            ["DELETE", /^subcategories\/([^/]+)$/, RPC.DELETE_SUBCATEGORY, "cat_id"],
            ["GET", /^user\/([^/]+)$/, RPC.GET_USER, "user_id"],
            ["PUT", /^userdata\/([^/]+)$/, RPC.PUT_USERDATA, "user_id"],
            ["DELETE", /^userdata\/([^/]+)$/, RPC.DELETE_USERDATA, "user_id"],
            ["GET", /^database\/tables\/([^/]+)\/info$/, RPC.GET_DATABASE_TABLE_INFO, "table_name"],
            ["GET", /^database\/tables\/([^/]+)\/data$/, RPC.GET_DATABASE_TABLE_DATA, "table_name"],
            ["POST", /^database\/tables\/([^/]+)\/rows$/, RPC.POST_DATABASE_TABLE_ROW, "table_name"],
            ["PUT", /^database\/tables\/([^/]+)\/rows\/([^/]+)$/, RPC.PUT_DATABASE_TABLE_ROW, ["table_name", "pk_value"]],
            ["DELETE", /^database\/tables\/([^/]+)\/rows\/([^/]+)$/, RPC.DELETE_DATABASE_TABLE_ROW, ["table_name", "pk_value"]],
            ["DELETE", /^gst\/credit-notes\/([^/]+)$/, RPC.DELETE_GST_CREDIT_NOTE, "cn_id"],
            ["DELETE", /^gst\/debit-notes\/([^/]+)$/, RPC.DELETE_GST_DEBIT_NOTE, "dn_id"],
            ["DELETE", /^backups\/older-than-days\/(\d+)$/, RPC.DELETE_BACKUPS_OLDER_THAN_DAYS, "days"],
            ["POST", /^backups\/([^/]+)\/restore$/, RPC.POST_BACKUP_RESTORE, "backup_id"],
            ["DELETE", /^backups\/([^/]+)$/, RPC.DELETE_BACKUP, "backup_id"]
        ];
        if (rpc == null) {
            for (const [verb, pattern, ordinal, keys] of dynamic) {
                const match = method === verb ? path.match(pattern) : null;
                if (!match) continue;
                rpc = ordinal;
                (Array.isArray(keys) ? keys : [keys]).forEach((key, index) => {
                    request[key] = decodeURIComponent(match[index + 1]);
                });
                break;
            }
        }
        if (rpc == null) unsupported(method, path);
        return { rpc, request, unwrapData: LIST_RESULTS.has(rpc) };
    }

    function responseError(value) {
        let detail = value?.errorMessage || "RPC request failed";
        try { detail = JSON.parse(detail); } catch (_) {}
        const code = value?.errorCode || "RPC_FAILED";
        const match = /^HTTP_(\d+)$/.exec(code);
        const message = typeof detail === "string" ? detail : (detail?.message || JSON.stringify(detail));
        return new JrpcApiError(message, match ? Number(match[1]) : 500, detail, code);
    }

    class FirebaseJrpcClient {
        constructor(database, auth, serverId, timeoutMs) {
            this.database = database;
            this.auth = auth;
            this.serverId = serverId;
            this.timeoutMs = timeoutMs || 45000;
        }

        async call(rpc, request) {
            const user = this.auth.currentUser;
            if (!user) throw new JrpcApiError("Please login first.", 401);
            const authenticatedRequest = Object.assign({}, request || {}, {
                _auth_token: await user.getIdToken()
            });
            const requestId = `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`;
            const relativePath = `${this.serverId}/${user.uid}/rpc/${requestId}`;
            const requestRef = this.database.ref(`ServerReq/${relativePath}`);
            const responseRef = this.database.ref(`ServerResp/${relativePath}`);

            return new Promise((resolve, reject) => {
                let finished = false;
                const cleanup = () => {
                    responseRef.off("value", onResponse);
                    responseRef.remove().catch(() => {});
                };
                const finish = callback => {
                    if (finished) return;
                    finished = true;
                    clearTimeout(timer);
                    cleanup();
                    callback();
                };
                const onResponse = snapshot => {
                    if (!snapshot.exists()) return;
                    const value = snapshot.val() || {};
                    if (!value.success) return finish(() => reject(responseError(value)));
                    try {
                        const parsed = JSON.parse(value.responseJson || "{}");
                        finish(() => resolve(parsed));
                    } catch (error) {
                        finish(() => reject(new JrpcApiError("JRPC returned invalid JSON.", 502, error.message)));
                    }
                };
                const timer = setTimeout(() => {
                    requestRef.remove().catch(() => {});
                    finish(() => reject(new JrpcApiError(
                        `JRPC server '${this.serverId}' did not respond within ${this.timeoutMs} ms.`, 504)));
                }, this.timeoutMs);
                responseRef.on("value", onResponse,
                    error => finish(() => reject(new JrpcApiError(error.message, 503))));
                requestRef.set({
                    createdAtEpochMs: Date.now(),
                    rpcs: [rpc],
                    requestJson: JSON.stringify(authenticatedRequest)
                }).catch(error => finish(() => reject(new JrpcApiError(error.message, 503))));
            });
        }
    }

    class PetsFortJrpcTransport {
        constructor(client) { this.client = client; }
        async callApi(method, url, body, parseJson) {
            if (parseJson === false) unsupported(method, String(url));
            const route = resolveEndpoint(method, url, body);
            const response = await this.client.call(route.rpc, route.request);
            return route.unwrapData ? (response.data || []) : response;
        }
    }

    const api = { RPC, JrpcApiError, FirebaseJrpcClient, PetsFortJrpcTransport, resolveEndpoint };
    root.PetsFortJrpc = api;
    if (typeof module !== "undefined" && module.exports) module.exports = api;
})(typeof window !== "undefined" ? window : globalThis);
