package com.example.android.sunshine.wear.util;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * Created by lars on 10.03.17.
 */

public class MessageItemUtil {

    private static final String TAG = MessageItemUtil.class.getSimpleName();

    public static final String TEMP_PATH = "/temp";

    public static final String REQUEST_UPDATE_PATH = "/request_update";

    private static final long DEFAULT_MESSAGE_TIMEOUT = 10;

    public static final String ITEM_EXTRA_MIN_TEMP = "item.extra.min_temp";

    public static final String ITEM_EXTRA_MAX_TEMP = "item.extra.max_temp";

    public static final String ITEM_EXTRA_ASSET = "item.extra.asset";

    public static final String ITEM_EXTRA_TIMESTAMP = "item.extra.timestamp";

    public static void sendRequestUpdateMessage(final GoogleApiClient googleApiClient, final ResultCallback<MessageApi.SendMessageResult> resultCallback) {
        if (googleApiClient == null) {
            Log.w(TAG, "sendRequestUpdateMessage: GoogleApiClient was null");
            return;
        }
        if (!googleApiClient.isConnected()) {
            Log.w(TAG, "sendRequestUpdateMessage: GoogleApiClient not connected");
        }
        Log.d(TAG, "sendRequestUpdateMessage");
        new Thread(new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleApiClient).await(DEFAULT_MESSAGE_TIMEOUT, TimeUnit.SECONDS);
                for (Node node : nodes.getNodes()) {
                    sendRequestUpdateMessage(googleApiClient, node.getId(),resultCallback);
                }
            }
        }).start();
    }

    private static void sendRequestUpdateMessage(final GoogleApiClient googleApiClient, String nodeId, ResultCallback<MessageApi.SendMessageResult> resultCallback) {
        Log.d(TAG, "sendRequestUpdateMessage: nodeId=" + nodeId);
        PendingResult<MessageApi.SendMessageResult> result = Wearable.MessageApi.sendMessage(googleApiClient, nodeId, REQUEST_UPDATE_PATH, new byte[0]);
        if (resultCallback != null) {
            result.setResultCallback(resultCallback);
        } else {
            result.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                    if (!sendMessageResult.getStatus().isSuccess()) {
                        Log.e(TAG, "Failed to send message with status code: "
                                + sendMessageResult.getStatus().getStatusCode());
                    } else {
                        Log.d(TAG, "Succeeded to send RequestUpdateMessage");
                    }
                }
            }, DEFAULT_MESSAGE_TIMEOUT, TimeUnit.SECONDS);
        }
    }

}