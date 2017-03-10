package com.example.android.sunshine.utilities;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by lars on 10.03.17.
 */

public class DataItemUtil {

    private static final String TAG = DataItemUtil.class.getSimpleName();

    public static final String TEMP_PATH = "/temp";

    public static final String REQUEST_UPDATE_PATH = "/request_update";

    private static final long DEFAULT_MESSAGE_TIMEOUT = 10;

    public static final String ITEM_EXTRA_MIN_TEMP = "item.extra.min_temp";

    public static final String ITEM_EXTRA_MAX_TEMP = "item.extra.min_temp";

    public static final String ITEM_EXTRA_ASSET = "item.extra.min_temp";

    public static void sendTempDataItem(GoogleApiClient googleApiClient, int minTemp, int maxTemp, Asset asset, ResultCallback<DataApi.DataItemResult> resultCallback) {
        if (googleApiClient == null) {
            Log.w(TAG, "sendTempDataItem: GoogleApiClient was null");
            return;
        }
        if (!googleApiClient.isConnected()) {
            Log.w(TAG, "sendTempDataItem: GoogleApiClient not connected");
            return;
        }
        if (asset == null) {
            Log.w(TAG, "sendTempDataItem: Asset was null");
            return;
        }
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(TEMP_PATH);
        putDataMapRequest.getDataMap().putInt(ITEM_EXTRA_MIN_TEMP, minTemp);
        putDataMapRequest.getDataMap().putInt(ITEM_EXTRA_MAX_TEMP, maxTemp);
        putDataMapRequest.getDataMap().putAsset(ITEM_EXTRA_MIN_TEMP, asset);
        PutDataRequest request = putDataMapRequest.asPutDataRequest();
        request.setUrgent();
        PendingResult<DataApi.DataItemResult> result = Wearable.DataApi.putDataItem(googleApiClient, request);
        if (resultCallback != null) {
            result.setResultCallback(resultCallback);
        } else {
            result.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                @Override
                public void onResult(DataApi.DataItemResult dataItemResult) {
                    if (!dataItemResult.getStatus().isSuccess()) {
                        Log.e(TAG, "ERROR: failed to putDataItem, status code: "
                                + dataItemResult.getStatus().getStatusCode());
                    } else {
                        Log.d(TAG, "putDataItem success");
                    }
                }
            });
        }
    }

}
