@echo off
title Push HuTube to GitHub
echo ===================================================
echo   🚀 Pushing HuTube to GitHub...
echo ===================================================
echo.

cd /d "%~dp0"
git push -u origin main

echo.
if %ERRORLEVEL% equ 0 (
    echo ===================================================
    echo   ✅ Push successful!
    echo   GitHub Actions will now build your Android APK.
    echo ===================================================
) else (
    echo ===================================================
    echo   ⚠️ Push failed.
    echo   Make sure you have created the empty repository:
    echo   https://github.com/new (Name: HuTube)
    echo ===================================================
)

echo.
pause
