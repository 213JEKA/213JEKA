package com.keytrins.btcnewssignal;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private WebView web;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        NotificationHelper.ensureChannel(this);
        requestNotificationPermission();

        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(), "Android");
        web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 201);
        }
    }

    private void send(String function, String json) {
        runOnUiThread(() -> web.evaluateJavascript(function + "(" + JSONObject.quote(json) + ")", null));
    }

    public final class Bridge {
        @JavascriptInterface public void initialData() {
            List<NewsEvent> events = BlsCalendar.cached(MainActivity.this);
            send("window.receiveEvents", BlsCalendar.toJson(events));
            refreshCalendar();
            refreshBtc();
        }

        @JavascriptInterface public void refreshCalendar() {
            executor.execute(() -> {
                try {
                    List<NewsEvent> events = BlsCalendar.download();
                    BlsCalendar.save(MainActivity.this, events);
                    AlarmScheduler.schedule(MainActivity.this, events);
                    send("window.receiveEvents", BlsCalendar.toJson(events));
                } catch (Exception e) {
                    send("window.receiveError", "Не удалось обновить календарь BLS");
                }
            });
        }

        @JavascriptInterface public void refreshBtc() {
            executor.execute(() -> {
                try { send("window.receiveMarket", MarketData.fetchSignal().toString()); }
                catch (Exception e) { send("window.receiveError", "Нет соединения с Binance"); }
            });
        }

        @JavascriptInterface public void enableExactAlarms() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                if (!am.canScheduleExactAlarms()) {
                    Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName()));
                    startActivity(i);
                }
            }
        }
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
