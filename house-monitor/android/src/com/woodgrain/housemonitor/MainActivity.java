package com.woodgrain.housemonitor;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** House Monitor: the monitor page full screen. It updates itself: whenever the app opens (or comes back, at most every
 *  10 minutes) it fetches the page from monitorsim.vercel.app and, if that build is newer, saves it and switches to it.
 *  Offline it runs the newest saved copy, or the one built into the APK (assets/index.html). Its sound comes to NativeAudio as 16-bit
 *  mono PCM blocks and is played as ordinary media audio, so phone screen recorders capture it. */
public class MainActivity extends Activity {
    private WebView web;
    private AudioTrack track;
    private final LinkedBlockingQueue<byte[]> blocks = new LinkedBlockingQueue<>(48);

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web = new WebView(this);
        setContentView(web);
        hideBars();
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);   // "Check for updates" fetches the newest monitor from GitHub
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                if ("file".equals(r.getUrl().getScheme())) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); } catch (Exception e) { }
                return true;   // links open in the browser, the monitor stays
            }
        });
        web.addJavascriptInterface(new NativeAudio(), "NativeAudio");
        web.addJavascriptInterface(new HMApp(), "HMApp");
        applyOrientation();
        web.loadUrl(pageUrl());
        checkUpdate();
    }

    // ---- self-update ----------------------------------------------------------------------------------------------
    private static final String SITE = "https://monitorsim.vercel.app/";
    private static final Pattern BUILD = Pattern.compile("HM_BUILD\\s*=\\s*'([0-9.\\-]+)'");
    private volatile String loadedBuild = "";
    private long lastCheck = 0;

    private File savedPage() { return new File(new File(getFilesDir(), "web"), "index.html"); }

    private static String buildOf(String html) { Matcher m = BUILD.matcher(html); return m.find() ? m.group(1) : ""; }

    /** "2026-11-25.5" style builds, compared number by number. */
    static int cmpBuild(String a, String b) {
        String[] x = a.split("[^0-9]+"), y = b.split("[^0-9]+");
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            long p = i < x.length && !x[i].isEmpty() ? Long.parseLong(x[i]) : 0, q = i < y.length && !y[i].isEmpty() ? Long.parseLong(y[i]) : 0;
            if (p != q) return p < q ? -1 : 1;
        }
        return 0;
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream o = new ByteArrayOutputStream(); byte[] b = new byte[65536]; int n;
        while ((n = in.read(b)) > 0) o.write(b, 0, n);
        in.close(); return o.toString("UTF-8");
    }

    /** The newest page on the phone: the saved download if it is newer than the APK's own, else the APK's. */
    private String pageUrl() {
        String bundled = "";
        try { bundled = buildOf(readAll(getAssets().open("index.html"))); } catch (Exception e) { }
        File f = savedPage();
        if (f.exists()) {
            try {
                String b = buildOf(readAll(new java.io.FileInputStream(f)));
                if (cmpBuild(b, bundled) > 0) { loadedBuild = b; return "file://" + f.getAbsolutePath(); }
            } catch (Exception e) { }
        }
        loadedBuild = bundled;
        return "file:///android_asset/index.html";
    }

    private void checkUpdate() {
        long now = System.currentTimeMillis();
        if (now - lastCheck < 10 * 60 * 1000L) return;
        lastCheck = now;
        new Thread(new Runnable() { @Override public void run() {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL(SITE + "?app=" + now).openConnection();
                c.setConnectTimeout(8000); c.setReadTimeout(20000); c.setUseCaches(false);
                c.setRequestProperty("Cache-Control", "no-cache");
                if (c.getResponseCode() != 200) return;
                final String html = readAll(c.getInputStream());
                final String b = buildOf(html);
                // only a whole, newer page: it must name its build and end properly
                if (b.isEmpty() || html.length() < 100000 || !html.trim().endsWith("</html>") || cmpBuild(b, loadedBuild) <= 0) return;
                File dir = savedPage().getParentFile(); dir.mkdirs();
                for (String icon : new String[]{"icon-192.png", "icon-512.png", "manifest.webmanifest"}) {
                    File t = new File(dir, icon);
                    if (t.exists()) continue;
                    try { InputStream in = getAssets().open(icon); FileOutputStream o = new FileOutputStream(t); byte[] buf = new byte[65536]; int n; while ((n = in.read(buf)) > 0) o.write(buf, 0, n); in.close(); o.close(); } catch (Exception e) { }
                }
                File tmp = new File(dir, "index.html.part");
                FileOutputStream o = new FileOutputStream(tmp); o.write(html.getBytes("UTF-8")); o.close();
                if (!tmp.renameTo(savedPage())) return;
                runOnUiThread(new Runnable() { @Override public void run() {
                    loadedBuild = b;
                    web.loadUrl("file://" + savedPage().getAbsolutePath());
                    web.postDelayed(new Runnable() { @Override public void run() {
                        web.evaluateJavascript("try{toast('Updated to build " + b + "')}catch(e){}", null);
                    } }, 2500);
                } });
            } catch (Exception e) { /* offline: keep the page we have */ }
        } }, "monitor-update").start();
    }

    private class NativeAudio {
        @JavascriptInterface public void start(int rate) { startTrack(rate); }
        @JavascriptInterface public void write(String b64) { blocks.offer(Base64.decode(b64, Base64.DEFAULT)); }
    }

    /** Full screen in the monitor's Monitor Setup: sideways (landscape, either way up) and remembered, or free rotation. */
    private class HMApp {
        @JavascriptInterface public boolean isLandscape() { return prefs().getBoolean("landscape", false); }
        @JavascriptInterface public void setLandscape(boolean on) {
            prefs().edit().putBoolean("landscape", on).apply();
            runOnUiThread(new Runnable() { @Override public void run() { applyOrientation(); } });
        }
    }

    private SharedPreferences prefs() { return getSharedPreferences("monitor", MODE_PRIVATE); }

    private void applyOrientation() {
        setRequestedOrientation(prefs().getBoolean("landscape", false)
            ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private synchronized void startTrack(int rate) {
        if (track != null) return;
        int min = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        track = new AudioTrack(
            new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
            new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
            Math.max(min, 8192), AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
        track.play();
        final AudioTrack out = track;
        Thread t = new Thread(new Runnable() { @Override public void run() {
            try { while (true) { byte[] d = blocks.take(); out.write(d, 0, d.length); } } catch (InterruptedException e) { }
        } }, "monitor-audio");
        t.setDaemon(true);
        t.start();
    }

    private void hideBars() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override public void onWindowFocusChanged(boolean focus) { super.onWindowFocusChanged(focus); if (focus) hideBars(); }
    @Override protected void onPause() { super.onPause(); web.onPause(); if (track != null) track.pause(); }
    @Override protected void onResume() { super.onResume(); web.onResume(); if (track != null) track.play(); hideBars(); checkUpdate(); }
    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
