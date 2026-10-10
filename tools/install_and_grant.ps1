# AdaptiveHz: PowerShell installer and permission granter for Samsung S20 FE
$OutputEncoding = [System.Text.Encoding]::UTF8
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$adbPath = "adb"
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    if (Test-Path "$env:USERPROFILE\.bubblewrap\android_sdk\platform-tools\adb.exe") {
        $adbPath = "$env:USERPROFILE\.bubblewrap\android_sdk\platform-tools\adb.exe"
    } elseif (Test-Path "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe") {
        $adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    }
}

Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "          AdaptiveHz: Установка и настройка для Samsung S20 FE       " -ForegroundColor Green
Write-Host "=====================================================================" -ForegroundColor Cyan

Write-Host "`n[1/4] Проверка подключения телефона по USB..." -ForegroundColor Yellow
$devices = & $adbPath devices
Write-Host $devices

$apkPath = "$PSScriptRoot\..\app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apkPath)) {
    Write-Host "[!] Файл APK не найден: $apkPath" -ForegroundColor Red
    exit 1
}

Write-Host "`n[2/4] Установка приложения AdaptiveHz..." -ForegroundColor Yellow
& $adbPath install -r $apkPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "[!] Ошибка установки APK." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Приложение успешно установлено!" -ForegroundColor Green

Write-Host "`n[3/4] Выдача системного разрешения WRITE_SECURE_SETTINGS..." -ForegroundColor Yellow
& $adbPath shell pm grant com.healthcraft.adaptivehz android.permission.WRITE_SECURE_SETTINGS
Write-Host "[OK] Системное разрешение WRITE_SECURE_SETTINGS выдано!" -ForegroundColor Green

Write-Host "`n[4/4] Запуск AdaptiveHz..." -ForegroundColor Yellow
& $adbPath shell am start -n com.healthcraft.adaptivehz/com.antigravity.adaptivehz.ui.MainActivity

Write-Host "`n=====================================================================" -ForegroundColor Cyan
Write-Host "                    УСТАНОВКА ЗАВЕРШЕНА УСПЕШНО!                     " -ForegroundColor Green
Write-Host "=====================================================================" -ForegroundColor Cyan
Write-Host "Включите службу специальных возможностей в: Настройки -> Спец. возможности -> Установленные приложения -> AdaptiveHz`n"
