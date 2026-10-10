param(
    [string]$KeystorePass = $(
        $localProp = Join-Path (Resolve-Path (Join-Path $PSScriptRoot "..")) "local.properties"
        if (Test-Path $localProp) {
            $match = Select-String -Path $localProp -Pattern "^keystore\.storePassword=(.*)$"
            if ($match) { $match.Matches[0].Groups[1].Value.Trim() } else { $env:KEYSTORE_PASSWORD }
        } else {
            $env:KEYSTORE_PASSWORD
        }
    ),
    [string]$KeyAlias = "adaptivehz",
    [string]$KeystorePath = "adaptivehz-release.keystore"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $ProjectRoot

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "  AdaptiveHz - Release APK Build Script  " -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan

# 1. Environment Discovery
$JdkDir = if ($env:JAVA_HOME -and (Test-Path $env:JAVA_HOME)) {
    $env:JAVA_HOME
} elseif (Test-Path "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot") {
    "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
} elseif (Test-Path "C:\Program Files\Java\jdk-17") {
    "C:\Program Files\Java\jdk-17"
} else {
    $javaCmd = (Get-Command java -ErrorAction SilentlyContinue).Source
    if ($javaCmd) { Split-Path (Split-Path $javaCmd) } else { "" }
}
if (-not $JdkDir -or -not (Test-Path $JdkDir)) {
    Write-Error "JDK 17+ not found! Please set JAVA_HOME."
}
$env:JAVA_HOME = $JdkDir

$SdkDir = if ($env:ANDROID_HOME -and (Test-Path $env:ANDROID_HOME)) {
    $env:ANDROID_HOME
} elseif ($env:ANDROID_SDK_ROOT -and (Test-Path $env:ANDROID_SDK_ROOT)) {
    $env:ANDROID_SDK_ROOT
} elseif (Test-Path "$env:LOCALAPPDATA\Android\Sdk") {
    "$env:LOCALAPPDATA\Android\Sdk"
} elseif (Test-Path "$env:USERPROFILE\.bubblewrap\android_sdk") {
    "$env:USERPROFILE\.bubblewrap\android_sdk"
} else {
    Write-Error "Android SDK not found! Please set ANDROID_HOME."
}
$env:ANDROID_HOME = $SdkDir

$BuildToolsDirs = Get-ChildItem (Join-Path $SdkDir "build-tools") | Sort-Object Name -Descending
if ($BuildToolsDirs.Count -eq 0) {
    Write-Error "build-tools not found in Android SDK: $SdkDir"
}
$BuildToolsDir = $BuildToolsDirs[0].FullName
$ZipAlign = Join-Path $BuildToolsDir "zipalign.exe"
$ApkSigner = Join-Path $BuildToolsDir "apksigner.bat"

Write-Host "[1/4] Environment setup:" -ForegroundColor Green
Write-Host "   JDK (64-bit): $JdkDir"
Write-Host "   Android SDK:  $SdkDir"
Write-Host "   Build Tools:  $BuildToolsDir"

# 2. Build Release APK via Gradle
Write-Host "`n[2/4] Compiling Release APK via Gradle..." -ForegroundColor Green
.\gradlew.bat assembleRelease
if ($LASTEXITCODE -ne 0) {
    Write-Error "Gradle build failed!"
}

# 3. Verify Generated APK
Write-Host "`n[3/4] Verifying APK signature..." -ForegroundColor Green
$GradleReleaseApk = Join-Path $ProjectRoot "app\build\outputs\apk\release\app-release.apk"
if (-not (Test-Path $GradleReleaseApk)) {
    Write-Error "Release APK not found at $GradleReleaseApk"
}

$FinalApkRoot = Join-Path $ProjectRoot "AdaptiveHz-release.apk"
$FinalApkStore = Join-Path $ProjectRoot "store_assets\AdaptiveHz-release.apk"

Copy-Item $GradleReleaseApk $FinalApkRoot -Force
if (-not (Test-Path (Join-Path $ProjectRoot "store_assets"))) {
    New-Item -ItemType Directory -Path (Join-Path $ProjectRoot "store_assets") -Force | Out-Null
}
Copy-Item $GradleReleaseApk $FinalApkStore -Force

# Verify Signature
Write-Host "`n[4/4] apksigner verification:" -ForegroundColor Green
& $ApkSigner verify --verbose --print-certs "$FinalApkRoot"
if ($LASTEXITCODE -eq 0) {
    $SizeMb = [math]::Round(((Get-Item $FinalApkRoot).Length / 1MB), 2)
    Write-Host "`n=================================================" -ForegroundColor Green
    Write-Host "  SUCCESS! Release APK ready for RuStore upload!" -ForegroundColor Green
    Write-Host "  Root APK:        $FinalApkRoot" -ForegroundColor Yellow
    Write-Host "  Store Assets:    $FinalApkStore" -ForegroundColor Yellow
    Write-Host "  Size:            $SizeMb MB" -ForegroundColor Yellow
    Write-Host "=================================================" -ForegroundColor Green
} else {
    Write-Error "APK signature verification failed!"
}
