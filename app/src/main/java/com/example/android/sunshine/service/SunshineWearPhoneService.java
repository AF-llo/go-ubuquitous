package com.example.android.sunshine.service;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.content.ContentResolverCompat;
import android.support.v4.os.CancellationSignal;
import android.util.Log;

import com.example.android.sunshine.MainActivity;
import com.example.android.sunshine.data.WeatherContract;
import com.example.android.sunshine.utilities.DataItemUtil;
import com.example.android.sunshine.utilities.SunshineWeatherUtils;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;

import static com.example.android.sunshine.MainActivity.MAIN_FORECAST_PROJECTION;
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
            new WeatherQueryTask().execute();
        }
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "createAsssetFromBitmap: bitmap was null");
            return null;
        }
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    private class WeatherQueryTask extends AsyncTask<Void, Void, Cursor> {
        @Override
        protected Cursor doInBackground(Void... params) {
            Uri forecastQueryUri = WeatherContract.WeatherEntry.CONTENT_URI;
            String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
            String selection = WeatherContract.WeatherEntry.getSqlSelectForTodayOnwards();
            return ContentResolverCompat.query(getApplicationContext().getContentResolver(),
                    forecastQueryUri, MAIN_FORECAST_PROJECTION, selection, null, sortOrder,
                    new CancellationSignal());
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            if (cursor != null) {
                cursor.moveToPosition(0);
                int weatherId = cursor.getInt(MainActivity.INDEX_WEATHER_CONDITION_ID);
                double lowTemp = cursor.getDouble(MainActivity.INDEX_WEATHER_MIN_TEMP);
                double highTemp = cursor.getDouble(MainActivity.INDEX_WEATHER_MAX_TEMP);
                DataItemUtil.syncTempDataItem(mGoogleApiClient,
                        SunshineWeatherUtils.formatTemperature(getApplicationContext(), lowTemp),
                        SunshineWeatherUtils.formatTemperature(getApplicationContext(), highTemp), createAssetFromBitmap(
                        BitmapFactory.decodeResource(getResources(), SunshineWeatherUtils
                                .getSmallArtResourceIdForWeatherCondition(weatherId))), null);
            }
        }
    }
}
