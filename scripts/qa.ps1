$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path "$PSScriptRoot\..").Path
Push-Location $projectRoot
try {
    Write-Host "Building project..."
    & "$PSScriptRoot\build.ps1"

    Write-Host "Running quality checks..."
    & java "-cp" "out" "com.zhixing.navigation.app.QualityCheckRunner"

    Write-Host "Quality checks completed."
}
finally {
    Pop-Location
}

