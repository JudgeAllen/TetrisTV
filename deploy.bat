@echo off
chcp 65001 >nul 2>&1
title TetrisTV Deploy
echo ===================================
echo   TetrisTV APK Deploy Script
echo ===================================
echo.

set ADB=C:\Users\USSRer\Android\Sdk\platform-tools\adb.exe
set APK=F:\Git\TetrisTV\app\build\outputs\apk\debug\app-debug.apk
set TV_IP=192.168.1.205

echo [1/4] Connecting to TV at %TV_IP%:5555 ...
%ADB% connect %TV_IP%:5555
timeout /t 2 /nobreak >nul

echo.
echo [2/4] Checking connection ...
%ADB% devices

echo.
echo [3/4] Installing APK (bypass TCL install control) ...
%ADB% -s %TV_IP%:5555 shell pm disable-user --user 0 com.android.packageinstaller
%ADB% -s %TV_IP%:5555 install -r "%APK%"
%ADB% -s %TV_IP%:5555 shell pm enable com.android.packageinstaller

echo.
echo [4/4] Launching TetrisTV ...
%ADB% -s %TV_IP%:5555 shell am start -n com.tetristv/.MainActivity

echo.
echo ===================================
echo   Done! Check your TV.
echo ===================================
pause
