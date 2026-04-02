$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path "$PSScriptRoot\..").Path
Push-Location $projectRoot
try {
    Write-Host "Running regression suite..."
    & "$PSScriptRoot\qa.ps1"
    Write-Host "Regression suite finished."
}
finally {
    Pop-Location
}
