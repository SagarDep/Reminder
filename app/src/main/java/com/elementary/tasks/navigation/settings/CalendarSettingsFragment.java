package com.elementary.tasks.navigation.settings;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.elementary.tasks.R;
import com.elementary.tasks.core.utils.Prefs;
import com.elementary.tasks.core.utils.ThemeUtil;
import com.elementary.tasks.databinding.FragmentCalendarSettingsBinding;
import com.elementary.tasks.navigation.settings.calendar.FragmentBirthdaysColor;
import com.elementary.tasks.navigation.settings.calendar.FragmentEventsImport;
import com.elementary.tasks.navigation.settings.calendar.FragmentRemindersColor;
import com.elementary.tasks.navigation.settings.calendar.FragmentTodayColor;

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

public class CalendarSettingsFragment extends BaseSettingsFragment {

    private FragmentCalendarSettingsBinding binding;
    private int mItemSelect;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCalendarSettingsBinding.inflate(inflater, container, false);
        initBackgroundPrefs();
        initFuturePrefs();
        initRemindersPrefs();
        initFirstDayPrefs();
        binding.eventsImportPrefs.setOnClickListener(view -> replaceFragment(new FragmentEventsImport(), getString(R.string.import_events)));
        return binding.getRoot();
    }

    private void initFirstDayPrefs() {
        binding.startDayPrefs.setOnClickListener(view -> showFirstDayDialog());
    }

    private void showFirstDayDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setCancelable(true);
        builder.setTitle(mContext.getString(R.string.first_day));
        String[] items = {mContext.getString(R.string.sunday),
                mContext.getString(R.string.monday)};
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(mContext,
                android.R.layout.simple_list_item_single_choice, items);
        mItemSelect = Prefs.getInstance(mContext).getStartDay();
        builder.setSingleChoiceItems(adapter, mItemSelect, (dialog, which) -> {
            mItemSelect = which;
        });
        builder.setPositiveButton(mContext.getString(R.string.ok), (dialogInterface, i) -> {
            Prefs.getInstance(mContext).setStartDay(mItemSelect);
            dialogInterface.dismiss();
        });
        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(dialogInterface -> mItemSelect = 0);
        dialog.setOnDismissListener(dialogInterface -> mItemSelect = 0);
        dialog.show();
    }

    private void initRemindersColorPrefs() {
        binding.reminderColorPrefs.setDependentView(binding.reminderInCalendarPrefs);
        binding.reminderColorPrefs.setOnClickListener(view -> replaceFragment(new FragmentRemindersColor(), getString(R.string.reminders_color)));
        binding.reminderColorPrefs.setViewResource(ThemeUtil.getInstance(mContext).getIndicator(Prefs.getInstance(mContext).getReminderColor()));
    }

    private void initRemindersPrefs() {
        binding.reminderInCalendarPrefs.setChecked(Prefs.getInstance(mContext).isRemindersInCalendarEnabled());
        binding.reminderInCalendarPrefs.setOnClickListener(view -> changeRemindersPrefs());
    }

    private void changeRemindersPrefs() {
        boolean isChecked = binding.reminderInCalendarPrefs.isChecked();
        binding.reminderInCalendarPrefs.setChecked(!isChecked);
        Prefs.getInstance(mContext).setRemindersInCalendarEnabled(!isChecked);
    }

    private void initFuturePrefs() {
        binding.featureRemindersPrefs.setChecked(Prefs.getInstance(mContext).isFutureEventEnabled());
        binding.featureRemindersPrefs.setOnClickListener(view -> changeFuturePrefs());
    }

    private void changeFuturePrefs() {
        boolean isChecked = binding.featureRemindersPrefs.isChecked();
        binding.featureRemindersPrefs.setChecked(!isChecked);
        Prefs.getInstance(mContext).setFutureEventEnabled(!isChecked);
    }

    private void initBackgroundPrefs() {
        binding.bgImagePrefs.setChecked(Prefs.getInstance(mContext).isCalendarImagesEnabled());
        binding.bgImagePrefs.setOnClickListener(view -> changeBackgroundPrefs());
    }

    private void changeBackgroundPrefs() {
        boolean isChecked = binding.bgImagePrefs.isChecked();
        binding.bgImagePrefs.setChecked(!isChecked);
        Prefs.getInstance(mContext).setCalendarImagesEnabled(!isChecked);
    }

    @Override
    public void onResume() {
        super.onResume();
        initRemindersColorPrefs();
        initTodayColorPrefs();
        initBirthdaysColorPrefs();
        if (mCallback != null) {
            mCallback.onTitleChange(getString(R.string.calendar));
            mCallback.onFragmentSelect(this);
        }
    }

    private void initBirthdaysColorPrefs() {
        binding.selectedColorPrefs.setOnClickListener(view -> replaceFragment(new FragmentBirthdaysColor(), getString(R.string.birthdays_color)));
        binding.selectedColorPrefs.setViewResource(ThemeUtil.getInstance(mContext).getIndicator(Prefs.getInstance(mContext).getBirthdayColor()));
    }

    private void initTodayColorPrefs() {
        binding.themeColorPrefs.setOnClickListener(view -> replaceFragment(new FragmentTodayColor(), getString(R.string.today_color)));
        binding.themeColorPrefs.setViewResource(ThemeUtil.getInstance(mContext).getIndicator(Prefs.getInstance(mContext).getTodayColor()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {

        }
    }
}