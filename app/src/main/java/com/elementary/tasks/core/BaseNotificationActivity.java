package com.elementary.tasks.core;

import android.app.Activity;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;

import com.backdoor.shared.SharedConst;
import com.elementary.tasks.R;
import com.elementary.tasks.ReminderApp;
import com.elementary.tasks.core.interfaces.SendListener;
import com.elementary.tasks.core.utils.Configs;
import com.elementary.tasks.core.utils.Constants;
import com.elementary.tasks.core.utils.Language;
import com.elementary.tasks.core.utils.Module;
import com.elementary.tasks.core.utils.Prefs;
import com.elementary.tasks.core.utils.Sound;
import com.elementary.tasks.core.utils.SuperUtil;
import com.elementary.tasks.core.utils.ViewUtils;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * Copyright 2016 Nazar Suhovich
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

public abstract class BaseNotificationActivity extends ThemedActivity {

    private static final String TAG = "BNActivity";
    private static final int MY_DATA_CHECK_CODE = 111;

    protected Sound mSound;
    protected Prefs mPrefs;
    protected Tracker mTracker;
    protected GoogleApiClient mGoogleApiClient;
    private TextToSpeech tts;
    private ProgressDialog mSendDialog;
    private Handler handler = new Handler();

    private int currVolume;
    private int streamVol;
    private int mVolume;
    private int mStream;

