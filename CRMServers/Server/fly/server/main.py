import uvicorn
from fastapi import FastAPI, HTTPException, Query, Request, Response
from fastapi.responses import RedirectResponse, HTMLResponse 
from fastapi.responses import StreamingResponse
from fastapi.responses import JSONResponse, PlainTextResponse
import httpx
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
import json
import random, string
import aiosqlite  # Replaced sqlite3 with aiosqlite

import asyncio
import json
import time
import platform
import os
import psutil
from sse_starlette.sse import EventSourceResponse

from fastapi import WebSocket, WebSocketDisconnect
import pty
import select
import termios
import struct
import fcntl
import signal
import subprocess


from enum import Enum
from datetime import datetime
from fastapi.middleware.cors import CORSMiddleware

from uuid import uuid4
import sqlite3 # Keep standard sqlite3 for backup operation
from urllib.parse import urlparse

import firebaseAuth

# Define database path
DB_PATH = "/app/sqlite/products.db"
# DB_PATH_SMS = "sms.db" # This was never used in the original code
# sudo sysctl -w vm.drop_caches=3

# --- SQLite Schema (as provided by the user) ---
# Used during the restoration process to recreate tables
TABLE_SCHEMAS = {
    "products": """
        CREATE TABLE IF NOT EXISTS products (
            id TEXT PRIMARY KEY,
            product_id TEXT UNIQUE,
            product_name TEXT NOT NULL,
            product_desc TEXT,
            product_hsn TEXT DEFAULT '',
            product_cid TEXT DEFAULT '',
            product_img TEXT, -- Stored as JSON string
            cat_id TEXT,
            cat_sub TEXT, -- Comma-separated string
            cost_rate REAL,
            cost_mrp REAL,
            cost_gst REAL,
            cost_dis REAL,
            stock INTEGER,
            created_at TIMESTAMP,
            updated_at TIMESTAMP
        )
        """,
    "orders": """
        CREATE TABLE IF NOT EXISTS orders (
            order_id TEXT PRIMARY KEY,
            user_id TEXT NOT NULL,
            items TEXT NOT NULL,       -- Stored as JSON string
            items_detail TEXT NOT NULL, -- Stored as JSON string
            order_status TEXT NOT NULL,
            total_rate REAL,
            total_gst REAL,
            total_discount REAL,
            total REAL,
            created_at TEXT,
            address TEXT,
            notes TEXT
        )
        """,
    "category": """
        CREATE TABLE IF NOT EXISTS category (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            image TEXT DEFAULT ''
        )
        """,
    "subcategory": """
        CREATE TABLE IF NOT EXISTS subcategory (
            id TEXT PRIMARY KEY,
            parentid TEXT NOT NULL,
            name TEXT NOT NULL,
            image TEXT DEFAULT ''
        )
        """,
    "userdata": """
        CREATE TABLE IF NOT EXISTS userdata (
            id TEXT PRIMARY KEY,
            uid TEXT NOT NULL,
            name TEXT NOT NULL,
            contact TEXT DEFAULT 'N/A',
            gstin TEXT DEFAULT 'N/A',
            email TEXT NOT NULL,
            role TEXT NOT NULL,
            address TEXT NOT NULL,
            credits REAL,
            creditse TEXT NOT NULL,
            isblocked INTEGER NOT NULL DEFAULT 0
        )
        """,
    "bills": """
        CREATE TABLE IF NOT EXISTS bills (
            order_id TEXT PRIMARY KEY,
            bill TEXT NOT NULL
        )
        """
}


