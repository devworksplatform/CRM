# JRPC web client

The admin panel no longer calls the `proxy_request` or `server_config` Firebase
Functions. Business requests use Firebase Realtime Database as the JRPC
transport:

```text
ServerReq/{serverId}/{firebaseAuthUid}/rpc/{requestId}
ServerResp/{serverId}/{firebaseAuthUid}/rpc/{requestId}
```

`public/basic/jrpcClient.js` maps the existing HTTP-shaped `callApi()` calls to
the stable `CrmRpc` enum ordinals. This lets the existing admin modules keep
their response handling unchanged. Python endpoints that returned arrays are
unwrapped from the JRPC `{ "data": [...] }` envelope.

The default server name is `PetsFort-CRM`. The `dev@petsfort.in` account can
change the server name and timeout from **Server Config**. This setting is kept
in the browser's local storage under `petsfortJrpcConfig`; no configuration
Function is required.

The administrator-only Backup Manager uses JRPC for list, create, delete,
retention cleanup, history reset, and restore operations. Firebase ID tokens
are attached by the transport and verified by the Java server. Server
status/log streaming and terminal operations remain excluded and are not
loaded by the admin panel.

## Test

Open `tests/jrpcClient.browser.html` in a browser. It checks all routes currently
used by the active admin modules and reports `PASS 49 routes`. A Node-compatible
version is also available at `tests/jrpcClient.test.js`.
