#!/bin/sh
# Builds "House Monitor.exe" (Windows, x64) with the current House-Monitor.html and the icon inside.
# Needs Go and rsrc (go install github.com/akavel/rsrc@latest).
set -e
cd "$(dirname "$0")"
cp ../House-Monitor.html House-Monitor.html
rsrc -ico ../House-Monitor.ico -o rsrc_windows_amd64.syso
GOOS=windows GOARCH=amd64 CGO_ENABLED=0 go build -trimpath -ldflags "-s -w -H windowsgui" -o "../House Monitor.exe" .
rm -f House-Monitor.html rsrc_windows_amd64.syso
