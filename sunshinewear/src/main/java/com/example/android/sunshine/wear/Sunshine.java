/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.wear;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.Px;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.android.sunshine.wear.service.SunshineWearService;
import com.example.android.sunshine.wear.util.FormatUtil;
import com.example.android.sunshine.wear.util.MessageItemUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class Sunshine extends CanvasWatchFaceService {

    private static final String TAG = Sunshine.class.getSimpleName();

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the tvTime periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<Sunshine.Engine> mWeakReference;

        public EngineHandler(Sunshine.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            Sunshine.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    public class Engine extends CanvasWatchFaceService.Engine implements GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener {

        private int specW;
        private int specH;
        final Point displaySize = new Point();

        private View mLayout;
        private TextView tvTime;
        private TextView tvDate;
        private TextView tvMinTemp;
        private TextView tvMaxTemp;
        private ImageView ivIcon;
        private TextView tvNoData;
        private View stroke;
        private View dataContainer;

        private boolean dataChanged = false;
        private boolean iconLoaded = false;
        private String minTemp = "";
        private String maxTemp = "";
        private Bitmap iconImage = null;

        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;

        boolean mAmbient;
        Calendar mCalendar;
        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        final BroadcastReceiver mDataUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "DataUpdateReceiver: onReceive");
                Bundle data = intent.getExtras();
                if (data != null) {
                    String lowTemp = data.getString(MessageItemUtil.ITEM_EXTRA_MIN_TEMP);
                    String highTemp = data.getString(MessageItemUtil.ITEM_EXTRA_MAX_TEMP);
                    if (lowTemp == null | highTemp == null) {
                        return;
                    }
                    minTemp = data.getString(MessageItemUtil.ITEM_EXTRA_MIN_TEMP);
                    maxTemp = data.getString(MessageItemUtil.ITEM_EXTRA_MAX_TEMP);
                    Asset iconAsset = data.getParcelable(MessageItemUtil.ITEM_EXTRA_ASSET);
                    new LoadIconTask().execute(iconAsset);
                    dataChanged = true;
                    showData();
                    invalidate();
                }
            }
        };

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;

        private GoogleApiClient mGoogleApiClient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(Sunshine.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .setHideStatusBar(true)
                    .setHideHotwordIndicator(true)
                    .build());

            mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
            mGoogleApiClient.connect();

            // Use layout based watch face
            LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
            mLayout = inflater.inflate(R.layout.watch_face_layout, null);
            Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay();
            display.getSize(displaySize);
            specW = View.MeasureSpec.makeMeasureSpec(displaySize.x,
                    View.MeasureSpec.EXACTLY);
            specH = View.MeasureSpec.makeMeasureSpec(displaySize.y,
                    View.MeasureSpec.EXACTLY);
            Log.d(TAG, "W:" + displaySize.x + ",H:" + displaySize.y);

            tvTime = (TextView) mLayout.findViewById(R.id.time);
            tvDate = (TextView) mLayout.findViewById(R.id.date);
            tvMaxTemp = (TextView) mLayout.findViewById(R.id.max_temp);
            tvMinTemp = (TextView) mLayout.findViewById(R.id.min_temp);
            ivIcon = (ImageView) mLayout.findViewById(R.id.weather_prev);
            dataContainer = mLayout.findViewById(R.id.data_container);
            tvNoData = (TextView) mLayout.findViewById(R.id.no_data);
            stroke = mLayout.findViewById(R.id.stroke);
            showNoData();
            mCalendar = Calendar.getInstance();
        }

        private void showData() {
            if (tvNoData != null && dataContainer != null) {
                tvNoData.setVisibility(View.GONE);
                dataContainer.setVisibility(View.VISIBLE);
            }
        }

        private void showNoData() {
            if (tvNoData != null && dataContainer != null) {
                tvNoData.setVisibility(View.VISIBLE);
                dataContainer.setVisibility(View.GONE);
            }
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update tvTime zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            Sunshine.this.registerReceiver(mTimeZoneReceiver, new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED));
            Sunshine.this.registerReceiver(mDataUpdateReceiver, new IntentFilter(SunshineWearService.ACTION_DATA_UPDATE));
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            Sunshine.this.unregisterReceiver(mTimeZoneReceiver);
            Sunshine.this.unregisterReceiver(mDataUpdateReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            if (insets.isRound()) {
                @Px int padding = (int) getResources().getDimension(R.dimen.round_padding);
                mLayout.setPadding(padding, padding, padding, padding);
            }

        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLayout == null) {
                    return;
                }
                Resources resources = getResources();

                int colorWhite = resources.getColor(R.color.white);
                int colorBlueWhite = resources.getColor(R.color.sunshine_blue_white);
                if (mLowBitAmbient) {
                    tvTime.getPaint().setAntiAlias(!inAmbientMode);
                    tvDate.getPaint().setAntiAlias(!inAmbientMode);
                    tvMinTemp.getPaint().setAntiAlias(!inAmbientMode);
                    tvMaxTemp.getPaint().setAntiAlias(!inAmbientMode);
                    tvNoData.getPaint().setAntiAlias(!inAmbientMode);
                }
                tvDate.setTextColor(mAmbient ? colorWhite : colorBlueWhite);
                tvMinTemp.setTextColor(mAmbient ? colorWhite : colorBlueWhite);
                tvNoData.setTextColor(mAmbient ? colorWhite : colorBlueWhite);
                stroke.setVisibility(mAmbient ? View.GONE : View.VISIBLE);
                ivIcon.setVisibility(mAmbient ? View.GONE : View.VISIBLE);
                invalidate();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                mLayout.setBackgroundColor(Color.BLACK);
            } else {
                mLayout.setBackgroundColor(getResources().getColor(R.color.sunshine_blue));
            }


            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            tvTime.setText(FormatUtil.formattedTime(mCalendar, mAmbient));
            tvDate.setText(FormatUtil.formattedDate(mCalendar, mAmbient));
            if (dataChanged) {
                dataChanged = false;
                tvMinTemp.setText(minTemp);
                tvMaxTemp.setText(maxTemp);
            }
            if (iconLoaded) {
                iconLoaded = false;
                ivIcon.setImageBitmap(iconImage);
            }

            mLayout.measure(specW, specH);
            mLayout.layout(0, 0, mLayout.getMeasuredWidth(), mLayout.getMeasuredHeight());
            canvas.drawColor(Color.BLACK);
            mLayout.draw(canvas);

        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Log.d(TAG, "GoogleApiClient onConnected");
            MessageItemUtil.sendRequestUpdateMessage(mGoogleApiClient, null);
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "GoogleApiClient onConnectionSuspended");
        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            Log.d(TAG, "GoogleApiClient onConnectionFailed");
        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the tvTime periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        private class LoadIconTask extends AsyncTask<Asset, Void, Bitmap> {
            @Override
            protected Bitmap doInBackground(Asset... params) {
                if (params != null && params.length > 0 && params[0] != null) {
                    Asset asset = params[0];
                    InputStream inputStream = Wearable.DataApi.getFdForAsset(
                            mGoogleApiClient, asset).await().getInputStream();
                    if (inputStream != null) {
                        return BitmapFactory.decodeStream(inputStream);
                    }
                }
                return null;
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                if (bitmap != null) {
                    iconImage = bitmap;
                    iconLoaded = true;
                    invalidate();
                }
            }
        }
    }
}
