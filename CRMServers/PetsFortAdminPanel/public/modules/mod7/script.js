const TARGET_URL = "https://ec2-13-203-205-116.ap-south-1.compute.amazonaws.com"

async function initMod7() { // Assuming this is your entry point for this module
    const API_URL = TARGET_URL+"/system-stats/live"; // Your FastAPI endpoint

    const ramChartCtx = document.getElementById('ramChart').getContext('2d');
    let ramChart = new Chart(ramChartCtx, {
        type: 'line',
        data: {
            labels: [],
            datasets: [{
                label: 'RAM Usage %',
                data: [],
                borderColor: '#4f46e5',
                backgroundColor: 'rgba(79, 70, 229, 0.2)',
                fill: true,
                tension: 0.3,
                pointRadius: 0
            }]
        },
        options: {
            responsive: true,
            animation: false,
            scales: {
                x: { display: false },
                y: {
                    beginAtZero: true,
                    max: 100,
                    ticks: { stepSize: 20 }
                }
            },
            plugins: {
                legend: { display: false }
            }
        }
    });

    const refreshLog = document.getElementById('refreshLog');
    const deleteLog = document.getElementById('deleteLog');
    const logsDiv = document.getElementById('logsDiv');


    let logInterval = null;
    let isRefreshing = false;

    // Function to fetch and display logs
    refreshLog.addEventListener('click', () => {
        if (!isRefreshing) {
            // Start refreshing logs every second
            logInterval = setInterval(async () => {
                let logs = await callApi("GET", "logs", null, false);
                // Filter out lines containing "GET /logs HTTP/1.1"
                logs = logs
                    .split('\n')
                    .filter(line => !line.includes("GET /logs HTTP/1.1"))
                    .filter(line => !line.includes("GET /ram HTTP/1.1"))
                    .join('\n');
                logsDiv.textContent = logs;
                logsDiv.scrollTop = logsDiv.scrollHeight;
            }, 1000);
            isRefreshing = true;
            refreshLog.innerHTML = `Stop Logs`;
        } else {
            // Stop refreshing
            clearInterval(logInterval);
            logInterval = null;
            isRefreshing = false;
            refreshLog.innerHTML = `Start Logs`;
        }
    });


    // Function to delete logs
    deleteLog.addEventListener('click', async () => {
        await callApi("DELETE", "logs", null, false);
        if(!isRefreshing){
            logsDiv.textContent = "";
        }
    });

    refreshLog.click();

    const refreshRam = document.getElementById('refreshRam');
    const ramDiv = document.getElementById('ramDiv');

    let ramInterval = null;
    let isRamRefreshing = false;

    // Function to fetch and display logs
    refreshRam.addEventListener('click', () => {
        if (!isRamRefreshing) {
            // Start refreshing logs every second
            ramInterval = setInterval(async () => {
                let ramDetails = await callApi("GET", "ram", null, false);
                ramDiv.textContent = ramDetails;
            }, 1000);
            isRamRefreshing = true;
            refreshRam.innerHTML = `Stop Stream`;
        } else {
            // Stop refreshing
            clearInterval(ramInterval);
            ramInterval = null;
            isRamRefreshing = false;
            refreshRam.innerHTML = `Start Stream`;
        }
    });

    refreshRam.click()



    const connectionStatusEl = document.getElementById('connection-status');
    const statusIndicatorEl = document.getElementById('status-indicator');
    const lastUpdatedEl = document.getElementById('last-updated');
    const toggleLiveButton = document.getElementById('toggle-live-button');

    // CPU Elements
    const cpuTotalUsageEl = document.getElementById('cpu-total-usage');
    const cpuTotalUsageBarEl = document.getElementById('cpu-total-usage-bar');
    const cpuLogicalCoresEl = document.getElementById('cpu-logical-cores');
    const cpuPhysicalCoresEl = document.getElementById('cpu-physical-cores');
    const cpuFrequencyEl = document.getElementById('cpu-frequency');
    const cpuPerCoreUsageContainer = document.getElementById('cpu-per-core-usage');
    const cpuLoad1mEl = document.getElementById('cpu-load-1m');
    const cpuLoad5mEl = document.getElementById('cpu-load-5m');
    const cpuLoad15mEl = document.getElementById('cpu-load-15m');
    const cpuCtxSwitchesEl = document.getElementById('cpu-ctx-switches');
    const cpuInterruptsEl = document.getElementById('cpu-interrupts');

    // Memory Elements
    const memVirtualTotalEl = document.getElementById('mem-virtual-total');
    const memVirtualUsedEl = document.getElementById('mem-virtual-used');
    const memVirtualUsedBarEl = document.getElementById('mem-virtual-used-bar');
    const memVirtualAvailableEl = document.getElementById('mem-virtual-available');
    const memVirtualPercentEl = document.getElementById('mem-virtual-percent');
    const memSwapTotalEl = document.getElementById('mem-swap-total');
    const memSwapUsedEl = document.getElementById('mem-swap-used');
    const memSwapUsedBarEl = document.getElementById('mem-swap-used-bar');
    const memSwapFreeEl = document.getElementById('mem-swap-free');
    const memSwapPercentEl = document.getElementById('mem-swap-percent');

    // Disk Elements
    const diskPartitionsContainer = document.getElementById('disk-partitions-container');
    const diskReadCountEl = document.getElementById('disk-read-count');
    const diskWriteCountEl = document.getElementById('disk-write-count');
    const diskReadBytesEl = document.getElementById('disk-read-bytes');
    const diskWriteBytesEl = document.getElementById('disk-write-bytes');

    // Network Elements
    const netBytesSentEl = document.getElementById('net-bytes-sent');
    const netBytesRecvEl = document.getElementById('net-bytes-recv');
    const netPacketsSentEl = document.getElementById('net-packets-sent');
    const netPacketsRecvEl = document.getElementById('net-packets-recv');
    const networkInterfacesContainer = document.getElementById('network-interfaces-container');

    // Sensors Elements
    const sensorsContainer = document.getElementById('sensors-container');

    // System/OS Elements
    const osHostnameEl = document.getElementById('os-hostname');
    const osPlatformEl = document.getElementById('os-platform');
    const osReleaseEl = document.getElementById('os-release');
    const osVersionEl = document.getElementById('os-version');
    const osArchEl = document.getElementById('os-arch');
    const osPythonVersionEl = document.getElementById('os-python-version');
    const osBootTimeEl = document.getElementById('os-boot-time');
    const osUsersListEl = document.getElementById('os-users-list');

    // Process Elements
    const procTotalEl = document.getElementById('proc-total');
    const procTopCpuListEl = document.getElementById('proc-top-cpu');
    const procTopMemListEl = document.getElementById('proc-top-mem');

    let currentEventSource = null;
    let isLiveEnabled = false; // Defaultly disable live updates

    function updateText(element, value, unit = '', notAvailableValue = 'N/A') {
        if (element) {
            element.textContent = (value !== undefined && value !== null && value !== 'N/A') ? `${value}${unit}` : notAvailableValue;
        }
    }

    function updateProgressBar(barElement, value) {
        if (barElement) {
            const percentage = parseFloat(value) || 0;
            barElement.style.width = `${percentage}%`;
            barElement.classList.remove('high-usage', 'medium-usage');
            if (percentage > 85) {
                barElement.classList.add('high-usage');
            } else if (percentage > 60) {
                barElement.classList.add('medium-usage');
            }
        }
    }

    function formatBytes(bytes, decimals = 2) {
        if (!+bytes) return '0 Bytes'
        const k = 1024
        const dm = decimals < 0 ? 0 : decimals
        const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
        const i = Math.floor(Math.log(bytes) / Math.log(k))
        return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`
    }

    function connectEventSource() {
        if (!isLiveEnabled || currentEventSource) { // Don't connect if not enabled or already connected
            return;
        }

        currentEventSource = new EventSource(API_URL);
        connectionStatusEl.textContent = "Connecting...";
        statusIndicatorEl.className = "status-indicator"; // Default (yellowish/connecting)

        currentEventSource.onopen = function () {
            connectionStatusEl.textContent = "Connected";
            statusIndicatorEl.className = "status-indicator connected";
            console.log("Connection to SSE opened.");
        };

        currentEventSource.onmessage = function (event) {
            try {
                const data = JSON.parse(event.data);
                updateUI(data);
                lastUpdatedEl.textContent = `Last updated: ${new Date().toLocaleTimeString()}`;
            } catch (error) {
                console.error("Failed to parse JSON data:", error, "Raw data:", event.data);
                 connectionStatusEl.textContent = "Data Error";
                 statusIndicatorEl.className = "status-indicator error";
            }
        };

        currentEventSource.onerror = function (err) {
            console.error("EventSource failed:", err);
            connectionStatusEl.textContent = "Disconnected. Retrying...";
            statusIndicatorEl.className = "status-indicator"; // Default red for error
            if (currentEventSource) {
                currentEventSource.close();
                currentEventSource = null;
            }
            // Optional: Implement a retry mechanism with backoff if isLiveEnabled is still true
            if (isLiveEnabled) {
                setTimeout(connectEventSource, 5000); // Retry after 5 seconds ONLY if still enabled
            } else {
                connectionStatusEl.textContent = "Live Disabled";
                statusIndicatorEl.className = "status-indicator"; // Neutral/off
            }
        };
    }

    function disconnectEventSource() {
        if (currentEventSource) {
            currentEventSource.close();
            currentEventSource = null;
            console.log("Connection to SSE closed by user.");
        }
        connectionStatusEl.textContent = "Live Disabled";
        statusIndicatorEl.className = "status-indicator"; // Neutral/off
        lastUpdatedEl.textContent = 'Last updated: N/A';
        // Optionally, clear the data fields or set them to 'N/A'
        // For simplicity, this example doesn't clear all fields upon disconnect.
        // You might want to call updateUI with empty/default data or add a clearUI() function.
    }

    toggleLiveButton.addEventListener('click', () => {
        isLiveEnabled = !isLiveEnabled;
        if (isLiveEnabled) {
            toggleLiveButton.textContent = 'Disable Live';
            connectEventSource();
        } else {
            toggleLiveButton.textContent = 'Enable Live';
            disconnectEventSource();
        }
    });

    // Set initial state for UI elements based on isLiveEnabled = false
    connectionStatusEl.textContent = "Live Disabled";
    statusIndicatorEl.className = "status-indicator"; // Neutral/off state
    lastUpdatedEl.textContent = 'Last updated: N/A';


    function updateUI(data) {
        // CPU Data
        if (data.cpu) {
            const cpu = data.cpu;
            updateText(cpuTotalUsageEl, cpu.total_cpu_usage_percent?.toFixed(1), '%');
            updateProgressBar(cpuTotalUsageBarEl, cpu.total_cpu_usage_percent);
            updateText(cpuLogicalCoresEl, cpu.logical_cores);
            updateText(cpuPhysicalCoresEl, cpu.physical_cores);
            updateText(cpuFrequencyEl, cpu.frequency?.current, ' MHz');
            updateText(cpuLoad1mEl, cpu.load_average ? cpu.load_average["1_min"]?.toFixed(2) : 'N/A');
            updateText(cpuLoad5mEl, cpu.load_average ? cpu.load_average["5_min"]?.toFixed(2) : 'N/A');
            updateText(cpuLoad15mEl, cpu.load_average ? cpu.load_average["15_min"]?.toFixed(2) : 'N/A');
            updateText(cpuCtxSwitchesEl, cpu.stats?.ctx_switches?.toLocaleString());
            updateText(cpuInterruptsEl, cpu.stats?.interrupts?.toLocaleString());


            if (cpu.usage_percent_per_cpu && cpuPerCoreUsageContainer) {
                cpuPerCoreUsageContainer.innerHTML = ''; // Clear previous
                cpu.usage_percent_per_cpu.forEach((coreUsage, index) => {
                    const coreDiv = document.createElement('div');
                    coreDiv.classList.add('core-usage-item');
                    coreDiv.innerHTML = `
                        <span class="core-label">Core ${index + 1}</span>
                        <div class="progress-bar-container">
                            <div class="progress-bar" style="width: ${coreUsage.toFixed(1)}%; background-color: ${coreUsage > 85 ? '#e74c3c' : coreUsage > 60 ? '#f39c12' : '#3498db'};"></div>
                        </div>
                        <span class="metric-value small">${coreUsage.toFixed(1)}%</span>
                    `;
                    cpuPerCoreUsageContainer.appendChild(coreDiv);
                });
            }

        }

        // Memory Data
        if (data.memory) {
            const mem = data.memory;
            if(mem.virtual_memory) {
                updateText(memVirtualTotalEl, mem.virtual_memory.total_gb, ' GB');
                updateText(memVirtualUsedEl, mem.virtual_memory.used_gb, ' GB');
                updateProgressBar(memVirtualUsedBarEl, mem.virtual_memory.percent_used);
                updateText(memVirtualAvailableEl, mem.virtual_memory.available_gb, ' GB');
                updateText(memVirtualPercentEl, mem.virtual_memory.percent_used?.toFixed(1), '%');
            }
            if(mem.swap_memory) {
                updateText(memSwapTotalEl, mem.swap_memory.total_gb, ' GB');
                updateText(memSwapUsedEl, mem.swap_memory.used_gb, ' GB');
                updateProgressBar(memSwapUsedBarEl, mem.swap_memory.percent_used);
                updateText(memSwapFreeEl, mem.swap_memory.free_gb, ' GB');
                updateText(memSwapPercentEl, mem.swap_memory.percent_used?.toFixed(1), '%');
            }
                        // RAM Chart Update
            if (isLiveEnabled && mem.virtual_memory?.percent_used !== undefined) { // Only update chart if live
                const now = new Date().toLocaleTimeString();
                const usage = mem.virtual_memory.percent_used;

                ramChart.data.labels.push(now);
                ramChart.data.datasets[0].data.push(usage);

                // Keep only latest 30 entries
                if (ramChart.data.labels.length > 30) {
                    ramChart.data.labels.shift();
                    ramChart.data.datasets[0].data.shift();
                }
                ramChart.update();
            } else if (!isLiveEnabled && ramChart.data.labels.length > 0) { // Clear chart if disabled
                ramChart.data.labels = [];
                ramChart.data.datasets[0].data = [];
                ramChart.update();
            }
        }

        // Disk Data
        if (data.disk && diskPartitionsContainer) {
            const disk = data.disk;
            diskPartitionsContainer.innerHTML = ''; // Clear previous
            if(Array.isArray(disk.partitions)) {
                disk.partitions.forEach(part => {
                    const partCard = document.createElement('div');
                    partCard.classList.add('metric-card');
                    let usageBarHtml = '';
                    if (part.percent_used !== undefined && !part.error) {
                         usageBarHtml = `
                            <div class="progress-bar-container">
                                <div class="progress-bar" style="width: ${part.percent_used}%;"></div>
                            </div>
                            <p class="metric-value small">${part.percent_used}% Used</p>`;
                    } else if (part.error) {
                         usageBarHtml = `<p class="metric-value small error-text">${part.error}</p>`;
                    }

                    partCard.innerHTML = `
                        <p class="metric-title">${part.mountpoint} (${part.device})</p>
                        <p class="metric-value">${part.total_gb !== undefined ? part.total_gb + ' GB Total' : 'N/A'}</p>
                        <p class="metric-value small">${part.fstype ? 'Type: '+ part.fstype : ''}</p>
                        ${usageBarHtml}
                    `;
                    diskPartitionsContainer.appendChild(partCard);
                });
            } else {
                 diskPartitionsContainer.innerHTML = `<div class="metric-card"><p>${typeof disk.partitions === 'string' ? disk.partitions : 'Disk partition data unavailable.'}</p></div>`;
            }

            if(disk.io_counters) {
                updateText(diskReadCountEl, disk.io_counters.read_count?.toLocaleString());
                updateText(diskWriteCountEl, disk.io_counters.write_count?.toLocaleString());
                updateText(diskReadBytesEl, disk.io_counters.read_bytes_gb, ' GB');
                updateText(diskWriteBytesEl, disk.io_counters.write_bytes_gb, ' GB');
            }
        }

        // Network Data
        if (data.network && networkInterfacesContainer) {
            const net = data.network;
             if(net.io_counters) {
                updateText(netBytesSentEl, net.io_counters.bytes_sent_gb, ' GB');
                updateText(netBytesRecvEl, net.io_counters.bytes_recv_gb, ' GB');
                updateText(netPacketsSentEl, net.io_counters.packets_sent?.toLocaleString());
                updateText(netPacketsRecvEl, net.io_counters.packets_recv?.toLocaleString());
            }

            networkInterfacesContainer.innerHTML = '<h3>Network Interfaces:</h3>'; // Clear previous, keep heading
            if (net.interfaces && typeof net.interfaces === 'object') {
                Object.entries(net.interfaces).forEach(([ifName, ifDetails]) => {
                    const ifCard = document.createElement('div');
                    ifCard.classList.add('metric-card', 'full-width');
                    let addressesHtml = 'N/A';
                    if (ifDetails.addresses && ifDetails.addresses.length > 0) {
                        addressesHtml = ifDetails.addresses.map(addr =>
                            `<p><strong>${addr.family.replace('AddressFamily.', '')}:</strong> ${addr.address} ${addr.netmask ? '(Mask: ' + addr.netmask + ')' : ''}</p>`
                        ).join('');
                    }
                    ifCard.innerHTML = `
                        <p class="metric-title">${ifName}</p>
                        <div class="interface-details">
                            ${addressesHtml}
                            <p><strong>Status:</strong> ${ifDetails.stats?.is_up ? 'Up' : 'Down'}</p>
                            <p><strong>Speed:</strong> ${ifDetails.stats?.speed_mbps || 'N/A'} Mbps</p>
                            <p><strong>MTU:</strong> ${ifDetails.stats?.mtu_bytes || 'N/A'}</p>
                        </div>
                    `;
                    networkInterfacesContainer.appendChild(ifCard);
                });
            } else {
                 networkInterfacesContainer.innerHTML += `<div class="metric-card full-width"><p>Network interface data unavailable.</p></div>`;
            }
        }

        // Sensors Data
        if (data.sensors && sensorsContainer) {
            sensorsContainer.innerHTML = ''; // Clear previous
            const sensors = data.sensors;
            let foundSensorData = false;

            // Temperatures
            if (sensors.temperatures && typeof sensors.temperatures === 'object' && Object.keys(sensors.temperatures).length > 0) {
                 foundSensorData = true;
                Object.entries(sensors.temperatures).forEach(([name, entries]) => {
                    entries.forEach(entry => {
                        const tempCard = document.createElement('div');
                        tempCard.classList.add('metric-card');
                        tempCard.innerHTML = `
                            <p class="metric-title">Temp: ${name} ${entry.label || ''}</p>
                            <p class="metric-value">${entry.current_celsius}°C</p>
                            <p class="metric-value small">High: ${entry.high_celsius || 'N/A'}°C / Crit: ${entry.critical_celsius || 'N/A'}°C</p>
                        `;
                        sensorsContainer.appendChild(tempCard);
                    });
                });
            }

            // Fans
             if (sensors.fans && typeof sensors.fans === 'object' && Object.keys(sensors.fans).length > 0) {
                foundSensorData = true;
                Object.entries(sensors.fans).forEach(([name, entries]) => {
                    entries.forEach(entry => {
                        const fanCard = document.createElement('div');
                        fanCard.classList.add('metric-card');
                        fanCard.innerHTML = `
                            <p class="metric-title">Fan: ${name} ${entry.label || ''}</p>
                            <p class="metric-value">${entry.current_rpm} RPM</p>
                        `;
                        sensorsContainer.appendChild(fanCard);
                    });
                });
            }

            // Battery
            if (sensors.battery && typeof sensors.battery.percent !== 'undefined') {
                foundSensorData = true;
                const batCard = document.createElement('div');
                batCard.classList.add('metric-card');
                let secsLeftText = 'N/A';
                if (sensors.battery.secsleft === -1) secsLeftText = 'Calculating...';
                else if (sensors.battery.secsleft === -2 || sensors.battery.power_plugged) secsLeftText = 'Unlimited (Plugged In)';
                else if (sensors.battery.secsleft > 0) {
                    const hours = Math.floor(sensors.battery.secsleft / 3600);
                    const minutes = Math.floor((sensors.battery.secsleft % 3600) / 60);
                    secsLeftText = `${hours}h ${minutes}m left`;
                }

                batCard.innerHTML = `
                    <p class="metric-title">Battery <i class="fas ${sensors.battery.power_plugged ? 'fa-bolt' : 'fa-battery-full'}"></i></p>
                    <p class="metric-value">${sensors.battery.percent}%</p>
                    <div class="progress-bar-container">
                        <div class="progress-bar" style="width: ${sensors.battery.percent}%;"></div>
                    </div>
                    <p class="metric-value small">${secsLeftText}</p>
                `;
                sensorsContainer.appendChild(batCard);
            }

            if (!foundSensorData) {
                 sensorsContainer.innerHTML = `<div class="metric-card"><p>Sensor data not available or not supported on this system.</p></div>`;
            }
        }


        // System OS Data
        if (data.system_os) {
            const sys = data.system_os;
            updateText(osHostnameEl, sys.platform_details?.node_hostname);
            updateText(osPlatformEl, sys.platform_details?.system);
            updateText(osReleaseEl, sys.platform_details?.release);
            updateText(osVersionEl, sys.platform_details?.version);
            updateText(osArchEl, sys.platform_details?.machine_arch);
            updateText(osPythonVersionEl, sys.platform_details?.python_version);
            updateText(osBootTimeEl, sys.boot_time_readable);

            if (sys.users && osUsersListEl) {
                osUsersListEl.innerHTML = '';
                if(sys.users.length > 0) {
                    sys.users.forEach(user => {
                        const li = document.createElement('li');
                        li.textContent = `${user.name} (Terminal: ${user.terminal || 'N/A'}, Host: ${user.host || 'N/A'})`;
                        osUsersListEl.appendChild(li);
                    });
                } else {
                    osUsersListEl.innerHTML = '<li>No users currently logged in.</li>';
                }
            }
        }

        // Process Summary Data
        if (data.processes_summary) {
            const proc = data.processes_summary;
            updateText(procTotalEl, proc.total_processes?.toLocaleString());

            function populateProcessList(listElement, processes) {
                if (listElement) {
                    listElement.innerHTML = '';
                    if (processes && processes.length > 0) {
                        processes.forEach(p => {
                            const li = document.createElement('li');
                            li.innerHTML = `<span class="proc-name">${p.name} (PID: ${p.pid})</span> <span class="proc-value">${p.cpu_percent !== undefined ? p.cpu_percent.toFixed(1) + '% CPU' : ''} ${p.memory_percent !== undefined ? p.memory_percent.toFixed(1) + '% Mem' : ''}</span>`;
                            listElement.appendChild(li);
                        });
                    } else {
                        listElement.innerHTML = '<li>No process data available.</li>';
                    }
                }
            }
            populateProcessList(procTopCpuListEl, proc.top_processes_by_cpu);
            populateProcessList(procTopMemListEl, proc.top_processes_by_memory);
        }
    }

    // Initial call to connect is removed, live updates start as disabled.
    // connectEventSource(); // No longer called automatically
}
window.initMod7 = initMod7;