# Initialize FastAPI
app = FastAPI(title="Async SQLite Products API") # Updated title
app.add_middleware(
    CORSMiddleware,
    # allow_origins=["*"],
    allow_origins=["https://pets-fort.web.app","https://petsfort.in","https://server.petsfort.in","http://localhost:5500", "https://ec2-13-203-205-116.ap-south-1.compute.amazonaws.com"],  # Or specify: ["http://127.0.0.1:5500"]
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


ALLOWED_HOST = "admin.petsfort.in"
PROXY_URL = "https://pets-fort.web.app"

headers = {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': '*',
        'Access-Control-Allow-Headers': '*',
    }

@app.middleware("http")
async def proxy_petsfort(request: Request, call_next):
    
    host = request.headers.get("host")
    if host == ALLOWED_HOST:
        url = urlparse(str(request.url))
        proxy_path = url.path
        proxy_query = url.query
        proxy_url = f"{PROXY_URL}{proxy_path}"
        if proxy_query:
            proxy_url += f"?{proxy_query}"
        return RedirectResponse(url=proxy_url, status_code=307)
    # return await call_next(request)
    response = await call_next(request)
    response.headers.update(headers)
    return response

# async def reverse_proxy(request: Request, proxy_url: str):
#     url = urlparse(str(request.url))
#     target_url = f"{proxy_url}{url.path}"
#     if url.query:
#         target_url += f"?{url.query}"
#     target_hostname = urlparse(proxy_url).netloc

#     async with httpx.AsyncClient() as client:
#         try:
#             proxy_request = client.build_request(
#                 method=request.method,
#                 url=target_url,
#                 headers=dict(request.headers),
#                 content=await request.body()
#             )
#             # Remove original host and set the target host
#             proxy_request.headers.pop("host", None)
#             proxy_request.headers["Host"] = target_hostname
#             proxy_request.headers["X-Forwarded-Host"] = request.headers.get("host", "")

#             proxy_response = await client.send(proxy_request)
#             proxy_response.raise_for_status()  # Raise HTTPError for bad responses

#             # Forward relevant headers, explicitly EXCLUDING content-encoding
#             response_headers = {
#                 key: value
#                 for key, value in proxy_response.headers.items()
#                 if key.lower() not in ("transfer-encoding", "content-encoding", "content-length")
#             }

#             return Response(
#                 content=proxy_response.content,
#                 status_code=proxy_response.status_code,
#                 headers=response_headers,
#                 media_type=proxy_response.headers.get("Content-Type")
#             )
#         except httpx.ConnectError as e:
#             return JSONResponse(
#                 content={"error": f"Could not connect to the proxy target: {e}"},
#                 status_code=502,
#             )
#         except httpx.HTTPStatusError as e:
#             return Response(
#                 content=e.response.content,
#                 status_code=e.response.status_code,
#                 headers=e.response.headers,
#                 media_type=e.response.headers.get("Content-Type")
#             )
#         except Exception as e:
#             return JSONResponse(
#                 content={"error": f"An error occurred during proxying: {e}"},
#                 status_code=500,
#             )

# @app.middleware("http")
# async def proxy_petsfort(request: Request, call_next):
#     host = request.headers.get("host")
#     if host == ALLOWED_HOST:
#         return await reverse_proxy(request, PROXY_URL)
#     return await call_next(request)

# --- Data Models (Unchanged from original) ---
class Product(BaseModel):
    product_id: Optional[str] = None
    product_name: str
    product_desc: str
    product_hsn: Optional[str] = "",
    product_cid: Optional[str] = "",
    product_img: Optional[List[str]] = []
    cat_id: str
    cat_sub: str  # Comma-separated string
    cost_rate: float
    cost_mrp: float
    cost_gst: float
    cost_dis: float
    stock: int

class ProductResponse(Product):
    id: str

class OrderResponse(BaseModel):
    order_id: str
    user_id: str
    items: dict  # Assuming items is stored as JSON
    items_detail: list # Assuming items_detail is stored as JSON
    order_status: str
    total_rate: float
    total_gst: float
    total_discount: float
    total: float
    created_at: str
    address: str
    notes: Optional[str] = None

class OrderQueryResponse(OrderResponse):
    user_name: str

class OperatorEnum(str, Enum):
    eq = "eq"  # Equal
    neq = "neq"  # Not equal
    gt = "gt"  # Greater than
    lt = "lt"  # Less than
    gte = "gte"  # Greater than or equal
    lte = "lte"  # Less than or equal
    contains = "contains"  # Contains substring
    startswith = "startswith"  # Starts with
    endswith = "endswith"  # Ends with
    in_list = "in"  # In a list of values

class QueryParam(BaseModel):
    field: str
    operator: OperatorEnum
    value: Any

class QueryRequest(BaseModel):
    filters: List[QueryParam]
    limit: Optional[int] = None
    offset: Optional[int] = 0
    order_by: Optional[str] = None
    order_direction: Optional[str] = "ASC"

class ColumnOperation(BaseModel):
    column_name: str
    column_type: str
    default_value: Optional[Any] = None

class AddColumnRequest(BaseModel):
    columns: List[ColumnOperation]

class RemoveColumnRequest(BaseModel):
    columns: List[str]

class OrderUpdate(BaseModel):
    order_status: Optional[str] = None
    items: Optional[dict] = None
    items_detail: Optional[list] = None
    total_rate: Optional[float] = None
    total_gst: Optional[float] = None
    total_discount: Optional[float] = None
    total: Optional[float] = None
# --- End Data Models ---


# --- Database Setup and Helpers ---

# Async database connection context manager
async def get_db_connection():
    """Provides an asynchronous connection to the SQLite database."""
    conn = await aiosqlite.connect(DB_PATH)
    conn.row_factory = aiosqlite.Row  # Use Row factory for dict-like access
    return conn

# Helper functions to convert rows (No async change needed, aiosqlite.Row is dict-like)
def dict_to_product(row: aiosqlite.Row):
    """Convert an aiosqlite row to a dictionary, handling JSON"""
    if not row:
        return None
    row_dict = dict(row)
    # Handle the product_img list stored as JSON
    row_dict['cost_rate'] = row_dict['cost_mrp'] - (row_dict['cost_mrp'] * row_dict['cost_dis'] / 100)

    if 'product_img' in row_dict and row_dict['product_img']:
        try:
            row_dict['product_img'] = json.loads(row_dict['product_img'])
        except (json.JSONDecodeError, TypeError):
            row_dict['product_img'] = []
    else:
        row_dict['product_img'] = []
    return row_dict

def dict_to_order(row: aiosqlite.Row):
    """Convert an aiosqlite row to a dictionary, handling JSON fields"""
    if not row:
        return None
    row_dict = dict(row)
    # Handle the items and items_detail lists stored as JSON
    if 'items' in row_dict and row_dict['items']:
        try:
            row_dict['items'] = json.loads(row_dict['items'])
        except (json.JSONDecodeError, TypeError):
            row_dict['items'] = {}
    else:
        row_dict['items'] = {}
    if 'items_detail' in row_dict and row_dict['items_detail']:
        try:
            row_dict['items_detail'] = json.loads(row_dict['items_detail'])
        except (json.JSONDecodeError, TypeError):
            row_dict['items_detail'] = []
    else:
        row_dict['items_detail'] = []
    return row_dict


def build_query(filters: List[QueryParam]):
    """Build SQL WHERE clause and parameters from filters (No async change needed)"""
    query_parts = []
    params = []
    for filter_item in filters:
        field = filter_item.field
        operator = filter_item.operator
        value = filter_item.value
        if operator == OperatorEnum.eq:
            query_parts.append(f"{field} = ?")
            params.append(value)
        elif operator == OperatorEnum.neq:
            query_parts.append(f"{field} != ?")
            params.append(value)
        elif operator == OperatorEnum.gt:
            query_parts.append(f"{field} > ?")
            params.append(value)
        elif operator == OperatorEnum.lt:
            query_parts.append(f"{field} < ?")
            params.append(value)
        elif operator == OperatorEnum.gte:
            query_parts.append(f"{field} >= ?")
            params.append(value)
        elif operator == OperatorEnum.lte:
            query_parts.append(f"{field} <= ?")
            params.append(value)
        elif operator == OperatorEnum.contains:
            query_parts.append(f"{field} LIKE ?")
            params.append(f"%{value}%")
        elif operator == OperatorEnum.startswith:
            query_parts.append(f"{field} LIKE ?")
            params.append(f"{value}%")
        elif operator == OperatorEnum.endswith:
            query_parts.append(f"{field} LIKE ?")
            params.append(f"%{value}")
        elif operator == OperatorEnum.in_list:
            if isinstance(value, list) and value:
                placeholders = ', '.join(['?'] * len(value))
                query_parts.append(f"{field} IN ({placeholders})")
                params.extend(value)
            else:
                # Handle case where value is not a list or is empty for 'in' operator
                query_parts.append("1=0") # Ensures no results match
    # where_clause = " AND ".join(query_parts) if query_parts else "1=1"
    if query_parts:
        if len(query_parts) > 1:
            where_clause = f"{query_parts[0]} AND ({' OR '.join(query_parts[1:])})"
        else:
            where_clause = query_parts[0]
    else:
        where_clause = "1=1"
    return where_clause, params

def generate_id():
    """Generate a unique ID (No async change needed)"""
    return str(uuid4())

def generate_short_random_id():
    """Generate a short semi-random ID (No async change needed)"""
    now = datetime.now()
    date_str = now.strftime("%y%m%d")
    random_num = random.randint(10, 99)
    letters = ''.join(random.choices(string.ascii_uppercase, k=2))
    return f"{date_str}{letters}{random_num:02d}"


# Initialize database (Run once on startup)
async def init_db():
    """Initializes the database tables asynchronously."""
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            await cursor.execute(TABLE_SCHEMAS["products"])
            await cursor.execute(TABLE_SCHEMAS["orders"])
            await cursor.execute(TABLE_SCHEMAS["category"])
            await cursor.execute(TABLE_SCHEMAS["subcategory"])
            await cursor.execute(TABLE_SCHEMAS["userdata"])
            await cursor.execute(TABLE_SCHEMAS["bills"])
            # await cursor.execute("""
            # CREATE TABLE IF NOT EXISTS products (
            #     id TEXT PRIMARY KEY,
            #     product_id TEXT UNIQUE,
            #     product_name TEXT NOT NULL,
            #     product_desc TEXT,
            #     product_img TEXT, -- Stored as JSON string
            #     cat_id TEXT,
            #     cat_sub TEXT, -- Comma-separated string
            #     cost_rate REAL,
            #     cost_mrp REAL,
            #     cost_gst REAL,
            #     cost_dis REAL,
            #     stock INTEGER,
            #     created_at TIMESTAMP,
            #     updated_at TIMESTAMP
            # )
            # """)
            # await cursor.execute("""
            # CREATE TABLE IF NOT EXISTS orders (
            #     order_id TEXT PRIMARY KEY,
            #     user_id TEXT NOT NULL,
            #     items TEXT NOT NULL,      -- Stored as JSON string
            #     items_detail TEXT NOT NULL, -- Stored as JSON string
            #     order_status TEXT NOT NULL,
            #     total_rate REAL,
            #     total_gst REAL,
            #     total_discount REAL,
            #     total REAL,
            #     created_at TEXT
            # )
            # """)
            # await cursor.execute("""
            # CREATE TABLE IF NOT EXISTS category (
            #     id TEXT PRIMARY KEY,
            #     name TEXT NOT NULL,
            #     image TEXT DEFAULT ''
            # )
            # """)
            # await cursor.execute("""
            # CREATE TABLE IF NOT EXISTS subcategory (
            #     id TEXT PRIMARY KEY,
            #     parentid TEXT NOT NULL,
            #     name TEXT NOT NULL,
            #     image TEXT DEFAULT ''
            # )
            # """)
            # await cursor.execute("""
            # CREATE TABLE IF NOT EXISTS userdata (
            #     id TEXT PRIMARY KEY,
            #     uid TEXT NOT NULL,
            #     name TEXT NOT NULL,
            #     email TEXT NOT NULL,
            #     role TEXT NOT NULL,
            #     address TEXT NOT NULL,
            #     credits REAL,
            #     creditse TEXT NOT NULL,
            #     isblocked INTEGER NOT NULL DEFAULT 0
            # )
            # """)

            await conn.commit()
    finally:
        await conn.close()

@app.on_event("startup")
async def startup_event():
    """Run database initialization on startup."""
    print("Initializing database...")
    await init_db()
    print("Database initialized.")

# --- API Routes (Modified for Async) ---

# @app.get("/")
# async def rootPage(): # Made async
#     return {"message": "API is running"}

@app.get("/")
async def rootPage(request: Request):
    host = request.headers.get("host", "")
    host = host.split(":")[0]
    
    if host == "server.petsfort.in":
        return {"message": "API is running"}
    elif host == "petsfort.in":
        html_file_path = "index.html"
        try:
            with open(html_file_path, "r", encoding="utf-8") as f:
                html_content = f.read()
            return HTMLResponse(content=html_content)
        except FileNotFoundError:
            logger.error(f"HTML file not found at {html_file_path}")
            raise HTTPException(status_code=404, detail=f"{html_file_path} not found in the current directory.")
        except Exception as e:
            logger.error(f"Error reading HTML file {html_file_path}: {e}")
            raise HTTPException(status_code=500, detail=f"Could not load interface: {e}")
    else:
        return {"message": "Hello From AWS Domain"}
        # raise HTTPException(status_code=404, detail=f"Could not load the page you are requesting")


from fastapi import Request, HTTPException
from fastapi.responses import Response

@app.get("/sitemap.xml")
async def rootPage(request: Request):
    html_file_path = "sitemap.xml"
    try:
        with open(html_file_path, "r", encoding="utf-8") as f:
            xml_content = f.read()
        return Response(content=xml_content, media_type="application/xml")
    except Exception as e:
        raise HTTPException(status_code=500, detail="Could not find the sitemap.xml")

@app.get("/logs")
async def get_logs(request: Request):
    log_file_path = "serverLogs.txt"
    try:
        with open(log_file_path, "r", encoding="utf-8") as f:
            content = f.read()
        return Response(content=content, media_type="text/plain")
    except Exception as e:
        raise HTTPException(status_code=500, detail="Could not find the serverLogs.txt")

@app.delete("/logs")
async def delete_logs(request: Request):
    log_file_path = "serverLogs.txt"
    try:
        open(log_file_path, "w", encoding="utf-8")
        return {"detail": "Log file deleted successfully."}
    except FileNotFoundError:
        raise HTTPException(status_code=404, detail="Log file not found.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error deleting log file: {str(e)}")

@app.get("/ram")
async def ram_data(request: Request):
    result = subprocess.run(["free", "-h"], capture_output=True, text=True)
    return PlainTextResponse(result.stdout)

from datetime import datetime, timedelta, timezone
import dbbackup

@app.get("/restore/{restore_path}")
async def restoreAPI(restore_path: str): # Made async
    errRdb = None
    rlog = None
    try:
        root_ref = firebaseAuth.db.reference()
        rlog = dbbackup.restore_firebase_to_sqlite(DB_PATH, root_ref, restore_path)
    except NameError:
        errRdb = ("\nRestore skipped because Firebase Admin SDK is not initialized (check credentials).")
    except Exception as e:
        errRdb = (f"\nError during restore call: {e}")


    return {"path":"tables/"+restore_path,"err": str(errRdb), "log":rlog}

@app.get("/backup")
async def backupAPI(): # Made async
    IST = timezone(timedelta(hours=5, minutes=30))

    # Current time in IST
    now_ist = datetime.now(IST)
    
    blog = None

    # Format it as yyyy-mm-dd--HH-MM-SS
    formatted = now_ist.strftime("%Y-%m-%d--%H-%M-%S")

    path,url,err = firebaseAuth.upload_file_to_storage(DB_PATH,"backups/sqliteDBs/"+formatted+".db")
    
    errRdb = None
    try:
        root_ref = firebaseAuth.db.reference()
        blog = dbbackup.backup_sqlite_to_firebase(DB_PATH, root_ref, formatted)
    except NameError:
        errRdb = ("\nBackup skipped because Firebase Admin SDK is not initialized (check credentials).")
    except Exception as e:
        errRdb = (f"\nError during backup call: {e}")

    return {"realtimeDb":{
        "path":"tables/"+formatted,
        "err":str(errRdb)
    },"storage":{"path":path,"url": url, "err": str(err)},
    "log":blog}



# Create a new product
@app.post("/products/", response_model=ProductResponse)
async def create_product(product: Product): # Made async
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            product_dict = product.dict(exclude_unset=True)

            # Use provided product_id or generate one
            product_id = product_dict.get("product_id") or generate_id()
            product_dict["product_id"] = product_id

            # Convert list to JSON string
            if "product_img" in product_dict:
                product_dict["product_img"] = json.dumps(product_dict["product_img"])

            # Add timestamps
            now = datetime.now().isoformat()
            product_dict["created_at"] = now
            product_dict["updated_at"] = now

            # Generate a unique document ID
            doc_id = generate_id()

            # Prepare for insertion
            columns = list(product_dict.keys())
            columns.append("id")
            values = list(product_dict.values())
            values.append(doc_id)

            # Build the SQL query
            placeholders = ", ".join(["?"] * len(values))
            columns_str = ", ".join(columns)

            query = f"INSERT INTO products ({columns_str}) VALUES ({placeholders})"

            try:
                await cursor.execute(query, values)
                await conn.commit()
            except aiosqlite.IntegrityError as e:
                # Handle potential unique constraint violation (e.g., duplicate product_id)
                if "UNIQUE constraint failed: products.product_id" in str(e):
                     raise HTTPException(status_code=409, detail=f"Product ID '{product_id}' already exists.")
                else:
                     raise HTTPException(status_code=500, detail=f"Database integrity error: {str(e)}")

            # Get the inserted product
            await cursor.execute("SELECT * FROM products WHERE id = ?", (doc_id,))
            result = await cursor.fetchone()
            if not result:
                 raise HTTPException(status_code=500, detail="Failed to retrieve created product.")

            return dict_to_product(result)
    except HTTPException:
        raise # Re-raise HTTP exceptions
    except Exception as e:
        # Log the error for debugging
        print(f"Error in create_product: {e}")
        raise HTTPException(status_code=500, detail=f"An unexpected database error occurred: {str(e)}")
    finally:
        await conn.close()

# Advanced query endpoint
@app.post("/products/query", response_model=List[ProductResponse])
async def query_products(query_request: QueryRequest): # Made async
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            where_clause, params = build_query(query_request.filters)

            # Build the complete query
            query = f"SELECT * FROM products WHERE {where_clause}"

            # Add ordering if specified
            if query_request.order_by:
                direction = query_request.order_direction.upper() if query_request.order_direction else "ASC"
                if direction not in ("ASC", "DESC"):
                    direction = "ASC"
                # Basic validation to prevent SQL injection in order_by
                # Allow only alphanumeric characters and underscores
                safe_order_by = ''.join(c for c in query_request.order_by if c.isalnum() or c == '_')
                if safe_order_by: # Only add if the column name is potentially valid
                    query += f" ORDER BY {safe_order_by} {direction}"

            # Add limit and offset if specified
            if query_request.limit is not None:
                query += f" LIMIT ?"
                params.append(query_request.limit)
                if query_request.offset:
                    query += f" OFFSET ?"
                    params.append(query_request.offset)

            await cursor.execute(query, params)
            results = await cursor.fetchall()

            return [dict_to_product(row) for row in results]
    except Exception as e:
        print(f"Error in query_products: {e}")
        raise HTTPException(status_code=500, detail=f"Database query error: {str(e)}")
    finally:
        await conn.close()

# Get all products
@app.get("/products/", response_model=List[ProductResponse])
async def read_products( # Made async
    limit: int = Query(100, ge=1, le=1000),
    offset: int = Query(0, ge=0)
):
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            await cursor.execute("SELECT * FROM products LIMIT ? OFFSET ?", (limit, offset))
            results = await cursor.fetchall()
            return [dict_to_product(row) for row in results]
    except Exception as e:
        print(f"Error in read_products: {e}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()

# Get a specific product by ID or product_id
@app.get("/products/{product_identifier}", response_model=ProductResponse)
async def read_product(product_identifier: str): # Made async
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            # First try to find by id (primary key)
            await cursor.execute("SELECT * FROM products WHERE id = ?", (product_identifier,))
            result = await cursor.fetchone()

            # If not found, try to find by product_id field
            if not result:
                await cursor.execute("SELECT * FROM products WHERE product_id = ?", (product_identifier,))
                result = await cursor.fetchone()

            if not result:
                raise HTTPException(status_code=404, detail="Product not found")

            return dict_to_product(result)
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error in read_product: {e}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()

# Update a product
@app.put("/products/{product_identifier}", response_model=ProductResponse)
async def update_product(product_identifier: str, product_update: Product): # Made async
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            # Check if product exists by id or product_id
            await cursor.execute("SELECT * FROM products WHERE id = ? OR product_id = ?", (product_identifier, product_identifier))
            result = await cursor.fetchone()

            if not result:
                raise HTTPException(status_code=404, detail="Product not found")

            existing_product = dict(result)
            db_id = existing_product["id"] # Use the primary key 'id' for update

            # Prepare update data
            update_data = product_update.dict(exclude_unset=True)

            # Convert list to JSON string
            if "product_img" in update_data:
                update_data["product_img"] = json.dumps(update_data["product_img"])

            # Add updated timestamp
            update_data["updated_at"] = datetime.now().isoformat()

            # Cannot update the primary key 'id' or the potentially unique 'product_id' via PUT easily
            # Ensure these keys are not in the update data if they are meant to be immutable identifiers
            update_data.pop("id", None)
            # update_data.pop("product_id", None) # Allow updating product_id if needed, but be careful with uniqueness

            if not update_data:
                 raise HTTPException(status_code=400, detail="No update data provided.")

            # Build the SQL query
            set_clause = ", ".join([f"{k} = ?" for k in update_data.keys()])
            params = list(update_data.values())
            params.append(db_id) # Use primary key in WHERE clause

            query = f"UPDATE products SET {set_clause} WHERE id = ?"
            await cursor.execute(query, params)
            await conn.commit()

            # Get the updated product
            await cursor.execute("SELECT * FROM products WHERE id = ?", (db_id,))
            updated_result = await cursor.fetchone()
            if not updated_result:
                 raise HTTPException(status_code=404, detail="Updated product not found after update.")


            return dict_to_product(updated_result)
    except HTTPException:
        raise
    except aiosqlite.IntegrityError as e:
         # Handle potential unique constraint violation if product_id was updated
         if "UNIQUE constraint failed: products.product_id" in str(e):
              raise HTTPException(status_code=409, detail=f"Cannot update: Product ID '{product_update.product_id}' might already exist.")
         else:
              raise HTTPException(status_code=500, detail=f"Database integrity error during update: {str(e)}")
    except Exception as e:
        print(f"Error in update_product: {e}")
        raise HTTPException(status_code=500, detail=f"Database error during update: {str(e)}")
    finally:
        await conn.close()

# Delete a product
@app.delete("/products/{product_identifier}", response_model=Dict[str, Any])
async def delete_product(product_identifier: str): # Made async
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            # Check if product exists and get its data
            await cursor.execute("SELECT * FROM products WHERE id = ? OR product_id = ?", (product_identifier, product_identifier))
            result = await cursor.fetchone()

            if not result:
                raise HTTPException(status_code=404, detail="Product not found")

            product_data = dict_to_product(result)
            db_id = product_data["id"] # Use primary key for deletion

            # Delete the product
            await cursor.execute("DELETE FROM products WHERE id = ?", (db_id,))
            await conn.commit()

            return {
                "message": "Product deleted successfully",
                "deleted_product": product_data # Return the data of the deleted product
            }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error in delete_product: {e}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()

# --- Schema Manipulation Routes (Modified for Async) ---

@app.post("/schema/add-columns", response_model=Dict[str, Any])
async def add_columns(request: AddColumnRequest): # Made async
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            # Get current column information
            await cursor.execute("PRAGMA table_info(products)")
            existing_columns_info = await cursor.fetchall()
            existing_columns = {row["name"] for row in existing_columns_info}

            added_columns = []
            for column in request.columns:
                if column.column_name in existing_columns:
                    print(f"Column '{column.column_name}' already exists, skipping.")
                    continue  # Skip if column already exists

                # Basic validation for column name and type
                if not column.column_name.isidentifier():
                     raise HTTPException(status_code=400, detail=f"Invalid column name: {column.column_name}")
                # Add more type validation if needed

                # Build ALTER TABLE query
                query = f"ALTER TABLE products ADD COLUMN {column.column_name} {column.column_type}"

                # Add default value if provided
                if column.default_value is not None:
                    # Properly quote string defaults
                    if isinstance(column.default_value, str):
                        # Basic sanitation for default string to prevent injection
                        safe_default = column.default_value.replace("'", "''")
                        query += f" DEFAULT '{safe_default}'"
                    elif isinstance(column.default_value, (int, float, bool)):
                         # Handle boolean specifically if needed by DB type
                         default_val = 1 if column.default_value is True else (0 if column.default_value is False else column.default_value)
                         query += f" DEFAULT {default_val}"
                    else:
                         # Consider raising error for unsupported default types
                         print(f"Warning: Default value type {type(column.default_value)} for column {column.column_name} might not be directly supported.")
                         # Attempt to add without quoting - may fail depending on type/DB
                         query += f" DEFAULT {column.default_value}"


                try:
                    await cursor.execute(query)
                    added_columns.append(column.column_name)
                    print(f"Executed: {query}")
                except Exception as alter_err:
                     print(f"Error executing ALTER TABLE: {alter_err}")
                     # Rollback is implicitly handled by closing connection on error typically,
                     # but explicit rollback might be needed depending on transaction state management elsewhere
                     raise HTTPException(status_code=500, detail=f"Failed to add column '{column.column_name}': {alter_err}")

            await conn.commit()

            return {
                "message": "Columns added successfully (or skipped if existing)",
                "added_columns": added_columns
            }
    except HTTPException:
        raise
    except Exception as e:
        print(f"Error in add_columns: {e}")
        raise HTTPException(status_code=500, detail=f"Database schema error: {str(e)}")
    finally:
        await conn.close()


@app.post("/schema/remove-columns", response_model=Dict[str, Any])
async def remove_columns(request: RemoveColumnRequest): # Made async
    """Removes columns by recreating the table (standard SQLite practice)."""
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            # SQLite doesn't support DROP COLUMN directly
            # We need to create a new table without those columns and migrate the data

            # Get current table structure
            await cursor.execute("PRAGMA table_info(products)")
            columns = await cursor.fetchall()
            column_details = {col["name"]: col for col in columns}
            column_names = [col["name"] for col in columns]

            # Validate requested columns to remove
            columns_to_remove = set(request.columns)
            existing_columns = set(column_names)

            if not columns_to_remove.issubset(existing_columns):
                non_existent = columns_to_remove - existing_columns
                raise HTTPException(status_code=400, detail=f"Columns don't exist: {', '.join(non_existent)}")

            # Don't allow removing the id primary key column
            if "id" in columns_to_remove:
                raise HTTPException(status_code=400, detail="Cannot remove the primary key 'id' column")

            # Create new column list and definitions without the columns to remove
            new_columns = [col for col in column_names if col not in columns_to_remove]
            if not new_columns:
                 raise HTTPException(status_code=400, detail="Cannot remove all columns.")

            columns_definition = []
            for name in new_columns:
                col = column_details[name]
                # Recreate column definition
                type_name = col["type"]
                not_null = "NOT NULL" if col["notnull"] else ""
                pk = "PRIMARY KEY" if col["pk"] else ""
                # Correctly handle default values, including strings
                default_val_str = ""
                if col["dflt_value"] is not None:
                    default_val = col["dflt_value"]
                    # Check if the default value looks like a string literal (starts/ends with ')
                    # or if the type suggests it should be a string
                    if isinstance(default_val, str) and (default_val.startswith("'") and default_val.endswith("'")):
                         default_val_str = f"DEFAULT {default_val}" # Already quoted
                    elif isinstance(default_val, str):
                         safe_default = default_val.replace("'", "''") # Quote if it's a string but not already quoted by PRAGMA
                         default_val_str = f"DEFAULT '{safe_default}'"
                    else:
                        # Assume numeric/other types don't need quotes from PRAGMA output
                         default_val_str = f"DEFAULT {default_val}"

                col_def = f"{name} {type_name} {pk} {not_null} {default_val_str}".strip().replace("  ", " ")
                columns_definition.append(col_def)

            # Use explicit transaction control for safety
            await conn.execute("BEGIN TRANSACTION")

            try:
                # Create new table
                new_table_sql = f"CREATE TABLE products_new ({', '.join(columns_definition)})"
                print(f"Executing: {new_table_sql}")
                await cursor.execute(new_table_sql)

                # Copy data
                copy_sql = f"INSERT INTO products_new ({', '.join(new_columns)}) SELECT {', '.join(new_columns)} FROM products"
                print(f"Executing: {copy_sql}")
                await cursor.execute(copy_sql)

                # Drop old table
                print("Executing: DROP TABLE products")
                await cursor.execute("DROP TABLE products")

                # Rename new one
                print("Executing: ALTER TABLE products_new RENAME TO products")
                await cursor.execute("ALTER TABLE products_new RENAME TO products")

                # Commit the transaction
                await conn.commit()

            except Exception as migration_err:
                 print(f"Error during table migration: {migration_err}. Rolling back.")
                 await conn.rollback() # Rollback changes on error
                 raise HTTPException(status_code=500, detail=f"Failed to migrate table structure: {migration_err}")


            return {
                "message": "Columns removed successfully by recreating the table",
                "removed_columns": list(columns_to_remove)
            }
    except HTTPException:
        await conn.rollback() # Ensure rollback if HTTP error occurred before commit
        raise
    except Exception as e:
        await conn.rollback() # Ensure rollback on generic error
        print(f"Error in remove_columns: {e}")
        raise HTTPException(status_code=500, detail=f"Database schema error: {str(e)}")
    finally:
        await conn.close()


@app.get("/schema", response_model=Dict[str, Any])
async def get_schema(): # Made async
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            # Get table information
            await cursor.execute("PRAGMA table_info(products)")
            columns = await cursor.fetchall()

            if not columns:
                 # Consider what to return if table doesn't exist or has no columns
                 # This might indicate an issue post-initialization or after remove_columns
                 raise HTTPException(status_code=404, detail="Products table schema not found or table is empty.")


            schema_info = []
            for col in columns:
                schema_info.append({
                    "name": col["name"],
                    "type": col["type"],
                    "not_null": bool(col["notnull"]),
                    "default_value": col["dflt_value"],
                    "primary_key": bool(col["pk"])
                })

            # Optionally get info for orders table too
            await cursor.execute("PRAGMA table_info(orders)")
            orders_columns = await cursor.fetchall()
            orders_schema_info = []
            for col in orders_columns:
                 orders_schema_info.append({
                     "name": col["name"],
                     "type": col["type"],
                     "not_null": bool(col["notnull"]),
                     "default_value": col["dflt_value"],
                     "primary_key": bool(col["pk"])
                 })


            return {
                "products_table": {
                     "table_name": "products",
                     "columns": schema_info
                },
                 "orders_table": {
                     "table_name": "orders",
                     "columns": orders_schema_info
                }
            }
    except Exception as e:
        print(f"Error in get_schema: {e}")
        raise HTTPException(status_code=500, detail=f"Database schema retrieval error: {str(e)}")
    finally:
        await conn.close()


# --- Backup Route (Modified for Async - uses synchronous backup within) ---

@app.get("/backup", response_model=Dict[str, str])
async def create_backup(): # Made async
    """Creates a backup of the database file."""
    backup_path = f"backup_{datetime.now().strftime('%Y%m%d_%H%M%S')}.db"
    try:
        # aiosqlite doesn't have async backup. Use standard sqlite3 for this.
        # This will block the event loop briefly during backup.
        # For large DBs, consider running this in a separate thread using asyncio.to_thread
        source_conn = await aiosqlite.connect(DB_PATH) # Connect async to ensure current state
        await source_conn.commit() # Ensure any pending async writes are flushed

        # Use synchronous connection for the backup target and the backup call itself
        backup_conn_sync = sqlite3.connect(backup_path)
        source_conn_sync = sqlite3.connect(DB_PATH) # Need sync connection for backup source arg

        with backup_conn_sync:
             source_conn_sync.backup(backup_conn_sync)

        source_conn_sync.close()
        backup_conn_sync.close()
        await source_conn.close() # Close the async connection used initially

        print(f"Backup created: {backup_path}")
        return {
            "message": "Backup created successfully",
            "backup_file": backup_path
        }
    except Exception as e:
        # Attempt to close connections if they were opened
        try: await source_conn.close()
        except: pass
        try: source_conn_sync.close()
        except: pass
        try: backup_conn_sync.close()
        except: pass

        print(f"Error during backup: {e}")
        raise HTTPException(status_code=500, detail=f"Backup error: {str(e)}")


# --- Product Bulk Details & Order Routes (Modified for Async) ---

@app.post("/products/bulk-details")
async def get_products_bulk(data: dict): # Made async
    """Get details for multiple products and calculate totals."""
    conn = await get_db_connection()
    product_details = []
    total_mrp = 0.0
    total_rate = 0.0
    total_gst = 0.0
    total_discount = 0.0
    total = 0.0
    not_found_ids = []

    try:
        async with conn.cursor() as cursor:
            for prod_id, item_data in data.items():
                count = item_data.get("count", 0)
                if not isinstance(count, int) or count < 0:
                    count = 0 # Ignore invalid counts

                # Try to find product by id or product_id
                await cursor.execute("SELECT * FROM products WHERE id = ? OR product_id = ?", (prod_id, prod_id))
                row = await cursor.fetchone()

                if not row:
                    not_found_ids.append(prod_id)
                    continue # Skip to next product if not found

                product = dict_to_product(row)
                product['requested_count'] = count # Add requested count to details
                product_details.append(product)

                # Calculation based on fetched product data
                mrp = product.get("cost_mrp", 0.0)
                rate = product.get("cost_rate", 0.0)
                gst_percent = product.get("cost_gst", 0.0)
                disc_percent = product.get("cost_dis", 0.0)

                gst_amount = (rate * gst_percent) / 100.0
                actual_rate = rate + gst_amount
                disc_amount = mrp * disc_percent / 100.0  # Discount based on the original rate

                total_mrp += mrp * count
                total_rate += rate * count
                total_gst += gst_amount * count
                total_discount += disc_amount * count
                total += actual_rate * count

        response = {
            "product_details": product_details,
            "cost": {
                "total_mrp": round(total_mrp, 2),
                "total_rate": round(total_rate, 2),
                "total_gst": round(total_gst, 2),
                "total_discount": round(total_discount, 2),
                "total": round(total, 2)
            }
        }
        if not_found_ids:
             response["warnings"] = [f"Product ID '{pid}' not found." for pid in not_found_ids]

        return response

    except Exception as e:
        print(f"Error in get_products_bulk: {e}")
        raise HTTPException(status_code=500, detail=f"Database error processing bulk details: {str(e)}")
    finally:
        await conn.close()

# from num2words import num2words
# --- Helper function to convert amount to Indian currency words ---
def amount_to_words_inr(amount: float) -> str:
    """Converts a float amount to Indian Rupees and Paise in words without external libraries."""
    
    ones = ["", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen",
            "Sixteen", "Seventeen", "Eighteen", "Nineteen"]
    
    tens = ["", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"]

    def two_digit_word(n):
        if n < 20:
            return ones[n]
        else:
            return tens[n // 10] + (" " + ones[n % 10] if (n % 10) != 0 else "")

    def convert_to_words(n):
        if n == 0:
            return "Zero"
        parts = []
        if n >= 10000000:
            parts.append(convert_to_words(n // 10000000) + " Crore")
            n %= 10000000
        if n >= 100000:
            parts.append(convert_to_words(n // 100000) + " Lakh")
            n %= 100000
        if n >= 1000:
            parts.append(convert_to_words(n // 1000) + " Thousand")
            n %= 1000
        if n >= 100:
            parts.append(convert_to_words(n // 100) + " Hundred")
            n %= 100
        if n > 0:
            if parts:
                parts.append("and " + two_digit_word(n))
            else:
                parts.append(two_digit_word(n))
        return " ".join(parts)

    rupees = int(amount)

    # Truncate paise to 2 digits without rounding
    if('.' in str(amount)):
        paise_str = str(amount).split('.')[-1][:2]
    else:
        paise_str = str(amount)
    
    paise = int(paise_str) if paise_str else 0

    rupees_words = convert_to_words(rupees)
    paise_words = convert_to_words(paise) if paise > 0 else ""

    result = f"INR {rupees_words} Rupees"
    if paise > 0:
        result += f" and {paise_words} Paise"
    result += " Only"
    return result


def generate_invoice_data(
    order_id: str,
    creation_time_str: str, # UTC ISO format string like '2023-10-27T10:00:00Z'
    user_id: str,
    product_details_for_order: List[Dict[str, Any]], # List of product dicts
    user_data_get,
    order_values: Dict[str, Any] # Corresponds to 'value' in store_order
) -> Dict[str, Any]:
    """
    Generates an invoice data dictionary in the desired format using
    data collected during the store_order process.
    """

    # --- 1. Initialize Invoice Structure ---
    invoice_data = {
        'company': {},
        'details': {},
        'consignee': {},
        'buyer': {},
        'items': [],
        'totals': {},
        'gstDetails': [],
        'amountsInWords': {}
    }

    # --- 2. Populate 'details' ---
    # Extract date from the ISO string
    creation_date = datetime.fromisoformat(creation_time_str.replace('Z', '+00:00'))
    formatted_date = creation_date.strftime('%Y-%m-%d')

    invoice_data['details'] = {
        'invoiceNo': f"INV-{order_id}", # Use order_id for uniqueness
        'dated': formatted_date,
        'deliveryNote': f"DN-{order_id}", # Placeholder using order_id
        'refNoDate': f"REF-{user_id}", # Placeholder
        'otherRef': order_values.get("notes", "N/A"), # Use notes if available
        'checkedBy': "System Generated" # Placeholder
    }

    # --- 3. Populate 'consignee' and 'buyer' ---
    # Assuming the address in 'value' is the consignee (shipping address)
    invoice_data['consignee'] = {
        'name': user_data_get("name", "N/A"),
        'address': order_values.get("address", "N/A"),
        # 'contactNo': order_values.get("contactNoConsignee", "N/A") # Placeholder - Not available in store_order data
    }

    # Buyer details are not directly available in store_order
    # You might need to fetch these based on user_id separately
    invoice_data['buyer'] = {
        'name': user_data_get("name", "N/A"),
        'address': user_data_get("address", "N/A"),
        'contactNo': user_data_get("contact", "N/A"),
        'gstin': user_data_get("gstin", "N/A")
    }

    # --- 4. Process Items and Calculate Totals ---
    sub_total = 0.0
    total_cgst_amount = 0.0
    total_sgst_amount = 0.0
    gst_summary = {} # To aggregate GST details by HSN

    for index, prod in enumerate(product_details_for_order):
        s_no = index + 1
        mrp = prod.get("cost_mrp", 0.0)
        rate = prod.get("cost_rate", 0.0)
        count = prod.get("count", 0)
        discount_percent = prod.get("cost_dis", 0.0)
        gst_percent = prod.get("cost_gst", 0.0)
        hsn_sac = prod.get("product_hsn", "N/A")

        taxable_rate = rate * count
        # Calculate GST amounts for this item (assuming CGST = SGST = GST/2)
        cgst_rate_item = gst_percent / 2.0
        sgst_rate_item = gst_percent / 2.0
        cgst_amount_item = ((rate * cgst_rate_item) / 100.0) * count
        sgst_amount_item = ((rate * sgst_rate_item) / 100.0) * count
        total_tax_item = cgst_amount_item + sgst_amount_item

        # Append to 'items' list
        prod_cid = prod.get("product_cid", "N/A")
        invoice_data['items'].append({
            'sNo': s_no,
            'description': prod.get("product_name", "N/A"),
            'hsnSac': hsn_sac,
            'partNo': prod_cid,
            'quantityShipped': f'{count} No',
            'quantityBilled': f'{count} No',
            'mrp': round(mrp, 2),
            'discount': f'{discount_percent} %',
            'rate': round(rate, 2),
            'amount': round(taxable_rate, 2) # Amount after discount
        })

        # Aggregate GST details by HSN
        if prod_cid not in gst_summary:
            gst_summary[prod_cid] = {
                'taxableValue': 0.0,
                'cgstRate': f'{cgst_rate_item}%', # Assuming same rate for all items with same HSN
                'cgstAmount': 0.0,
                'sgstUtgstRate': f'{sgst_rate_item}%', # Assuming same rate
                'sgstUtgstAmount': 0.0,
                'totalTaxAmount': 0.0
            }
        gst_summary[prod_cid]['taxableValue'] += taxable_rate
        gst_summary[prod_cid]['cgstAmount'] += cgst_amount_item
        gst_summary[prod_cid]['sgstUtgstAmount'] += sgst_amount_item
        gst_summary[prod_cid]['totalTaxAmount'] += total_tax_item

        # Add to overall totals
        sub_total += taxable_rate
        total_cgst_amount += cgst_amount_item
        total_sgst_amount += sgst_amount_item

    # --- 5. Populate 'gstDetails' from summary ---
    for prod_cid, details in gst_summary.items():
         invoice_data['gstDetails'].append({
            'hsnSac': prod_cid,
            'taxableValue': round(details['taxableValue'], 2),
            'cgstRate': details['cgstRate'],
            'cgstAmount': round(details['cgstAmount'], 2),
            'sgstUtgstRate': details['sgstUtgstRate'],
            'sgstUtgstAmount': round(details['sgstUtgstAmount'], 2),
            'totalTaxAmount': round(details['totalTaxAmount'], 2)
        })

    # --- 6. Populate 'totals' ---
    grand_total = sub_total + total_cgst_amount + total_sgst_amount
    # Assuming no special discount or rounding based on store_order logic
    special_discount = 0.0
    round_off = 0.00 # Or calculate if specific rounding rules apply
    final_total = grand_total - special_discount + round_off


    invoice_data['totals'] = {
        'subTotal': round(sub_total, 2),
        'cgstAmount': round(total_cgst_amount, 2),
        'sgstAmount': round(total_sgst_amount, 2),
        'specialDiscount': round(special_discount, 2), # Placeholder
        'roundOff': round(round_off, 2), # Placeholder
        'total': round(final_total, 2)
    }

    # --- 7. Populate 'amountsInWords' ---
    total_tax_amount = total_cgst_amount + total_sgst_amount
    invoice_data['amountsInWords'] = {
        'amountChargeable': amount_to_words_inr(final_total),
        'taxAmount': amount_to_words_inr(total_tax_amount)
    }

    invoice_data["company"] = {
        'name': 'Petsfort',
        'address': 'Your Company Address, City, Postal Code',
        'gstNo': 'YOUR_GST_NUMBER',
        'email': 'petsfort.in@gamil.com'
    }


    # --- 8. Return the complete invoice data ---
    return invoice_data

import asyncio
order_lock = asyncio.Lock()
@app.post("/orders/checkout/{user_id}", response_model=Dict[str, Any])
async def store_order(user_id: str, data: dict): # Made async
    """Stores a new order after calculating costs based on product data."""
    async with order_lock:
        conn = await get_db_connection()
        products_to_update_stock = []
        value = data.pop('otherData')

        product_details_for_order = [] # Details stored in the order
        total_mrp = 0.0
        total_rate = 0.0
        total_gst = 0.0
        total_discount = 0.0
        total = 0.0
        not_found_ids = []
        order_items_payload = {} # Payload as received


        # Validate input data structure
        if not isinstance(data, dict):
            raise HTTPException(status_code=400, detail="Invalid request body: Expected a JSON object of product IDs and counts.")

        try:
            async with conn.cursor() as cur:
                # First pass: Validate products and calculate totals
                for pid, item_info in data.items():
                    if not isinstance(item_info, dict) or "count" not in item_info:
                        raise HTTPException(status_code=400, detail=f"Invalid item data for product ID '{pid}'. Expected {{'count': number}}.")

                    count = item_info.get("count", 0)
                    if not isinstance(count, int) or count <= 0:
                        print(f"Warning: Invalid or zero count ({count}) for product ID '{pid}', skipping.")
                        continue # Skip items with invalid or zero count

                    await cur.execute("SELECT * FROM products WHERE id=? OR product_id=?", (pid, pid))
                    row = await cur.fetchone()
                    if not row:
                        not_found_ids.append(pid)
                        continue # Collect not found IDs, proceed if others are valid

                    prod = dict(row)

                    requested_count = item_info.get("count", 0)
                    available_stock = prod.get("stock", 0)

                    # Check stock
                    if available_stock < requested_count:
                        return {
                            "message" : "OutOfStock",
                            "product_available_stock" : available_stock,
                            "product_id" : pid,
                            "product_name" : prod.get("product_name", "product")
                        }
                    else:
                        products_to_update_stock.append((pid,available_stock-requested_count))



                    # Store necessary details for the order record
                    product_details_for_order.append({
                        "id": prod["id"],
                        "product_id": prod.get("product_id"),
                        "product_name": prod.get("product_name"),
                        "product_desc": prod.get("product_desc"),
                        "product_hsn": prod.get("product_hsn"),
                        "product_cid": prod.get("product_cid"),
                        "product_img": prod.get("product_img"),
                        "cat_id": prod.get("cat_id"),
                        "cat_sub": prod.get("cat_sub"),
                        "created_at": prod.get("created_at"),
                        "updated_at": prod.get("updated_at"),
                        "cost_mrp": prod.get("cost_mrp", 0.0),
                        "cost_rate": prod.get("cost_mrp", 0.0) - ((prod.get("cost_mrp", 0.0) * prod.get("cost_dis", 0.0)) / 100),
                        "cost_gst": prod.get("cost_gst", 0.0),
                        "stock": prod.get("stock", 0.0),
                        "cost_dis": prod.get("cost_dis", 0.0),
                        "count": count # Store the count for this item in the order details
                    })
                    
                    order_items_payload[pid] = {"count": count} # Rebuild payload with only valid items

                    # Recalculate totals based on DB data for valid items
                    # rate = prod.get("cost_rate", 0.0)
                    # gst_percent = prod.get("cost_gst", 0.0)
                    # disc_percent = prod.get("cost_dis", 0.0)
                    # gst_amt = (rate * gst_percent) / 100.0
                    # base = rate + gst_amt
                    # disc_amt = (base * disc_percent) / 100.0

                    # total_rate += rate * count
                    # total_gst += gst_amt * count
                    # total_discount += disc_amt * count
                    # total += (base - disc_amt) * count


                    mrp = prod.get("cost_mrp", 0.0)
                    rate = prod.get("cost_mrp", 0.0) - ((prod.get("cost_mrp", 0.0) * prod.get("cost_dis", 0.0)) / 100)
                    gst_percent = prod.get("cost_gst", 0.0)
                    disc_percent = prod.get("cost_dis", 0.0)

                    gst_amount = (rate * gst_percent) / 100.0
                    actual_rate = rate + gst_amount
                    disc_amount = mrp * disc_percent / 100.0  # Discount based on the original rate

                    total_mrp += mrp * count
                    total_rate += rate * count
                    total_gst += gst_amount * count
                    total_discount += disc_amount * count
                    total += actual_rate * count


                # Check if any products were not found
                if not_found_ids:
                    raise HTTPException(status_code=404, detail=f"Products not found: {', '.join(not_found_ids)}")

                # Check if there are any valid items to create an order
                if not order_items_payload:
                    raise HTTPException(status_code=400, detail="No valid items found in the request to create an order.")

                # await cur.execute("SELECT * FROM userdata WHERE id=? OR uid=?", (user_id, user_id))
                # user_row = await cur.fetchone()
                # user_name = "A User"
                # if user_row:
                #     user_name = user_row[0]
                #     user_blocked = user_row[1]
                #     if(int(user_blocked) != 0):
                #         raise HTTPException(status_code=400, detail="User is Blocked")
                # else:
                #     raise HTTPException(status_code=400, detail="User Not found.")
                await cur.execute("SELECT * FROM userdata WHERE id=? OR uid=?", (user_id, user_id))
                user_detail = await cur.fetchone()

                if not user_detail:
                    raise HTTPException(status_code=400, detail="User Not found.")

                # Get column names from cursor description
                column_names = [desc[0] for desc in cur.description]

                def user_data(key, default):
                    # Find the indexes of 'name' and 'isblocked' dynamically
                    try:
                        index = column_names.index(key)
                        return user_detail[index]
                    except:
                        return default

                user_name = user_data("name","Not Found")
                user_blocked = user_data("isblocked", 1)

                if int(user_blocked) != 0:
                    raise HTTPException(status_code=400, detail="User is Blocked")


                for product_id_t, new_stock_count_t in products_to_update_stock:
                    await cur.execute("UPDATE products SET stock = ? WHERE id = ? OR product_id=?", (new_stock_count_t, product_id_t, product_id_t))

                await cur.execute("UPDATE userdata SET credits=credits-? WHERE id=? OR uid=?", (total,user_id,user_id))

                # Insert order if all products were found and valid
                order_id = generate_short_random_id() # Use the short ID generator
                now = datetime.utcnow().isoformat() + "Z" # Add Z for UTC timezone indicator

                insert_query = """
                    INSERT INTO orders
                    (order_id, user_id, items, items_detail, order_status, total_rate, total_gst, total_discount, total, created_at, address, notes)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """
                params = (
                    order_id, user_id,
                    json.dumps(order_items_payload), # Store the validated item counts
                    json.dumps(product_details_for_order), # Store the fetched product details for this order
                    "ORDER_PENDING",
                    round(total_rate, 3),
                    round(total_gst, 3),
                    round(total_discount, 3),
                    round(total, 3),
                    now,
                    value["address"],
                    value["notes"]
                )
                await cur.execute(insert_query, params)
                await conn.commit()


                try:
                    # *** Generate the invoice data HERE ***
                    final_invoice_data = generate_invoice_data(
                        order_id=order_id,
                        creation_time_str=now,
                        user_id=user_id,
                        product_details_for_order=product_details_for_order,
                        user_data_get=user_data,
                        order_values=value # Pass the 'otherData' dict
                    )

                    raw_json_string = json.dumps(final_invoice_data, indent=None, separators=(',', ':'))
                    await cur.execute("INSERT INTO bills(order_id, bill) VALUES(?, ?)", (order_id, raw_json_string))
                    await conn.commit()
                except Exception as e:
                    print(str(e))

                await FCM_notify_order_checkout(user_id, round(total, 3), user_name)


                return {
                    "message": "Order created successfully",
                    "order_id": order_id,
                    "user_id": user_id,
                    "order_status": "ORDER_PENDING",
                    "total": round(total, 3)
                }
        except HTTPException:
            await conn.rollback() # Rollback if HTTP error occurred
            raise
        except Exception as e:
            await conn.rollback() # Rollback on generic error
            print(f"Error in store_order: {e}")
            # Consider more specific error handling (e.g., serialization errors)
            raise HTTPException(status_code=500, detail=f"Failed to store your order")
        finally:
            await conn.close()


import traceback
@app.get("/bills/{order_id}")
async def get_bill(order_id: str):
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            await cursor.execute("SELECT bill FROM bills WHERE order_id=?", (order_id,))
            bill_row = await cursor.fetchone()
            bill = "{}"
            if bill_row:
                bill = bill_row[0]
                bill = json.loads(bill)
            return bill
    except Exception as e:
        print(f"Error in get_bill: {e}")
        raise HTTPException(status_code=500, detail=f"Database query error: {str(e)}")
    finally:
        await conn.close()



@app.post("/orders/query", response_model=List[OrderQueryResponse])
async def query_orders(query_request: QueryRequest):
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            where_clause, params = build_query(query_request.filters)
            query = f"""
                SELECT orders.*, userdata.name AS user_name
                FROM orders
                JOIN userdata ON orders.user_id = userdata.uid
                WHERE {where_clause}
            """

            # Add ordering
            if query_request.order_by:
                direction = query_request.order_direction.upper() if query_request.order_direction else "ASC"
                if direction not in ("ASC", "DESC"): direction = "ASC"
                safe_order_by = ''.join(c for c in query_request.order_by if c.isalnum() or c == '_')
                if safe_order_by:
                    query += f" ORDER BY {safe_order_by} {direction}"

            # Add limit/offset
            if query_request.limit is not None:
                query += f" LIMIT ?"
                params.append(query_request.limit)
                if query_request.offset:
                    query += f" OFFSET ?"
                    params.append(query_request.offset)

            await cursor.execute(query, params)
            results = await cursor.fetchall()
            return [dict_to_order(row) for row in results]
    except Exception as e:
        print(f"Error in query_orders: {e}")
        raise HTTPException(status_code=500, detail=f"Database query error: {str(e)}")
    finally:
        await conn.close()

@app.put("/orders/{order_id}", response_model=OrderResponse)
async def update_order(order_id: str, order_update: OrderUpdate): # Made async
    conn = await get_db_connection()
    update_fields = []
    params = []
    notifyStatus = False
    
    # Build SET clause and params dynamically
    if order_update.order_status is not None:
        update_fields.append("order_status = ?")
        params.append(order_update.order_status)
        notifyStatus = True
    if order_update.items is not None:
        # Add validation for items structure if needed
        update_fields.append("items = ?")
        params.append(json.dumps(order_update.items))
    if order_update.items_detail is not None:
        # Add validation for items_detail structure if needed
        update_fields.append("items_detail = ?")
        params.append(json.dumps(order_update.items_detail))
    # Add other fields similarly
    if order_update.total_rate is not None:
        update_fields.append("total_rate = ?")
        params.append(round(order_update.total_rate, 3))
    if order_update.total_gst is not None:
        update_fields.append("total_gst = ?")
        params.append(round(order_update.total_gst, 3))
    if order_update.total_discount is not None:
        update_fields.append("total_discount = ?")
        params.append(round(order_update.total_discount, 3))
    if order_update.total is not None:
        update_fields.append("total = ?")
        params.append(round(order_update.total, 3))


    if not update_fields:
        # Close connection before raising HTTP exception if no updates
        await conn.close()
        raise HTTPException(status_code=400, detail="No valid fields provided for update.")

    params.append(order_id) # Add order_id for WHERE clause
    set_clause = ", ".join(update_fields)
    update_query = f"UPDATE orders SET {set_clause} WHERE order_id = ?"

    try:
        async with conn.cursor() as cur:
            await cur.execute(update_query, params)
            if cur.rowcount == 0:
                 raise HTTPException(status_code=404, detail="Order not found or no changes made.")
            await conn.commit()

            # Fetch the updated order
            await cur.execute("SELECT * FROM orders WHERE order_id = ?", (order_id,))
            updated_order = await cur.fetchone()
            if not updated_order: # Should not happen if update succeeded, but check anyway
                 raise HTTPException(status_code=404, detail="Order not found after update attempt.")
            
            status = updated_order["order_status"]
            if(status == "ORDER_PENDING"):
                status = "pending"
            elif(status == "ORDER_IN_PROGRESS"):
                status = "in progress"
            elif(status == "ORDER_DELIVERED"):
                status = "delivered"
            elif(status == "ORDER_CANCELLED"):
                status = "cancelled"
            await FCM_notify_order_status_change(updated_order["user_id"],status)
            return dict_to_order(updated_order)
    except HTTPException:
        await conn.rollback() # Rollback on explicit HTTP errors
        raise
    except Exception as e:
        await conn.rollback() # Rollback on other errors
        print(f"Error in update_order: {e}")
        raise HTTPException(status_code=500, detail=f"Failed to update order: {str(e)}")
    finally:
        await conn.close()


@app.delete("/orders/{order_id}", response_model=Dict[str, str])
async def delete_order(order_id: str): # Made async
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute("DELETE FROM orders WHERE order_id = ?", (order_id,))
            if cur.rowcount == 0:
                raise HTTPException(status_code=404, detail="Order not found.")
            await conn.commit()
            return {"message": "Order deleted successfully"}
    except HTTPException:
        # No rollback needed for DELETE usually, but doesn't hurt
        await conn.rollback()
        raise
    except Exception as e:
        await conn.rollback()
        print(f"Error in delete_order: {e}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()



class Category(BaseModel):
    id: str
    name: str
    image: Optional[str] = None

class Subcategory(BaseModel):
    id: str
    parentid: str
    name: str
    image: Optional[str] = None

# --- Category Endpoints ---

@app.get("/categories", response_model=List[Category])
async def get_category_list():
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            await cursor.execute("SELECT id, name, image FROM category")
            rows = await cursor.fetchall()
            categories = [Category(**row) for row in rows]

            # START Bug Fix
            fixed_categories = []
            category = None
            for cat in categories:
                if(cat.id != "cc41f1da652f4"):
                    fixed_categories.append(cat)
                else:
                    category = cat
            categories = fixed_categories
            if category:categories.insert(0, category)
            # END Bug Fix

            return categories
    except Exception as e:
        print(f"Error in get_category_list: {e}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()

@app.post("/categories")
async def add_category(category: Category):
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            await cursor.execute(
                "INSERT OR REPLACE INTO category (id, name, image) VALUES (?, ?, ?)",
                (category.id, category.name, category.image),
            )
            await conn.commit()
        return {"message": "Category added successfully"}
    except Exception as e:
        print(f"Error in add_category: {e}")
        await conn.rollback()
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()

@app.delete("/categories/{cat_id}", response_model=Dict[str, str])
async def delete_order(cat_id: str): # Made async
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute("DELETE FROM category WHERE id = ?", (cat_id,))
            if cur.rowcount == 0:
                raise HTTPException(status_code=404, detail="Order not found.")
            await conn.commit()
            return {"message": "Category deleted successfully"}
    except HTTPException:
        # No rollback needed for DELETE usually, but doesn't hurt
        await conn.rollback()
        raise
    except Exception as e:
        await conn.rollback()
        print(f"Error in delete_category: {e}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()


# --- Subcategory Endpoints ---

@app.get("/subcategories", response_model=List[Subcategory])
async def get_subcategory_list():
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            await cursor.execute("SELECT id, parentid, name, image FROM subcategory")
            rows = await cursor.fetchall()
            categories = [Subcategory(**row) for row in rows]
            return categories
    except Exception as e:
        print(f"Error in get_category_list: {e}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()

# @app.get("/subcats/{category_id}", response_model=List[Subcategory])
# async def get_subcategory_list2(category_id: str):
#     conn = await get_db_connection()
#     try:
#         async with conn.cursor() as cursor:
#             await cursor.execute("SELECT id, parentid, name, image FROM subcategory WHERE parentid=?", (category_id,))
#             rows = await cursor.fetchall()
#             categories = [Subcategory(**row) for row in rows]
#             return categories
#     except Exception as e:
#         print(f"Error in get_category_list: {e}")
#         raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
#     finally:
#         await conn.close()

@app.get("/subcats_v0/{category_id}", response_model=List[Subcategory])
async def get_available_subcategory_list(category_id: str):
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            await cursor.execute("""
                SELECT DISTINCT s.id, s.parentid, s.name, s.image
                FROM subcategory s
                JOIN products p ON s.id = p.cat_sub
                WHERE s.parentid=?
            """, (category_id,))
            rows = await cursor.fetchall()
            available_subcategories = [Subcategory(**row) for row in rows]
            return available_subcategories
    except Exception as e:
        print(f"Error in get_available_subcategory_list: {e}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()
import json

@app.get("/subcats/{category_id}", response_model=List[Subcategory])
async def get_available_subcategory_list(category_id: str):
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            # Fetch subcategories and fallback product_img if subcat image is missing
            await cursor.execute("""
                SELECT DISTINCT 
                    s.id, 
                    s.parentid, 
                    s.name,
                    s.image,
                    (
                        SELECT p1.product_img 
                        FROM products p1 
                        WHERE p1.cat_sub = s.id
                        LIMIT 1
                    ) AS fallback_img
                FROM subcategory s
                JOIN products p ON s.id = p.cat_sub
                WHERE s.parentid = ?
            """, (category_id,))
            
            columns = [column[0] for column in cursor.description]
            rows = await cursor.fetchall()

            subcategories = []
            for row in rows:
                row_data = dict(zip(columns, row))
                image = row_data["image"]
                
                # If subcategory image is empty or null, use the first image from product_img
                if not image or image.strip() == "":
                    try:
                        product_img_list = json.loads(row_data["fallback_img"])
                        if isinstance(product_img_list, list) and product_img_list:
                            image = product_img_list[0]
                    except Exception as e:
                        print(f"Error parsing product_img JSON: {e}")
                
                subcategories.append(Subcategory(
                    id=row_data["id"],
                    parentid=row_data["parentid"],
                    name=row_data["name"],
                    image=image
                ))

            return subcategories

    except Exception as e:
        print(f"Error in get_available_subcategory_list: {e}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()


@app.post("/subcategories")
async def add_subcategory(category: Subcategory):
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            await cursor.execute(
                "INSERT OR REPLACE INTO subcategory (id, parentid, name, image) VALUES (?, ?, ?, ?)",
                (category.id, category.parentid, category.name, category.image),
            )
            await conn.commit()
        return {"message": "Category added successfully"}
    except Exception as e:
        print(f"Error in add_category: {e}")
        await conn.rollback()
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()

@app.delete("/subcategories/{cat_id}", response_model=Dict[str, str])
async def delete_subcategoty(cat_id: str): # Made async
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cur:
            await cur.execute("DELETE FROM subcategory WHERE id = ?", (cat_id,))
            if cur.rowcount == 0:
                raise HTTPException(status_code=404, detail="Order not found.")
            await conn.commit()
            return {"message": "Category deleted successfully"}
    except HTTPException:
        # No rollback needed for DELETE usually, but doesn't hurt
        await conn.rollback()
        raise
    except Exception as e:
        await conn.rollback()
        print(f"Error in delete_category: {e}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()



# --- UserData Endpoints ---
class UserData(BaseModel):
    uid: str
    id: str
    name: str
    contact: str
    gstin: str
    email: str
    role: str
    address: str
    credits: float
    creditse: str
    isblocked: Optional[int] = 0

class UserDataCreate(BaseModel):
    id: str
    name: str
    contact: str
    gstin: str
    email: str
    role: str
    address: str
    credits: float
    creditse: str
    isblocked: Optional[int] = 0
    pwd: str

class UserDataUpdate(BaseModel):
    name: str
    contact: str
    gstin: str
    email: str
    role: str
    address: str
    credits: float
    creditse: str
    isblocked: Optional[int] = 0
    pwd: str


@app.get("/userdata", response_model=List[UserData])
async def get_userdata_list():
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            await cursor.execute("SELECT uid, id, name, contact, gstin, email, role, address, credits, creditse, isblocked FROM userdata")
            rows = await cursor.fetchall()
            categories = [UserData(**row) for row in rows]
            return categories
    except Exception as e:
        print(f"Error in get_UserData_list: {e}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()


@app.get("/user/{user_id}", response_model=UserData)
async def get_userdata(user_id: str):
    conn = await get_db_connection()
    try:
        async with conn.cursor() as cursor:
            await cursor.execute("SELECT uid, id, name, contact, gstin, email, role, address, credits, creditse, isblocked FROM userdata WHERE id=? or uid=?", (user_id, user_id))
            row = await cursor.fetchone()
            if row:
                return UserData(**dict(zip(("uid", "id", "name", "contact", "gstin", "email", "role", "address", "credits", "creditse", "isblocked"), row)))
            else:
                raise HTTPException(status_code=404, detail=f"User with ID '{user_id}' not found")
    except Exception as e:
        print(f"Error in get_userdata: {e}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()

@app.post("/userdata")
async def add_userdata(data: UserDataCreate):
    conn = await get_db_connection()
    try:
        uid,errStr = firebaseAuth.create_user_account(data.email, data.pwd)
        if uid is None:
            raise Exception("Failed to Create Account:"+str(errStr))

        async with conn.cursor() as cursor:
            await cursor.execute(
                "INSERT OR REPLACE INTO userdata (id, uid, name, contact, gstin, email, role, address, credits, creditse, isblocked) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                (data.id, uid, data.name, data.contact, data.gstin, data.email, data.role, data.address, data.credits, data.creditse, data.isblocked),
            )
            await conn.commit()
        return {"message": "User added successfully", "uid":uid}
    except Exception as e:
        print(f"Error in add_UserData: {e}")
        await conn.rollback()
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()


@app.put("/userdata/{user_id}")
async def put_userdata(user_id: str,data: UserDataUpdate):
    conn = await get_db_connection()
    try:
        if(data.pwd):
            errStr = firebaseAuth.change_user_password(user_id, data.pwd)
            if errStr is not None:
                raise Exception("Failed to Update Password: "+str(errStr))        
        async with conn.cursor() as cursor:
            await cursor.execute("UPDATE userdata SET name=?, email=?, contact=?, gstin=?, role=?, address=?, credits=?, creditse=?, isblocked=? WHERE uid=? or id=?", (data.name, data.email, data.contact, data.gstin, data.role, data.address, data.credits, data.creditse, data.isblocked, user_id, user_id))
            await conn.commit()
        return {"message": "User updated successfully", "uid":"0"}
    except Exception as e:
        print(f"Error in add_UserData: {e}")
        await conn.rollback()
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()

@app.delete("/userdata/{user_id}", response_model=Dict[str, str])
async def delete_userdata(user_id: str): # Made async
    conn = await get_db_connection()
    try:
        errStr = firebaseAuth.remove_user_account(user_id)
        if errStr is not None:
            raise Exception("Failed to Delete Account:"+str(errStr))
        
        async with conn.cursor() as cur:
            await cur.execute("DELETE FROM userdata WHERE id=? or uid=?", (user_id,user_id))
            if cur.rowcount == 0:
                raise HTTPException(status_code=404, detail="Order not found.")
            await conn.commit()
            return {"message": "User deleted successfully"}
    except HTTPException:
        # No rollback needed for DELETE usually, but doesn't hurt
        await conn.rollback()
        raise
    except Exception as e:
        await conn.rollback()
        print(f"Error in delete_UserData: {e}")
        raise HTTPException(status_code=500, detail=f"Database error: {str(e)}")
    finally:
        await conn.close()

# //---------------------------------------------SOURCE

import logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)




async def fetch_tables() -> List[str]:
    """Fetches all table names asynchronously."""
    conn = None
    try:
        conn = await get_db_connection()
        async with conn.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';") as cursor:
            tables = [row[0] for row in await cursor.fetchall()]
            return tables
    except aiosqlite.Error as e:
        logger.error(f"Database error fetching tables: {e}")
        raise HTTPException(status_code=500, detail=f"Database error fetching tables: {e}")
    except ConnectionError as e:
         raise HTTPException(status_code=500, detail=str(e))
    finally:
        if conn:
            await conn.close()
            logger.debug("Connection closed after fetching tables.")

async def fetch_table_info(table_name: str) -> List[Dict[str, Any]]:
    """Fetches column information asynchronously."""
    conn = None
    # Basic table name validation (prevent obvious SQL injection patterns)
    # A more robust validation might check against fetched table list first
    if not table_name.isalnum() and '_' not in table_name:
         logger.warning(f"Invalid table name format received: {table_name}")
         raise HTTPException(status_code=400, detail="Invalid table name format.")
    try:
        conn = await get_db_connection()
        # Use placeholder for table name in PRAGMA? Not directly possible.
        # Ensure table_name is sanitized or validated before using f-string.
        # We rely on the initial validation and potentially validating against fetch_tables() result.
        sql = f"PRAGMA table_info(`{table_name}`)" # Use backticks for safety
        logger.debug(f"Executing SQL: {sql}")
        async with conn.execute(sql) as cursor:
            columns = await cursor.fetchall()
        if not columns:
            raise HTTPException(status_code=404, detail=f"Table '{table_name}' not found or has no columns.")
        # Convert aiosqlite.Row to dict
        return [{"cid": col[0], "name": col[1], "type": col[2], "notnull": col[3], "dflt_value": col[4], "pk": col[5]} for col in columns]
    except aiosqlite.Error as e:
        logger.error(f"Database error fetching table info for '{table_name}': {e}")
        raise HTTPException(status_code=500, detail=f"Database error fetching table info: {e}")
    except ConnectionError as e:
         raise HTTPException(status_code=500, detail=str(e))
    finally:
        if conn:
            await conn.close()
            logger.debug(f"Connection closed after fetching info for {table_name}.")

async def find_primary_key_column(table_info: List[Dict[str, Any]]) -> Optional[str]:
    """Finds the primary key column name from table info (Sync function)."""
    for col in table_info:
        if col.get('pk', 0) == 1: # Check if 'pk' key exists and is 1
            return col['name']
    return None # No single primary key found

async def check_table_exists(table_name: str):
    """Checks if the table name exists asynchronously."""
    existing_tables = await fetch_tables() # Reuses the helper, manages its own connection
    if table_name not in existing_tables:
        raise HTTPException(status_code=404, detail=f"Table '{table_name}' not found.")


# --- FastAPI Routes (Async & Manual Connection Management) ---

@app.get("/privacy_policy", response_class=HTMLResponse)
async def privacy_policy(request: Request):
    html_file_path = "privacy_policy.html"
    try:
        with open(html_file_path, "r", encoding="utf-8") as f:
            html_content = f.read()
        # No Jinja - Initial data (like tables) must be fetched by JS on page load
        return HTMLResponse(content=html_content)
    except FileNotFoundError:
        logger.error(f"HTML file not found at {html_file_path}")
        raise HTTPException(status_code=404, detail=f"{html_file_path} not found in the current directory.")
    except Exception as e:
        logger.error(f"Error reading HTML file {html_file_path}: {e}")
        raise HTTPException(status_code=500, detail=f"Could not load interface: {e}")

@app.get("/database", response_class=HTMLResponse)
async def read_root(request: Request):
    """Serves the main HTML page from the current directory."""
    html_file_path = "database.html"
    try:
        with open(html_file_path, "r", encoding="utf-8") as f:
            html_content = f.read()
        # No Jinja - Initial data (like tables) must be fetched by JS on page load
        return HTMLResponse(content=html_content)
    except FileNotFoundError:
        logger.error(f"HTML file not found at {html_file_path}")
        raise HTTPException(status_code=404, detail=f"{html_file_path} not found in the current directory.")
    except Exception as e:
        logger.error(f"Error reading HTML file {html_file_path}: {e}")
        raise HTTPException(status_code=500, detail=f"Could not load interface: {e}")


@app.get("/api/tables", response_model=List[str])
async def api_get_tables():
    """API endpoint to get list of tables."""
    try:
        return await fetch_tables()
    except HTTPException as http_exc:
        raise http_exc # Re-raise exceptions from helper
    except Exception as e:
        logger.error(f"Unexpected error in /api/tables: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="An unexpected server error occurred.")

@app.get("/api/table/{table_name}/info")
async def api_get_table_info(table_name: str):
    """API endpoint to get table schema (columns, pk)."""
    # Re-check existence here before proceeding
    await check_table_exists(table_name)
    try:
        info = await fetch_table_info(table_name) # Manages its own connection
        pk_column = await find_primary_key_column(info) # This helper is sync
        return JSONResponse(content={"columns": info, "pk_column": pk_column})
    except HTTPException as http_exc:
        raise http_exc # Re-raise exceptions from helpers
    except Exception as e:
        logger.error(f"Unexpected error in /api/table/{table_name}/info: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="An unexpected server error occurred.")


@app.get("/api/table/{table_name}/data")
async def api_get_table_data(table_name: str):
    """API endpoint to get data rows for a table."""
    await check_table_exists(table_name)
    conn = None
    try:
        conn = await get_db_connection()
        # Use backticks for safety
        sql = f"SELECT * FROM `{table_name}`"
        logger.debug(f"Executing SQL: {sql}")
        async with conn.execute(sql) as cursor:
            rows = await cursor.fetchall()
        # Convert aiosqlite.Row objects to dictionaries for JSON serialization
        data = [dict(row) for row in rows]
        return JSONResponse(content={"data": data})
    except aiosqlite.Error as e:
        logger.error(f"Database error fetching data for '{table_name}': {e}")
        raise HTTPException(status_code=500, detail=f"Database error fetching data: {e}")
    except ConnectionError as e:
         raise HTTPException(status_code=500, detail=str(e))
    except HTTPException as http_exc:
        raise http_exc
    except Exception as e:
        logger.error(f"Unexpected error in /api/table/{table_name}/data: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="An unexpected server error occurred.")
    finally:
        if conn:
            await conn.close()
            logger.debug(f"Connection closed after fetching data for {table_name}.")


@app.post("/api/table/{table_name}/row", status_code=201)
async def api_add_row(table_name: str, request: Request):
    """API endpoint to add a new row."""
    await check_table_exists(table_name)
    conn = None
    try:
        form_data = await request.form()
        data_dict = dict(form_data)
        logger.info(f"Adding row to {table_name} with data: {data_dict}")

        # Basic data cleaning: Handle empty strings -> None for potential NULL columns
        cleaned_data = {k: (v if v != '' else None) for k, v in data_dict.items()}

        columns = list(cleaned_data.keys())
        placeholders = ', '.join(['?'] * len(columns))
        # Use backticks for safety
        column_clause = ', '.join(f'`{col}`' for col in columns)
        sql = f"INSERT INTO `{table_name}` ({column_clause}) VALUES ({placeholders})"
        logger.debug(f"Executing SQL: {sql} with values: {list(cleaned_data.values())}")

        conn = await get_db_connection()
        cursor = await conn.execute(sql, list(cleaned_data.values()))
        await conn.commit()
        last_id = cursor.lastrowid # Get last row ID if needed
        await cursor.close() # Close cursor explicitly

        return JSONResponse(
            content={"message": "Row added successfully", "row_id": last_id},
            status_code=201
        )
    except aiosqlite.IntegrityError as e:
         logger.warning(f"Integrity error adding row to {table_name}: {e}")
         if conn: await conn.rollback() # Rollback on integrity error
         raise HTTPException(status_code=400, detail=f"Failed to add row (Integrity Error): {e}. Check unique constraints or non-null fields.")
    except aiosqlite.Error as e:
        logger.error(f"Database error adding row to {table_name}: {e}")
        if conn: await conn.rollback()
        raise HTTPException(status_code=500, detail=f"Database error adding row: {e}")
    except ConnectionError as e:
         raise HTTPException(status_code=500, detail=str(e))
    except HTTPException as http_exc:
        if conn: await conn.rollback()
        raise http_exc
    except Exception as e:
        logger.error(f"Unexpected error adding row to {table_name}: {e}", exc_info=True)
        if conn: await conn.rollback()
        raise HTTPException(status_code=500, detail="An unexpected server error occurred.")
    finally:
        if conn:
            await conn.close()
            logger.debug(f"Connection closed after adding row to {table_name}.")


@app.put("/api/table/{table_name}/row/{pk_value}", status_code=200)
async def api_update_row(table_name: str, pk_value: str, request: Request):
    """API endpoint to update an existing row."""
    await check_table_exists(table_name)
    conn = None
    try:
        form_data = await request.form()
        data_dict = dict(form_data)
        logger.info(f"Updating row in {table_name} where PK={pk_value} with data: {data_dict}")

        # Basic data cleaning
        cleaned_data = {k: (v if v != '' else None) for k, v in data_dict.items()}

        # Fetch table info to find PK (manages its own connection)
        table_info = await fetch_table_info(table_name)
        pk_column = await find_primary_key_column(table_info)

        if not pk_column:
            raise HTTPException(status_code=400, detail=f"Cannot update: Table '{table_name}' does not have a single primary key defined.")

        # Ensure PK column itself is not in the SET clause if it exists in form data
        # (Generally bad practice to update PK, but handle if submitted)
        update_data = {k: v for k, v in cleaned_data.items() if k != pk_column}

        if not update_data:
             raise HTTPException(status_code=400, detail="No columns provided to update.")

        # Use backticks for safety
        set_clauses = ', '.join([f"`{col}` = ?" for col in update_data.keys()])
        sql = f"UPDATE `{table_name}` SET {set_clauses} WHERE `{pk_column}` = ?"
        values = list(update_data.values()) + [pk_value]
        logger.debug(f"Executing SQL: {sql} with values: {values}")

        conn = await get_db_connection()
        cursor = await conn.execute(sql, values)

        if cursor.rowcount == 0:
             await conn.rollback() # Rollback if no rows were affected
             raise HTTPException(status_code=404, detail=f"Row with {pk_column}='{pk_value}' not found in table '{table_name}'.")

        await conn.commit()
        await cursor.close()
        return JSONResponse(content={"message": f"Row with {pk_column}='{pk_value}' updated successfully."})

    except aiosqlite.IntegrityError as e:
         logger.warning(f"Integrity error updating row in {table_name}: {e}")
         if conn: await conn.rollback()
         raise HTTPException(status_code=400, detail=f"Failed to update row (Integrity Error): {e}. Check unique constraints or non-null fields.")
    except aiosqlite.Error as e:
        logger.error(f"Database error updating row in {table_name}: {e}")
        if conn: await conn.rollback()
        raise HTTPException(status_code=500, detail=f"Database error updating row: {e}")
    except ConnectionError as e:
         raise HTTPException(status_code=500, detail=str(e))
    except HTTPException as http_exc:
        if conn: await conn.rollback() # Ensure rollback on HTTP exceptions too if conn exists
        raise http_exc
    except Exception as e:
        logger.error(f"Unexpected error updating row in {table_name}: {e}", exc_info=True)
        if conn: await conn.rollback()
        raise HTTPException(status_code=500, detail="An unexpected server error occurred.")
    finally:
        if conn:
            await conn.close()
            logger.debug(f"Connection closed after updating row in {table_name}.")


@app.delete("/api/table/{table_name}/row/{pk_value}", status_code=200)
async def api_delete_row(table_name: str, pk_value: str):
    """API endpoint to delete a row."""
    await check_table_exists(table_name)
    conn = None
    try:
        logger.info(f"Deleting row from {table_name} where PK={pk_value}")
        # Fetch table info to find PK (manages its own connection)
        table_info = await fetch_table_info(table_name)
        pk_column = await find_primary_key_column(table_info)

        if not pk_column:
            raise HTTPException(status_code=400, detail=f"Cannot delete: Table '{table_name}' does not have a single primary key defined.")

        # Use backticks for safety
        sql = f"DELETE FROM `{table_name}` WHERE `{pk_column}` = ?"
        logger.debug(f"Executing SQL: {sql} with value: {pk_value}")

        conn = await get_db_connection()
        cursor = await conn.execute(sql, (pk_value,))

        if cursor.rowcount == 0:
             await conn.rollback() # Rollback if no rows were affected
             raise HTTPException(status_code=404, detail=f"Row with {pk_column}='{pk_value}' not found in table '{table_name}'.")

        await conn.commit()
        await cursor.close()
        return JSONResponse(content={"message": f"Row with {pk_column}='{pk_value}' deleted successfully."})
    except aiosqlite.Error as e:
        logger.error(f"Database error deleting row from {table_name}: {e}")
        if conn: await conn.rollback()
        raise HTTPException(status_code=500, detail=f"Database error deleting row: {e}")
    except ConnectionError as e:
         raise HTTPException(status_code=500, detail=str(e))
    except HTTPException as http_exc:
        if conn: await conn.rollback()
        raise http_exc
    except Exception as e:
        logger.error(f"Unexpected error deleting row from {table_name}: {e}", exc_info=True)
        if conn: await conn.rollback()
        raise HTTPException(status_code=500, detail="An unexpected server error occurred.")
    finally:
        if conn:
            await conn.close()
            logger.debug(f"Connection closed after deleting row from {table_name}.")



async def FCM_notify_user_credits_change(user_id, msg):
    try:        
        firebaseAuth.send_topic_notification("user_"+str(user_id),"Credits Updated","Hi, "+msg+"!", {})
    except Exception as e:
        logger.error(f"Unexpected error fcm notify when FCM_notify_user_credits_change in FCM: {e}", exc_info=True)

async def FCM_notify_order_status_change(user_id, status):
    try:        
        firebaseAuth.send_topic_notification("user_"+str(user_id),"Order Status Updated","Hi, your order is "+status+"!", {})
    except Exception as e:
        logger.error(f"Unexpected error fcm notify when FCM_notify_order_status_change in FCM: {e}", exc_info=True)

async def FCM_notify_order_checkout(user_id, total_rate, user_name):
    try:        
        firebaseAuth.send_topic_notification("user_"+str(user_id),"Order Made","Thank you for making order, your order is in pending, we will update you!", {})
        firebaseAuth.send_topic_notification("order_checkout","New Order",str(user_name)+" is made a new order of Rs."+str(total_rate), {})
    except Exception as e:
        logger.error(f"Unexpected error fcm notify when FCM_notify_order_checkout in FCM: {e}", exc_info=True)


# NEW: Analytics Page Route
@app.get("/analytics", response_class=HTMLResponse)
async def analytics_page(request: Request):
    """Serves the analytics HTML page."""
    html_file_path = "analytics.html" # New HTML file for analytics
    try:
        with open(html_file_path, "r", encoding="utf-8") as f:
            html_content = f.read()
        return HTMLResponse(content=html_content)
    except FileNotFoundError:
        logger.error(f"HTML file not found at {html_file_path}", exc_info=True)
        raise HTTPException(status_code=404, detail=f"{html_file_path} not found in the current directory.")
    except Exception as e:
        logger.error(f"Error reading HTML file {html_file_path}: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Could not load analytics interface: {e}")


from datetime import date, timedelta
# --- Analytics Data API (NEW) ---
@app.get("/analytics/summary")
async def get_analytics_summary():
    """Provides a comprehensive summary data for the analytics dashboard."""
    conn = await get_db_connection()
    summary_data = {}
    # Helper to safely get value or default
    def get_val(row, idx=0, default=0):
        return row[idx] if row and row[idx] is not None else default

    try:
        async with conn.cursor() as cursor:
            # --- Core Metrics ---
            # Total Earnings Overall (from delivered orders, using precalculated total)
            await cursor.execute("SELECT SUM(total) FROM orders WHERE order_status = 'ORDER_DELIVERED'")
            summary_data["total_earnings_overall"] = round(get_val(await cursor.fetchone()), 2)

            # Total Orders Overall
            await cursor.execute("SELECT COUNT(order_id) FROM orders")
            summary_data["total_orders_overall"] = get_val(await cursor.fetchone())

            # Total Orders This Month
            await cursor.execute("SELECT COUNT(order_id) FROM orders WHERE strftime('%Y-%m', created_at) = strftime('%Y-%m', 'now')")
            summary_data["total_orders_this_month"] = get_val(await cursor.fetchone())

            # Total Orders Last Month
            await cursor.execute("SELECT COUNT(order_id) FROM orders WHERE strftime('%Y-%m', created_at) = strftime('%Y-%m', 'now', '-1 month')")
            summary_data["total_orders_last_month"] = get_val(await cursor.fetchone())

            # Total Earnings This Month (Delivered Orders)
            await cursor.execute("SELECT SUM(total) FROM orders WHERE order_status = 'ORDER_DELIVERED' AND strftime('%Y-%m', created_at) = strftime('%Y-%m', 'now')")
            summary_data["total_earnings_this_month"] = round(get_val(await cursor.fetchone()), 2)

            # Total Earnings Last Month (Delivered Orders)
            await cursor.execute("SELECT SUM(total) FROM orders WHERE order_status = 'ORDER_DELIVERED' AND strftime('%Y-%m', created_at) = strftime('%Y-%m', 'now', '-1 month')")
            summary_data["total_earnings_last_month"] = round(get_val(await cursor.fetchone()), 2)


            # Low Stock Products Count (< 5)
            LOW_STOCK_THRESHOLD = 5
            await cursor.execute("SELECT COUNT(id) FROM products WHERE stock < ?", (LOW_STOCK_THRESHOLD,))
            summary_data["low_stock_products_count"] = get_val(await cursor.fetchone())
            summary_data["low_stock_threshold"] = LOW_STOCK_THRESHOLD

            # Low Stock Products List (< 5) - Details requested on click, but providing list in API for now
            await cursor.execute("SELECT id, product_name, stock FROM products WHERE stock < ?", (LOW_STOCK_THRESHOLD,))
            low_stock_rows = await cursor.fetchall()
            summary_data["low_stock_products_list"] = [{"id": row[0], "name": row[1], "stock": row[2]} for row in low_stock_rows] if low_stock_rows else []


            # Order Status Distribution
            await cursor.execute("SELECT order_status, COUNT(order_id) FROM orders GROUP BY order_status")
            order_status_rows = await cursor.fetchall()
            summary_data["order_status_distribution"] = {row[0]: row[1] for row in order_status_rows} if order_status_rows else {}

            # --- Product Insights ---

            # Top 10 Selling Products (by total revenue from items_detail)
            top_selling_products_list = []
            try:
                await cursor.execute("""
                    SELECT
                        p.product_name,
                        SUM(CAST(json_extract(item.value, '$.count') AS INTEGER) * CAST(json_extract(item.value, '$.cost_mrp') AS REAL)) AS total_revenue
                    FROM orders o,
                         json_each(o.items_detail) AS item
                    JOIN products p ON json_extract(item.value, '$.product_id') = p.product_id
                    WHERE o.order_status = 'ORDER_DELIVERED'
                    GROUP BY p.product_name
                    ORDER BY total_revenue DESC
                    LIMIT 10;
                """)
                top_products_rows = await cursor.fetchall()
                if top_products_rows:
                    top_selling_products_list = [{"name": row[0], "revenue": round(row[1], 2)} for row in top_products_rows]
                else:
                    top_selling_products_list = [{"note": "No product sales data found."}]
            except Exception as e_tsp:
                print(f"Warning: Could not query top selling products: {e_tsp}")
                top_selling_products_list = [{"note": "Using mock data due to query error."}] # Keep mock structure
            summary_data["top_selling_products_revenue"] = top_selling_products_list # Renamed key for clarity

            # --- User Insights ---

            # Top 5 Order Taking Users (by total order value from delivered orders)
            top_order_users_list = []
            try:
                await cursor.execute("""
                    SELECT
                        u.name,
                        SUM(o.total) AS total_order_value
                    FROM orders o
                    JOIN userdata u ON o.user_id = u.uid
                    WHERE o.order_status = 'ORDER_DELIVERED'
                    GROUP BY u.name
                    ORDER BY total_order_value DESC
                    LIMIT 5;
                """)
                top_users_rows = await cursor.fetchall()
                if top_users_rows:
                    top_order_users_list = [{"username": row[0], "total_value": round(row[1], 2)} for row in top_users_rows]
                else:
                    top_order_users_list = [{"note": "No user order data found."}]
            except Exception as e_tou:
                print(f"Warning: Could not query top order taking users: {e_tou}")
                top_order_users_list = [{"note": "Using mock data due to query error."}] # Keep mock structure
            summary_data["top_order_taking_users"] = top_order_users_list


            # --- Trends ---

            # Orders Trend (Last 12 Months) by count of orders
            orders_trend_data_final = {}
            try:
                await cursor.execute("""
                    SELECT
                        strftime('%Y-%m', created_at) AS order_month,
                        COUNT(order_id) AS monthly_orders
                    FROM orders
                    WHERE created_at >= DATE('now', '-12 months')
                    GROUP BY order_month
                    ORDER BY order_month ASC;
                """)
                orders_trend_rows = await cursor.fetchall()

                # Generate labels for the last 12 months
                date_labels_expected = []
                today = date.today()
                for i in range(12):
                    past_month_start = (today.replace(day=1) - timedelta(days=(i+1)*30)).replace(day=1) # Approximation for month calculation
                    date_labels_expected.append(past_month_start.strftime('%Y-%m'))
                date_labels_expected.sort() # Ensure correct order

                if orders_trend_rows:
                    orders_map = {row[0]: row[1] for row in orders_trend_rows}
                    final_data_points = [orders_map.get(month_label, 0) for month_label in date_labels_expected]
                    orders_trend_data_final = {"labels": date_labels_expected, "data": final_data_points}
                else:
                    orders_trend_data_final = {
                        "labels": date_labels_expected,
                        "data": [0 for _ in range(12)],
                        "note": "No order data for the last 12 months."
                    }
            except Exception as e_trend:
                print(f"Warning: Could not generate real orders trend: {e_trend}")
                # Generate mock labels for the last 12 months
                mock_labels = []
                today = date.today()
                for i in range(12):
                     past_month_start = (today.replace(day=1) - timedelta(days=(i+1)*30)).replace(day=1)
                     mock_labels.append(past_month_start.strftime('%Y-%m'))
                mock_labels.sort()
                orders_trend_data_final = {
                    "labels": mock_labels,
                    "data": [random.randint(50, 200) for _ in range(12)], # Mock data
                    "note": "Using mock data due to an error or no data."
                }
            summary_data["orders_trend_12_months"] = orders_trend_data_final


            return summary_data

    except Exception as e:
        print(f"Error in get_analytics_summary: {e}")
        raise e # Re-raise the original exception for debugging or higher-level handling
    finally:
        if conn:
            await conn.close() # Close connection in actual implementation




async def get_system_details():
    """
    Asynchronously retrieves detailed system information.
    """
    # --- CPU Information ---
    cpu_times = psutil.cpu_times()
    cpu_stats = psutil.cpu_stats()
    try:
        cpu_freq = psutil.cpu_freq() # May not be available on all systems or may require permissions
    except Exception:
        cpu_freq = None
    load_avg = psutil.getloadavg() # Unix-like specific

    cpu_info = {
        "logical_cores": psutil.cpu_count(logical=True),
        "physical_cores": psutil.cpu_count(logical=False),
        "usage_percent_per_cpu": psutil.cpu_percent(interval=0.1, percpu=True),
        "total_cpu_usage_percent": psutil.cpu_percent(interval=0.1, percpu=False),
        "times": {
            "user": cpu_times.user,
            "system": cpu_times.system,
            "idle": cpu_times.idle,
            "nice": getattr(cpu_times, 'nice', 'N/A'),  # POSIX
            "iowait": getattr(cpu_times, 'iowait', 'N/A'), # Linux
            "irq": getattr(cpu_times, 'irq', 'N/A'), # Linux, BSD
            "softirq": getattr(cpu_times, 'softirq', 'N/A'), # Linux
            "steal": getattr(cpu_times, 'steal', 'N/A'), # Linux (virtualized)
            "guest": getattr(cpu_times, 'guest', 'N/A'), # Linux (virtualized)
            "guest_nice": getattr(cpu_times, 'guest_nice', 'N/A'), # Linux (virtualized)
        },
        "stats": {
            "ctx_switches": cpu_stats.ctx_switches,
            "interrupts": cpu_stats.interrupts,
            "soft_interrupts": cpu_stats.soft_interrupts,
            "syscalls": cpu_stats.syscalls if hasattr(cpu_stats, 'syscalls') else 'N/A', # May not be available
        },
        "frequency": {
            "current": cpu_freq.current if cpu_freq else 'N/A',
            "min": cpu_freq.min if cpu_freq else 'N/A',
            "max": cpu_freq.max if cpu_freq else 'N/A',
        } if cpu_freq else 'N/A',
        "load_average": { # Unix-like specific
            "1_min": load_avg[0],
            "5_min": load_avg[1],
            "15_min": load_avg[2],
        } if hasattr(psutil, 'getloadavg') else 'N/A',
    }

    # --- Memory Information ---
    virtual_mem = psutil.virtual_memory()
    swap_mem = psutil.swap_memory()
    memory_info = {
        "virtual_memory": {
            "total_gb": round(virtual_mem.total / (1024**3), 2),
            "available_gb": round(virtual_mem.available / (1024**3), 2),
            "percent_used": virtual_mem.percent,
            "used_gb": round(virtual_mem.used / (1024**3), 2),
            "free_gb": round(virtual_mem.free / (1024**3), 2),
            "active_gb": round(getattr(virtual_mem, 'active', 0) / (1024**3), 2), # Linux, macOS
            "inactive_gb": round(getattr(virtual_mem, 'inactive', 0) / (1024**3), 2), # Linux, macOS
            "buffers_gb": round(getattr(virtual_mem, 'buffers', 0) / (1024**3), 2), # Linux, BSD
            "cached_gb": round(getattr(virtual_mem, 'cached', 0) / (1024**3), 2), # Linux, BSD
            "shared_gb": round(getattr(virtual_mem, 'shared', 0) / (1024**3), 2), # Linux
            "slab_gb": round(getattr(virtual_mem, 'slab', 0) / (1024**3), 2), # Linux
        },
        "swap_memory": {
            "total_gb": round(swap_mem.total / (1024**3), 2),
            "used_gb": round(swap_mem.used / (1024**3), 2),
            "free_gb": round(swap_mem.free / (1024**3), 2),
            "percent_used": swap_mem.percent,
            "sin_gb": round(swap_mem.sin / (1024**3), 2), # Bytes Swapped In
            "sout_gb": round(swap_mem.sout / (1024**3), 2), # Bytes Swapped Out
        }
    }

    # --- Disk Information ---
    disk_partitions_info = []
    try:
        for part in psutil.disk_partitions():
            try:
                usage = psutil.disk_usage(part.mountpoint)
                disk_partitions_info.append({
                    "device": part.device,
                    "mountpoint": part.mountpoint,
                    "fstype": part.fstype,
                    "opts": part.opts,
                    "total_gb": round(usage.total / (1024**3), 2),
                    "used_gb": round(usage.used / (1024**3), 2),
                    "free_gb": round(usage.free / (1024**3), 2),
                    "percent_used": usage.percent,
                })
            except Exception: # Handle potential errors like "disk not ready"
                disk_partitions_info.append({
                    "device": part.device,
                    "mountpoint": part.mountpoint,
                    "fstype": part.fstype,
                    "opts": part.opts,
                    "error": "Could not retrieve usage",
                })
    except Exception:
        disk_partitions_info = "Could not retrieve disk partitions"


    disk_io = psutil.disk_io_counters()
    disk_info = {
        "partitions": disk_partitions_info,
        "io_counters": {
            "read_count": disk_io.read_count,
            "write_count": disk_io.write_count,
            "read_bytes_gb": round(disk_io.read_bytes / (1024**3), 2),
            "write_bytes_gb": round(disk_io.write_bytes / (1024**3), 2),
            "read_time_ms": disk_io.read_time,
            "write_time_ms": disk_io.write_time,
            "read_merged_count": getattr(disk_io, 'read_merged_count', 'N/A'), # Linux
            "write_merged_count": getattr(disk_io, 'write_merged_count', 'N/A'), # Linux
            "busy_time_ms": getattr(disk_io, 'busy_time', 'N/A'), # Linux
        } if disk_io else 'N/A',
    }

    # --- Network Information ---
    net_io = psutil.net_io_counters()
    net_if_addrs = psutil.net_if_addrs()
    interfaces = {}
    for if_name, addrs in net_if_addrs.items():
        stats = psutil.net_if_stats().get(if_name)
        interfaces[if_name] = {
            "addresses": [
                {
                    "family": str(addr.family),
                    "address": addr.address,
                    "netmask": addr.netmask,
                    "broadcast": addr.broadcast,
                    "ptp": addr.ptp, # Point-to-Point
                } for addr in addrs
            ],
            "stats": {
                "is_up": stats.isup if stats else 'N/A',
                "duplex": str(stats.duplex) if stats else 'N/A', # e.g., NIC_DUPLEX_FULL
                "speed_mbps": stats.speed if stats else 'N/A',
                "mtu_bytes": stats.mtu if stats else 'N/A',
            } if stats else 'N/A'
        }

    try:
        net_connections = psutil.net_connections() # Can be slow or require permissions
        connections_info = [
            {
                "fd": conn.fd,
                "family": str(conn.family),
                "type": str(conn.type), # e.g., SOCK_STREAM
                "laddr": {"ip": conn.laddr.ip if conn.laddr else 'N/A', "port": conn.laddr.port if conn.laddr else 'N/A'},
                "raddr": {"ip": conn.raddr.ip if conn.raddr else 'N/A', "port": conn.raddr.port if conn.raddr else 'N/A'},
                "status": conn.status,
                "pid": conn.pid
            } for conn in net_connections[:20] # Limit for performance
        ]
    except psutil.AccessDenied:
        connections_info = "Access Denied to network connections"
    except Exception as e:
        connections_info = f"Could not retrieve network connections: {str(e)}"


    network_info = {
        "io_counters": {
            "bytes_sent_gb": round(net_io.bytes_sent / (1024**3), 2),
            "bytes_recv_gb": round(net_io.bytes_recv / (1024**3), 2),
            "packets_sent": net_io.packets_sent,
            "packets_recv": net_io.packets_recv,
            "errin": net_io.errin,
            "errout": net_io.errout,
            "dropin": net_io.dropin,
            "dropout": net_io.dropout,
        },
        "interfaces": interfaces,
        "connections_count": len(net_connections) if isinstance(net_connections, list) else 'N/A',
        "connections_sample": connections_info # A sample to avoid overwhelming data
    }

    # --- Sensor Information ---
    temperatures = {}
    try:
        temps = psutil.sensors_temperatures()
        if temps:
            for name, entries in temps.items():
                temperatures[name] = [
                    {
                        "label": entry.label or 'N/A',
                        "current_celsius": entry.current,
                        "high_celsius": entry.high,
                        "critical_celsius": entry.critical,
                    } for entry in entries
                ]
        else:
            temperatures = "Not available or not supported"
    except Exception:
        temperatures = "Could not retrieve temperatures (permissions or not supported)"

    fans = {}
    try:
        fan_speeds = psutil.sensors_fans()
        if fan_speeds:
            for name, entries in fan_speeds.items():
                fans[name] = [
                    {
                        "label": entry.label or 'N/A',
                        "current_rpm": entry.current,
                    } for entry in entries
                ]
        else:
            fans = "Not available or not supported"
    except Exception:
        fans = "Could not retrieve fan speeds (permissions or not supported)"

    battery_info = {}
    try:
        battery = psutil.sensors_battery()
        if battery:
            battery_info = {
                "percent": battery.percent,
                "secsleft": battery.secsleft, # Seconds left, -1 if N/A, -2 if unlimited
                "power_plugged": battery.power_plugged,
            }
        else:
            battery_info = "Not available or not supported"
    except Exception:
        battery_info = "Could not retrieve battery info (permissions or not supported)"

    sensor_info = {
        "temperatures": temperatures,
        "fans": fans,
        "battery": battery_info,
    }

    # --- System/OS Information ---
    plat = platform.uname()
    system_os_info = {
        "boot_time_timestamp": psutil.boot_time(),
        "boot_time_readable": time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(psutil.boot_time())),
        "users": [{"name": user.name, "terminal": user.terminal, "host": user.host, "started": user.started, "pid": user.pid} for user in psutil.users()],
        "platform_details": {
            "system": plat.system,
            "node_hostname": plat.node,
            "release": plat.release,
            "version": plat.version,
            "machine_arch": plat.machine,
            "processor": plat.processor,
            "python_version": platform.python_version(),
            "os_name": os.name,
        }
    }

    # --- Process Information (Summary) ---
    pids = psutil.pids()
    process_summary = {
        "total_processes": len(pids),
        "top_processes_by_memory": [],
        "top_processes_by_cpu": []
    }
    processes = []
    for pid in pids:
        try:
            p = psutil.Process(pid)
            processes.append({
                "pid": p.pid,
                "name": p.name(),
                "cpu_percent": 0,#p.cpu_percent(interval=0.01), # Short interval for quick snapshot
                "memory_percent": p.memory_percent(),
                "status": p.status(),
                "username": p.username() if hasattr(p, 'username') else 'N/A'
            })
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            continue

    # Sort processes (get top 5 for brevity in stream)
    process_summary["top_processes_by_memory"] = sorted(
        processes, key=lambda x: x["memory_percent"], reverse=True
    )[:5]
    process_summary["top_processes_by_cpu"] = sorted(
        processes, key=lambda x: x["cpu_percent"], reverse=True
    )[:5]


    return {
        "timestamp": time.time(),
        "cpu": cpu_info,
        "memory": memory_info,
        "disk": disk_info,
        "network": network_info,
        "sensors": sensor_info,
        "system_os": system_os_info,
        "processes_summary": process_summary
    }

async def system_stats_event_generator(request: Request):
    """
    Generator function to stream system stats.
    """
    while True:
        # Check if the client is still connected before doing heavy work
        if await request.is_disconnected():
            print("Client disconnected, stopping stream.")
            break

        details = await get_system_details()
        yield {"data": json.dumps(details)}
        await asyncio.sleep(1)  # Adjust the refresh interval as needed (e.g., 1 second)

@app.get("/system-stats/live")
async def stream_system_stats(request: Request):
    """
    Endpoint to stream live system statistics using Server-Sent Events.
    """
    event_generator = system_stats_event_generator(request)
    return EventSourceResponse(event_generator)

@app.get("/system-stats/snapshot")
async def get_system_stats_snapshot():
    """
    Endpoint to get a single snapshot of system statistics.
    """
    return await get_system_details()




# terminals
# Store active terminals
active_terminals = {}

@app.websocket("/ws/terminal")
async def websocket_terminal(websocket: WebSocket):
    await websocket.accept()
    
    terminal_id = None
    
    try:
        # Wait for initial configuration
        config = await websocket.receive_json()
        command = config.get("command", "/bin/bash")
        cols = config.get("cols", 80)
        rows = config.get("rows", 24)
        
        # Create PTY
        master_fd, slave_fd = pty.openpty()
        
        # Set terminal size
        term_size = struct.pack("HHHH", rows, cols, 0, 0)
        fcntl.ioctl(slave_fd, termios.TIOCSWINSZ, term_size)
        
        # Start the process in a new session
        env = os.environ.copy()
        env["TERM"] = "xterm-256color"
        
        process = subprocess.Popen(
            command,
            shell=True,
            stdin=slave_fd,
            stdout=slave_fd,
            stderr=slave_fd,
            universal_newlines=False,
            start_new_session=True,
            env=env
        )
        
        # Close the slave file descriptor as it's now used by the child process
        os.close(slave_fd)
        
        # Set master fd to non-blocking mode
        fl = fcntl.fcntl(master_fd, fcntl.F_GETFL)
        fcntl.fcntl(master_fd, fcntl.F_SETFL, fl | os.O_NONBLOCK)
        
        # Create a unique terminal ID
        terminal_id = str(process.pid)
        
        # Store terminal info
        active_terminals[terminal_id] = {
            "pid": process.pid,
            "master_fd": master_fd,
            "process": process
        }
        
        # Send terminal ID back to client
        await websocket.send_json({"type": "connected", "terminalId": terminal_id})
        
        # Create read task
        read_task = asyncio.create_task(read_from_terminal(websocket, terminal_id))
        
        # Handle incoming messages
        while True:
            data = await websocket.receive_text()
            message = json.loads(data)
            
            if message["type"] == "input":
                # Send input to terminal
                os.write(active_terminals[terminal_id]["master_fd"], message["data"].encode())
            elif message["type"] == "resize":
                # Handle window resize
                rows = message["rows"]
                cols = message["cols"]
                term_size = struct.pack("HHHH", rows, cols, 0, 0)
                fcntl.ioctl(active_terminals[terminal_id]["master_fd"], termios.TIOCSWINSZ, term_size)
    
    except WebSocketDisconnect:
        # Clean up on disconnect
        if terminal_id and terminal_id in active_terminals:
            await close_terminal(terminal_id)
    except Exception as e:
        # Handle any other exceptions
        print(f"Error in websocket: {str(e)}")
        if terminal_id and terminal_id in active_terminals:
            await close_terminal(terminal_id)

async def read_from_terminal(websocket: WebSocket, terminal_id: str):
    """Read output from the terminal and send to websocket"""
    terminal = active_terminals[terminal_id]
    
    # Buffer for incomplete UTF-8 sequences
    incomplete_utf8 = b""
    
    try:
        while True:
            # Check if process is still running
            if terminal["process"].poll() is not None:
                await websocket.send_json({"type": "exit", "code": terminal["process"].returncode})
                await close_terminal(terminal_id)
                break
            
            # Wait for data to be available
            r, w, e = select.select([terminal["master_fd"]], [], [], 0.1)
            
            if terminal["master_fd"] in r:
                try:
                    data = os.read(terminal["master_fd"], 8192)
                    
                    if data:
                        # Combine with any incomplete UTF-8 from last time
                        data = incomplete_utf8 + data
                        
                        try:
                            # Try to decode as UTF-8
                            text = data.decode('utf-8')
                            incomplete_utf8 = b""  # Reset buffer
                            await websocket.send_json({"type": "output", "data": text})
                        except UnicodeDecodeError:
                            # If we got an error, try to find a valid UTF-8 sequence
                            for i in range(len(data), 0, -1):
                                try:
                                    text = data[:i].decode('utf-8')
                                    incomplete_utf8 = data[i:]
                                    await websocket.send_json({"type": "output", "data": text})
                                    break
                                except UnicodeDecodeError:
                                    continue
                            else:
                                # If we can't find any valid UTF-8, save the whole buffer
                                incomplete_utf8 = data
                                continue
                    else:
                        # EOF
                        await websocket.send_json({"type": "eof"})
                        await close_terminal(terminal_id)
                        break
                except OSError:
                    # Process likely terminated
                    await close_terminal(terminal_id)
                    break
            
            # Allow other tasks to run
            await asyncio.sleep(0.01)
    except Exception as e:
        print(f"Error in read_from_terminal: {str(e)}")
        await close_terminal(terminal_id)

async def close_terminal(terminal_id: str):
    """Close a terminal session"""
    if terminal_id not in active_terminals:
        return
    
    terminal = active_terminals[terminal_id]
    
    # Try to terminate process gracefully first
    try:
        os.kill(terminal["pid"], signal.SIGTERM)
    except ProcessLookupError:
        pass
    
    # Clean up resources
    try:
        os.close(terminal["master_fd"])
    except OSError:
        pass
    
    # Remove from active terminals
    del active_terminals[terminal_id]

# Shutdown handler to clean up all terminals
@app.on_event("shutdown")
def shutdown_event():
    for terminal_id in list(active_terminals.keys()):
        try:
            terminal = active_terminals[terminal_id]
            # Try to terminate process
            try:
                os.kill(terminal["pid"], signal.SIGTERM)
            except ProcessLookupError:
                pass
            
            # Close file descriptor
            try:
                os.close(terminal["master_fd"])
            except OSError:
                pass
        except Exception:
            pass
    
    active_terminals.clear()



# if __name__ == "__main__":
#     uvicorn.run(app, host="0.0.0.0", port=5000)

if (not os.path.exists(DB_PATH)) :
    print("DB Initialization...")
    errRdb = None
    rlog = None
    try:
        root_ref = firebaseAuth.db.reference()
        rlog = dbbackup.restore_firebase_to_sqlite(DB_PATH, root_ref, "latest")

        try:
            for key, message in rlog.items():
                print(f"Step {key}: {message}")
        except Exception as e:
            print(rlog)
    except NameError:
        errRdb = ("\nRestore skipped because Firebase Admin SDK is not initialized (check credentials).")
    except Exception as e:
        errRdb = (f"\nError during restore call: {e}")
    print(errRdb)
