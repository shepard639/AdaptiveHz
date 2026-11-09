param(
    [string]$TagName = "v1.3.2",
    [string]$ReleaseName = "AdaptiveHz v1.3.2 - YouTube 60Hz Fix, Dual-Path Detection & IPC Power Optimization",
    [string]$ApkPath = "AdaptiveHz-release.apk"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $ProjectRoot

Write-Host "=============================================" -ForegroundColor Cyan
Write-Host "   Publishing GitHub Release $TagName        " -ForegroundColor Cyan
Write-Host "=============================================" -ForegroundColor Cyan

# 1. Ensure signed release APK exists
$FullApkPath = Join-Path $ProjectRoot $ApkPath
if (-not (Test-Path $FullApkPath)) {
    Write-Host "[1/4] Building signed release APK..." -ForegroundColor Yellow
    & "$PSScriptRoot\build-release.ps1"
} else {
    Write-Host "[1/4] Found signed release APK: $FullApkPath" -ForegroundColor Green
}

# 2. Extract GitHub Token from Git Credential Manager
Write-Host "[2/4] Retrieving GitHub credentials from Git Credential Manager..." -ForegroundColor Yellow
$credInput = "protocol=https`nhost=github.com`n`n"
$credOutput = $credInput | git credential fill
$tokenLine = $credOutput | Where-Object { $_ -like "password=*" }
if (-not $tokenLine) {
    Write-Error "Could not find password/token from git credential fill for github.com"
}
$githubToken = $tokenLine.Substring(9).Trim()

$headers = @{
    "Authorization" = "Bearer $githubToken"
    "Accept" = "application/vnd.github+json"
    "User-Agent" = "AdaptiveHz-Release-Publisher"
    "X-GitHub-Api-Version" = "2022-11-28"
}

# Verify user
$userResponse = Invoke-RestMethod -Uri "https://api.github.com/user" -Headers $headers -Method Get
Write-Host "Authenticated as GitHub user: $($userResponse.login)" -ForegroundColor Green

# 3. Create or Update Release
Write-Host "[3/4] Creating GitHub Release $TagName..." -ForegroundColor Yellow

$releaseNotes = @"
## 🌟 AdaptiveHz v1.3.2 — YouTube 60Hz Fix, Dual-Path Detection & IPC Power Optimization

### 🚀 Ключевые изменения и улучшения:
- **Устранение залипания на 120 Гц в YouTube и видеоприложениях (Fix 60Hz App Profiles)**:
  - Ликвидирован баг перехвата активного пакета системными оверлеями (клавиатура Samsung Honeyboard, Gboard, регулятор громкости и шторка SystemUI, Google Play Services).
  - Внедрен список исключений \`STATIC_IGNORED_PACKAGES\` и динамическое распознавание активных клавиатур (\`InputMethodManager\`).
- **Двухконтурная детекция приложений (Dual-Path Detection)**:
  - При любых касаниях и скролле (\`TYPE_TOUCH_INTERACTION_START\`, \`TYPE_VIEW_CLICKED\`, \`TYPE_VIEW_SCROLLED\`) проверяется принадлежность события целевому пакету.
  - При переключении через список недавних или жестовую панель навигации 60 Гц фиксируются мгновенно до любого буста.
- **Оптимизация энергопотребления и ликвидация IPC-шторма**:
  - Конфигурация службы доступности переведена с \`typeAllMask\` на строгий фильтр необходимых событий.
  - Устранены сотни паразитных вызовов \`TYPE_WINDOW_CONTENT_CHANGED\` при воспроизведении видео, позволяя процессору уходить в глубокий сон (C-states).
  - Удалены тяжелые флаги \`flagIncludeNotImportantViews\` и \`flagReportViewIds\`.
- **Дросселирование сброса таймера при скролле (350 мс)**:
  - Исключена циклическая перепостановка Handler Runnable при частоте скролла 120 раз/сек.

---
### 📦 Установка и хеши пакета:
- **Файл**: \`AdaptiveHz-release.apk\`
- **Подпись**: HealthCraft 4096-bit RSA (V3 Scheme)
- **Совместимость**: Android 10+ (Samsung Galaxy One UI 2.0 – 6.1+, Non-LTPO и LTPO)
"@

$bodyObj = @{
    tag_name = $TagName
    target_commitish = "main"
    name = $ReleaseName
    body = $releaseNotes
    draft = $false
    prerelease = $false
} | ConvertTo-Json

$repo = "shepard639/AdaptiveHz"
$releaseUri = "https://api.github.com/repos/$repo/releases"

# Check if release already exists
$existingRelease = $null
try {
    $existingRelease = Invoke-RestMethod -Uri "$releaseUri/tags/$TagName" -Headers $headers -Method Get -ErrorAction SilentlyContinue
} catch {}

$release = $null
if ($existingRelease) {
    Write-Host "Release for $TagName already exists (id: $($existingRelease.id)). Updating..." -ForegroundColor Yellow
    $updateUri = "$releaseUri/$($existingRelease.id)"
    $release = Invoke-RestMethod -Uri $updateUri -Headers $headers -Method Patch -Body ([System.Text.Encoding]::UTF8.GetBytes($bodyObj)) -ContentType "application/json; charset=utf-8"
} else {
    $release = Invoke-RestMethod -Uri $releaseUri -Headers $headers -Method Post -Body ([System.Text.Encoding]::UTF8.GetBytes($bodyObj)) -ContentType "application/json; charset=utf-8"
}

Write-Host "Release created/updated successfully: $($release.html_url)" -ForegroundColor Green

# 4. Upload Release Asset (AdaptiveHz-release.apk)
Write-Host "[4/4] Uploading $ApkPath to release assets..." -ForegroundColor Yellow

# Delete existing asset with same name if present
$existingAsset = $release.assets | Where-Object { $_.name -eq [System.IO.Path]::GetFileName($FullApkPath) }
if ($existingAsset) {
    Write-Host "Deleting old asset id: $($existingAsset.id)..." -ForegroundColor Yellow
    Invoke-RestMethod -Uri "https://api.github.com/repos/$repo/releases/assets/$($existingAsset.id)" -Headers $headers -Method Delete
}

$apkBytes = [System.IO.File]::ReadAllBytes($FullApkPath)
$assetUploadUri = "https://uploads.github.com/repos/$repo/releases/$($release.id)/assets?name=$([System.IO.Path]::GetFileName($FullApkPath))"

$uploadHeaders = @{
    "Authorization" = "Bearer $githubToken"
    "Content-Type" = "application/vnd.android.package-archive"
    "User-Agent" = "AdaptiveHz-Release-Publisher"
}

$uploadResponse = Invoke-RestMethod -Uri $assetUploadUri -Headers $uploadHeaders -Method Post -Body $apkBytes

Write-Host "`n============================================================" -ForegroundColor Green
Write-Host "  SUCCESS! Release published on GitHub!" -ForegroundColor Green
Write-Host "  Release URL: $($release.html_url)" -ForegroundColor Cyan
Write-Host "  Download:    $($uploadResponse.browser_download_url)" -ForegroundColor Yellow
Write-Host "  Size:        $([math]::Round(($uploadResponse.size / 1MB), 2)) MB" -ForegroundColor Yellow
Write-Host "============================================================" -ForegroundColor Green
