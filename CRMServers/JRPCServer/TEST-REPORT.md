# Differential compatibility report

Test date: 2026-07-26

Reference database:
`../backups_sqliteDBs_2026-07-26--12-49-45.db`

Both runtimes were started against separate byte-identical copies. The
reference database was never opened for writing.

## Automated results

- Java unit/integration suite: 5 passed
- Live FastAPI/JRPC differential suite: 9 passed
- Failures: 0

The live suite covers the complete read/reporting surface and a cleaned-up CRUD
workflow. See `tests/differential_contract.py`.

## Additional mutation scenarios exercised

- Product create, advanced query, update, delete
- Bulk product details, totals, missing-product warnings, and offer quantities
- Offer-group create, update, apply, cancel, and delete
- Schema add/remove and post-migration default-value behavior
- Checkout, free-item stock reservation, user credits, order, and invoice
- Order query, status/amount update, and delete
- Category and subcategory create/delete
- Generic table row create/update/delete
- Credit and debit note create/list/delete
- Firebase Authentication user create, password/profile update, and delete
- HTML operations return the agreed `HTTP_501`/status 501 result
- Backup snapshot/restore round trip, including a dynamic column and unique index

Generated IDs and timestamps were normalized only where each isolated runtime
must generate a different value. All stable fields, nested payloads, numeric
types, rounding, status/error details, and stored business data were compared.

## Agreed exclusions

- Sitemap
- System statistics and SSE streaming
- Terminal WebSocket
- Rendering HTML; HTML enum operations return `NOT_IMPLEMENTED` behavior
