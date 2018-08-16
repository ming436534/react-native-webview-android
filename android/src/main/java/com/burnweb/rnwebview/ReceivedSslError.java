package com.burnweb.rnwebview;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;
import com.facebook.react.uimanager.events.RCTEventEmitter;

public class ReceivedSslError extends Event<NavigationStateChangeEvent> {

    public static final String EVENT_NAME = "receivedSslError";
    private final String mUrl;

    public ReceivedSslError(int viewId, String url) {
        super(viewId);
        mUrl = url;
    }

    @Override
    public String getEventName() {
        return EVENT_NAME;
    }

    @Override
    public void dispatch(RCTEventEmitter rctEventEmitter) {
        rctEventEmitter.receiveEvent(getViewTag(), getEventName(), serializeEventData());
    }

    private WritableMap serializeEventData() {
        WritableMap eventData = Arguments.createMap();
        eventData.putString("url", mUrl);
        return eventData;
    }

}
