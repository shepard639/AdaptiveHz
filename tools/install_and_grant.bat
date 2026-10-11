@echo off
chcp 65001 >nul
title AdaptiveHz - Автоматическая установка на Samsung S20 FE
cls

echo =====================================================================
echo           AdaptiveHz: Установка и настройка для Samsung S20 FE
echo =====================================================================
echo.

set ADB=adb
where adb >nul 2>nul
if %ERRORLEVEL% neq 0 (
    if exist "%USERPROFILE%\.bubblewrap\android_sdk\platform-tools\adb.exe" (
        set ADB="%USERPROFILE%\.bubblewrap\android_sdk\platform-tools\adb.exe"
    ) else if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set ADB="%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
    )
)

echo [1/4] Проверка подключения телефона по USB...
%ADB% get-state >nul 2>&1
if errorlevel 1 (
    echo [!] Телефон не обнаружен или не авторизован!
    echo.
    echo Пожалуйста:
    echo  1. Подключите Samsung S20 FE к компьютеру через USB-кабель.
    echo  2. Убедитесь, что в Настройки -> Параметры разработчика включена "Отладка по USB".
    echo  3. Если на экране телефона появится запрос "Разрешить отладку по USB?",
    echo     поставьте галочку "Всегда разрешать" и нажмите "Разрешить".
    echo.
    echo Нажмите любую клавишу для повторной проверки...
    pause >nul
    goto :retry
)

:retry
%ADB% devices
echo.

echo [2/4] Установка приложения AdaptiveHz (app-debug.apk)...
set APK_PATH="%~dp0..\app\build\outputs\apk\debug\app-debug.apk"
if not exist %APK_PATH% (
    echo [!] Файл APK не найден по пути: %APK_PATH%
    echo     Запустите сборку: gradlew.bat assembleDebug
    pause
    exit /b 1
)

%ADB% install -r %APK_PATH%
if errorlevel 1 (
    echo [!] Ошибка при установке APK. Проверьте разблокировку экрана телефона.
    pause
    exit /b 1
)
echo [OK] Приложение успешно установлено!
echo.

echo [3/4] Выдача системного разрешения WRITE_SECURE_SETTINGS...
%ADB% shell pm grant com.healthcraft.adaptivehz android.permission.WRITE_SECURE_SETTINGS
if errorlevel 1 (
    echo [!] Предупреждение при выдаче прав. Проверьте права ADB.
) else (
    echo [OK] Системное разрешение WRITE_SECURE_SETTINGS успешно выдано!
)
echo.

echo [4/4] Запуск AdaptiveHz на телефоне...
%ADB% shell am start -n com.healthcraft.adaptivehz/.ui.MainActivity >nul 2>&1
echo.
echo =====================================================================
echo                     УСТАНОВКА ЗАВЕРШЕНА УСПЕШНО!
echo =====================================================================
echo.
echo Последний шаг на телефоне:
echo   1. Откройте приложение AdaptiveHz (оно уже открылось на экране).
echo   2. Нажмите кнопку "Включить" напротив "Служба специальных возможностей".
echo   3. В открывшемся меню найдите "Установленные приложения" -> "AdaptiveHz"
echo      и переведите переключатель в положение ВКЛ.
echo.
echo Теперь ваш S20 FE работает на адаптивных 120 Гц при касаниях и
echo автоматически сбрасывает на 60 Гц при чтении, таймерах и в YouTube!
echo =====================================================================
pause
