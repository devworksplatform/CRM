from main import firebaseAuth,TABLE_SCHEMAS
import sqlite3
import json
import os
from datetime import datetime

# --- Function 1: Backup SQLite to Firebase ---

def get_table_names(conn):
    """Gets a list of table names from the SQLite database."""
    cursor = conn.cursor()
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
    tables = [table[0] for table in cursor.fetchall()]
    # Filter out internal sqlite tables if necessary
    tables = [t for t in tables if not t.startswith('sqlite_')]
    return tables

def get_primary_key(conn, table_name):
    """Gets the primary key column name for a table."""
    cursor = conn.cursor()
    cursor.execute(f"PRAGMA table_info({table_name});")
    columns = cursor.fetchall()
    for col in columns:
        # col[5] is the 'pk' column in PRAGMA table_info output (1 if PK, 0 otherwise)
        if col[5] == 1:
            return col[1] # col[1] is the 'name' of the column
    # Fallback if no explicit PK is found (might use rowid or first column)
    # For this use case, relying on explicit PKs like 'id' or 'order_id' is safer.
    # If tables might lack explicit PKs, this needs refinement.
    print(f"Warning: No explicit primary key found for table '{table_name}'. Backup might be incomplete or incorrect if duplicates exist without PK.")
    # Trying to guess based on common names, otherwise returning the first column name
    first_col_name = columns[0][1] if columns else None
    if first_col_name in ['id', 'order_id', 'uid']: # Common PK names from schema
         return first_col_name
    print(f"Warning: Assuming first column '{first_col_name}' as key for table '{table_name}'.")
    return first_col_name


def backup_sqlite_to_firebase(db_file_path, firebase_root_ref, formatted):
    """
    Reads data from an SQLite database and backs it up to Firebase Realtime Database.

    Args:
        db_file_path (str): The path to the SQLite database file.
        firebase_root_ref (firebase_admin.db.Reference): The root reference of your Firebase DB.
    """
    if not os.path.exists(db_file_path):
        print(f"Error: SQLite database file not found at '{db_file_path}'")
        return

    print(f"Starting backup from '{db_file_path}' to Firebase...")
    conn = None
    try:
        conn = sqlite3.connect(db_file_path)
        # Improve performance and read data as dictionaries
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()

        tables = get_table_names(conn)
        print(f"Found tables: {tables}")

        backup_data = {}

        for table_name in tables:
            print(f"  Processing table: {table_name}")
            pk_column = get_primary_key(conn, table_name)
            if not pk_column:
                 print(f"  Skipping table '{table_name}' due to missing primary key information.")
                 continue

            cursor.execute(f"SELECT * FROM {table_name}")
            rows = cursor.fetchall()

            table_data_for_firebase = {}
            for row in rows:
                row_dict = dict(row) # Convert sqlite3.Row to dict
                primary_key_value = row_dict.get(pk_column)

                if primary_key_value is None:
                    print(f"    Warning: Row in table '{table_name}' has NULL primary key ('{pk_column}'). Skipping row.")
                    continue

                # Ensure primary_key_value is a string for Firebase keys
                firebase_key = str(primary_key_value)

                # Optional: Convert complex types if needed (e.g., Timestamps)
                # Firebase often handles Timestamps okay, but explicit conversion might be safer
                for key, value in row_dict.items():
                    if isinstance(value, datetime):
                         row_dict[key] = value.isoformat() # Convert datetime to ISO string
                    # Add more type conversions if necessary (e.g., BLOBs to base64)

                table_data_for_firebase[firebase_key] = row_dict

            if table_data_for_firebase:
                backup_data[table_name] = table_data_for_firebase
            else:
                print(f"  Table '{table_name}' is empty or all rows lacked valid primary keys.")


        # Upload to Firebase under 'tables/' node
        if backup_data:
            print("\nUploading data to Firebase under '/tables' node...")
            target_ref = firebase_root_ref.child('tables').child(formatted)
            target_ref.set(backup_data) # Use set to overwrite existing backup
            print("Firebase backup completed successfully.")
        else:
            print("No data found in SQLite tables to backup.")

    except sqlite3.Error as e:
        print(f"SQLite Error during backup: {e}")
    except Exception as e:
        print(f"An error occurred during backup: {e}")
    finally:
        if conn:
            conn.close()
            print("SQLite connection closed.")


