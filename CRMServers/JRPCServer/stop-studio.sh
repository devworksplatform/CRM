#!/usr/bin/env bash

set -Eeuo pipefail

port="${JRPC_STUDIO_PORT:-8080}"

usage() {
    cat <<'EOF'
Usage: ./stop-studio.sh [--port PORT]

Stops JRPC Studio and its workers. JRPC_STUDIO_PORT may also set the port.
EOF
}

while (($#)); do
    case "$1" in
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

command -v lsof >/dev/null 2>&1 || {
    echo "lsof is required to safely identify the process listening on port $port." >&2
    exit 1
}

mapfile -t listener_pids < <(
    lsof -nP -t -iTCP:"$port" -sTCP:LISTEN 2>/dev/null | sort -u
)

if ((${#listener_pids[@]} == 0)); then
    echo "JRPC Studio is already stopped. Port $port is closed."
    exit 0
fi

if ((${#listener_pids[@]} != 1)); then
    echo "Port $port has multiple listening processes (${listener_pids[*]}). Nothing was stopped." >&2
    exit 1
fi

studio_pid="${listener_pids[0]}"
studio_command="$(tr '\0' ' ' < "/proc/$studio_pid/cmdline" 2>/dev/null || true)"
if [[ "$studio_command" != *"jrpc-studio.jar"* ]]; then
    echo "Port $port is owned by process $studio_pid, but it is not JRPC Studio. Nothing was stopped." >&2
    exit 1
fi

base_url="http://127.0.0.1:$port"
if command -v curl >/dev/null 2>&1 && command -v jq >/dev/null 2>&1; then
    echo "Stopping JRPC workers cleanly..."
    if registry="$(curl --fail --silent --show-error --max-time 10 "$base_url/api/registry" 2>/dev/null)"; then
        while IFS=$'\t' read -r server_id server_name; do
            [[ -n "$server_id" ]] || continue
            body="$(jq -nc --arg server "$server_id" '{server: $server}')"
            if result="$(curl --fail --silent --show-error --max-time 45 \
                -H "Content-Type: application/json" -d "$body" \
                "$base_url/api/runtime/stop" 2>&1)"; then
                state="$(jq -r '.state // "unknown"' <<<"$result")"
                message="$(jq -r '.message // ""' <<<"$result")"
                echo "  $server_name: $state - $message"
            else
                echo "Warning: could not cleanly stop $server_name: $result" >&2
            fi
        done < <(jq -r '.servers[]? | [.id, (.name // .id)] | @tsv' <<<"$registry")
    else
        echo "Warning: could not read the Studio registry." >&2
    fi
else
    echo "Warning: curl and jq are needed for clean worker shutdown; continuing with process shutdown." >&2
fi

descendants=()
queue=("$studio_pid")
while ((${#queue[@]})); do
    parent="${queue[0]}"
    queue=("${queue[@]:1}")
    mapfile -t children < <(pgrep -P "$parent" 2>/dev/null || true)
    for child in "${children[@]}"; do
        descendants+=("$child")
        queue+=("$child")
    done
done

targets=("${descendants[@]}" "$studio_pid")
kill -TERM "${targets[@]}" 2>/dev/null || true

deadline=$((SECONDS + 10))
while ((SECONDS < deadline)) && kill -0 "$studio_pid" 2>/dev/null; do
    sleep 0.25
done

remaining=()
for pid in "${targets[@]}"; do
    kill -0 "$pid" 2>/dev/null && remaining+=("$pid")
done
((${#remaining[@]} == 0)) || kill -KILL "${remaining[@]}" 2>/dev/null || true

# Clean up workers whose Studio parent exited earlier.
mapfile -t orphan_workers < <(
    ps -eo pid=,args= | awk '
        /com[.]jay[.]server[.]RpcServerWorker/ && /[.]jrpc-studio/ { print $1 }
    '
)
((${#orphan_workers[@]} == 0)) || kill -KILL "${orphan_workers[@]}" 2>/dev/null || true

deadline=$((SECONDS + 10))
while ((SECONDS < deadline)) && lsof -nP -t -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; do
    sleep 0.25
done

if lsof -nP -t -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1; then
    echo "JRPC Studio was stopped, but port $port is still listening." >&2
    exit 1
fi

echo "JRPC Studio and all workers are stopped. Port $port is closed."
