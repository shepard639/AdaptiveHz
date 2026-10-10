@rem Gradle execution helper for AdaptiveHz
@echo off
setlocal

where gradle >nul 2>nul
if %ERRORLEVEL% equ 0 (
    gradle %*
    exit /b %ERRORLEVEL%
)

if exist "%USERPROFILE%\.gradle\wrapper\dists" (
    for /f "delims=" %%G in ('dir /b /s "%USERPROFILE%\.gradle\wrapper\dists\*gradle.bat" 2^>nul') do (
        if exist "%%G" (
            "%%G" %*
            exit /b %ERRORLEVEL%
        )
    )
)

echo [ERROR] Gradle executable not found in PATH or in %%USERPROFILE%%\.gradle.
echo Please install Gradle or configure GRADLE_HOME.
exit /b 1
