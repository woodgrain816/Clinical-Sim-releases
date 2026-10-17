@echo off
rem Opens the B450 simulator in its own app window (Microsoft Edge is built into Windows 10/11).
setlocal
set "HTML=%~dp0B450-Simulator.html"
set "URL=file:///%HTML:\=/%"
set "PROFILE=%LOCALAPPDATA%\B450Simulator"
set "OPTS=--user-data-dir="%PROFILE%" --no-first-run --autoplay-policy=no-user-gesture-required --start-fullscreen"
for %%B in ("%ProgramFiles(x86)%\Microsoft\Edge\Application\msedge.exe" "%ProgramFiles%\Microsoft\Edge\Application\msedge.exe" "%LOCALAPPDATA%\Microsoft\Edge\Application\msedge.exe" "%ProgramFiles%\Google\Chrome\Application\chrome.exe" "%ProgramFiles(x86)%\Google\Chrome\Application\chrome.exe" "%LOCALAPPDATA%\Google\Chrome\Application\chrome.exe") do (
  if exist %%B (
    start "" %%B --app="%URL%" %OPTS%
    exit /b
  )
)
rem No Edge/Chrome found: open in the default browser instead.
start "" "%HTML%"
