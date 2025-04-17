import sys
import subprocess
import os
import time
# import signal # No longer needed for os.killpg

# Configuration
LOG_FILE = "serverLogs.txt"
# Command to start the server
CMD_START = ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]

# --- New approach using pattern matching ---
# Pattern to find the process for status and stopping.
# Make this AS SPECIFIC AS POSSIBLE to avoid matching other processes.
# Including arguments like host/port makes it safer.
PROCESS_PATTERN = "uvicorn main:app --host 0.0.0.0 --port 8000"

# Command to find the process (using pgrep). -a lists PID and full command.
CMD_PGREP = ["pgrep", "-af", PROCESS_PATTERN]
# Command to kill the process (using pkill). -f matches the full command line.
CMD_PKILL = ["pkill", "-f", PROCESS_PATTERN]
# --- End of new approach config ---


def check_process_status():
    """
    Checks if a process matching PROCESS_PATTERN is running using pgrep.

    Returns:
        tuple: (bool_is_running, str_details_or_none)
               Details contain the output of pgrep if running.
    """
    print(f"Checking for process matching: '{PROCESS_PATTERN}'")
    try:
        # Run pgrep, capture output, don't raise error on non-zero exit
        result = subprocess.run(
            CMD_PGREP,
            capture_output=True,
            text=True,
            check=False # Important: pgrep returns 1 if not found, don't treat as error
        )

        # pgrep returns 0 if match(es) found, 1 if no matches, >1 for errors
        if result.returncode == 0:
            # Process(es) found
            return True, result.stdout.strip()
        elif result.returncode == 1:
            # No process found
            return False, None
        else:
            # pgrep command itself failed
            print(f"Error running pgrep: {result.stderr}", file=sys.stderr)
            return False, None

    except FileNotFoundError:
        print(f"Error: '{CMD_PGREP[0]}' command not found. Cannot check status.", file=sys.stderr)
        print("Please install 'procps' or 'procps-ng' package.", file=sys.stderr)
        return False, None # Treat as not running, but indicate the error
    except Exception as e:
        print(f"Error checking server status: {e}", file=sys.stderr)
        return False, None


def start():
    """Starts the server if not already running."""
    is_running, details = check_process_status()
    if is_running:
        print("Server appears to be already running:")
        print(details)
        return

    print(f"Starting server with command: {' '.join(CMD_START)}")
    try:
        with open(LOG_FILE, "a") as log:
            # Use DEVNULL for stdin, log stdout/stderr
            # os.setsid is still good practice to detach the process
            # fully from the controlling terminal and make it a group leader.
            process = subprocess.Popen(
                CMD_START,
                stdout=log,
                stderr=log,
                stdin=subprocess.DEVNULL,
                preexec_fn=os.setsid # Optional but generally recommended
            )
        # Note: We don't store process.pid anymore
        print(f"Server start initiated in background. Check logs '{LOG_FILE}' and status.")
        # Give it a moment to potentially start up before next status check
        time.sleep(1)
        status() # Show status after attempting start

    except FileNotFoundError:
         print(f"Error: '{CMD_START[0]}' command not found. Cannot start server.", file=sys.stderr)
    except Exception as e:
         print(f"Error starting server: {e}", file=sys.stderr)


def stop(onStopped=None):
    """Stops the server process using pkill."""
    is_running, details = check_process_status()

    if not is_running:
        print("Server is not running.")
        print("Trying Force Kill.")
    
    if details:
        print(f"Found running process(es):\n{details}")

    print(f"Attempting to stop using: {' '.join(CMD_PKILL)}")

    try:
        # Run pkill, check=False to handle non-zero exit codes manually
        result = subprocess.run(CMD_PKILL, check=False, capture_output=True, text=True)

        # pkill returns 0 if at least one process was signaled, 1 if no match
        if result.returncode == 0:
            print("Stop signal sent successfully (pkill exited with 0). Verifying...")
            # Give processes time to terminate gracefully
            time.sleep(2)
            is_running_after, _ = check_process_status()
            if not is_running_after:
                print("Server stopped successfully.")
                if onStopped:
                    onStopped()
            else:
                print("Warning: Server process might still be running after sending signal.", file=sys.stderr)
                print("Check status again or try 'pkill -9 -f \"{PROCESS_PATTERN}\"' manually if needed.", file=sys.stderr)
        elif result.returncode == 1:
             # This is unexpected if check_process_status found it just before
             print("Warning: pkill reported no processes matched, contradicting earlier check.", file=sys.stderr)
        else:
            # pkill command itself failed
            print(f"Error running pkill: {result.stderr}", file=sys.stderr)

    except FileNotFoundError:
        print(f"Error: '{CMD_PKILL[0]}' command not found. Cannot stop server.", file=sys.stderr)
        print("Please install 'procps' or 'procps-ng' package.", file=sys.stderr)
    except Exception as e:
        print(f"Error stopping server: {e}", file=sys.stderr)


def restart():
    """Restarts the server."""
    print("Restarting server...")
    # Pass the start function as the callback to run after stopping completes
    stop(onStopped=start)


def status():
    """Checks and displays the detailed status of the server process."""
    is_running, details = check_process_status()
    if is_running:
        print("--- Server Status: Running ---")
        print("Process details (PID and Command):")
        print(details)
        print("---")
    else:
        # check_process_status already prints errors if commands are missing
        print("--- Server Status: Not Running ---")


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
    else:
        print("Invalid command. Use start, stop, res, or status.")
        sys.exit(1)