# --- Function 2: Restore Firebase to SQLite ---

def restore_firebase_to_sqlite(db_file_path, firebase_root_ref, formatted):
    """
    Restores data from Firebase Realtime Database to an SQLite database file.
    WARNING: This will overwrite the existing SQLite file if it exists.

    Args:
        db_file_path (str): The path where the SQLite database file will be created/overwritten.
        firebase_root_ref (firebase_admin.db.Reference): The root reference of your Firebase DB.
    """
    print(f"Starting restore from Firebase to '{db_file_path}'...")

    # Fetch data from Firebase
    try:
        print("Fetching data from Firebase '/tables' node...")
        tables_data = firebase_root_ref.child('tables').child(formatted).get()
        if not tables_data:
            print("No data found under '/tables' in Firebase. Restoration aborted.")
            return
        print("Data fetched successfully.")
    except Exception as e:
        print(f"Error fetching data from Firebase: {e}")
        return

    # Remove existing DB file to start fresh
    if os.path.exists(db_file_path):
        print(f"Removing existing SQLite file: '{db_file_path}'")
        os.remove(db_file_path)

    conn = None
    try:
        conn = sqlite3.connect(db_file_path)
        cursor = conn.cursor()
        print("SQLite connection opened.")

        # Iterate through tables fetched from Firebase
        for table_name, records in tables_data.items():
            print(f"  Processing table: {table_name}")

            # Get the schema for the table
            if table_name not in TABLE_SCHEMAS:
                print(f"    Warning: No schema found for table '{table_name}'. Skipping table.")
                continue

            schema_sql = TABLE_SCHEMAS[table_name]

            try:
                # Create the table
                print(f"    Creating table '{table_name}'...")
                cursor.execute(schema_sql)
                print(f"    Table '{table_name}' created successfully.")

                # Insert data
                if records and isinstance(records, dict): # Ensure records is a dict
                    print(f"    Inserting {len(records)} records into '{table_name}'...")
                    count = 0
                    for record_key, row_data in records.items():
                         if not isinstance(row_data, dict):
                              print(f"    Warning: Skipping invalid record data for key '{record_key}' in table '{table_name}'. Expected a dictionary.")
                              continue

                         # Prepare insert statement
                         columns = list(row_data.keys())
                         placeholders = ', '.join(['?'] * len(columns))
                         sql = f"INSERT OR REPLACE INTO {table_name} ({', '.join(columns)}) VALUES ({placeholders})"

                         # Prepare values tuple - order must match columns list
                         values = tuple(row_data.get(col) for col in columns)

                         try:
                             cursor.execute(sql, values)
                             count += 1
                         except sqlite3.Error as insert_err:
                             print(f"      Error inserting record {record_key} into {table_name}: {insert_err}")
                             print(f"      SQL: {sql}")
                             print(f"      Values: {values}")
                         except Exception as gen_err:
                              print(f"      Unexpected error inserting record {record_key} into {table_name}: {gen_err}")


                    print(f"    Inserted {count} records into '{table_name}'.")
                else:
                     print(f"    No records found for table '{table_name}' in Firebase data or data format is incorrect.")


            except sqlite3.Error as e:
                print(f"    SQLite Error processing table '{table_name}': {e}")
                print(f"    Schema SQL attempted:\n{schema_sql}")


        # Commit changes
        print("\nCommitting changes to SQLite database...")
        conn.commit()
        print("Restore completed successfully.")

    except sqlite3.Error as e:
        print(f"SQLite Error during restore: {e}")
        if conn:
            conn.rollback() # Rollback changes on error
    except Exception as e:
        print(f"An error occurred during restore: {e}")
        if conn:
            conn.rollback()
    finally:
        if conn:
            conn.close()
            print("SQLite connection closed.")


