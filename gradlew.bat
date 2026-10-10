@rem Gradle execution helper for AdaptiveHz
@echo off
setlocal

where gradle >nul 2>nul
if %ERRORLEVEL% equ 0 (
    gradle %*
    goto :done
)

if exist "%USERPROFILE%\.gradle\wrapper\dists" (
    for /f "delims=" %%G in ('dir /b /s "%USERPROFILE%\.gradle\wrapper\dists\*gradle.bat" 2^>nul') do (
        if exist "%%G" (
            call "%%G" %*
            goto :done
        )
    )
)

echo [ERROR] Gradle executable not found in PATH or in %%USERPROFILE%%\.gradle.
echo Please install Gradle or configure GRADLE_HOME.
exit /b 1

:done
exit /b %ERRORLEVEL%
