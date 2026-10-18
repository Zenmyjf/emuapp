package com.example.emu;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MainActivity extends Activity {

    private WebView webView;
    private FrameLayout root;
    private View customView;
    private WebChromeClient.CustomViewCallback customCallback;

    private static final String URL =
        "https://www.myandroid.org/playonline/androidemulator.php";

    // Runs inside every page: presses the single "Enter" button automatically,
    // and once the emulator is on screen, stretches it to fill the whole display.
    private static final String JS =
        "(function(){"
        + "if(window.__emuTweaks)return;window.__emuTweaks=true;"
        + "function W(){return window.innerWidth;}function H(){return window.innerHeight;}"
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
        + "function pick(){var el=document.elementFromPoint(W()/2,H()/2);"
        + "if(!el||el===document.body||el===document.documentElement)return null;"
        + "var cur=el;for(var i=0;i<6;i++){var p=cur.parentElement;"
        + "if(!p||p===document.body||p===document.documentElement)break;"
        + "var pr=p.getBoundingClientRect(),cr=cur.getBoundingClientRect();"
        + "if(pr.width<=cr.width*1.05&&pr.height<=cr.height*1.3)cur=p;else break;}"
        + "var r=cur.getBoundingClientRect();"
        + "return(r.width>W()*0.5&&r.height>H()*0.3)?cur:null;}"
        + "function fit(e){var s=e.style;"
        + "s.setProperty('position','fixed','important');"
        + "s.setProperty('top','0','important');"
        + "s.setProperty('left','0','important');"
        + "s.setProperty('width','100vw','important');"
        + "s.setProperty('height','100vh','important');"
        + "s.setProperty('max-width','none','important');"
        + "s.setProperty('max-height','none','important');"
        + "s.setProperty('margin','0','important');"
        + "s.setProperty('padding','0','important');"
        + "s.setProperty('border','0','important');"
        + "s.setProperty('background','#000','important');"
        + "s.setProperty('z-index','2147483647','important');"
        + "document.documentElement.style.overflow='hidden';"
        + "document.body.style.overflow='hidden';}"
        + "var target=null;"
        + "function hide(){"
        + "var h=document.querySelectorAll('#masthead,header,.site-header');"
        + "for(var i=0;i<h.length;i++){if(!target||!target.contains(h[i]))"
        + "h[i].style.setProperty('display','none','important');}"
        + "var l=document.querySelectorAll('div,iframe,ins,aside,a');"
        + "for(var j=0;j<l.length;j++){var e=l[j];"
        + "if(target&&(e===target||target.contains(e)||e.contains(target)))continue;"
        + "var cs=getComputedStyle(e);"
        + "if(cs.position==='fixed'){var r=e.getBoundingClientRect();"
        + "if(r.bottom>=H()-4&&r.height<H()*0.4&&r.width>W()*0.6)"
        + "e.style.setProperty('display','none','important');}}}"
        + "var n=0;"
        + "setInterval(function(){n++;"
        + "var b=enters();"
        + "if(b.length===1&&!b[0].__clicked&&n<80){b[0].__clicked=true;b[0].click();return;}"
        + "if(b.length===0&&hasExit()){"
        + "if(!target||!target.isConnected){target=pick();if(target)fit(target);}"
        + "if(target&&n%4===0)hide();}"
        + "},500);"
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

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(JS, null);
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

        webView.loadUrl(URL);
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
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
