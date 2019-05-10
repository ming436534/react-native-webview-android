/**
 * @providesModule WebViewAndroid
 */
"use strict";

var React = require("react");
var RN = require("react-native");
var createClass = require("create-react-class");
var PropTypes = require("prop-types");

var { requireNativeComponent, NativeModules } = require("react-native");
var RCTUIManager = NativeModules.UIManager;

var WEBVIEW_REF = "androidWebView";

var WebViewAndroid = createClass({
  propTypes: {
    url: PropTypes.string,
    source: PropTypes.object,
    baseUrl: PropTypes.string,
    html: PropTypes.string,
    htmlCharset: PropTypes.string,
    userAgent: PropTypes.string,
    injectedJavaScript: PropTypes.string,
    disablePlugins: PropTypes.bool,
    disableCookies: PropTypes.bool,
    javaScriptEnabled: PropTypes.bool,
    geolocationEnabled: PropTypes.bool,
    allowUrlRedirect: PropTypes.bool,
    builtInZoomControls: PropTypes.bool,
    onNavigationStateChange: PropTypes.func,
    onMessage: PropTypes.func,
    onShouldStartLoadWithRequest: PropTypes.func,
    evaluateJavascript: PropTypes.string
  },
  _onNavigationStateChange: function(event) {
    if (this.props.onNavigationStateChange) {
      this.props.onNavigationStateChange(event.nativeEvent);
    }
  },
  _onMessage: function(event) {
    if (this.props.onMessage) {
      this.props.onMessage(event.nativeEvent);
    }
  },
  _onReceivedSslError: async function(event) {
    let shouldProceed = false;
    if (this.props.onReceivedSslError) {
      shouldProceed = await this.props.onReceivedSslError(event.nativeEvent);
    }
    const handle = this._getWebViewHandle();
    if (!handle) return;
    RCTUIManager.dispatchViewManagerCommand(
      handle,
      RCTUIManager.RNWebViewAndroid.Commands.resolveSslError,
      [shouldProceed]
    );
  },
  _onShouldOverrideUrlLoading: function(event) {
    let shouldOverride = false;

    if (this.props.onShouldStartLoadWithRequest) {
      shouldOverride = !this.props.onShouldStartLoadWithRequest(event.nativeEvent);
    }
    const handle = this._getWebViewHandle();
    if (!handle) return;
    RCTUIManager.dispatchViewManagerCommand(
      handle,
      RCTUIManager.RNWebViewAndroid.Commands.shouldOverrideWithResult,
      [shouldOverride]
    );
  },
  goBack: function() {
    const handle = this._getWebViewHandle();
    if (!handle) return;
    RCTUIManager.dispatchViewManagerCommand(
      handle,
      RCTUIManager.RNWebViewAndroid.Commands.goBack,
      null
    );
  },
  goForward: function() {
    const handle = this._getWebViewHandle();
    if (!handle) return;
    RCTUIManager.dispatchViewManagerCommand(
      handle,
      RCTUIManager.RNWebViewAndroid.Commands.goForward,
      null
    );
  },
  reload: function() {
    const handle = this._getWebViewHandle();
    if (!handle) return;
    RCTUIManager.dispatchViewManagerCommand(
      handle,
      RCTUIManager.RNWebViewAndroid.Commands.reload,
      null
    );
  },
  stopLoading: function() {
    const handle = this._getWebViewHandle();
    if (!handle) return;
    RCTUIManager.dispatchViewManagerCommand(
      handle,
      RCTUIManager.RNWebViewAndroid.Commands.stopLoading,
      null
    );
  },
  evaluateJavascript: function(data) {
    const handle = this._getWebViewHandle();
    if (!handle) return;
    RCTUIManager.dispatchViewManagerCommand(
      handle,
      RCTUIManager.RNWebViewAndroid.Commands.evaluateJavascript,
      [String(data)]
    );
  },
  postMessage: function(data) {
    const handle = this._getWebViewHandle();
    if (!handle) return;
    RCTUIManager.dispatchViewManagerCommand(
      handle,
      RCTUIManager.RNWebViewAndroid.Commands.postMessage,
      [String(data)]
    );
  },
  injectJavaScript: function(data) {
    const handle = this._getWebViewHandle();
    if (!handle) return;
    RCTUIManager.dispatchViewManagerCommand(
      handle,
      RCTUIManager.RNWebViewAndroid.Commands.injectJavaScript,
      [data]
    );
  },
  render: function() {
    return (
      <RNWebViewAndroid
        ref={WEBVIEW_REF}
        {...this.props}
        onNavigationStateChange={this._onNavigationStateChange}
        onMessageEvent={this._onMessage}
        onReceivedSslError={this._onReceivedSslError}
        onShouldOverrideUrlLoading={this._onShouldOverrideUrlLoading}
      />
    );
  },
  _getWebViewHandle: function() {
    return RN.findNodeHandle(this.refs[WEBVIEW_REF]);
  },
});

var RNWebViewAndroid = requireNativeComponent("RNWebViewAndroid", null);

module.exports = WebViewAndroid;
