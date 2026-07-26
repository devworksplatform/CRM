"""Differential contract runner for FastAPI and the Java JRPC test gateway.

The runner never touches the supplied production backup. Each case receives two
byte-identical database copies. Set PYTHON_BASE_URL and JRPC_BASE_URL to the two
local test gateways, then run this file with pytest.
"""
from __future__ import annotations

import json
import os
import uuid
from copy import deepcopy

import pytest
import requests

PYTHON = os.environ.get("PYTHON_BASE_URL")
JRPC = os.environ.get("JRPC_BASE_URL")

READ_CASES = [
    ("GET_PRODUCTS", "GET", "/products/", {"limit": 100, "offset": 0}, {}),
    ("GET_CATEGORIES", "GET", "/categories", {}, {}),
    ("GET_SUBCATEGORIES", "GET", "/subcategories", {}, {}),
    ("GET_USERDATA", "GET", "/userdata", {}, {}),
    ("GET_ANALYTICS_SUMMARY", "GET", "/analytics/summary", {}, {}),
    ("GET_GST_STOCK_SUMMARY", "GET", "/gst/stock-summary", {}, {}),
    ("GET_GST_OUTSTANDING", "GET", "/gst/outstanding", {}, {}),
]


def normalize(value):
    """Remove only intentionally nondeterministic values; retain numeric types."""
    if isinstance(value, dict):
        return {key: normalize(item) for key, item in sorted(value.items())
                if key not in {"created_at", "updated_at"}}
    if isinstance(value, list):
        return [normalize(item) for item in value]
    return value


@pytest.mark.skipif(not PYTHON or not JRPC,
                    reason="Set PYTHON_BASE_URL and JRPC_BASE_URL")
@pytest.mark.parametrize("operation,method,path,params,body", READ_CASES)
def test_read_contract(operation, method, path, params, body):
    python_response = requests.request(
        method, PYTHON.rstrip("/") + path, params=params, json=body, timeout=30)
    python_response.raise_for_status()
    rpc_request = deepcopy(params)
    if body:
        rpc_request["body"] = body
    java_response = requests.post(
        JRPC.rstrip("/") + "/rpc/" + operation,
        json=rpc_request,
        timeout=30,
    )
    java_response.raise_for_status()
    actual = java_response.json()
    expected = python_response.json()
    if isinstance(expected, list):
        actual = actual["data"]
    assert normalize(actual) == normalize(expected)


@pytest.mark.skipif(not PYTHON or not JRPC,
                    reason="Set PYTHON_BASE_URL and JRPC_BASE_URL")
def test_complete_read_contract():
    products = requests.get(PYTHON + "/products/", params={"limit": 1}).json()
    categories = requests.get(PYTHON + "/categories").json()
    users = requests.get(PYTHON + "/userdata").json()
    orders = requests.post(PYTHON + "/orders/query",
                           json={"filters": [], "limit": 1}).json()
    product_id = products[0]["id"]
    category_id = categories[0]["id"]
    user_id = users[0]["uid"]
    order_id = orders[0]["order_id"]
    cases = [
        ("GET_PRODUCT", f"/products/{product_id}", {"product_identifier": product_id}),
        ("GET_OFFER_GROUPS", "/offer-groups", {}),
        ("GET_SCHEMA", "/schema", {}),
        ("GET_BILL", f"/bills/{order_id}", {"order_id": order_id}),
        ("GET_SUBCATEGORIES_V0_BY_CATEGORY", f"/subcats_v0/{category_id}",
         {"category_id": category_id}),
        ("GET_SUBCATEGORIES_BY_CATEGORY", f"/subcats/{category_id}",
         {"category_id": category_id}),
        ("GET_USER", f"/user/{user_id}", {"user_id": user_id}),
        ("GET_DATABASE_TABLES", "/api/tables", {}),
        ("GET_DATABASE_TABLE_INFO", "/api/table/products/info",
         {"table_name": "products"}),
        ("GET_DATABASE_TABLE_DATA", "/api/table/category/data",
         {"table_name": "category"}),
        ("GET_GST_DASHBOARD", "/gst/dashboard?fy=2026-27", {"fy": "2026-27"}),
        ("GET_GST_SALES_REGISTER",
         "/gst/sales-register?from_date=2020-01-01&to_date=2030-12-31",
         {"from_date": "2020-01-01", "to_date": "2030-12-31"}),
        ("GET_GST_CREDIT_NOTES", "/gst/credit-notes", {}),
        ("GET_GST_DEBIT_NOTES", "/gst/debit-notes", {}),
        ("GET_GST_PARTY_LEDGER",
         "/gst/party-ledger?from_date=2020-01-01&to_date=2030-12-31",
         {"from_date": "2020-01-01", "to_date": "2030-12-31"}),
        ("GET_GST_DAY_BOOK",
         "/gst/day-book?from_date=2020-01-01&to_date=2030-12-31",
         {"from_date": "2020-01-01", "to_date": "2030-12-31"}),
        ("GET_GST_PROFIT_LOSS", "/gst/profit-loss?fy=2026-27", {"fy": "2026-27"}),
        ("GET_GST_TAX_LEDGER",
         "/gst/tax-ledger?from_date=2020-01-01&to_date=2030-12-31",
         {"from_date": "2020-01-01", "to_date": "2030-12-31"}),
        ("GET_GST_DASHBOARD_EXTRAS",
         "/gst/dashboard-extras?fy=2026-27", {"fy": "2026-27"}),
    ]
    for operation, path, payload in cases:
        expected = requests.get(PYTHON.rstrip("/") + path, timeout=30)
        actual = requests.post(JRPC.rstrip("/") + "/rpc/" + operation,
                               json=payload, timeout=30)
        assert actual.status_code == expected.status_code, operation
        expected_json = expected.json()
        actual_json = actual.json()
        if isinstance(expected_json, list):
            actual_json = actual_json["data"]
        assert normalize(actual_json) == normalize(expected_json), operation


