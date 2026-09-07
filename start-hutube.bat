@echo off
title HuTube - Google Drive OTT Platform
echo ========================================================
echo   🍿 Starting HuTube - Google Drive Media Streaming...
echo ========================================================
echo.

cd /d "%~dp0"
start http://localhost:5000
node server/index.js

pause
