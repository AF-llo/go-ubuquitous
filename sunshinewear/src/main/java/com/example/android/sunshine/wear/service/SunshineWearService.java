package com.example.android.sunshine.wear.service;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.android.sunshine.wear.util.MessageItemUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

public class SunshineWearService extends WearableListenerService {

    private static final String TAG = SunshineWearService.class.getSimpleName();

    public static final String ACTION_DATA_UPDATE = "com.example.android.sunshine.wear.DATA_UPDATE";

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
        Log.d(TAG, "onDataChanged: " + dataEventBuffer);
        if (!mGoogleApiClient.isConnected() || !mGoogleApiClient.isConnecting()) {
            ConnectionResult connectionResult = mGoogleApiClient
                    .blockingConnect(30, TimeUnit.SECONDS);
            if (!connectionResult.isSuccess()) {
                Log.e(TAG, "SunshineWearService failed to connect to GoogleApiClient, "
                        + "error code: " + connectionResult.getErrorCode());
                return;
            }
        }
        for (DataEvent dataEvent : dataEventBuffer) {
            DataItem item = dataEvent.getDataItem();
            if (item.getUri().getPath().equals(MessageItemUtil.TEMP_PATH)) {
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                Log.d(TAG, "onDataChanged: minTemp=" + dataMap.getString(MessageItemUtil.ITEM_EXTRA_MIN_TEMP) +
                        ", maxTemp=" + dataMap.getString(MessageItemUtil.ITEM_EXTRA_MAX_TEMP));
                notifyDataUpdate(dataMap.toBundle());
            }
        }
    }

    private void notifyDataUpdate(Bundle data) {
        if (data == null) {
            return;
        }
        Intent intent = new Intent(ACTION_DATA_UPDATE);
        intent.putExtras(data);
        sendBroadcast(intent);
    }

}
