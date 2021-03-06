package com.elementary.tasks.creators.fragments;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TimePicker;
import android.widget.Toast;

import com.elementary.tasks.R;
import com.elementary.tasks.core.controller.EventControl;
import com.elementary.tasks.core.controller.EventControlFactory;
import com.elementary.tasks.core.utils.Constants;
import com.elementary.tasks.core.utils.IntervalUtil;
import com.elementary.tasks.core.utils.LogUtil;
import com.elementary.tasks.core.utils.Permissions;
import com.elementary.tasks.core.utils.Prefs;
import com.elementary.tasks.core.utils.SuperUtil;
import com.elementary.tasks.core.utils.ThemeUtil;
import com.elementary.tasks.core.utils.TimeCount;
import com.elementary.tasks.core.utils.TimeUtil;
import com.elementary.tasks.core.views.ActionView;
import com.elementary.tasks.databinding.FragmentWeekdaysBinding;
import com.elementary.tasks.reminder.models.Reminder;

import java.util.Calendar;
import java.util.Date;
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

public class WeekFragment extends RepeatableTypeFragment {

    private static final String TAG = "WeekFragment";
    private static final int CONTACTS = 112;

    protected int mHour = 0;
    protected int mMinute = 0;

    private FragmentWeekdaysBinding binding;
    private ActionView.OnActionListener mActionListener = new ActionView.OnActionListener() {
        @Override
        public void onActionChange(boolean hasAction) {
            if (!hasAction) {
                getInterface().setEventHint(getString(R.string.remind_me));
                getInterface().setHasAutoExtra(false, null);
            }
        }

        @Override
        public void onTypeChange(boolean isMessageType) {
            if (isMessageType) {
                getInterface().setEventHint(getString(R.string.message));
                getInterface().setHasAutoExtra(true, getString(R.string.enable_sending_sms_automatically));
            } else {
                getInterface().setEventHint(getString(R.string.remind_me));
                getInterface().setHasAutoExtra(true, getString(R.string.enable_making_phone_calls_automatically));
            }
        }
    };
    private TimePickerDialog.OnTimeSetListener mTimeSelect = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker timePicker, int hourOfDay, int minute) {
            mHour = hourOfDay;
            mMinute = minute;
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);
            String formattedTime = TimeUtil.getTime(c.getTime(), Prefs.getInstance(getActivity()).is24HourFormatEnabled());
            binding.timeField.setText(formattedTime);
        }
    };
    public View.OnClickListener timeClick = v -> TimeUtil.showTimePicker(getActivity(), mTimeSelect, mHour, mMinute);

    public WeekFragment() {
    }

    @Override
    public boolean save() {
        if (getInterface() == null) return false;
        Reminder reminder = getInterface().getReminder();
        int type = Reminder.BY_WEEK;
        boolean isAction = binding.actionView.hasAction();
        if (TextUtils.isEmpty(getInterface().getSummary()) && !isAction) {
            getInterface().showSnackbar(getString(R.string.task_summary_is_empty));
            return false;
        }
        String number = null;
        if (isAction) {
            number = binding.actionView.getNumber();
            if (TextUtils.isEmpty(number)) {
                getInterface().showSnackbar(getString(R.string.you_dont_insert_number));
                return false;
            }
            if (binding.actionView.getType() == ActionView.TYPE_CALL) {
                type = Reminder.BY_WEEK_CALL;
            } else {
                type = Reminder.BY_WEEK_SMS;
            }
        }
        List<Integer> weekdays = getDays();
        if (!IntervalUtil.isWeekday(weekdays)) {
            Toast.makeText(getContext(), getString(R.string.you_dont_select_any_day), Toast.LENGTH_SHORT).show();
            return false;
        }
        if (reminder == null) {
            reminder = new Reminder();
        }
        reminder.setWeekdays(weekdays);
        reminder.setTarget(number);
        reminder.setType(type);
        reminder.setRepeatInterval(0);
        reminder.setExportToCalendar(binding.exportToCalendar.isChecked());
        reminder.setExportToTasks(binding.exportToTasks.isChecked());
        reminder.setClear(getInterface());
        reminder.setEventTime(TimeUtil.getGmtFromDateTime(getTime()));
        long startTime = TimeCount.getInstance(getContext()).getNextWeekdayTime(reminder);
        reminder.setStartTime(TimeUtil.getGmtFromDateTime(startTime));
        reminder.setEventTime(TimeUtil.getGmtFromDateTime(startTime));
        LogUtil.d(TAG, "EVENT_TIME " + TimeUtil.getFullDateTime(startTime, true, true));
        if (!TimeCount.isCurrent(reminder.getEventTime())) {
            Toast.makeText(getContext(), R.string.reminder_is_outdated, Toast.LENGTH_SHORT).show();
            return false;
        }
        EventControl control = EventControlFactory.getController(getContext(), reminder);
        return control.start();
    }

    private long getTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.HOUR_OF_DAY, mHour);
        calendar.set(Calendar.MINUTE, mMinute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.fragment_date_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_limit:
                changeLimit();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentWeekdaysBinding.inflate(inflater, container, false);
        binding.timeField.setOnClickListener(timeClick);
        binding.timeField.setText(TimeUtil.getTime(updateTime(System.currentTimeMillis()),
                Prefs.getInstance(getActivity()).is24HourFormatEnabled()));
        binding.actionView.setListener(mActionListener);
        binding.actionView.setActivity(getActivity());
        binding.actionView.setContactClickListener(view -> selectContact());
        setToggleTheme();
        if (getInterface().isExportToCalendar()) {
            binding.exportToCalendar.setVisibility(View.VISIBLE);
        } else {
            binding.exportToCalendar.setVisibility(View.GONE);
        }
        if (getInterface().isExportToTasks()) {
            binding.exportToTasks.setVisibility(View.VISIBLE);
        } else {
            binding.exportToTasks.setVisibility(View.GONE);
        }
        editReminder();
        return binding.getRoot();
    }

    private void setToggleTheme() {
        ThemeUtil cs = ThemeUtil.getInstance(getContext());
        binding.mondayCheck.setBackgroundDrawable(cs.toggleDrawable());
        binding.tuesdayCheck.setBackgroundDrawable(cs.toggleDrawable());
        binding.wednesdayCheck.setBackgroundDrawable(cs.toggleDrawable());
        binding.thursdayCheck.setBackgroundDrawable(cs.toggleDrawable());
        binding.fridayCheck.setBackgroundDrawable(cs.toggleDrawable());
        binding.saturdayCheck.setBackgroundDrawable(cs.toggleDrawable());
        binding.sundayCheck.setBackgroundDrawable(cs.toggleDrawable());
    }

    protected Date updateTime(long millis) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        mHour = cal.get(Calendar.HOUR_OF_DAY);
        mMinute = cal.get(Calendar.MINUTE);
        return cal.getTime();
    }

    private void setCheckForDays(List<Integer> weekdays) {
        if (weekdays.get(0) == 1) {
            binding.sundayCheck.setChecked(true);
        } else binding.sundayCheck.setChecked(false);
        if (weekdays.get(1) == 1) {
            binding.mondayCheck.setChecked(true);
        } else binding.mondayCheck.setChecked(false);

        if (weekdays.get(2) == 1) {
            binding.tuesdayCheck.setChecked(true);
        } else binding.tuesdayCheck.setChecked(false);

        if (weekdays.get(3) == 1) {
            binding.wednesdayCheck.setChecked(true);
        } else binding.wednesdayCheck.setChecked(false);

        if (weekdays.get(4) == 1) {
            binding.thursdayCheck.setChecked(true);
        } else binding.thursdayCheck.setChecked(false);

        if (weekdays.get(5) == 1) {
            binding.fridayCheck.setChecked(true);
        } else binding.fridayCheck.setChecked(false);

        if (weekdays.get(6) == 1) {
            binding.saturdayCheck.setChecked(true);
        } else binding.saturdayCheck.setChecked(false);
    }

    public List<Integer> getDays() {
        return IntervalUtil.getWeekRepeat(binding.mondayCheck.isChecked(),
                binding.tuesdayCheck.isChecked(), binding.wednesdayCheck.isChecked(),
                binding.thursdayCheck.isChecked(), binding.fridayCheck.isChecked(),
                binding.saturdayCheck.isChecked(), binding.sundayCheck.isChecked());
    }

    private void editReminder() {
        if (getInterface().getReminder() == null) return;
        Reminder reminder = getInterface().getReminder();
        binding.exportToCalendar.setChecked(reminder.isExportToCalendar());
        binding.exportToTasks.setChecked(reminder.isExportToTasks());
        binding.timeField.setText(TimeUtil.getTime(updateTime(TimeUtil.getDateTimeFromGmt(reminder.getEventTime())),
                Prefs.getInstance(getActivity()).is24HourFormatEnabled()));
        if (reminder.getWeekdays() != null && reminder.getWeekdays().size() > 0) {
            setCheckForDays(reminder.getWeekdays());
        }
        if (reminder.getTarget() != null) {
            binding.actionView.setAction(true);
            binding.actionView.setNumber(reminder.getTarget());
            if (Reminder.isKind(reminder.getType(), Reminder.Kind.CALL)) {
                binding.actionView.setType(ActionView.TYPE_CALL);
            } else if (Reminder.isKind(reminder.getType(), Reminder.Kind.SMS)) {
                binding.actionView.setType(ActionView.TYPE_MESSAGE);
            }
        }
    }

    private void selectContact() {
        if (Permissions.checkPermission(getActivity(), Permissions.READ_CONTACTS, Permissions.READ_CALLS)) {
            SuperUtil.selectContact(getActivity(), Constants.REQUEST_CODE_CONTACTS);
        } else {
            Permissions.requestPermission(getActivity(), CONTACTS, Permissions.READ_CONTACTS, Permissions.READ_CALLS);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_CONTACTS && resultCode == Activity.RESULT_OK) {
            String number = data.getStringExtra(Constants.SELECTED_CONTACT_NUMBER);
            binding.actionView.setNumber(number);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CONTACTS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectContact();
                }
                break;
            case DateFragment.CONTACTS_ACTION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    binding.actionView.setAction(true);
                }
                break;
        }
    }
}