@pytest.mark.skipif(not PYTHON or not JRPC,
                    reason="Set PYTHON_BASE_URL and JRPC_BASE_URL")
def test_crud_contract_and_cleanup():
    tag = "DIFF_" + uuid.uuid4().hex[:10]

    def rpc(operation, payload):
        return requests.post(JRPC.rstrip("/") + "/rpc/" + operation,
                             json=payload, timeout=60)

    category = {"id": tag + "_C", "name": tag, "image": "image"}
    subcategory = {
        "id": tag + "_S", "parentid": category["id"],
        "name": tag + " sub", "image": ""
    }
    product = {
        "product_id": tag + "_P", "product_name": tag + " product",
        "product_desc": "description", "product_hsn": "1234",
        "product_cid": "CID", "product_img": ["image"],
        "cat_id": category["id"], "cat_sub": subcategory["id"],
        "cost_rate": 90, "cost_mrp": 100, "cost_gst": 18,
        "cost_dis": 10, "stock": 50, "offer_buy_qty": 0,
        "offer_free_qty": 0, "offer_active": False
    }
    try:
        pairs = [
            (requests.post(PYTHON + "/categories", json=category),
             rpc("POST_CATEGORY", {"body": category})),
            (requests.post(PYTHON + "/subcategories", json=subcategory),
             rpc("POST_SUBCATEGORY", {"body": subcategory})),
            (requests.post(PYTHON + "/products/", json=product),
             rpc("POST_PRODUCTS", {"body": product})),
        ]
        for index, (expected, actual) in enumerate(pairs):
            assert actual.status_code == expected.status_code
            expected_json, actual_json = expected.json(), actual.json()
            if index == 2:
                expected_json.pop("id", None)
                actual_json.pop("id", None)
            assert normalize(actual_json) == normalize(expected_json)

        query = {"filters": [{"field": "product_id", "operator": "eq",
                              "value": product["product_id"]}]}
        expected = requests.post(PYTHON + "/products/query", json=query).json()
        actual = rpc("POST_PRODUCTS_QUERY", {"body": query}).json()["data"]
        expected[0].pop("id", None)
        actual[0].pop("id", None)
        assert normalize(actual) == normalize(expected)
    finally:
        requests.delete(PYTHON + "/products/" + product["product_id"])
        rpc("DELETE_PRODUCT", {"product_identifier": product["product_id"]})
        requests.delete(PYTHON + "/subcategories/" + subcategory["id"])
        rpc("DELETE_SUBCATEGORY", {"cat_id": subcategory["id"]})
        requests.delete(PYTHON + "/categories/" + category["id"])
        rpc("DELETE_CATEGORY", {"cat_id": category["id"]})
