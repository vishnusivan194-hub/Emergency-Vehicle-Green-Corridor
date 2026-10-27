@echo off
cd /d "%~dp0"
title Emergency Vehicle Green Corridor
echo ====================================================
echo Emergency Vehicle Green Corridor - Simulation Only
echo Compiling project files from: %CD%
echo ====================================================
echo.
javac *.java
if errorlevel 1 (
  echo.
  echo Compilation failed. Check the errors above.
  pause
  exit /b 1
)
echo.
echo Starting dashboard...
java DashboardMain
if errorlevel 1 pause
