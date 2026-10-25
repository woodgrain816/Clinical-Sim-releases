// House Monitor.exe: a single-file Windows launcher. It carries House-Monitor.html, writes it to
// %LOCALAPPDATA%\HouseMonitor and opens it in a Microsoft Edge (or Chrome) app window on the TOP screen (the display
// whose top edge is highest, i.e. the monitor above the laptop), maximised, or full screen with --full.
// Same behaviour as launch.ps1.
package main

import (
	_ "embed"
	"encoding/json"
	"os"
	"os/exec"
	"path/filepath"
	"sort"
	"strconv"
	"strings"
	"syscall"
	"unsafe"
)

//go:embed House-Monitor.html
var page []byte

type rect struct{ Left, Top, Right, Bottom int32 }
type monitorInfo struct {
	Size    uint32
	Monitor rect
	Work    rect
	Flags   uint32
}
type screen struct {
	r       rect
	primary bool
}

func screens() []screen {
	user32 := syscall.NewLazyDLL("user32.dll")
	enum, info := user32.NewProc("EnumDisplayMonitors"), user32.NewProc("GetMonitorInfoW")
	var out []screen
	cb := syscall.NewCallback(func(h, hdc, lprc, data uintptr) uintptr {
		mi := monitorInfo{Size: uint32(unsafe.Sizeof(monitorInfo{}))}
		if r, _, _ := info.Call(h, uintptr(unsafe.Pointer(&mi))); r != 0 {
			out = append(out, screen{mi.Monitor, mi.Flags&1 != 0})
		}
		return 1
	})
	enum.Call(0, 0, cb, 0)
	return out
}

// the browser remembers where the window was and would put it back there (e.g. on the laptop screen): forget that
func forgetPlacement(prof string) {
	f := filepath.Join(prof, "Default", "Preferences")
	b, err := os.ReadFile(f)
	if err != nil {
		return
	}
	var p map[string]any
	if json.Unmarshal(b, &p) != nil {
		return
	}
	if br, ok := p["browser"].(map[string]any); ok {
		delete(br, "app_window_placement")
		delete(br, "window_placement")
		if nb, err := json.Marshal(p); err == nil {
			os.WriteFile(f, nb, 0o644)
		}
	}
}

func main() {
	full := false
	for _, a := range os.Args[1:] {
		if strings.EqualFold(strings.TrimLeft(a, "-/"), "full") {
			full = true
		}
	}
	local := os.Getenv("LOCALAPPDATA")
	dir := filepath.Join(local, "HouseMonitor")
	os.MkdirAll(dir, 0o755)
	html := filepath.Join(dir, "House-Monitor.html")
	if err := os.WriteFile(html, page, 0o644); err != nil {
		html = filepath.Join(os.TempDir(), "House-Monitor.html")
		os.WriteFile(html, page, 0o644)
	}
	url := "file:///" + strings.ReplaceAll(html, `\`, "/")
	prof := filepath.Join(local, "B450Simulator") // same profile as the .bat launchers, so saved settings carry over

	pf, pf86 := os.Getenv("ProgramFiles"), os.Getenv("ProgramFiles(x86)")
	var browser string
	for _, c := range []string{
		filepath.Join(pf86, `Microsoft\Edge\Application\msedge.exe`), filepath.Join(pf, `Microsoft\Edge\Application\msedge.exe`),
		filepath.Join(local, `Microsoft\Edge\Application\msedge.exe`), filepath.Join(pf, `Google\Chrome\Application\chrome.exe`),
		filepath.Join(pf86, `Google\Chrome\Application\chrome.exe`), filepath.Join(local, `Google\Chrome\Application\chrome.exe`),
	} {
		if _, err := os.Stat(c); err == nil {
			browser = c
			break
		}
	}
	if browser == "" { // no Edge/Chrome: the default browser
		exec.Command("rundll32", "url.dll,FileProtocolHandler", html).Start()
		return
	}

	x, y := 100, 100
	if s := screens(); len(s) > 0 {
		sort.SliceStable(s, func(i, j int) bool {
			if s[i].r.Top != s[j].r.Top {
				return s[i].r.Top < s[j].r.Top
			}
			return !s[i].primary && s[j].primary
		})
		b := s[0].r
		x = int(b.Left) + int(b.Right-b.Left)/2 - 400
		y = int(b.Top) + int(b.Bottom-b.Top)/2 - 300
	}
	forgetPlacement(prof)
	mode := "--start-maximized"
	if full {
		mode = "--start-fullscreen"
	}
	cmd := exec.Command(browser, "--app="+url, "--user-data-dir="+prof, "--no-first-run", "--autoplay-policy=no-user-gesture-required",
		"--window-position="+strconv.Itoa(x)+","+strconv.Itoa(y), "--window-size=800,600", mode)
	cmd.Start()
}