# --- Example Usage ---
# if __name__ == "__main__":
#     # --- Configuration ---
#     SQLITE_DB_FILE_BACKUP = 'my_database.db'      # Path to your existing SQLite DB
#     SQLITE_DB_FILE_RESTORE = 'my_database_restored.db' # Path for the restored DB file

#     # --- Create a dummy SQLite database for testing ---
#     print("--- Creating a dummy SQLite database for testing ---")
#     if os.path.exists(SQLITE_DB_FILE_BACKUP):
#         os.remove(SQLITE_DB_FILE_BACKUP)
#     temp_conn = sqlite3.connect(SQLITE_DB_FILE_BACKUP)
#     temp_cursor = temp_conn.cursor()
#     # Create tables using the schemas
#     for name, schema in TABLE_SCHEMAS.items():
#          temp_cursor.execute(schema)
#     # Add some sample data
#     temp_cursor.execute("INSERT INTO category (id, name, image) VALUES (?, ?, ?)", ('cat1', 'Electronics', 'electronics.jpg'))
#     temp_cursor.execute("INSERT INTO category (id, name, image) VALUES (?, ?, ?)", ('cat2', 'Books', 'books.jpg'))
#     temp_cursor.execute("INSERT INTO products (id, product_id, product_name, cat_id, cost_mrp, stock) VALUES (?, ?, ?, ?, ?, ?)",
#                        ('prod1', 'EL001', 'Laptop', 'cat1', 1200.50, 15))
#     temp_cursor.execute("INSERT INTO products (id, product_id, product_name, cat_id, cost_mrp, stock, product_img) VALUES (?, ?, ?, ?, ?, ?, ?)",
#                        ('prod2', 'BK001', 'Python Programming', 'cat2', 45.99, 50, json.dumps(['img1.jpg', 'img2.jpg']))) # Example JSON data
#     temp_cursor.execute("INSERT INTO userdata (id, uid, name, email, role, address, credits, creditse) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
#                        ('user1', 'uid123', 'Alice', 'alice@example.com', 'customer', '123 Main St', 10.0, '[]'))
#     temp_conn.commit()
#     temp_conn.close()
#     print(f"Dummy database '{SQLITE_DB_FILE_BACKUP}' created with sample data.\n")


#     # --- Perform Backup ---
#     print("--- Starting Backup Operation ---")
#     try:
#         root_ref = firebaseAuth.db.reference()
#         backup_sqlite_to_firebase(SQLITE_DB_FILE_BACKUP, root_ref)
#     except NameError:
#         print("\nBackup skipped because Firebase Admin SDK is not initialized (check credentials).")
#     except Exception as e:
#         print(f"\nError during backup call: {e}")


#     # --- Perform Restore ---
#     print("\n--- Starting Restore Operation ---")
#     # Optional: Add a small delay or manual confirmation before restoring
#     # input("Press Enter to proceed with restoring data from Firebase (will overwrite restore file)...")
#     try:
#         root_ref = firebaseAuth.db.reference()
#         restore_firebase_to_sqlite(SQLITE_DB_FILE_RESTORE, root_ref)

#         # Verification step (optional)
#         print("\n--- Verifying restored data (optional) ---")
#         if os.path.exists(SQLITE_DB_FILE_RESTORE):
#             verify_conn = sqlite3.connect(SQLITE_DB_FILE_RESTORE)
#             verify_cursor = verify_conn.cursor()
#             try:
#                  verify_cursor.execute("SELECT COUNT(*) FROM category")
#                  print(f"Category count in restored DB: {verify_cursor.fetchone()[0]}")
#                  verify_cursor.execute("SELECT COUNT(*) FROM products")
#                  print(f"Products count in restored DB: {verify_cursor.fetchone()[0]}")
#                  # Add more checks as needed
#             except sqlite3.Error as verify_err:
#                  print(f"Error verifying restored data: {verify_err}")
#             finally:
#                  verify_conn.close()
#         else:
#              print(f"Restored file '{SQLITE_DB_FILE_RESTORE}' not found for verification.")

#     except NameError:
#         print("\nRestore skipped because Firebase Admin SDK is not initialized (check credentials).")
#     except Exception as e:
#         print(f"\nError during restore call: {e}")

#     print("\n--- Script Finished ---")