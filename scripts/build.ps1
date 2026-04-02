$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path "$PSScriptRoot\..").Path
$srcDir = Join-Path $projectRoot "src\main\java"
$outDir = Join-Path $projectRoot "out"

if (Test-Path $outDir) {
    Remove-Item -Recurse -Force $outDir
}

New-Item -ItemType Directory -Path $outDir | Out-Null

$javaFiles = Get-ChildItem -Path $srcDir -Recurse -Filter *.java | ForEach-Object { $_.FullName }

if ($javaFiles.Count -eq 0) {
    throw "No Java files found in $srcDir"
}

javac -encoding UTF-8 -d $outDir $javaFiles
Write-Host "Build success. Classes generated in: $outDir"
