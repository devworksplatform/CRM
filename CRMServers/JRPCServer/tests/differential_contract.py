"""Differential contract runner for FastAPI and the Java JRPC test gateway.

The runner never touches the supplied production backup. Each case receives two
byte-identical database copies. Set PYTHON_BASE_URL and JRPC_BASE_URL to the two
local test gateways, then run this file with pytest.
"""
from __future__ import annotations

import json
import os
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

