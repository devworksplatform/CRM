@echo off
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-studio.ps1"
if errorlevel 1 pause
