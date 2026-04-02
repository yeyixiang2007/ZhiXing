$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path "$PSScriptRoot\..").Path
$mainClass = "com.zhixing.navigation.app.AppLauncher"

Push-Location $projectRoot
try {
    if (-not (Test-Path "out")) {
        Write-Host "Build output not found. Running build first..."
        & "$PSScriptRoot\build.ps1"
    }

    & java "-cp" "out" "$mainClass"
}
finally {
    Pop-Location
}
