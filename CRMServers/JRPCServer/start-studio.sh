#!/usr/bin/env bash

set -Eeuo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
database_path="${PETS_FORT_DB_PATH:-$script_dir/../backups_sqliteDBs_2026-07-26--12-49-45.db}"
firebase_credentials="${PETS_FORT_FIREBASE_CREDENTIALS:-/home/jay/Downloads/pets-fort-firebase-adminsdk-fbsvc-d431776ea2.json}"
studio_jar="${JRPC_STUDIO_JAR:-/home/jay/works/ServerlessCommunication/jrpc-samples/dist/jrpc-studio.jar}"
port="${JRPC_STUDIO_PORT:-8080}"

usage() {
    cat <<'EOF'
Usage: ./start-studio.sh [options]

Options:
  --database PATH       SQLite database path
  --credentials PATH    Firebase service-account JSON path
  --studio-jar PATH     JRPC Studio JAR path
  --port PORT           Studio HTTP port (default: 8080)
  -h, --help            Show this help

The PETS_FORT_DB_PATH, PETS_FORT_FIREBASE_CREDENTIALS,
JRPC_STUDIO_JAR, and JRPC_STUDIO_PORT environment variables may also be used.
EOF
}

while (($#)); do
    case "$1" in
        --database)
            (($# >= 2)) || { echo "Missing value for --database" >&2; exit 2; }
            database_path="$2"
            shift 2
            ;;
        --credentials)
            (($# >= 2)) || { echo "Missing value for --credentials" >&2; exit 2; }
            firebase_credentials="$2"
            shift 2
            ;;
        --studio-jar)
            (($# >= 2)) || { echo "Missing value for --studio-jar" >&2; exit 2; }
            studio_jar="$2"
            shift 2
            ;;
        --port)
            (($# >= 2)) || { echo "Missing value for --port" >&2; exit 2; }
            port="$2"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

[[ "$port" =~ ^[0-9]+$ ]] && ((port >= 1 && port <= 65535)) || {
    echo "Port must be an integer from 1 to 65535: $port" >&2
    exit 1
}

require_file() {
    local path="$1"
    local description="$2"
    [[ -f "$path" ]] || {
        echo "$description was not found: $path" >&2
        exit 1
    }
    realpath -- "$path"
}

database_path="$(require_file "$database_path" "PetsFort SQLite database")"
firebase_credentials="$(require_file "$firebase_credentials" "Firebase service-account JSON")"
studio_jar="$(require_file "$studio_jar" "JRPC Studio JAR")"

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]]; then
    java_path="$JAVA_HOME/bin/java"
elif java_path="$(command -v java)"; then
    :
else
    echo "Java was not found. Install Java or set JAVA_HOME." >&2
    exit 1
fi

export PETS_FORT_DB_PATH="$database_path"
export PETS_FORT_FIREBASE_CREDENTIALS="$firebase_credentials"
export JRPC_STUDIO_PORT="$port"

cd -- "$(dirname -- "$studio_jar")"

echo
echo "Starting JRPC Studio..."
echo "Studio URL:  http://127.0.0.1:$port"
echo "Database:    $database_path"
echo "Credentials: $firebase_credentials"
echo "Java:        $java_path"
echo
echo "Keep this terminal open. Press Ctrl+C to stop Studio and its workers."
echo

exec "$java_path" -jar "$studio_jar"
