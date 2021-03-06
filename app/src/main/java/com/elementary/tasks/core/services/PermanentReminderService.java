package com.elementary.tasks.core.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.elementary.tasks.core.utils.LogUtil;
import com.elementary.tasks.core.utils.Notifier;
import com.elementary.tasks.core.utils.Prefs;

/**
 * Copyright 2017 Nazar Suhovich
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

public class PermanentReminderService extends Service {

    public static final String ACTION_SHOW = "com.elementary.tasks.SHOW";
    public static final String ACTION_HIDE = "com.elementary.tasks.HIDE";

    private static final String TAG = "PermanentReminderS";

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (!Prefs.getInstance(getApplicationContext()).isSbNotificationEnabled()) {
            Notifier.hideReminderPermanent(this);
        }
        if (intent != null) {
            String action = intent.getAction();
            LogUtil.d(TAG, "onStartCommand: " + action);
            if (action != null && action.matches(ACTION_SHOW)) {
                Notifier.showReminderPermanent(this);
            } else {
                Notifier.hideReminderPermanent(this);
            }
        } else {
            Notifier.hideReminderPermanent(this);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtil.d(TAG, "onDestroy: ");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
