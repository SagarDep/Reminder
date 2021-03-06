package com.elementary.tasks.core.controller;

import android.content.Context;
import android.text.TextUtils;

import com.elementary.tasks.core.services.AlarmReceiver;
import com.elementary.tasks.core.services.GeolocationService;
import com.elementary.tasks.core.utils.Notifier;
import com.elementary.tasks.core.utils.RealmDb;
import com.elementary.tasks.core.utils.SuperUtil;
import com.elementary.tasks.core.utils.TimeCount;
import com.elementary.tasks.reminder.models.Reminder;

import java.util.List;

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

class LocationEvent extends EventManager {

    LocationEvent(Reminder reminder, Context context) {
        super(reminder, context);
    }

    @Override
    public boolean start() {
        getReminder().setActive(true);
        super.save();
        if (new AlarmReceiver().enablePositionDelay(getContext(), getReminder().getUuId())) {
            return true;
        } else {
            SuperUtil.startGpsTracking(getContext());
            return true;
        }
    }

    @Override
    public boolean stop() {
        new AlarmReceiver().cancelPositionDelay(getContext(), getReminder().getUniqueId());
        RealmDb.getInstance().saveObject(getReminder().setActive(false));
        Notifier.hideNotification(getContext(), getReminder().getUniqueId());
        getReminder().setActive(false);
        super.save();
        stopTracking(false);
        return true;
    }

    private void stopTracking(boolean isPaused) {
        List<Reminder> list = RealmDb.getInstance().getGpsReminders();
        if (list.size() == 0) {
            SuperUtil.stopService(getContext(), GeolocationService.class);
        }
        boolean hasActive = false;
        for (Reminder item : list) {
            if (isPaused) {
                if (item.getUniqueId() == getReminder().getUniqueId()) {
                    continue;
                }
                if (TextUtils.isEmpty(item.getEventTime()) || !TimeCount.isCurrent(item.getEventTime())) {
                    if (!item.isNotificationShown()) {
                        hasActive = true;
                        break;
                    }
                } else {
                    if (!item.isNotificationShown()) {
                        hasActive = true;
                        break;
                    }
                }
            } else {
                if (!item.isNotificationShown()) {
                    hasActive = true;
                    break;
                }
            }
        }
        if (!hasActive) {
            SuperUtil.stopService(getContext(), GeolocationService.class);
        }
    }

    @Override
    public boolean pause() {
        new AlarmReceiver().cancelPositionDelay(getContext(), getReminder().getUniqueId());
        stopTracking(true);
        return true;
    }

    @Override
    public boolean skip() {
        return false;
    }

    @Override
    public boolean resume() {
        if (getReminder().isActive()) {
            boolean b = new AlarmReceiver().enablePositionDelay(getContext(), getReminder().getUuId());
            if (!b) SuperUtil.startGpsTracking(getContext());
        }
        return true;
    }

    @Override
    public boolean next() {
        return stop();
    }

    @Override
    public boolean onOff() {
        if (isActive()) {
            return stop();
        } else {
            getReminder().setLocked(false).setNotificationShown(false);
            super.save();
            return start();
        }
    }

    @Override
    public boolean isActive() {
        return getReminder().isActive();
    }

    @Override
    public boolean canSkip() {
        return false;
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public void setDelay(int delay) {

    }

    @Override
    public long calculateTime(boolean isNew) {
        return TimeCount.getInstance(getContext()).generateDateTime(getReminder().getEventTime(), getReminder().getRepeatInterval());
    }
}
