package com.elementary.tasks.birthdays;

import android.app.AlarmManager;
import android.content.Context;

import com.elementary.tasks.core.utils.Configs;
import com.elementary.tasks.core.utils.RealmDb;
import com.elementary.tasks.core.utils.ThemeUtil;
import com.elementary.tasks.core.utils.TimeCount;
import com.elementary.tasks.core.utils.TimeUtil;
import com.elementary.tasks.groups.GroupItem;
import com.elementary.tasks.reminder.models.Reminder;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

public class DayViewProvider {

    private List<EventsItem> data = new ArrayList<>();
    private int hour, minute;
    private boolean isFeature;
    private boolean isBirthdays;
    private boolean isReminders;
    private Context mContext;

    public DayViewProvider(Context mContext) {
        this.mContext = mContext;
        data = new ArrayList<>();
    }

    public void setBirthdays(boolean isBirthdays) {
        this.isBirthdays = isBirthdays;
    }

    public void setReminders(boolean isReminders) {
        this.isReminders = isReminders;
    }

    public void setTime(long time) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        this.hour = calendar.get(Calendar.HOUR_OF_DAY);
        this.minute = calendar.get(Calendar.MINUTE);
    }

    public void setFeature(boolean isFeature) {
        this.isFeature = isFeature;
    }

    public List<EventsItem> getData() {
        return data;
    }

    public List<EventsItem> getMatches(int day, int month, int year, boolean sort) {
        List<EventsItem> res = new ArrayList<>();
        for (EventsItem item : data) {
            int mDay = item.getDay();
            int mMonth = item.getMonth();
            int mYear = item.getYear();
            int type = item.getViewType();
            if (type == AdapterItem.BIRTHDAY && mDay == day && mMonth == month) {
                res.add(item);
            } else {
                if (mDay == day && mMonth == month && mYear == year) {
                    res.add(item);
                }
            }
        }
        if (!sort) return res;
        Collections.sort(res, (eventsItem, t1) -> {
            long time1 = 0, time2 = 0;
            if (eventsItem.getObject() instanceof BirthdayItem) {
                BirthdayItem item = (BirthdayItem) eventsItem.getObject();
                TimeUtil.DateItem dateItem = TimeUtil.getFutureBirthdayDate(mContext, item.getDate());
                if (dateItem != null) {
                    Calendar calendar = dateItem.getCalendar();
                    time1 = calendar.getTimeInMillis();
                }
            } else if (eventsItem.getObject() instanceof Reminder) {
                Reminder reminder = (Reminder) eventsItem.getObject();
                time1 = TimeUtil.getDateTimeFromGmt(reminder.getEventTime());
            }
            if (t1.getObject() instanceof BirthdayItem) {
                BirthdayItem item = (BirthdayItem) t1.getObject();
                TimeUtil.DateItem dateItem = TimeUtil.getFutureBirthdayDate(mContext, item.getDate());
                if (dateItem != null) {
                    Calendar calendar = dateItem.getCalendar();
                    time2 = calendar.getTimeInMillis();
                }
            } else if (t1.getObject() instanceof Reminder) {
                Reminder reminder = (Reminder) t1.getObject();
                time2 = TimeUtil.getDateTimeFromGmt(reminder.getEventTime());
            }
            return (int) (time1 - time2);
        });
        return res;
    }

    public void fillArray() {
        if (isBirthdays) {
            loadBirthdays();
        }
        if (isReminders) {
            loadReminders();
        }
    }

    private void loadBirthdays() {
        List<BirthdayItem> list = RealmDb.getInstance().getAllBirthdays();
        ThemeUtil cs = ThemeUtil.getInstance(mContext);
        int color = cs.getColor(cs.colorBirthdayCalendar());
        for (BirthdayItem item : list) {
            Date date = null;
            try {
                date = CheckBirthdaysAsync.DATE_FORMAT.parse(item.getDate());
            } catch (ParseException e) {
                e.printStackTrace();
            }
            if (date != null) {
                Calendar calendar1 = Calendar.getInstance();
                calendar1.setTime(date);
                int bDay = calendar1.get(Calendar.DAY_OF_MONTH);
                int bMonth = calendar1.get(Calendar.MONTH);
                int bYear = calendar1.get(Calendar.YEAR);
                calendar1.setTimeInMillis(System.currentTimeMillis());
                calendar1.set(Calendar.MONTH, bMonth);
                calendar1.set(Calendar.DAY_OF_MONTH, bDay);
                calendar1.set(Calendar.HOUR_OF_DAY, hour);
                calendar1.set(Calendar.MINUTE, minute);
                data.add(new EventsItem(AdapterItem.BIRTHDAY, item, bDay, bMonth, bYear, color));
            }
        }
    }

    private void loadReminders() {
        List<GroupItem> allGroups = RealmDb.getInstance().getAllGroups();
        Map<String, Integer> map = new HashMap<>();
        for (GroupItem item : allGroups) {
            map.put(item.getUuId(), item.getColor());
        }
        List<Reminder> reminders = RealmDb.getInstance().getEnabledReminders();
        for (Reminder item : reminders) {
            int mType = item.getType();
            long eventTime = item.getDateTime();
            if (!Reminder.isGpsType(mType)) {
                long repeatTime = item.getRepeatInterval();
                long limit = item.getRepeatLimit();
                long count = item.getEventCount();
                boolean isLimited = limit > 0;
                int color = 0;
                if (map.containsKey(item.getGroupUuId())) {
                    color = map.get(item.getGroupUuId());
                }
                Calendar calendar1 = Calendar.getInstance();
                calendar1.setTimeInMillis(eventTime);
                int mDay = calendar1.get(Calendar.DAY_OF_MONTH);
                int mMonth = calendar1.get(Calendar.MONTH);
                int mYear = calendar1.get(Calendar.YEAR);
                if (eventTime > 0) {
                    data.add(new EventsItem(item.getViewType(), item, mDay, mMonth, mYear, color));
                } else {
                    continue;
                }
                if (isFeature) {
                    calendar1.setTimeInMillis(item.getStartDateTime());
                    if (Reminder.isBase(mType, Reminder.BY_WEEK)) {
                        long days = 0;
                        long max = Configs.MAX_DAYS_COUNT;
                        if (isLimited) {
                            max = limit - count;
                        }
                        List<Integer> list = item.getWeekdays();
                        do {
                            calendar1.setTimeInMillis(calendar1.getTimeInMillis() +
                                    AlarmManager.INTERVAL_DAY);
                            eventTime = calendar1.getTimeInMillis();
                            if (eventTime == item.getDateTime()) {
                                continue;
                            }
                            int weekDay = calendar1.get(Calendar.DAY_OF_WEEK);
                            if (list.get(weekDay - 1) == 1 && eventTime > 0) {
                                mDay = calendar1.get(Calendar.DAY_OF_MONTH);
                                mMonth = calendar1.get(Calendar.MONTH);
                                mYear = calendar1.get(Calendar.YEAR);
                                days++;
                                data.add(new EventsItem(item.getViewType(),
                                        new Reminder(item, true).setEventTime(TimeUtil.getGmtFromDateTime(eventTime)),
                                        mDay, mMonth, mYear, color));
                            }
                        } while (days < max);
                    } else if (Reminder.isBase(mType, Reminder.BY_MONTH)) {
                        long days = 0;
                        long max = Configs.MAX_DAYS_COUNT;
                        if (isLimited) {
                            max = limit - count;
                        }
                        do {
                            eventTime = TimeCount.getInstance(mContext).getNextMonthDayTime(item);
                            calendar1.setTimeInMillis(eventTime);
                            if (eventTime == item.getDateTime()) {
                                continue;
                            }
                            mDay = calendar1.get(Calendar.DAY_OF_MONTH);
                            mMonth = calendar1.get(Calendar.MONTH);
                            mYear = calendar1.get(Calendar.YEAR);
                            if (eventTime > 0) {
                                days++;
                                data.add(new EventsItem(item.getViewType(),
                                        new Reminder(item, true).setEventTime(TimeUtil.getGmtFromDateTime(eventTime)),
                                        mDay, mMonth, mYear, color));
                            }
                        } while (days < max);
                    } else {
                        if (repeatTime == 0) {
                            continue;
                        }
                        long days = 0;
                        long max = Configs.MAX_DAYS_COUNT;
                        if (isLimited) {
                            max = limit - count;
                        }
                        do {
                            calendar1.setTimeInMillis(calendar1.getTimeInMillis() + repeatTime);
                            eventTime = calendar1.getTimeInMillis();
                            if (eventTime == item.getDateTime()) {
                                continue;
                            }
                            mDay = calendar1.get(Calendar.DAY_OF_MONTH);
                            mMonth = calendar1.get(Calendar.MONTH);
                            mYear = calendar1.get(Calendar.YEAR);
                            if (eventTime > 0) {
                                days++;
                                data.add(new EventsItem(item.getViewType(),
                                        new Reminder(item, true).setEventTime(TimeUtil.getGmtFromDateTime(eventTime)),
                                        mDay, mMonth, mYear, color));
                            }
                        } while (days < max);
                    }
                }
            }
        }
    }
}
