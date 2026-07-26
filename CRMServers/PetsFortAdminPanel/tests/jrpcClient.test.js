"use strict";

const assert = require("node:assert/strict");
const { RPC, PetsFortJrpcTransport, resolveEndpoint } =
    require("../public/basic/jrpcClient.js");

const cases = [
    ["POST", "products/", {}, RPC.POST_PRODUCTS],
    ["POST", "products/query", { limit: 10 }, RPC.POST_PRODUCTS_QUERY],
    ["GET", "products/SKU%201", null, RPC.GET_PRODUCT, ["product_identifier", "SKU 1"]],
    ["PUT", "products/SKU-1", {}, RPC.PUT_PRODUCT],
    ["DELETE", "products/SKU-1", null, RPC.DELETE_PRODUCT],
    ["GET", "offer-groups", null, RPC.GET_OFFER_GROUPS],
    ["POST", "offer-groups", {}, RPC.POST_OFFER_GROUP],
    ["PUT", "offer-groups/7", {}, RPC.PUT_OFFER_GROUP],
    ["POST", "offer-groups/7/apply", {}, RPC.POST_OFFER_GROUP_APPLY],
    ["POST", "offer-groups/7/cancel", {}, RPC.POST_OFFER_GROUP_CANCEL],
    ["DELETE", "offer-groups/7", null, RPC.DELETE_OFFER_GROUP],
    ["GET", "bills/42", null, RPC.GET_BILL, ["order_id", "42"]],
    ["POST", "orders/query", {}, RPC.POST_ORDERS_QUERY],
    ["PUT", "orders/42", {}, RPC.PUT_ORDER],
    ["DELETE", "orders/42", null, RPC.DELETE_ORDER],
    ["GET", "categories", null, RPC.GET_CATEGORIES],
    ["POST", "categories", {}, RPC.POST_CATEGORY],
    ["DELETE", "categories/dogs", null, RPC.DELETE_CATEGORY],
    ["GET", "subcategories", null, RPC.GET_SUBCATEGORIES],
    ["POST", "subcategories", {}, RPC.POST_SUBCATEGORY],
    ["DELETE", "subcategories/food", null, RPC.DELETE_SUBCATEGORY],
    ["GET", "user/uid-1", null, RPC.GET_USER],
    ["GET", "userdata", null, RPC.GET_USERDATA],
    ["POST", "userdata", {}, RPC.POST_USERDATA],
    ["PUT", "userdata/uid-1", {}, RPC.PUT_USERDATA],
    ["DELETE", "userdata/uid-1", null, RPC.DELETE_USERDATA],
    ["GET", "analytics/summary", null, RPC.GET_ANALYTICS_SUMMARY],
    ["GET", "gst/dashboard?from_date=2026-01-01&to_date=2026-12-31", null, RPC.GET_GST_DASHBOARD],
    ["GET", "gst/sales-register?from_date=2026-01-01&to_date=2026-12-31", null, RPC.GET_GST_SALES_REGISTER],
    ["POST", "gst/credit-notes", {}, RPC.POST_GST_CREDIT_NOTE],
    ["GET", "gst/credit-notes", null, RPC.GET_GST_CREDIT_NOTES],
    ["DELETE", "gst/credit-notes/3", null, RPC.DELETE_GST_CREDIT_NOTE],
    ["POST", "gst/debit-notes", {}, RPC.POST_GST_DEBIT_NOTE],
    ["GET", "gst/debit-notes", null, RPC.GET_GST_DEBIT_NOTES],
    ["DELETE", "gst/debit-notes/3", null, RPC.DELETE_GST_DEBIT_NOTE],
    ["GET", "gst/party-ledger?user_id=u1", null, RPC.GET_GST_PARTY_LEDGER],
    ["GET", "gst/day-book", null, RPC.GET_GST_DAY_BOOK],
    ["GET", "gst/profit-loss", null, RPC.GET_GST_PROFIT_LOSS],
    ["GET", "gst/stock-summary", null, RPC.GET_GST_STOCK_SUMMARY],
    ["GET", "gst/outstanding", null, RPC.GET_GST_OUTSTANDING],
    ["GET", "gst/tax-ledger", null, RPC.GET_GST_TAX_LEDGER],
    ["GET", "gst/dashboard-extras", null, RPC.GET_GST_DASHBOARD_EXTRAS]
];

for (const [method, url, body, expectedRpc, expectedField] of cases) {
    const route = resolveEndpoint(method, url, body);
    assert.equal(route.rpc, expectedRpc, `${method} ${url}`);
    if (body && ["POST", "PUT"].includes(method)) assert.deepEqual(route.request.body, body);
    if (expectedField) assert.equal(route.request[expectedField[0]], expectedField[1]);
}

assert.throws(() => resolveEndpoint("GET", "logs"), error => error.status === 501);
assert.throws(() => resolveEndpoint("GET", "backups"), error => error.status === 501);

(async () => {
    const calls = [];
    const transport = new PetsFortJrpcTransport({
        call: async (rpc, request) => {
            calls.push({ rpc, request });
            return { data: [{ id: 1 }] };
        }
    });
    assert.deepEqual(await transport.callApi("GET", "categories"), [{ id: 1 }]);
    assert.equal(calls[0].rpc, RPC.GET_CATEGORIES);
    console.log(`JRPC web adapter: ${cases.length} route cases passed`);
})().catch(error => {
    console.error(error);
    process.exitCode = 1;
});
