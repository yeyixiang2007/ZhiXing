$ErrorActionPreference = "Stop"

$projectRoot = (Resolve-Path "$PSScriptRoot\..").Path
$srcDir = Join-Path $projectRoot "src\main\java"
$outDir = Join-Path $projectRoot "out"
$depsDir = Join-Path $projectRoot ".deps"

if (Test-Path $outDir) {
    Remove-Item -Recurse -Force $outDir
}

New-Item -ItemType Directory -Path $outDir | Out-Null

$javaFiles = Get-ChildItem -Path $srcDir -Recurse -Filter *.java | ForEach-Object { $_.FullName }

if ($javaFiles.Count -eq 0) {
    throw "No Java files found in $srcDir"
}

$classpathFile = Join-Path $outDir "classpath.txt"
$dependencyClasspath = ""
if (Get-Command mvn -ErrorAction SilentlyContinue) {
    Push-Location $projectRoot
    try {
        mvn -q dependency:build-classpath "-Dmdep.outputFile=$classpathFile"
        if (Test-Path $classpathFile) {
            $dependencyClasspath = (Get-Content $classpathFile -Raw).Trim()
        }
    } finally {
        Pop-Location
    }
}

if ([string]::IsNullOrWhiteSpace($dependencyClasspath)) {
    if (-not (Test-Path $depsDir)) {
        New-Item -ItemType Directory -Path $depsDir | Out-Null
    }
    $dependencyJars = @(
        @{
            Path = Join-Path $depsDir "slf4j-api-1.7.36.jar"
            Url = "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar"
        },
        @{
            Path = Join-Path $depsDir "logback-classic-1.2.11.jar"
            Url = "https://repo1.maven.org/maven2/ch/qos/logback/logback-classic/1.2.11/logback-classic-1.2.11.jar"
        },
        @{
            Path = Join-Path $depsDir "logback-core-1.2.11.jar"
            Url = "https://repo1.maven.org/maven2/ch/qos/logback/logback-core/1.2.11/logback-core-1.2.11.jar"
        }
    )
    foreach ($dependency in $dependencyJars) {
        if (-not (Test-Path $dependency.Path)) {
            Invoke-WebRequest -UseBasicParsing -Uri $dependency.Url -OutFile $dependency.Path
        }
    }
    $dependencyClasspath = ($dependencyJars | ForEach-Object { $_.Path }) -join [IO.Path]::PathSeparator
}

if ([string]::IsNullOrWhiteSpace($dependencyClasspath)) {
    # Fallback: include jars from local maven-lib
    $repoLib = "$env:USERPROFILE\.openclaw\tools\maven-lib"
    $localJars = @()
    if (Test-Path $repoLib) {
        $localJars = Get-ChildItem -Path $repoLib -Filter *.jar | ForEach-Object { $_.FullName }
    }
    $cp = if ($localJars.Count -gt 0) { $localJars -join ';' } else { '' }
    if ($cp) {
        javac -encoding UTF-8 -cp "$cp" -d $outDir $javaFiles
    } else {
        javac -encoding UTF-8 -d $outDir $javaFiles
    }
} else {
    javac -encoding UTF-8 -cp $dependencyClasspath -d $outDir $javaFiles
}
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}
Write-Host "Build success. Classes generated in: $outDir"