    protected TextToSpeech.OnInitListener mTextToSpeachListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(getTtsLocale());
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "This Language is not supported");
                } else {
                    if (!TextUtils.isEmpty(getSummary())) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (Module.isLollipop()) {
                            tts.speak(getSummary(), TextToSpeech.QUEUE_FLUSH, null, null);
                        } else {
                            tts.speak(getSummary(), TextToSpeech.QUEUE_FLUSH, null);
                        }
                    }
                }
            } else {
                Log.e(TAG, "Initialization Failed!");
            }
        }
    };
    protected SendListener mSendListener = isSent -> {
        hideProgressDialog();
        if (isSent) {
            finish();
        } else {
            showSendingError();
        }
    };
    protected GoogleApiClient.ConnectionCallbacks mGoogleCallback = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, mDataListener);
            sendDataToWear();
        }

        @Override
        public void onConnectionSuspended(int i) {

        }
    };

    private Runnable increaseVolume = new Runnable() {
        @Override
        public void run() {
            if (mVolume < streamVol) {
                mVolume++;
                handler.postDelayed(increaseVolume, 750);
                AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                am.setStreamVolume(mStream, mVolume, 0);
            } else handler.removeCallbacks(increaseVolume);
        }
    };

    protected DataApi.DataListener mDataListener = dataEventBuffer -> {
        Log.d(TAG, "Data received");
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                processDataEvent(event.getDataItem());
            }
        }
    };

    private void processDataEvent(DataItem item) {
        if (item.getUri().getPath().compareTo(SharedConst.PHONE_REMINDER) == 0) {
            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
            int keyCode = dataMap.getInt(SharedConst.REQUEST_KEY);
            if (keyCode == SharedConst.KEYCODE_OK) {
                ok();
            } else if (keyCode == SharedConst.KEYCODE_FAVOURITE) {
                favourite();
            } else if (keyCode == SharedConst.KEYCODE_CANCEL) {
                cancel();
            } else if (keyCode == SharedConst.KEYCODE_SNOOZE) {
                delay();
            } else {
                call();
            }
        }
    }

    protected abstract void sendDataToWear();

    protected abstract void call();

    protected abstract void delay();

    protected abstract void cancel();

    protected abstract void favourite();

    protected abstract void ok();

    protected abstract void showSendingError();

    protected abstract String getMelody();

    protected abstract boolean isScreenResumed();

    protected abstract boolean isVibrate();

    protected abstract String getSummary();

    protected abstract String getUuId();

    protected abstract int getId();

    protected abstract int getLedColor();

    protected abstract boolean isAwakeDevice();

    protected abstract boolean isUnlockDevice();

    protected abstract boolean isGlobal();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSound = new Sound(this);
        mPrefs = Prefs.getInstance(this);
        if (mPrefs.isWearEnabled()) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(mGoogleCallback)
                    .build();
        }
        if (SuperUtil.isGooglePlayServicesAvailable(this)) {
            ReminderApp application = (ReminderApp) getApplication();
            mTracker = application.getDefaultTracker();
        }
        setPlayerVolume();
        setUpScreenOptions();
    }

    private void setUpScreenOptions() {
        boolean isFull = mPrefs.isDeviceUnlockEnabled();
        boolean isWake = mPrefs.isDeviceAwakeEnabled();
        if (!isGlobal()) {
            isFull = isUnlockDevice();
            isWake = isAwakeDevice();
        }
        if (isFull) {
            runOnUiThread(() -> getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD));
        }
        if (isWake) {
            PowerManager.WakeLock screenLock = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "TAG");
            screenLock.acquire();
            screenLock.release();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                tts = new TextToSpeech(this, mTextToSpeachListener);
            } else {
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                try {
                    startActivity(installTTSIntent);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected void startTts() {
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        try {
            startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    protected void showReminderNotification(Class<Activity> activityClass) {
        Intent notificationIntent = new Intent(this, activityClass);
        notificationIntent.putExtra(Constants.INTENT_ID, getUuId());
        notificationIntent.putExtra(Constants.INTENT_NOTIFICATION, true);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(getSummary());
        builder.setContentIntent(intent);
        builder.setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        if (mPrefs.isManualRemoveEnabled()) {
            builder.setOngoing(false);
        } else {
            builder.setOngoing(true);
        }
        String appName;
        if (Module.isPro()) {
            appName = getString(R.string.app_name_pro);
            if (mPrefs.isLedEnabled()) {
                builder.setLights(mPrefs.getLedColor(), 500, 1000);
            }
        } else {
            appName = getString(R.string.app_name);
        }
        builder.setContentText(appName);
        builder.setSmallIcon(R.drawable.ic_notifications_black_24dp);
        if (Module.isLollipop()) {
            builder.setColor(ViewUtils.getColor(this, R.color.bluePrimary));
        }
        if (isScreenResumed()) {
            Uri soundUri = getSoundUri();
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                mSound.playAlarm(soundUri, mPrefs.isInfiniteSoundEnabled());
            } else {
                if (mPrefs.isSoundInSilentModeEnabled()) {
                    mSound.playAlarm(soundUri, mPrefs.isInfiniteSoundEnabled());
                }
            }
        }
        if (isVibrate()) {
            long[] pattern;
            if (mPrefs.isInfiniteVibrateEnabled()) {
                pattern = new long[]{150, 86400000};
            } else {
                pattern = new long[]{150, 400, 100, 450, 200, 500, 300, 500};
            }
            builder.setVibrate(pattern);
        }
        boolean isWear = mPrefs.isWearEnabled();
        if (isWear) {
            if (Module.isJellyMR2()) {
                builder.setOnlyAlertOnce(true);
                builder.setGroup("GROUP");
                builder.setGroupSummary(true);
            }
        }
        NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(this);
        mNotifyMgr.notify(getId(), builder.build());
        if (isWear) {
            showWearNotification(appName);
        }
    }

    protected void showTTSNotification(Class<Activity> activityClass) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setContentTitle(getSummary());
        if (mPrefs.isFoldingEnabled()) {
            Intent notificationIntent = new Intent(this, activityClass);
            notificationIntent.putExtra(Constants.INTENT_ID, getUuId());
            notificationIntent.putExtra(Constants.INTENT_NOTIFICATION, true);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            PendingIntent intent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(intent);
        }
        builder.setAutoCancel(false);
        builder.setPriority(Notification.PRIORITY_MAX);
        if (mPrefs.isManualRemoveEnabled()) {
            builder.setOngoing(false);
        } else {
            builder.setOngoing(true);
        }
        String appName;
        if (Module.isPro()) {
            appName = getString(R.string.app_name_pro);
            if (mPrefs.isLedEnabled()) {
                builder.setLights(mPrefs.getLedColor(), 500, 1000);
            }
        } else {
            appName = getString(R.string.app_name);
        }
        builder.setContentText(appName);
        builder.setSmallIcon(R.drawable.ic_notifications_black_24dp);
        if (Module.isLollipop()) {
            builder.setColor(ViewUtils.getColor(this, R.color.bluePrimary));
        }
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
            playDefaultMelody();
        } else {
            if (mPrefs.isSoundInSilentModeEnabled()) {
                playDefaultMelody();
            }
        }
        if (isVibrate()) {
            long[] pattern;
            if (mPrefs.isInfiniteVibrateEnabled()) {
                pattern = new long[]{150, 86400000};
            } else {
                pattern = new long[]{150, 400, 100, 450, 200, 500, 300, 500};
            }
            builder.setVibrate(pattern);
        }
        boolean isWear = mPrefs.isWearEnabled();
        if (isWear && Module.isJellyMR2()) {
            builder.setOnlyAlertOnce(true);
            builder.setGroup("GROUP");
            builder.setGroupSummary(true);
        }
        NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(this);
        mNotifyMgr.notify(getId(), builder.build());
        if (isWear) {
            showWearNotification(appName);
        }
    }

    private void setPlayerVolume() {
        boolean systemVol = mPrefs.isSystemLoudnessEnabled();
        boolean increasing = mPrefs.isIncreasingLoudnessEnabled();
        if (systemVol) {
            mStream = mPrefs.getSoundStream();
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            currVolume = am.getStreamVolume(mStream);
            streamVol = currVolume;
            mVolume = currVolume;
            if (increasing) {
                mVolume = 0;
                handler.postDelayed(increaseVolume, 750);
            }
            am.setStreamVolume(mStream, mVolume, 0);
        } else {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            mStream = 3;
            currVolume = am.getStreamVolume(mStream);
            int prefsVol = mPrefs.getLoudness();
            float volPercent = (float) prefsVol / Configs.MAX_VOLUME;
            int maxVol = am.getStreamMaxVolume(mStream);
            streamVol = (int) (maxVol * volPercent);
            mVolume = streamVol;
            if (increasing) {
                mVolume = 0;
                handler.postDelayed(increaseVolume, 750);
            }
            am.setStreamVolume(mStream, mVolume, 0);
        }
    }

    protected final void showProgressDialog(String message) {
        hideProgressDialog();
        mSendDialog = ProgressDialog.show(this, null, message, true, false);
    }

    protected final void hideProgressDialog() {
        if (mSendDialog != null && mSendDialog.isShowing()) {
            mSendDialog.dismiss();
        }
    }

    private Locale getTtsLocale() {
        return new Language().getLocale(this, false);
    }

    private Uri getSoundUri() {
        if (!TextUtils.isEmpty(getMelody())) {
            File sound = new File(getMelody());
            return Uri.fromFile(sound);
        } else {
            String defMelody = mPrefs.getMelodyFile();
            if (!TextUtils.isEmpty(defMelody)) {
                File sound = new File(defMelody);
                return Uri.fromFile(sound);
            } else {
                return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
        }
    }

    private void showWearNotification(String secondaryText) {
        if (Module.isJellyMR2()) {
            final NotificationCompat.Builder wearableNotificationBuilder = new NotificationCompat.Builder(this);
            wearableNotificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
            wearableNotificationBuilder.setContentTitle(getSummary());
            wearableNotificationBuilder.setContentText(secondaryText);
            if (Module.isLollipop()) {
                wearableNotificationBuilder.setColor(ViewUtils.getColor(this, R.color.bluePrimary));
            }
            wearableNotificationBuilder.setOngoing(false);
            wearableNotificationBuilder.setOnlyAlertOnce(true);
            wearableNotificationBuilder.setGroup("GROUP");
            wearableNotificationBuilder.setGroupSummary(false);
            NotificationManagerCompat mNotifyMgr = NotificationManagerCompat.from(this);
            mNotifyMgr.notify(getId(), wearableNotificationBuilder.build());
        }
    }

    private void playDefaultMelody() {
        try {
            AssetFileDescriptor afd = getAssets().openFd("sounds/beep.mp3");
            mSound.playAlarm(afd, false);
        } catch (IOException e) {
            e.printStackTrace();
            mSound.playAlarm(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), false);
        }
    }
}