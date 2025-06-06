
from main import firebaseAuth,TABLE_SCHEMAS
import sqlite3
import json
import os
from datetime import datetime

# --- Function 1: Backup SQLite to Firebase ---
def log_to_dict(logs):
    """Converts log messages into a dictionary with numerical keys."""
    log_dict = {}
    for idx, log in enumerate(logs, start=1):
        log_dict[idx] = str(log)  # Ensure all logs are in string format
    return log_dict

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
        if col[5] == 1:  # col[5] is the 'pk' column in PRAGMA table_info output
            return col[1]  # col[1] is the 'name' of the column
    # Fallback if no explicit PK is found (might use rowid or first column)
    print(f"Warning: No explicit primary key found for table '{table_name}'.")
    return columns[0][1]  # Assuming the first column as primary key if no explicit PK found

def backup_sqlite_to_firebase(db_file_path, firebase_root_ref, formatted):
    """
    Reads data from an SQLite database and backs it up to Firebase Realtime Database.
    """
    logs = []  # Collecting logs in a list

    if not os.path.exists(db_file_path):
        logs.append(f"Error: SQLite database file not found at '{db_file_path}'")
        return log_to_dict(logs)

    logs.append(f"Starting backup from '{db_file_path}' to Firebase...")
    conn = None
    try:
        conn = sqlite3.connect(db_file_path)
        conn.row_factory = sqlite3.Row
        cursor = conn.cursor()

        tables = get_table_names(conn)
        logs.append(f"Found tables: {tables}")

        backup_data = {}

        for table_name in tables:
            logs.append(f"  Processing table: {table_name}")
            pk_column = get_primary_key(conn, table_name)
            if not pk_column:
                logs.append(f"  Skipping table '{table_name}' due to missing primary key information.")
                continue

            cursor.execute(f"SELECT * FROM {table_name}")
            rows = cursor.fetchall()

            table_data_for_firebase = {}
            for row in rows:
                row_dict = dict(row)
                primary_key_value = row_dict.get(pk_column)

                if primary_key_value is None:
                    logs.append(f"    Warning: Row in table '{table_name}' has NULL primary key ('{pk_column}'). Skipping row.")
                    continue

                firebase_key = str(primary_key_value)

                for key, value in row_dict.items():
                    if isinstance(value, datetime):
                         row_dict[key] = value.isoformat()  # Convert datetime to ISO string

                table_data_for_firebase[firebase_key] = row_dict

            if table_data_for_firebase:
                backup_data[table_name] = table_data_for_firebase
            else:
                logs.append(f"  Table '{table_name}' is empty or all rows lacked valid primary keys.")

        if backup_data:
            logs.append("\nUploading data to Firebase under '/tables' node...")
            target_ref = firebase_root_ref.child('tables').child(formatted)
            target_ref.set(backup_data)
            target_ref = firebase_root_ref.child('tables').child("latest")
            target_ref.delete()
            target_ref.set(backup_data)
            logs.append("Firebase backup completed successfully.")
        else:
            logs.append("No data found in SQLite tables to backup.")

    except sqlite3.Error as e:
        logs.append(f"SQLite Error during backup: {e}")
    except Exception as e:
        logs.append(f"An error occurred during backup: {e}")
    finally:
        if conn:
            conn.close()
            logs.append("SQLite connection closed.")

    return log_to_dict(logs)

# --- Function 2: Restore Firebase to SQLite ---
def restore_firebase_to_sqlite(db_file_path, firebase_root_ref, formatted):
    """
    Restores data from Firebase Realtime Database to an SQLite database file.
    WARNING: This will overwrite the existing SQLite file if it exists.
    """
    logs = []  # Collecting logs in a list

    logs.append(f"Starting restore from Firebase to '{db_file_path}'...")

    try:
        logs.append("Fetching data from Firebase '/tables' node...")
        tables_data = firebase_root_ref.child('tables').child(formatted).get()
        if not tables_data:
            logs.append("No data found under '/tables' in Firebase. Restoration aborted.")
            return log_to_dict(logs)
        logs.append("Data fetched successfully.")
    except Exception as e:
        logs.append(f"Error fetching data from Firebase: {e}")
        return log_to_dict(logs)

    if os.path.exists(db_file_path):
        logs.append(f"Removing existing SQLite file: '{db_file_path}'")
        os.remove(db_file_path)

    conn = None
    try:
        conn = sqlite3.connect(db_file_path)
        cursor = conn.cursor()
        logs.append("SQLite connection opened.")

        for table_name, records in tables_data.items():
            logs.append(f"  Processing table: {table_name}")

            if table_name not in TABLE_SCHEMAS:
                logs.append(f"    Warning: No schema found for table '{table_name}'. Skipping table.")
                continue

            schema_sql = TABLE_SCHEMAS[table_name]

            try:
                logs.append(f"    Creating table '{table_name}'...")
                cursor.execute(schema_sql)
                logs.append(f"    Table '{table_name}' created successfully.")

                if records and isinstance(records, dict):
                    logs.append(f"    Inserting {len(records)} records into '{table_name}'...")
                    count = 0
                    for record_key, row_data in records.items():
                         if not isinstance(row_data, dict):
                              logs.append(f"    Warning: Skipping invalid record data for key '{record_key}' in table '{table_name}'. Expected a dictionary.")
                              continue

                         columns = list(row_data.keys())
                         placeholders = ', '.join(['?'] * len(columns))
                         sql = f"INSERT OR REPLACE INTO {table_name} ({', '.join(columns)}) VALUES ({placeholders})"
                         values = tuple(row_data.get(col) for col in columns)

                         try:
                             cursor.execute(sql, values)
                             count += 1
                         except sqlite3.Error as insert_err:
                             logs.append(f"      Error inserting record {record_key} into {table_name}: {insert_err}")
                             logs.append(f"      SQL: {sql}")
                             logs.append(f"      Values: {values}")
                         except Exception as gen_err:
                              logs.append(f"      Unexpected error inserting record {record_key} into {table_name}: {gen_err}")

                    logs.append(f"    Inserted {count} records into '{table_name}'.")
                else:
                     logs.append(f"    No records found for table '{table_name}' in Firebase data or data format is incorrect.")

            except sqlite3.Error as e:
                logs.append(f"    SQLite Error processing table '{table_name}': {e}")
                logs.append(f"    Schema SQL attempted:\n{schema_sql}")

        logs.append("\nCommitting changes to SQLite database...")
        conn.commit()
        logs.append("Restore completed successfully.")

    except sqlite3.Error as e:
        logs.append(f"SQLite Error during restore: {e}")
        if conn:
            conn.rollback()
    except Exception as e:
        logs.append(f"An error occurred during restore: {e}")
        if conn:
            conn.rollback()
    finally:
        if conn:
            conn.close()
            logs.append("SQLite connection closed.")

    return log_to_dict(logs)