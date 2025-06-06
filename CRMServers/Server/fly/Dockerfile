FROM python:3.11-slim

WORKDIR /app

# Only copy requirements first (for caching)
COPY server/requirements.txt .

# Install only what’s needed
RUN pip install --no-cache-dir -r requirements.txt

# Copy only the app code
COPY server/ .

# CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8080"]
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port 8080 > /app/serverLogs.txt 2>&1"]
