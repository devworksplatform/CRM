from fastapi import FastAPI
import sqlite3

app = FastAPI()

@app.get("/")
def read_root():
    conn = sqlite3.connect("/app/sqlite/mydata.db")
    cursor = conn.cursor()
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
    tables = cursor.fetchall()
    conn.close()
    return {"tables": tables}
