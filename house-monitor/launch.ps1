# Opens House-Monitor.html in a Microsoft Edge (or Chrome) app window on the TOP screen: the display whose top edge
# is highest, i.e. the monitor that sits above the laptop. If the screens are side by side, the one that is not the
# laptop's own (main) screen. With one screen it simply opens there.
#   -Full   full screen (Alt+F4 closes it); without it the window opens maximised
param([switch]$Full)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms

$html = Join-Path $PSScriptRoot 'House-Monitor.html'
$url = 'file:///' + ($html -replace '\\', '/')
$prof = Join-Path $env:LOCALAPPDATA 'B450Simulator'

$top = [System.Windows.Forms.Screen]::AllScreens |
    Sort-Object @{ Expression = { $_.Bounds.Y } }, @{ Expression = { $_.Primary } } |
    Select-Object -First 1
$b = $top.Bounds
# a small window placed in the middle of that screen; it then goes full screen / maximised on the screen it is on
$x = $b.X + [int]($b.Width / 2) - 400
$y = $b.Y + [int]($b.Height / 2) - 300

# the browser remembers where the window was last time and would put it back there (e.g. on the laptop screen):
# forget that, so the window always opens on the top screen
$pref = Join-Path $prof 'Default\Preferences'
if (Test-Path $pref) {
    try {
        $p = Get-Content $pref -Raw -Encoding UTF8 | ConvertFrom-Json
        if ($p.browser) {
            foreach ($k in 'app_window_placement', 'window_placement') { $p.browser.PSObject.Properties.Remove($k) }
            [IO.File]::WriteAllText($pref, ($p | ConvertTo-Json -Depth 100 -Compress), (New-Object Text.UTF8Encoding($false)))
        }
    } catch { }   # a browser still running holds the file; it just opens where it was
}

$exe = @(
    "${env:ProgramFiles(x86)}\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Microsoft\Edge\Application\msedge.exe",
    "$env:LOCALAPPDATA\Microsoft\Edge\Application\msedge.exe",
    "$env:ProgramFiles\Google\Chrome\Application\chrome.exe",
    "${env:ProgramFiles(x86)}\Google\Chrome\Application\chrome.exe",
    "$env:LOCALAPPDATA\Google\Chrome\Application\chrome.exe"
) | Where-Object { Test-Path $_ } | Select-Object -First 1

if (-not $exe) { Start-Process $html; exit }   # no Edge/Chrome: the default browser, wherever it opens

$argList = @("--app=`"$url`"", "--user-data-dir=`"$prof`"", '--no-first-run', '--autoplay-policy=no-user-gesture-required',
          "--window-position=$x,$y", '--window-size=800,600', $(if ($Full) { '--start-fullscreen' } else { '--start-maximized' }))
Start-Process -FilePath $exe -ArgumentList $argList
