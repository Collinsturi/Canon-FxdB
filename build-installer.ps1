<#
.SYNOPSIS
    Builds the fxDb shaded jar and wraps it into a native Windows app image/installer with jpackage.

.DESCRIPTION
    This is a plain PowerShell script rather than a Maven plugin (jpackage-maven-plugin) on
    purpose: fxDb's dependencies (JDBC drivers, POI, Jackson, commons-*) are ordinary,
    non-modular jars, and jlink's custom-runtime-image feature only accepts explicit JPMS
    modules - it rejects automatic modules outright. Fighting that would mean either
    modularizing every dependency (not realistic - several upstream jars aren't modular) or
    using jpackage in its simpler "classpath app" mode, which is what this script does: the
    shaded jar carries the whole classpath as regular (non-modular) content, and jpackage
    builds a full JRE-based runtime image around it via its own internal jlink call using only
    JDK + JavaFX modules (which *are* proper JPMS modules), with the app's own code and the
    rest of its dependencies left on the classpath inside the app image. No module-path
    gymnastics needed.

.PARAMETER Type
    "app-image" (default, always works, no extra tooling) or "exe"/"msi" (needs the WiX
    Toolset v3 installed and on PATH - jpackage shells out to it on Windows for installer
    types). If WiX isn't found and -Type exe/msi was requested, this script stops with a
    clear message rather than letting jpackage fail with a less obvious error.

.EXAMPLE
    ./build-installer.ps1
    ./build-installer.ps1 -Type exe
#>
param(
    [ValidateSet("app-image", "exe", "msi")]
    [string]$Type = "app-image"
)

$ErrorActionPreference = "Stop"
$repoRoot = $PSScriptRoot
$mvn = Join-Path $repoRoot "mvnw.cmd"  # self-bootstraps the pinned Maven version; no local install needed

if ($Type -ne "app-image") {
    $wix = Get-Command candle.exe -ErrorAction SilentlyContinue
    if (-not $wix) {
        Write-Error "Type '$Type' needs the WiX Toolset (v3) installed and on PATH - jpackage shells out to candle.exe/light.exe for Windows installers. Install it and re-run, or use -Type app-image (no extra tooling needed)."
    }
}

Write-Host "Building shaded jar..."
Push-Location $repoRoot
try {
    & $mvn -q package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        throw "mvn package failed with exit code $LASTEXITCODE"
    }
} finally {
    Pop-Location
}

$pomXml = [xml](Get-Content (Join-Path $repoRoot "pom.xml"))
$version = $pomXml.project.version
$shadedJar = Join-Path $repoRoot "target\fxdb-$version-shaded.jar"
if (-not (Test-Path $shadedJar)) {
    throw "Expected shaded jar not found: $shadedJar"
}

$inputDir = Join-Path $repoRoot "target\jpackage-input"
$destDir = Join-Path $repoRoot "target\installer"
Remove-Item $inputDir, $destDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $inputDir | Out-Null
Copy-Item $shadedJar (Join-Path $inputDir "fxdb.jar")

Write-Host "Running jpackage (type: $Type)..."
$jpackageArgs = @(
    "--type", $Type,
    "--input", $inputDir,
    "--dest", $destDir,
    "--name", "fxDb",
    "--app-version", ($version -replace '-SNAPSHOT$', ''),
    "--vendor", "fxDb",
    "--main-jar", "fxdb.jar",
    "--main-class", "com.fxdb.ui.Launcher"
)
if ($Type -ne "app-image") {
    # --win-shortcut/--win-dir-chooser only make sense for an actual installer, not a bare app-image folder.
    $jpackageArgs += @("--win-shortcut", "--win-dir-chooser")
}
& jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) {
    throw "jpackage failed with exit code $LASTEXITCODE"
}

Write-Host "Done. Output in $destDir"
