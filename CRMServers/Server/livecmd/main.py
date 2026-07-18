from fastapi import FastAPI, WebSocket, WebSocketDisconnect, HTTPException
from fastapi.responses import HTMLResponse, FileResponse
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.cors import CORSMiddleware
import os
import pty
import select
import termios
import struct
import fcntl
import signal
import asyncio
import json
import subprocess

app = FastAPI(title="Terminal API")

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, replace with specific origins
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Create a static directory if it doesn't exist
os.makedirs("static", exist_ok=True)

# Mount static files for the frontend
app.mount("/static", StaticFiles(directory="static"), name="static")


@app.get("/", response_class=HTMLResponse)
async def get_terminal():
    """Return the HTML page for the terminal"""
    return FileResponse("static/index.html")

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

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)