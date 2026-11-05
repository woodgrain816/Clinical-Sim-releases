package com.woodgrain.housemonitor;

import android.app.Activity;
import android.content.Intent;
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
import java.util.concurrent.LinkedBlockingQueue;

/** House Monitor: the monitor page (assets/index.html) full screen, offline. Its sound comes to NativeAudio as 16-bit
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
        if (saved != null) web.restoreState(saved); else web.loadUrl("file:///android_asset/index.html");
    }

    private class NativeAudio {
        @JavascriptInterface public void start(int rate) { startTrack(rate); }
        @JavascriptInterface public void write(String b64) { blocks.offer(Base64.decode(b64, Base64.DEFAULT)); }
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
    @Override protected void onResume() { super.onResume(); web.onResume(); if (track != null) track.play(); hideBars(); }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); web.saveState(out); }
    @Override public void onBackPressed() { if (web.canGoBack()) web.goBack(); else super.onBackPressed(); }
}
