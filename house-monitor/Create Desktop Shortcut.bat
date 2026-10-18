@echo off
rem Puts a "House Monitor" shortcut, with the monitor icon, on the desktop. Run it once after extracting.
set "DIR=%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -Command "$d=[Environment]::GetFolderPath('Desktop'); $s=(New-Object -ComObject WScript.Shell).CreateShortcut((Join-Path $d 'House Monitor.lnk')); $s.TargetPath=$env:DIR+'Start House Monitor.bat'; $s.WorkingDirectory=$env:DIR; $s.IconLocation=$env:DIR+'House-Monitor.ico'; $s.WindowStyle=7; $s.Save()"
echo.
echo The "House Monitor" shortcut is on your desktop.
pause
