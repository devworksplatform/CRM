# Import necessary libraries
from datetime import datetime, timezone
import json

from firebase_functions import https_fn, scheduler_fn
from firebase_admin import auth, db, initialize_app
import requests


initialize_app(options={
    "databaseURL": "https://pets-fort-default-rtdb.asia-southeast1.firebasedatabase.app"
})

FALLBACK_TARGET_URL = "https://ec2-3-27-240-197.ap-southeast-2.compute.amazonaws.com"
SERVER_CONFIG_DB_PATH = "appConfig/server"
SERVER_CONFIG_ADMIN_EMAIL = "dev@petsfort.in"
ALLOWED_ORIGINS = {
    "https://pets-fort.web.app",
    "https://petsfort.in",
    "https://server.petsfort.in",
    "http://localhost:5500",
    "http://127.0.0.1:5500",
}


def _normalize_base_url(value):
    candidate = (value or FALLBACK_TARGET_URL).strip()
    return candidate.rstrip("/")


def _build_terminal_ws_url(base_url):
    normalized = _normalize_base_url(base_url)
    if normalized.startswith("https://"):
        host = normalized[len("https://"):]
        return f"wss://{host}:8000/ws/terminal"
    if normalized.startswith("http://"):
        host = normalized[len("http://"):]
        return f"ws://{host}:8000/ws/terminal"
    return f"wss://{normalized}:8000/ws/terminal"


def _get_cors_headers(req):
    origin = req.headers.get("Origin", "")
    allowed_origin = origin if origin in ALLOWED_ORIGINS else ""
    headers = {
        "Access-Control-Allow-Methods": "GET,PUT,OPTIONS",
        "Access-Control-Allow-Headers": "Authorization,Content-Type",
        "Access-Control-Max-Age": "3600",
    }
    if allowed_origin:
        headers["Access-Control-Allow-Origin"] = allowed_origin
        headers["Vary"] = "Origin"
    return headers


def _get_server_config():
    raw_config = db.reference(SERVER_CONFIG_DB_PATH).get() or {}
    base_url = _normalize_base_url(raw_config.get("baseUrl") or raw_config.get("targetUrl"))
    terminal_ws_url = (raw_config.get("terminalWsUrl") or "").strip() or _build_terminal_ws_url(base_url)

    return {
        "baseUrl": base_url,
        "targetUrl": base_url,
        "terminalWsUrl": terminal_ws_url,
        "updatedAt": raw_config.get("updatedAt"),
        "updatedByEmail": raw_config.get("updatedByEmail", ""),
        "updatedByUid": raw_config.get("updatedByUid", ""),
    }


def _get_target_url():
    return _get_server_config()["targetUrl"]


def _verify_admin(req):
    authorization = req.headers.get("Authorization", "")
    if not authorization.startswith("Bearer "):
        raise PermissionError("Missing bearer token.")

    id_token = authorization.split(" ", 1)[1].strip()
    decoded_token = auth.verify_id_token(id_token)
    user_email = (decoded_token.get("email") or "").lower()
    if user_email != SERVER_CONFIG_ADMIN_EMAIL:
        raise PermissionError("Only dev@petsfort.in can update server configuration.")

    return decoded_token


@https_fn.on_request(region="asia-south1")
def proxy_request(req: https_fn.Request):
    try:
        method = req.method
        headers = dict(req.headers)
        path = req.path
        query_params = req.args

        body = req.get_json(silent=True)
        if body is None:
            body = req.data

        full_target_url = f"{_get_target_url()}{path}"

        response = requests.request(
            method=method,
            url=full_target_url,
            headers=headers,
            params=query_params,
            data=body if not isinstance(body, dict) and body is not None else None,
            json=body if isinstance(body, dict) else None,
            verify=False
        )

        return https_fn.Response(
            response.content,
            status=response.status_code,
            headers=dict(response.headers)
        )

    except requests.exceptions.RequestException as exc:
        print(f"Error making request to target URL: {exc}")
        return https_fn.Response(f"Proxy Error: Could not reach target URL. {exc}", status=500)
    except Exception as exc:
        print(f"An unexpected error occurred: {exc}")
        return https_fn.Response(f"Proxy Error: An internal error occurred. {exc}", status=500)


def perform_backup():
    full_target_url = f"{_get_target_url()}/backup"
    print(f"Attempting backup request to {full_target_url}")
    try:
        response = requests.request(
            method="GET",
            url=full_target_url,
            verify=False
        )
        print(f"Backup target response status: {response.status_code}")

        if response.status_code == 200:
            print("Backup request successful.")
        else:
            print(f"Backup request failed with status code: {response.status_code}")

        return response.content, response.status_code, dict(response.headers)

    except requests.exceptions.RequestException as exc:
        print(f"Error making backup request to target URL: {exc}")
        raise exc


@https_fn.on_request(region="asia-south1")
def backup_request(req: https_fn.Request):
    try:
        content, status, headers = perform_backup()
        return https_fn.Response(content, status=status, headers=headers)
    except Exception as exc:
        print(f"Error in backup_request (HTTP trigger): {exc}")
        return https_fn.Response(f"Backup Error: {exc}", status=500)


@scheduler_fn.on_schedule(schedule="0 */8 * * *", region="asia-south1")
def scheduled_backup(event: scheduler_fn.ScheduledEvent):
    print("Scheduled backup function triggered.")
    try:
        perform_backup()
        print("Scheduled backup completed successfully.")
    except Exception as exc:
        print(f"Error during scheduled backup: {exc}")


@https_fn.on_request(region="asia-south1")
def server_config(req: https_fn.Request):
    cors_headers = _get_cors_headers(req)

    if req.method == "OPTIONS":
        return https_fn.Response("", status=204, headers=cors_headers)

    if req.method == "GET":
        return https_fn.Response(
            json.dumps({"config": _get_server_config()}),
            status=200,
            headers={**cors_headers, "Content-Type": "application/json"},
        )

    if req.method != "PUT":
        return https_fn.Response("Method not allowed", status=405, headers=cors_headers)

    try:
        decoded_token = _verify_admin(req)
        body = req.get_json(silent=True) or {}
        base_url = _normalize_base_url(body.get("baseUrl") or body.get("targetUrl"))
        terminal_ws_url = (body.get("terminalWsUrl") or "").strip() or _build_terminal_ws_url(base_url)

        config = {
            "baseUrl": base_url,
            "targetUrl": base_url,
            "terminalWsUrl": terminal_ws_url,
            "updatedAt": datetime.now(timezone.utc).isoformat(),
            "updatedByEmail": decoded_token.get("email", ""),
            "updatedByUid": decoded_token.get("uid", ""),
        }

        db.reference(SERVER_CONFIG_DB_PATH).set(config)

        return https_fn.Response(
            json.dumps({"status": "ok", "config": config}),
            status=200,
            headers={**cors_headers, "Content-Type": "application/json"},
        )
    except PermissionError as exc:
        return https_fn.Response(str(exc), status=403, headers=cors_headers)
    except Exception as exc:
        print(f"Error updating server config: {exc}")
        return https_fn.Response(f"Failed to update server config: {exc}", status=500, headers=cors_headers)
