package com.floatingdock.visual;

import android.app.Activity;
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
        webView.addJavascriptInterface(new SpeechBridge(), "AndroidBridge");
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
        webView.loadUrl(HOME_URL);
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

    private boolean isTrusted(String uriString) {
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

    private final class SpeechBridge {
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
