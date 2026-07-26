[CmdletBinding()]
param(
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"
$baseUrl = "http://127.0.0.1:$Port"

try {
    $listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue |
        Where-Object { $_.LocalAddress -in @("127.0.0.1", "0.0.0.0", "::", "::1") } |
        Select-Object -First 1

    if (-not $listener) {
        Write-Host "JRPC Studio is already stopped. Port $Port is closed." -ForegroundColor Yellow
        exit 0
    }

    $studioPid = [int]$listener.OwningProcess
    $studioProcess = Get-CimInstance Win32_Process -Filter "ProcessId = $studioPid"
    if (-not $studioProcess -or $studioProcess.CommandLine -notmatch "jrpc-studio[.]jar") {
        throw "Port $Port is owned by process $studioPid, but it is not JRPC Studio. Nothing was stopped."
    }

    Write-Host "Stopping JRPC workers cleanly..." -ForegroundColor Cyan
    try {
        $registry = Invoke-RestMethod -Uri "$baseUrl/api/registry" -TimeoutSec 10
        foreach ($server in @($registry.servers)) {
            try {
                $body = @{ server = $server.id } | ConvertTo-Json -Compress
                $result = Invoke-RestMethod -Uri "$baseUrl/api/runtime/stop" -Method Post `
                    -ContentType "application/json" -Body $body -TimeoutSec 45
                Write-Host "  $($server.name): $($result.state) - $($result.message)"
            }
            catch {
                Write-Warning "Could not cleanly stop $($server.name): $($_.Exception.Message)"
            }
        }
    }
    catch {
        Write-Warning "Could not read the Studio registry: $($_.Exception.Message)"
    }

    $allProcesses = Get-CimInstance Win32_Process
    $targets = [System.Collections.Generic.HashSet[int]]::new()
    [void]$targets.Add($studioPid)
    do {
        $added = $false
        foreach ($process in $allProcesses) {
            if ($targets.Contains([int]$process.ParentProcessId) -and
                $targets.Add([int]$process.ProcessId)) {
                $added = $true
            }
        }
    } while ($added)

    foreach ($processId in @($targets) | Sort-Object -Descending) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }

    # Clean up workers whose Studio parent exited earlier. These workers can
    # otherwise keep a deployed application.jar locked on Windows.
    Get-CimInstance Win32_Process -Filter "Name = 'java.exe'" |
        Where-Object {
            $_.CommandLine -match "com[.]jay[.]server[.]RpcServerWorker" -and
            $_.CommandLine -match "[.]jrpc-studio"
        } |
        ForEach-Object {
            Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
        }

    $deadline = (Get-Date).AddSeconds(10)
    do {
        Start-Sleep -Milliseconds 250
        $stillListening = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    } while ($stillListening -and (Get-Date) -lt $deadline)

    if ($stillListening) {
        throw "JRPC Studio was stopped, but port $Port is still listening."
    }

    Write-Host "JRPC Studio and all workers are stopped. Port $Port is closed." -ForegroundColor Green
}
catch {
    Write-Error $_
    exit 1
}
