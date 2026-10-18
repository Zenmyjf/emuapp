package com.example.emu;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String BUILD = "build 6";
    private static final String URL =
        "https://www.myandroid.org/playonline/androidemulator.php";
    private static final String STREAM_PREFIX = "https://stream.myandroid.org/";

    private WebView webView;
    private FrameLayout root;
    private TextView dbgView;
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;
    private final Handler handler = new Handler();
    private float zoomK = 2.0f;
    private boolean inWrapper = false;

    // Runs on the site's pages. It presses the single "Enter" button, and once the
    // emulator page (the one with the blue Exit button) is up, it finds the hidden
    // emulator address and hands it to the app.
    private static final String JS =
        "(function(){"
        + "if(window.__emuTweaks)return;window.__emuTweaks=true;"
        + "function vis(e){var r=e.getBoundingClientRect();return r.width>0&&r.height>0;}"
        + "function enters(){var o=[];"
        + "var l=document.querySelectorAll('a,button,input[type=button],input[type=submit]');"
        + "for(var i=0;i<l.length;i++){var e=l[i];"
        + "var t=(e.innerText||e.value||'').trim().toLowerCase();"
        + "if(t==='enter'&&vis(e))o.push(e);}return o;}"
        + "function hasExit(){var l=document.querySelectorAll('a,button,div,span');"
        + "for(var i=0;i<l.length;i++){var e=l[i];"
        + "if(e.children.length===0&&vis(e)&&(e.innerText||'').trim().toLowerCase()==='exit')return true;}"
        + "return false;}"
        + "function findStream(){var l=document.querySelectorAll('iframe');"
        + "for(var i=0;i<l.length;i++){var s=l[i].src||'';"
        + "if(s.indexOf('stream.myandroid.org')>=0)return s;}return null;}"
        + "var n=0,sent=false;"
        + "setInterval(function(){n++;try{"
        + "var b=enters();"
        + "if(b.length===1&&!b[0].__clicked&&n<80){b[0].__clicked=true;b[0].click();return;}"
        + "var ex=hasExit();var s=findStream();"
        + "if(ex&&s&&!sent&&window.Android){sent=true;window.__dbg='emulator found';Android.openStream(s);return;}"
        + "window.__dbg='enters='+b.length+' exit='+ex+' stream='+(s?'yes':'no');"
        + "}catch(x){window.__dbg='ERR '+x.message;}},500);"
        + "})();";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));

        dbgView = new TextView(this);
        dbgView.setTextColor(Color.YELLOW);
        dbgView.setBackgroundColor(0x99000000);
        dbgView.setTextSize(10);
        dbgView.setPadding(8, 2, 8, 2);
        dbgView.setMaxLines(3);
        dbgView.setText(BUILD);
        FrameLayout.LayoutParams dlp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        dlp.gravity = Gravity.BOTTOM | Gravity.START;
        root.addView(dbgView, dlp);

        float d = getResources().getDisplayMetrics().density;
        LinearLayout zoomBox = new LinearLayout(this);
        zoomBox.setOrientation(LinearLayout.VERTICAL);
        zoomBox.addView(makeZoomButton("+", -0.25f, d));
        zoomBox.addView(makeZoomButton("-", 0.25f, d));
        FrameLayout.LayoutParams zlp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        zlp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        root.addView(zoomBox, zlp);
        setContentView(root);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setBuiltInZoomControls(false);
        s.setSupportZoom(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString(
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
            + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void openStream(final String src) {
                if (src == null || !src.startsWith(STREAM_PREFIX)) return;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loadWrapper(src);
                    }
                });
            }
        }, "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!inWrapper) {
                    view.evaluateJavascript(JS, null);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback cb) {
                if (customView != null) {
                    cb.onCustomViewHidden();
                    return;
                }
                customView = view;
                customCallback = cb;
                webView.setVisibility(View.GONE);
                root.addView(view, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            }

            @Override
            public void onHideCustomView() {
                hideCustom();
            }
        });

        handler.post(new Runnable() {
            @Override
            public void run() {
                webView.evaluateJavascript("(window.__dbg||'')", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String v) {
                        String t = (v == null) ? "" : v.replace("\"", "");
                        dbgView.setText(BUILD + " | " + t);
                    }
                });
                handler.postDelayed(this, 1000);
            }
        });

        webView.loadUrl(URL);
    }

    // Replaces the whole site page with a clean black page that holds only the
    // emulator, given a big virtual screen and then shrunk to fit the display.
    private void loadWrapper(String src) {
        inWrapper = true;
        String safe = src.replace("&", "&amp;").replace("'", "%27");
        String html =
            "<!doctype html><html><head>"
            + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<style>html,body{margin:0;height:100%;background:#000;overflow:hidden}"
            + "#f{position:fixed;left:0;top:0;border:0;transform-origin:0 0}</style>"
            + "</head><body>"
            + "<iframe id='f' src='" + safe + "' "
            + "allow='autoplay; fullscreen; microphone; clipboard-read; clipboard-write'></iframe>"
            + "<script>"
            + "window.__k=" + zoomK + ";"
            + "window.__apply=function(){var f=document.getElementById('f');"
            + "var w=window.innerWidth,h=window.innerHeight,k=window.__k;"
            + "f.style.width=(w*k)+'px';f.style.height=(h*k)+'px';"
            + "f.style.transform='scale('+(1/k)+')';"
            + "window.__dbg='zoom '+k.toFixed(2)+' virtual '+Math.round(w*k)+'x'+Math.round(h*k);};"
            + "window.addEventListener('resize',window.__apply);"
            + "window.__apply();"
            + "</script></body></html>";
        webView.loadDataWithBaseURL("https://www.myandroid.org/", html,
            "text/html", "UTF-8", null);
    }

    private Button makeZoomButton(String label, final float delta, float density) {
        Button b = new Button(this);
        b.setText(label);
        b.setAlpha(0.6f);
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setLayoutParams(new LinearLayout.LayoutParams(
            (int) (52 * density), (int) (52 * density)));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                zoomK = Math.max(1.0f, Math.min(4.0f, zoomK + delta));
                webView.evaluateJavascript(
                    "window.__k=" + zoomK + ";if(window.__apply)window.__apply();", null);
            }
        });
        return b;
    }

    private void hideCustom() {
        if (customView == null) return;
        root.removeView(customView);
        customView = null;
        webView.setVisibility(View.VISIBLE);
        if (customCallback != null) {
            customCallback.onCustomViewHidden();
            customCallback = null;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            hideCustom();
        } else if (inWrapper) {
            finish();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
