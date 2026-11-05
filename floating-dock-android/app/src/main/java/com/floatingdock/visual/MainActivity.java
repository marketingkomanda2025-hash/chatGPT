package com.floatingdock.visual;

import android.app.Activity;
import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.SystemClock;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.ArrayList;
import org.json.JSONObject;

/**
 * Small native Android shell for the Floating Dock web prototype.
 * No API keys are stored in the app. Speech transcription uses the
 * system speech-recognition activity and sends only text to the WebView.
 */
public final class MainActivity extends Activity {
    private static final String HOME_URL =
            "https://marketingkomanda2025-hash.github.io/chatGPT/floating-dock/";
    private static final String ALLOWED_HOST = "marketingkomanda2025-hash.github.io";
    private static final String ALLOWED_PATH = "/chatGPT/floating-dock/";

    private WebView webView;
    private static final int SPEECH_REQUEST = 7421;
    private static final int NOTIFICATION_PERMISSION_REQUEST = 7422;
    public static final String EXTRA_OPEN_HRYUNDEL = "com.floatingdock.visual.OPEN_HRYUNDEL";
    private static final long PUSH_DELAY_MILLIS = 20_000L;
    private boolean pendingNotification = false;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(11, 16, 32));
        getWindow().setNavigationBarColor(Color.rgb(11, 16, 32));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(11, 16, 32));
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);

        // JavaScript is only trusted for the dedicated published prototype URL.
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (isTrusted(request.getUrl().toString())) return false;
                // Never allow the JS bridge in untrusted top-level pages.
                return true;
            }
        });
        setContentView(webView, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        webView.loadUrl(HOME_URL + (getIntent() != null &&
                getIntent().getBooleanExtra(EXTRA_OPEN_HRYUNDEL, false) ? "?tab=chat" : ""));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SPEECH_REQUEST) return;
        if (resultCode != RESULT_OK || data == null) {
            sendSpeechError("Распознавание речи отменено");
            return;
        }
        ArrayList<String> alternatives = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (alternatives == null || alternatives.isEmpty()) {
            sendSpeechError("Не удалось распознать речь");
        } else {
            callWeb("receiveTranscription", alternatives.get(0));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null && intent.getBooleanExtra(EXTRA_OPEN_HRYUNDEL, false)) {
            if (webView != null && isTrusted(webView.getUrl())) {
                webView.evaluateJavascript("window.openHryundelChat && window.openHryundelChat();", null);
            } else if (webView != null) {
                webView.loadUrl(HOME_URL + "?tab=chat");
            }
        }
    }

    private void requestTestNotification() {
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingNotification = true;
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_REQUEST);
            return;
        }
        scheduleTestNotification();
    }

    private void scheduleTestNotification() {
        Intent intent = new Intent(this, HryundelNotificationReceiver.class);
        PendingIntent pending = PendingIntent.getBroadcast(
                this, 7401, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarm = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarm == null) {
            callWeb("receiveNotificationStatus", "Не удалось запланировать уведомление");
            return;
        }
        alarm.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + PUSH_DELAY_MILLIS, pending);
        callWeb("receiveNotificationStatus", "Хрюндель напишет примерно через 20 секунд. Можно свернуть приложение!");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode != NOTIFICATION_PERMISSION_REQUEST || !pendingNotification) return;
        pendingNotification = false;
        if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            scheduleTestNotification();
        } else {
            callWeb("receiveNotificationStatus", "Разреши уведомления для Floating Dock в настройках Android");
        }
    }

    private boolean isTrusted(String uriString) {
        if (uriString == null) return false;
        android.net.Uri uri = android.net.Uri.parse(uriString);
        String path = uri.getPath();
        return "https".equalsIgnoreCase(uri.getScheme())
                && ALLOWED_HOST.equalsIgnoreCase(uri.getHost())
                && path != null && (path.equals(ALLOWED_PATH.substring(0, ALLOWED_PATH.length() - 1))
                    || path.startsWith(ALLOWED_PATH));
    }

    private void callWeb(String function, String value) {
        if (webView == null || !isTrusted(webView.getUrl())) return;
        webView.evaluateJavascript("window." + function + "(" + JSONObject.quote(value) + ");", null);
    }

    private void sendSpeechError(String message) {
        if (webView != null) callWeb("receiveSpeechError", message);
    }

    private final class AndroidBridge {
        @JavascriptInterface
        public void scheduleHryundelNotification() {
            runOnUiThread(() -> {
                if (webView != null && isTrusted(webView.getUrl())) requestTestNotification();
            });
        }

        @JavascriptInterface
        public void startSpeechRecognition() {
            runOnUiThread(() -> {
                if (webView == null || !isTrusted(webView.getUrl())) return;
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU");
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите…");
                try {
                    startActivityForResult(intent, SPEECH_REQUEST);
                } catch (ActivityNotFoundException error) {
                    sendSpeechError("На устройстве нет приложения для распознавания речи");
                } catch (Exception error) {
                    sendSpeechError("Не получилось запустить голосовой ввод");
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidBridge");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
