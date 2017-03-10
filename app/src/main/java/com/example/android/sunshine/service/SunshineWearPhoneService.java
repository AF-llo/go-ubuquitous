package com.example.android.sunshine.service;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import static com.example.android.sunshine.utilities.DataItemUtil.REQUEST_UPDATE_PATH;

public class SunshineWearPhoneService extends WearableListenerService {

    private static final String TAG = SunshineWearPhoneService.class.getSimpleName();

    GoogleApiClient mGoogleApiClient;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        super.onDataChanged(dataEventBuffer);
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        Log.d(TAG, "onMessageReceived: " + messageEvent.getPath());
        if (messageEvent.getPath().equals(REQUEST_UPDATE_PATH)) {
            // TODO: 10.03.17 send mintemp, maxtemp, asset
        }
    }
}
