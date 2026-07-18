import sys
import subprocess
import os
import time
# import signal # No longer needed for os.killpg

# Configuration
LOG_FILE = "serverLogs.txt"
LOG_FILE_80 = "serverLogs_80.txt"

# --- Server on port 443 (HTTPS with SSL) ---
CMD_START_443 = ["sudo", "/home/ubuntu/CRM/venv/bin/uvicorn", "main:app", "--host", "0.0.0.0", "--port", "443", "--ssl-certfile", "/etc/letsencrypt/live/petsfort.in/fullchain.pem", "--ssl-keyfile", "/etc/letsencrypt/live/petsfort.in/privkey.pem"]
PROCESS_PATTERN_443 = "uvicorn main:app --host 0.0.0.0 --port 443"

# --- Server on port 80 (HTTP) ---
CMD_START_80 = ["sudo", "/home/ubuntu/CRM/venv/bin/uvicorn", "main:app", "--host", "0.0.0.0", "--port", "80"]
PROCESS_PATTERN_80 = "uvicorn main:app --host 0.0.0.0 --port 80"

# List of all server instances
SERVERS = [
    {"name": "HTTPS (443)", "cmd_start": CMD_START_443, "pattern": PROCESS_PATTERN_443, "log_file": LOG_FILE},
    {"name": "HTTP (80)",   "cmd_start": CMD_START_80,  "pattern": PROCESS_PATTERN_80,  "log_file": LOG_FILE_80},
]


def check_process_status_for(pattern):
    """Check if a process matching the given pattern is running."""
    try:
        result = subprocess.run(
            ["pgrep", "-af", pattern],
            capture_output=True,
            text=True,
            check=False
        )
        if result.returncode == 0:
            return True, result.stdout.strip()
        elif result.returncode == 1:
            return False, None
        else:
            print(f"Error running pgrep: {result.stderr}", file=sys.stderr)
            return False, None
    except FileNotFoundError:
        print("Error: 'pgrep' command not found. Install 'procps' package.", file=sys.stderr)
        return False, None
    except Exception as e:
        print(f"Error checking server status: {e}", file=sys.stderr)
        return False, None


def start():
    all_running = True
    for server in SERVERS:
        is_running, details = check_process_status_for(server["pattern"])
        if is_running:
            print(f"[{server['name']}] Already running: {details}")
        else:
            all_running = False
            print(f"[{server['name']}] Starting: {' '.join(server['cmd_start'])}")
            try:
                with open(server["log_file"], "a") as log:
                    subprocess.Popen(
                        server["cmd_start"],
                        stdout=log,
                        stderr=log,
                        stdin=subprocess.DEVNULL,
                        preexec_fn=os.setsid
                    )
                print(f"[{server['name']}] Start initiated. Logs: {server['log_file']}")
            except FileNotFoundError:
                print(f"[{server['name']}] Error: command not found.", file=sys.stderr)
            except Exception as e:
                print(f"[{server['name']}] Error starting: {e}", file=sys.stderr)

    if all_running:
        print("All servers already running.")
    else:
        time.sleep(1)
        status()


def stop(onStopped=None):
    any_stopped = False
    for server in SERVERS:
        is_running, details = check_process_status_for(server["pattern"])
        if not is_running:
            print(f"[{server['name']}] Not running.")
        else:
            if details:
                print(f"[{server['name']}] Found: {details}")

        print(f"[{server['name']}] Stopping with pkill...")
        try:
            result = subprocess.run(
                ["pkill", "-f", server["pattern"]],
                check=False, capture_output=True, text=True
            )
            if result.returncode == 0:
                any_stopped = True
                print(f"[{server['name']}] Stop signal sent.")
            elif result.returncode == 1:
                print(f"[{server['name']}] No process matched for pkill.")
            else:
                print(f"[{server['name']}] pkill error: {result.stderr}", file=sys.stderr)
        except FileNotFoundError:
            print("Error: 'pkill' command not found.", file=sys.stderr)
        except Exception as e:
            print(f"[{server['name']}] Error stopping: {e}", file=sys.stderr)

    if any_stopped:
        time.sleep(2)
        # Verify
        all_stopped = True
        for server in SERVERS:
            is_running, _ = check_process_status_for(server["pattern"])
            if is_running:
                all_stopped = False
                print(f"[{server['name']}] WARNING: Still running after stop signal.", file=sys.stderr)
        if all_stopped:
            print("All servers stopped successfully.")
            if onStopped:
                onStopped()
        else:
            print("Some servers may still be running. Try force kill manually.", file=sys.stderr)
    else:
        print("No servers were running.")
        if onStopped:
            onStopped()


def restart():
    print("Restarting all servers...")
    stop(onStopped=start)


def status():
    print("=" * 50)
    for server in SERVERS:
        is_running, details = check_process_status_for(server["pattern"])
        if is_running:
            print(f"[{server['name']}] Status: RUNNING")
            print(f"  PID/Command: {details}")
        else:
            print(f"[{server['name']}] Status: NOT RUNNING")
    print("=" * 50)

def logs():
    for server in SERVERS:
        log_file_path = server["log_file"]
        print(f"\n{'=' * 20} [{server['name']}] Logs ({log_file_path}) {'=' * 20}")
        try:
            with open(log_file_path, "r", encoding="utf-8") as f:
                content = f.read()
            print(content)
        except Exception as e:
            print(f"Could not read {log_file_path}: {str(e)}")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print(f"Usage: python {sys.argv[0]} <start|stop|res|status>")
        sys.exit(1)

    command = sys.argv[1].lower()
    if command == "start":
        start()
    elif command == "stop":
        stop()
    elif command == "res": # Assuming 'res' means restart
        restart()
    elif command == "status":
        status()
    elif command == "log":
        logs()
    else:
        print("Invalid command. Use start, stop, res, or status.")
        sys.exit(1)