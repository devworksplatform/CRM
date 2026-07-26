[CmdletBinding()]
param(
    [string]$DatabasePath = "D:\Jay\Works\CRM\CRMServers\backups_sqliteDBs_2026-07-26--12-49-45.db",
    [string]$FirebaseCredentials = "C:\Users\acer\Downloads\pets-fort-firebase-adminsdk-fbsvc-81040ca6c3.json",
    [string]$StudioJar = "D:\Jay\Works\ServerlessCommunication\jrpc-samples\dist\jrpc-studio.jar",
    [int]$Port = 8080
)

$ErrorActionPreference = "Stop"

function Resolve-RequiredFile {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Description
    )

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "$Description was not found: $Path"
    }

    return (Resolve-Path -LiteralPath $Path).Path
}

try {
    $resolvedDatabase = Resolve-RequiredFile -Path $DatabasePath -Description "PetsFort SQLite database"
    $resolvedCredentials = Resolve-RequiredFile -Path $FirebaseCredentials -Description "Firebase service-account JSON"
    $resolvedStudioJar = Resolve-RequiredFile -Path $StudioJar -Description "JRPC Studio JAR"

    $java = Get-Command java -ErrorAction Stop

    $env:PETS_FORT_DB_PATH = $resolvedDatabase
    $env:PETS_FORT_FIREBASE_CREDENTIALS = $resolvedCredentials
    $env:JRPC_STUDIO_PORT = [string]$Port

    $studioDirectory = Split-Path -Parent $resolvedStudioJar
    Set-Location -LiteralPath $studioDirectory

    Write-Host ""
    Write-Host "Starting JRPC Studio..." -ForegroundColor Cyan
    Write-Host "Studio URL: http://127.0.0.1:$Port"
    Write-Host "Database:   $resolvedDatabase"
    Write-Host "Credentials: $resolvedCredentials"
    Write-Host ""
    Write-Host "Keep this window open. Press Ctrl+C to stop Studio and its workers." -ForegroundColor Yellow
    Write-Host ""

    & $java.Source -jar $resolvedStudioJar
    if ($LASTEXITCODE -ne 0) {
        throw "JRPC Studio exited with code $LASTEXITCODE."
    }
}
catch {
    Write-Error $_
    exit 1
}
