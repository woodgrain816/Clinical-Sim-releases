@echo off
rem Puts a "House Monitor" shortcut, with the monitor icon, on the desktop. Run it once after extracting.
set "DIR=%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$d=[Environment]::GetFolderPath('Desktop'); $s=(New-Object -ComObject WScript.Shell).CreateShortcut((Join-Path $d 'House Monitor.lnk')); $s.TargetPath=$env:DIR+'House Monitor.exe'; $s.WorkingDirectory=$env:DIR; $s.IconLocation=$env:DIR+'House Monitor.exe,0'; $s.Save()"
echo.
echo The "House Monitor" shortcut is on your desktop.
pause
