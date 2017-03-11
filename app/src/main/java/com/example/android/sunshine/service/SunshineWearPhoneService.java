package com.example.android.sunshine.service;

import android.content.Context;
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
import java.lang.ref.WeakReference;

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
            new WeatherQueryTask(mGoogleApiClient, getApplicationContext()).execute();
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

    public static class WeatherQueryTask extends AsyncTask<Void, Void, Cursor> {

        private WeakReference<Context> mContextReference;

        private GoogleApiClient mGoogleApiClient;

        private boolean disconnectAfterFinished = false;

        public WeatherQueryTask(GoogleApiClient googleApiClient, Context context) {
            this(googleApiClient, context, false);
        }

        public WeatherQueryTask(GoogleApiClient googleApiClient, Context context, boolean disconnectAfterFinished) {
            mGoogleApiClient = googleApiClient;
            if (context != null) {
                mContextReference = new WeakReference<>(context);
            }
            this.disconnectAfterFinished = disconnectAfterFinished;
        }

        @Override
        protected Cursor doInBackground(Void... params) {
            if (mContextReference == null || mContextReference.get() == null) {
                return null;
            }
            Uri forecastQueryUri = WeatherContract.WeatherEntry.CONTENT_URI;
            String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
            String selection = WeatherContract.WeatherEntry.getSqlSelectForTodayOnwards();
            return ContentResolverCompat.query(mContextReference.get().getContentResolver(),
                    forecastQueryUri, MAIN_FORECAST_PROJECTION, selection, null, sortOrder,
                    new CancellationSignal());
        }

        @Override
        protected void onPostExecute(Cursor cursor) {
            if (cursor != null && mContextReference.get() != null ) {
                if (cursor.getCount() == 0) {
                    Log.i(TAG, "Cursor was empty");
                    return;
                }
                Context context = mContextReference.get();
                cursor.moveToPosition(0);
                int weatherId = cursor.getInt(MainActivity.INDEX_WEATHER_CONDITION_ID);
                double lowTemp = cursor.getDouble(MainActivity.INDEX_WEATHER_MIN_TEMP);
                double highTemp = cursor.getDouble(MainActivity.INDEX_WEATHER_MAX_TEMP);
                DataItemUtil.syncTempDataItem(mGoogleApiClient,
                        SunshineWeatherUtils.formatTemperature(context, lowTemp),
                        SunshineWeatherUtils.formatTemperature(context, highTemp), createAssetFromBitmap(
                        BitmapFactory.decodeResource(context.getResources(), SunshineWeatherUtils
                                .getSmallArtResourceIdForWeatherCondition(weatherId))), null);
            }
            if (disconnectAfterFinished) {
                mGoogleApiClient.disconnect();
            }
            mGoogleApiClient = null;
        }
    }
}
