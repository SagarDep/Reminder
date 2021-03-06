package com.elementary.tasks.creators.fragments;

import android.view.View;

import com.elementary.tasks.reminder.models.Reminder;

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

public interface ReminderInterface {
    Reminder getReminder();

    boolean getUseGlobal();

    boolean getVoice();

    boolean getVibration();

    boolean getNotificationRepeat();

    boolean getWake();

    boolean getUnlock();

    boolean getAuto();

    int getVolume();

    int getLedColor();

    int getRepeatLimit();

    String getGroup();

    void setRepeatLimit(int repeatLimit);

    void setEventHint(String hint);

    boolean isExportToCalendar();

    boolean isExportToTasks();

    String getSummary();

    String getAttachment();

    String getMelodyPath();

    void showSnackbar(String title);

    void showSnackbar(String title, String actionName, View.OnClickListener listener);

    void setExclusionAction(View.OnClickListener listener);

    void setRepeatAction(View.OnClickListener listener);

    void setFullScreenMode(boolean b);

    void setHasAutoExtra(boolean b, String label);
}
