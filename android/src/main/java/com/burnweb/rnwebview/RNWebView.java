package com.burnweb.rnwebview;

import android.annotation.SuppressLint;

import android.content.DialogInterface;
import android.net.Uri;
import android.graphics.Bitmap;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.os.Build;
import android.os.Debug;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.MimeTypeMap;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.SystemClock;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerModule;
import com.facebook.react.uimanager.events.EventDispatcher;

import java.security.cert.Certificate;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class RNWebView extends WebView implements LifecycleEventListener {

    private final EventDispatcher mEventDispatcher;
    private final RNWebViewManager mViewManager;
    private final EventWebClient mWebViewClient;

    private String charset = "UTF-8";
    private String baseUrl = "file:///";
    private String injectedJavaScript = null;
    private boolean allowUrlRedirect = false;

    private String currentUrl = "";
    private String shouldOverrideUrlLoadingUrl = "";
    private SslErrorHandler m_SslErrorHandler;

    public Boolean disableOverrideUrlLoading = false;

    protected class EventWebClient extends WebViewClient {
        public boolean shouldOverrideUrlLoading(WebView view, String url){
            int navigationType = 0;

            if (currentUrl.equals(url) || url.equals("about:blank")) { // for regular .reload() and html reload.
                navigationType = 3;
            }

            shouldOverrideUrlLoadingUrl = url;
            if (!disableOverrideUrlLoading) {
                mEventDispatcher.dispatchEvent(new ShouldOverrideUrlLoadingEvent(getId(), SystemClock.nanoTime(), url, navigationType));
            }
            return !disableOverrideUrlLoading;
        }
        @Override
        public void onReceivedSslError(WebView view, final SslErrorHandler handler, SslError error) {

            SslCertificate cert = error.getCertificate();
            SslCertificate.DName issuedBy = cert.getIssuedBy();
            SslCertificate.DName issuedTo = cert.getIssuedTo();
            if (issuedBy.getDName().contentEquals("CN=DigiCert SHA2 Secure Server CA,O=DigiCert Inc,C=US") &&
                    issuedTo.getCName().contentEquals("*.cityline.com") &&
                    issuedTo.getDName().contentEquals("CN=*.cityline.com,O=CityLine (Hong Kong) Ltd,L=Hong Kong,C=HK")) {
                handler.proceed();
            } else {
                if (m_SslErrorHandler != null) m_SslErrorHandler.cancel();
                m_SslErrorHandler = handler;
                mEventDispatcher.dispatchEvent(new ReceivedSslError(getId(), view.getUrl()));
            }

        }
        public void resolveSslError(boolean isContinue) {
            if (m_SslErrorHandler == null) return;
            if (isContinue) {
                m_SslErrorHandler.proceed();
            } else {
                m_SslErrorHandler.cancel();
            }
            m_SslErrorHandler = null;
        }

        public void onPageFinished(WebView view, String url) {
            mEventDispatcher.dispatchEvent(new NavigationStateChangeEvent(getId(), SystemClock.nanoTime(), view.getTitle(), false, url, view.canGoBack(), view.canGoForward()));

            currentUrl = url;

            if(RNWebView.this.getInjectedJavaScript() != null) {
                view.loadUrl("javascript:(function() {\n" + RNWebView.this.getInjectedJavaScript() + ";\n})();");
            }
        }

        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            mEventDispatcher.dispatchEvent(new NavigationStateChangeEvent(getId(), SystemClock.nanoTime(), view.getTitle(), true, url, view.canGoBack(), view.canGoForward()));
        }
//        @Override
//        public WebResourceResponse shouldInterceptRequest(final WebView view, final String url) {
//            return interceptRequest(view, url);
//        }
//
//        @SuppressLint("NewApi")
//        @Override
//        public WebResourceResponse shouldInterceptRequest(final WebView view, final WebResourceRequest request) {
//            return interceptRequest(view, request.getUrl().toString());
//        }
//
//        public WebResourceResponse interceptRequest(final WebView view, final String url) {
//            String extension = MimeTypeMap.getFileExtensionFromUrl(url);
//
//            if (extension != null && extension.length() > 0 && !extension.equalsIgnoreCase("html")) {
//                OkHttpClient httpClient = new OkHttpClient.Builder().build();
//                Request.Builder req = new Request.Builder()
//                        .url(url.trim());
//                try {
//                    Response response = httpClient.newCall(req.build()).execute();
//                    return new WebResourceResponse(
//                            response.header("content-type").split(";")[0],
//                            response.body().contentType().charset().displayName(),
//                            response.body().byteStream()
//                    );
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//            return null;
//        }

    }

    protected class CustomWebChromeClient extends WebChromeClient {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            getModule().showAlert(url, message, result);
            return true;
        }

        // For Android 4.1+
        @SuppressWarnings("unused")
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            getModule().startFileChooserIntent(uploadMsg, acceptType);
        }

        // For Android 5.0+
        @SuppressLint("NewApi")
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            return getModule().startFileChooserIntent(filePathCallback, fileChooserParams.createIntent());
        }
    }

    protected class GeoWebChromeClient extends CustomWebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            callback.invoke(origin, true, false);
        }
    }

    public RNWebView(RNWebViewManager viewManager, ThemedReactContext reactContext) {
        super(reactContext);

        mViewManager = viewManager;
        mEventDispatcher = reactContext.getNativeModule(UIManagerModule.class).getEventDispatcher();

        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setBuiltInZoomControls(false);
        this.getSettings().setDomStorageEnabled(true);
        this.getSettings().setGeolocationEnabled(false);
        this.getSettings().setPluginState(WebSettings.PluginState.ON);
        this.getSettings().setAllowFileAccess(true);
        this.getSettings().setAllowFileAccessFromFileURLs(true);
        this.getSettings().setAllowUniversalAccessFromFileURLs(true);
        this.getSettings().setLoadsImagesAutomatically(true);
        this.getSettings().setBlockNetworkImage(false);
        this.getSettings().setBlockNetworkLoads(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        mWebViewClient = new EventWebClient();
        this.setWebViewClient(mWebViewClient);
        this.setWebChromeClient(getCustomClient());

        this.addJavascriptInterface(RNWebView.this, "webView");
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public String getCharset() {
        return this.charset;
    }

    public void setAllowUrlRedirect(boolean a) {
        this.allowUrlRedirect = a;
    }

    public boolean getAllowUrlRedirect() {
        return this.allowUrlRedirect;
    }

    public void setInjectedJavaScript(String injectedJavaScript) {
        this.injectedJavaScript = injectedJavaScript;
    }

    public String getInjectedJavaScript() {
        return this.injectedJavaScript;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void shouldOverrideWithResult(RNWebView view, ReadableArray args, Map<String, String> headersMap) {
        if (!args.getBoolean(0)) {
            view.loadUrl(shouldOverrideUrlLoadingUrl, headersMap);
        }
    }

    public void resolveSslError(ReadableArray args) {
        mWebViewClient.resolveSslError(args.getBoolean(0));
    }

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public CustomWebChromeClient getCustomClient() {
        return new CustomWebChromeClient();
    }

    public GeoWebChromeClient getGeoClient() {
        return new GeoWebChromeClient();
    }

    public RNWebViewModule getModule() {
        return mViewManager.getPackage().getModule();
    }

    @Override
    public void onHostResume() {

    }

    @Override
    public void onHostPause() {

    }

    @Override
    public void onHostDestroy() {
        destroy();
    }

    @Override
    public void onDetachedFromWindow() {
        this.loadDataWithBaseURL(this.getBaseUrl(), "<html></html>", "text/html", this.getCharset(), null);
        super.onDetachedFromWindow();
    }

    @JavascriptInterface
     public void postMessage(String jsParamaters) {
        mEventDispatcher.dispatchEvent(new MessageEvent(getId(), jsParamaters));
    }
}
