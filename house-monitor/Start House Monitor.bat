@echo off
rem Opens the House Monitor in its own window, maximised on the TOP screen (the monitor above the laptop).
powershell -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "%~dp0launch.ps1"
