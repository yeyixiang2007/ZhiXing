$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path "$PSScriptRoot\..").Path
Push-Location $projectRoot
try {
    Write-Host "Preparing demo build..."
    & "$PSScriptRoot\build.ps1"

    Write-Host "Launching GUI demo..."
    & "$PSScriptRoot\run-gui.ps1"
}
finally {
    Pop-Location
}
