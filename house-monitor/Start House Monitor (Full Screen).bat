@echo off
rem Opens the House Monitor full screen on the TOP screen (the monitor above the laptop). Alt+F4 closes it.
powershell -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "%~dp0launch.ps1" -Full
