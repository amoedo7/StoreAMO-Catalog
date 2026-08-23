package com.desarrollamo.batch40core;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        WebView web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(false);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(false);
        settings.setBlockNetworkLoads(true);
        web.setBackgroundColor(0xFF07111F);
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }
    @Override public void onDestroy() {
        if (getWindow() != null && getWindow().getDecorView() instanceof android.view.ViewGroup) {
            // WebView lifecycle is tied to this Activity; no background service is created.
        }
        super.onDestroy();
    }
}