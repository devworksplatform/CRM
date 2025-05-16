// const TARGET_URL_WS = "https://ec2-13-203-205-116.ap-south-1.compute.amazonaws.com"

async function initMod8() { // Assuming this is your entry point for this module
    // const API_URL = TARGET_URL+"/system-stats/live"; // Your FastAPI endpoint

    // Enable the fit addon for xterm
        Terminal.applyAddon(fit);

        const terminal = new Terminal({
            cursorBlink: true,
            fontFamily: 'Courier New, monospace',
            fontSize: 16,
            theme: {
                background: '#1e1e1e',
                foreground: '#f0f0f0',
                cursor: '#ffffff',
                selection: 'rgba(255, 255, 255, 0.3)',
                black: '#000000',
                red: '#e06c75',
                green: '#98c379',
                yellow: '#e5c07b',
                blue: '#61afef',
                magenta: '#c678dd',
                cyan: '#56b6c2',
                white: '#d0d0d0',
                brightBlack: '#808080',
                brightRed: '#f44747',
                brightGreen: '#b5e890',
                brightYellow: '#f1d798',
                brightBlue: '#8fc6f4',
                brightMagenta: '#d8a6e0',
                brightCyan: '#7bc6d0',
                brightWhite: '#ffffff'
            }
        });

        // DOM elements
        const statusIndicator = document.getElementById('status-indicator');
        const statusText = document.getElementById('status-text');
        const terminalSizeElement = document.getElementById('terminal-size');
        const commandInput = document.getElementById('command-input');
        const connectButton = document.getElementById('connect-btn');
        const disconnectButton = document.getElementById('disconnect-btn');
        const terminalElement = document.getElementById('terminal');

        // WebSocket connection
        let socket = null;
        let connected = false;
        let terminalId = null;

        // Open terminal and fit it to container
        terminal.open(terminalElement);
        fitTerminal();

        // Handle terminal resizing
        window.addEventListener('resize', () => {
            fitTerminal();
            if (connected) {
                sendResizeMessage();
            }
        });

        // Connect button click handler
        connectButton.addEventListener('click', connectToTerminal);
        
        // Disconnect button click handler
        disconnectButton.addEventListener('click', disconnectFromTerminal);

        // Terminal key input handler
        terminal.onData(data => {
            if (connected) {
                sendInputMessage(data);
            }
        });

        function connectToTerminal() {
            // Get the host dynamically based on the current URL
            const host = window.location.host;
            const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';


            console.log(host)

            // const wsUrl = `${protocol}//${host}/ws/terminal`;
            const wsUrl = `ws://ec2-13-203-205-116.ap-south-1.compute.amazonaws.com:8000/ws/terminal`;

            updateStatus('connecting', 'Connecting...');
            
            // Close existing socket if any
            if (socket) {
                socket.close();
            }
            
            // Create new WebSocket connection
            socket = new WebSocket(wsUrl);
            
            // Connection opened
            socket.addEventListener('open', () => {
                // Send initial configuration
                const command = commandInput.value.trim() || '/bin/bash';
                const { cols, rows } = terminal;
                
                socket.send(JSON.stringify({
                    command: command,
                    cols: cols,
                    rows: rows
                }));
                
                updateStatus('connected', 'Connected');
                connected = true;
                
                // Update button states
                connectButton.disabled = true;
                disconnectButton.disabled = false;
                
                // Clear terminal
                terminal.clear();
            });
            
            // Listen for messages
            socket.addEventListener('message', (event) => {
                const message = JSON.parse(event.data);
                
                switch (message.type) {
                    case 'connected':
                        terminalId = message.terminalId;
                        break;
                        
                    case 'output':
                        terminal.write(message.data);
                        break;
                        
                    case 'exit':
                        terminal.writeln(`\r\n\nProcess exited with code ${message.code}`);
                        disconnectFromTerminal();
                        break;
                        
                    case 'eof':
                        terminal.writeln('\r\n\nConnection closed');
                        disconnectFromTerminal();
                        break;
                }
            });
            
            // Socket closed
            socket.addEventListener('close', () => {
                disconnectFromTerminal();
            });
            
            // Socket error
            socket.addEventListener('error', (error) => {
                console.error('WebSocket error:', error);
                terminal.writeln('\r\n\nConnection error');
                disconnectFromTerminal();
            });
        }

        function disconnectFromTerminal() {
            if (socket) {
                socket.close();
                socket = null;
            }
            
            connected = false;
            terminalId = null;
            
            updateStatus('disconnected', 'Disconnected');
            
            // Update button states
            connectButton.disabled = false;
            disconnectButton.disabled = true;
        }

        function sendInputMessage(data) {
            if (socket && socket.readyState === WebSocket.OPEN) {
                socket.send(JSON.stringify({
                    type: 'input',
                    data: data
                }));
            }
        }

        function sendResizeMessage() {
            if (socket && socket.readyState === WebSocket.OPEN) {
                const { cols, rows } = terminal;
                
                socket.send(JSON.stringify({
                    type: 'resize',
                    cols: cols,
                    rows: rows
                }));
                
                terminalSizeElement.textContent = `${cols}x${rows}`;
            }
        }

        function fitTerminal() {
            terminal.fit();
            const { cols, rows } = terminal;
            terminalSizeElement.textContent = `${cols}x${rows}`;
        }

        function updateStatus(status, text) {
            statusIndicator.className = `status-indicator status-${status}`;
            statusText.textContent = text;
        }

        // Initial welcome message
        terminal.writeln('Web Terminal - Connect to start');
        terminal.writeln('');
        terminal.writeln('Enter a command in the input box above or use the default /bin/bash');
        terminal.writeln('Click Connect to start your terminal session');
}
window.initMod8 = initMod8;
