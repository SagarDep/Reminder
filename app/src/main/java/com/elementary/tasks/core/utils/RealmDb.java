package com.elementary.tasks.core.utils;

import android.app.AlarmManager;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.elementary.tasks.R;
import com.elementary.tasks.birthdays.BirthdayItem;
import com.elementary.tasks.birthdays.RealmBirthdayItem;
import com.elementary.tasks.core.calendar.CalendarEvent;
import com.elementary.tasks.core.calendar.RealmCalendarEvent;
import com.elementary.tasks.core.cloud.Google;
import com.elementary.tasks.core.interfaces.RealmCallback;
import com.elementary.tasks.google_tasks.RealmTask;
import com.elementary.tasks.google_tasks.RealmTaskList;
import com.elementary.tasks.google_tasks.TaskItem;
import com.elementary.tasks.google_tasks.TaskListItem;
import com.elementary.tasks.groups.GroupItem;
import com.elementary.tasks.groups.Position;
import com.elementary.tasks.groups.RealmGroup;
import com.elementary.tasks.missed_calls.CallItem;
import com.elementary.tasks.missed_calls.RealmCallItem;
import com.elementary.tasks.navigation.settings.additional.RealmTemplate;
import com.elementary.tasks.navigation.settings.additional.TemplateItem;
import com.elementary.tasks.navigation.settings.images.ImageItem;
import com.elementary.tasks.notes.EditableRealmImage;
import com.elementary.tasks.notes.NoteImage;
import com.elementary.tasks.notes.NoteItem;
import com.elementary.tasks.notes.RealmNote;
import com.elementary.tasks.places.PlaceItem;
import com.elementary.tasks.places.RealmPlace;
import com.elementary.tasks.reminder.models.RealmReminder;
import com.elementary.tasks.reminder.models.Reminder;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
import io.realm.Sort;

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

public class RealmDb {

    private static final String TAG = "RealmDb";
    private static final SimpleDateFormat birthFormat = new SimpleDateFormat("dd|MM", Locale.getDefault());

    private static RealmDb instance;

    private RealmDb() {
    }

    public static RealmDb getInstance() {
        if (instance == null) {
            synchronized (RealmDb.class) {
                if (instance == null) instance = new RealmDb();
            }
        }
        return instance;
    }

    private Realm getRealm() {
        return Realm.getDefaultInstance();
    }

    public void saveObject(@Nullable Object o) {
        if (o == null) return;
        LogUtil.d(TAG, "saveObject: " + o);
        if (o instanceof TemplateItem) {
            saveTemplate((TemplateItem) o);
        } else if (o instanceof NoteItem) {
            saveNote((NoteItem) o);
        } else if (o instanceof PlaceItem) {
            savePlace((PlaceItem) o);
        } else if (o instanceof TaskItem) {
            saveTask((TaskItem) o);
        } else if (o instanceof TaskListItem) {
            saveTaskList((TaskListItem) o);
        } else if (o instanceof GroupItem) {
            saveGroup((GroupItem) o);
        } else if (o instanceof Reminder) {
            saveReminder((Reminder) o);
        } else if (o instanceof CalendarEvent) {
            saveCalendarEvent((CalendarEvent) o);
        } else if (o instanceof CallItem) {
            saveMissedCall((CallItem) o);
        } else if (o instanceof BirthdayItem) {
            saveBirthday((BirthdayItem) o);
        }
    }

    @NonNull
    public List<ImageItem> getImages() {
        return getRealm().where(ImageItem.class).findAll();
    }

    public void saveImages(List<ImageItem> imageItems) {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(imageItems);
        realm.commitTransaction();
    }

    public void saveImage(@NonNull NoteImage item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(new EditableRealmImage(item));
        realm.commitTransaction();
    }

    @Nullable
    public NoteImage getImage() {
        Realm realm = getRealm();
        EditableRealmImage item = realm.where(EditableRealmImage.class).equalTo("id", 0).findFirst();
        if (item != null) {
            return new NoteImage(item);
        } else {
            return null;
        }
    }

    private void saveBirthday(@NonNull BirthdayItem item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(new RealmBirthdayItem(item));
        realm.commitTransaction();
    }

    public void deleteBirthday(@NonNull BirthdayItem item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmBirthdayItem birthdayItem = realm.where(RealmBirthdayItem.class).equalTo("uuId", item.getUuId()).findFirst();
        if (birthdayItem != null) {
            birthdayItem.deleteFromRealm();
        }
        getRealm().commitTransaction();
    }

    @Nullable
    public BirthdayItem getBirthday(@NonNull String key) {
        Realm realm = getRealm();
        RealmBirthdayItem item = realm.where(RealmBirthdayItem.class).equalTo("uuId", key).findFirst();
        if (item != null) {
            return new BirthdayItem(item);
        } else {
            return null;
        }
    }

    @NonNull
    public List<BirthdayItem> getAllBirthdays() {
        Realm realm = getRealm();
        List<RealmBirthdayItem> list = new ArrayList<>(realm.where(RealmBirthdayItem.class).findAll());
        Collections.sort(list, (realmBirthdayItem, t1) -> {
            int res = realmBirthdayItem.getMonth() - t1.getMonth();
            if (res == 0) {
                res = realmBirthdayItem.getDay() - t1.getDay();
            }
            return res;
        });
        List<BirthdayItem> items = new ArrayList<>();
        for (RealmBirthdayItem item : list) {
            WeakReference<BirthdayItem> reference = new WeakReference<>(new BirthdayItem(item));
            items.add(reference.get());
        }
        return items;
    }

    @NonNull
    public List<BirthdayItem> getTodayBirthdays(int daysBefore) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        int mYear = cal.get(Calendar.YEAR);
        String mDate = birthFormat.format(cal.getTime());
        List<BirthdayItem> list = new ArrayList<>();
        for (BirthdayItem item : getAllBirthdays()) {
            int year = item.getShowedYear();
            String birthValue = getBirthdayValue(item.getMonth(), item.getDay(), daysBefore);
            if (birthValue.equals(mDate) && year != mYear) {
                list.add(item);
            }
        }
        return list;
    }

    @NonNull
    private String getBirthdayValue(int month, int day, int daysBefore) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.setTimeInMillis(calendar.getTimeInMillis() - (AlarmManager.INTERVAL_DAY * daysBefore));
        return birthFormat.format(calendar.getTime());
    }

    @NonNull
    public List<BirthdayItem> getBirthdays(int day, int month) {
        Realm realm = getRealm();
        List<RealmBirthdayItem> list = realm.where(RealmBirthdayItem.class).equalTo("dayMonth", day + "|" + month).findAll();
        List<BirthdayItem> items = new ArrayList<>();
        for (RealmBirthdayItem item : list) {
            WeakReference<BirthdayItem> reference = new WeakReference<>(new BirthdayItem(item));
            items.add(reference.get());
        }
        return items;
    }

    private void saveMissedCall(@NonNull CallItem item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(new RealmCallItem(item));
        realm.commitTransaction();
    }

    public void deleteMissedCall(@NonNull CallItem item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmCallItem callItem = realm.where(RealmCallItem.class).equalTo("number", item.getNumber()).findFirst();
        if (callItem != null) {
            callItem.deleteFromRealm();
        }
        realm.commitTransaction();
    }

    @Nullable
    public CallItem getMissedCall(String number) {
        Realm realm = getRealm();
        RealmCallItem template = realm.where(RealmCallItem.class).equalTo("number", number).findFirst();
        if (template != null) {
            return new CallItem(template);
        } else {
            return null;
        }
    }

    private void saveCalendarEvent(@NonNull CalendarEvent item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(new RealmCalendarEvent(item));
        realm.commitTransaction();
    }

    public void deleteCalendarEvent(@NonNull CalendarEvent item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmCalendarEvent event = realm.where(RealmCalendarEvent.class).equalTo("uuId", item.getUuId()).findFirst();
        if (event != null) {
            event.deleteFromRealm();
        }
        realm.commitTransaction();
    }

    @NonNull
    public List<CalendarEvent> getCalendarEvents(@NonNull String reminderId) {
        Realm realm = getRealm();
        List<RealmCalendarEvent> list = realm.where(RealmCalendarEvent.class).equalTo("reminderId", reminderId).findAll();
        List<CalendarEvent> items = new ArrayList<>();
        for (RealmCalendarEvent item : list) {
            WeakReference<CalendarEvent> reference = new WeakReference<>(new CalendarEvent(item));
            items.add(reference.get());
        }
        return items;
    }

    @NonNull
    public List<Long> getCalendarEventsIds() {
        Realm realm = getRealm();
        List<RealmCalendarEvent> list = realm.where(RealmCalendarEvent.class).findAll();
        List<Long> items = new ArrayList<>();
        for (RealmCalendarEvent item : list) {
            WeakReference<CalendarEvent> reference = new WeakReference<>(new CalendarEvent(item));
            items.add(reference.get().getEventId());
        }
        return items;
    }

    private void saveTemplate(@NonNull TemplateItem item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(new RealmTemplate(item));
        realm.commitTransaction();
    }

    public void deleteTemplates(@NonNull TemplateItem item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmTemplate template = realm.where(RealmTemplate.class).equalTo("key", item.getKey()).findFirst();
        if (template != null) {
            template.deleteFromRealm();
        }
        realm.commitTransaction();
    }

    @Nullable
    public TemplateItem getTemplate(@NonNull String id) {
        Realm realm = getRealm();
        RealmTemplate template = realm.where(RealmTemplate.class).equalTo("key", id).findFirst();
        if (template != null) {
            return new TemplateItem(template);
        } else {
            return null;
        }
    }

    @NonNull
    public List<TemplateItem> getAllTemplates() {
        Realm realm = getRealm();
        List<RealmTemplate> list = realm.where(RealmTemplate.class).findAll();
        List<TemplateItem> items = new ArrayList<>();
        for (RealmTemplate template : list) {
            WeakReference<TemplateItem> reference = new WeakReference<>(new TemplateItem(template));
            items.add(reference.get());
        }
        return items;
    }

    @NonNull
    public String setDefaultGroups(@NonNull Context context) {
        Realm realm = getRealm();
        realm.beginTransaction();
        Random random = new Random();
        GroupItem def = new GroupItem(context.getString(R.string.general), random.nextInt(16));
        realm.copyToRealmOrUpdate(new RealmGroup(def));
        realm.copyToRealmOrUpdate(new RealmGroup(new GroupItem(context.getString(R.string.work), random.nextInt(16))));
        realm.copyToRealmOrUpdate(new RealmGroup(new GroupItem(context.getString(R.string.personal), random.nextInt(16))));
        realm.commitTransaction();
        return def.getUuId();
    }

    private void saveGroup(@NonNull GroupItem item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(new RealmGroup(item));
        realm.commitTransaction();
    }

    public void deleteGroup(@NonNull GroupItem item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmGroup object = realm.where(RealmGroup.class).equalTo("uuId", item.getUuId()).findFirst();
        if (object != null) {
            RealmQuery<RealmReminder> query = realm.where(RealmReminder.class);
            query.equalTo("groupUuId", object.getUuId());
            RealmResults<RealmReminder> list = query.findAll();
            object.deleteFromRealm();
            RealmGroup realmGroup = realm.where(RealmGroup.class).findFirst();
            for (RealmReminder reminder : list) {
                reminder.setGroupUuId(realmGroup.getUuId());
            }
        }
        realm.commitTransaction();
    }

    @Nullable
    public GroupItem getGroup(@NonNull String id) {
        Realm realm = getRealm();
        RealmGroup object = realm.where(RealmGroup.class).equalTo("uuId", id).findFirst();
        if (object != null) {
            return new GroupItem(object);
        } else return null;
    }

    public void changeGroupColor(@NonNull String id, int color) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmGroup object = realm.where(RealmGroup.class).equalTo("uuId", id).findFirst();
        object.setColor(color);
        realm.commitTransaction();
    }

    @Nullable
    public GroupItem getDefaultGroup() {
        Realm realm = getRealm();
        RealmGroup realmGroup = realm.where(RealmGroup.class).findFirst();
        if (realmGroup != null) {
            return new GroupItem(realmGroup);
        }
        return null;
    }

    @NonNull
    public List<GroupItem> getAllGroups() {
        Realm realm = getRealm();
        List<RealmGroup> list = realm.where(RealmGroup.class).findAll();
        List<GroupItem> items = new ArrayList<>();
        for (RealmGroup object : list) {
            WeakReference<GroupItem> reference = new WeakReference<>(new GroupItem(object));
            items.add(reference.get());
        }
        return items;
    }

    @NonNull
    public List<String> getAllGroupsNames(List<GroupItem> items, @Nullable String uuId, @NonNull Position p) {
        Realm realm = getRealm();
        List<RealmGroup> list = realm.where(RealmGroup.class).findAll();
        for (RealmGroup object : list) {
            WeakReference<GroupItem> reference = new WeakReference<>(new GroupItem(object));
            items.add(reference.get());
        }
        if (uuId == null) uuId = "";
        List<String> names = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            GroupItem item = items.get(i);
            names.add(item.getTitle());
            if (item.getUuId() != null && item.getUuId().equals(uuId)) {
                p.setI(i);
            }
        }
        return names;
    }

    private void savePlace(@NonNull PlaceItem item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(new RealmPlace(item));
        realm.commitTransaction();
    }

    public void deletePlace(@NonNull PlaceItem item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmPlace object = realm.where(RealmPlace.class).equalTo("key", item.getKey()).findFirst();
        if (object != null) {
            object.deleteFromRealm();
        }
        realm.commitTransaction();
    }

    @Nullable
    public PlaceItem getPlace(@NonNull String id) {
        Realm realm = getRealm();
        RealmPlace object = realm.where(RealmPlace.class).equalTo("key", id).findFirst();
        if (object != null) {
            return new PlaceItem(object);
        } else {
            return null;
        }
    }

    @NonNull
    public List<PlaceItem> getAllPlaces() {
        Realm realm = getRealm();
        List<RealmPlace> list = realm.where(RealmPlace.class).findAll();
        List<PlaceItem> items = new ArrayList<>();
        for (RealmPlace object : list) {
            WeakReference<PlaceItem> reference = new WeakReference<>(new PlaceItem(object));
            items.add(reference.get());
        }
        return items;
    }

    private void saveNote(@NonNull NoteItem item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(new RealmNote(item));
        realm.commitTransaction();
    }

    public void deleteNote(@NonNull NoteItem item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmNote object = realm.where(RealmNote.class).equalTo("key", item.getKey()).findFirst();
        if (object != null) {
            object.deleteFromRealm();
        }
        realm.commitTransaction();
    }

    @Nullable
    public NoteItem getNote(@NonNull String id) {
        Realm realm = getRealm();
        RealmNote object = realm.where(RealmNote.class).equalTo("key", id).findFirst();
        if (object != null) {
            return new NoteItem(object);
        } else {
            return null;
        }
    }

    public void changeNoteColor(@NonNull String id, int color) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmNote object = realm.where(RealmNote.class).equalTo("key", id).findFirst();
        object.setColor(color);
        realm.commitTransaction();
    }

    @NonNull
    public List<NoteItem> getAllNotes(@Nullable String orderPrefs) {
        Realm realm = getRealm();
        String field = "date";
        Sort order = Sort.DESCENDING;
        if (orderPrefs != null) {
            if (orderPrefs.matches(Constants.ORDER_DATE_A_Z)) {
                field = "date";
                order = Sort.ASCENDING;
            } else if (orderPrefs.matches(Constants.ORDER_DATE_Z_A)) {
                field = "date";
                order = Sort.DESCENDING;
            } else if (orderPrefs.matches(Constants.ORDER_NAME_A_Z)) {
                field = "summary";
                order = Sort.ASCENDING;
            } else if (orderPrefs.matches(Constants.ORDER_NAME_Z_A)) {
                field = "summary";
                order = Sort.DESCENDING;
            }
        }
        List<RealmNote> list = realm.where(RealmNote.class).findAllSorted(field, order);
        List<NoteItem> items = new ArrayList<>();
        for (RealmNote object : list) {
            WeakReference<NoteItem> reference = new WeakReference<>(new NoteItem(object));
            items.add(reference.get());
        }
        return items;
    }

    private void saveTask(@NonNull TaskItem item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(new RealmTask(item));
        realm.commitTransaction();
    }

    public void deleteTask(@NonNull TaskItem item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmTask object = realm.where(RealmTask.class).equalTo("taskId", item.getTaskId()).findFirst();
        if (object != null) {
            object.deleteFromRealm();
        }
        realm.commitTransaction();
    }

    @Nullable
    public TaskItem getTaskByReminder(@NonNull String id) {
        Realm realm = getRealm();
        RealmTask object = realm.where(RealmTask.class).equalTo("uuId", id).findFirst();
        if (object == null) return null;
        else return new TaskItem(object);
    }

    @Nullable
    public TaskItem getTask(@NonNull String id) {
        Realm realm = getRealm();
        RealmTask object = realm.where(RealmTask.class).equalTo("taskId", id).findFirst();
        if (object == null) return null;
        else return new TaskItem(object);
    }

    @NonNull
    public List<TaskItem> getTasks(@Nullable String orderPrefs) {
        Realm realm = getRealm();
        String field = "position";
        Sort order = Sort.ASCENDING;
        if (orderPrefs != null) {
            if (orderPrefs.matches(Constants.ORDER_DEFAULT)) {
                field = "position";
                order = Sort.ASCENDING;
            } else if (orderPrefs.matches(Constants.ORDER_DATE_A_Z)) {
                field = "dueDate";
                order = Sort.ASCENDING;
            } else if (orderPrefs.matches(Constants.ORDER_DATE_Z_A)) {
                field = "dueDate";
                order = Sort.DESCENDING;
            } else if (orderPrefs.matches(Constants.ORDER_COMPLETED_A_Z)) {
                field = "completeDate";
                order = Sort.ASCENDING;
            } else if (orderPrefs.matches(Constants.ORDER_COMPLETED_Z_A)) {
                field = "completeDate";
                order = Sort.DESCENDING;
            }
        }
        List<RealmTask> list = realm.where(RealmTask.class).findAllSorted(field, order);
        List<TaskItem> items = new ArrayList<>();
        for (RealmTask object : list) {
            WeakReference<TaskItem> reference = new WeakReference<>(new TaskItem(object));
            items.add(reference.get());
        }
        return items;
    }

    @NonNull
    public List<TaskItem> getTasks(@NonNull String listId, @Nullable String orderPrefs) {
        Realm realm = getRealm();
        String field = "position";
        Sort order = Sort.ASCENDING;
        if (orderPrefs != null) {
            if (orderPrefs.matches(Constants.ORDER_DEFAULT)) {
                field = "position";
                order = Sort.ASCENDING;
            } else if (orderPrefs.matches(Constants.ORDER_DATE_A_Z)) {
                field = "dueDate";
                order = Sort.ASCENDING;
            } else if (orderPrefs.matches(Constants.ORDER_DATE_Z_A)) {
                field = "dueDate";
                order = Sort.DESCENDING;
            } else if (orderPrefs.matches(Constants.ORDER_COMPLETED_A_Z)) {
                field = "completeDate";
                order = Sort.ASCENDING;
            } else if (orderPrefs.matches(Constants.ORDER_COMPLETED_Z_A)) {
                field = "completeDate";
                order = Sort.DESCENDING;
            }
        }
        List<RealmTask> list = realm.where(RealmTask.class).equalTo("listId", listId).findAllSorted(field, order);
        List<TaskItem> items = new ArrayList<>();
        for (RealmTask object : list) {
            WeakReference<TaskItem> reference = new WeakReference<>(new TaskItem(object));
            items.add(reference.get());
        }
        return items;
    }

    public void deleteTasks(@NonNull String listId) {
        Realm realm = getRealm();
        realm.executeTransaction(realm1 -> {
            RealmResults<RealmTask> list = realm.where(RealmTask.class).equalTo("listId", listId).findAll();
            list.deleteAllFromRealm();
        });
    }

    public void deleteTasks() {
        Realm realm = getRealm();
        realm.executeTransaction(realm1 -> {
            RealmResults<RealmTask> list = realm.where(RealmTask.class).findAll();
            list.deleteAllFromRealm();
        });
    }

    public void deleteCompletedTasks(@NonNull String listId) {
        Realm realm = getRealm();
        realm.executeTransaction(realm1 -> {
            RealmResults<RealmTask> list = realm.where(RealmTask.class).equalTo("listId", listId).equalTo("status", Google.TASKS_COMPLETE).findAll();
            list.deleteAllFromRealm();
        });
    }

    @Nullable
    public TaskListItem getTaskList(@NonNull String listId) {
        Realm realm = getRealm();
        RealmTaskList object = realm.where(RealmTaskList.class).equalTo("listId", listId).findFirst();
        if (object == null) return null;
        else return new TaskListItem(object);
    }

    @Nullable
    public TaskListItem getDefaultTaskList() {
        Realm realm = getRealm();
        RealmTaskList object = realm.where(RealmTaskList.class).equalTo("def", 1).findFirst();
        if (object == null) return null;
        else return new TaskListItem(object);
    }

    @NonNull
    public List<TaskListItem> getTaskLists() {
        Realm realm = getRealm();
        List<RealmTaskList> list = realm.where(RealmTaskList.class).findAll();
        List<TaskListItem> items = new ArrayList<>();
        for (RealmTaskList object : list) {
            WeakReference<TaskListItem> reference = new WeakReference<>(new TaskListItem(object));
            items.add(reference.get());
        }
        return items;
    }

    public void deleteTaskLists() {
        Realm realm = getRealm();
        realm.executeTransaction(realm1 -> {
            RealmResults<RealmTaskList> list = realm.where(RealmTaskList.class).findAll();
            list.deleteAllFromRealm();
        });
    }

    public boolean deleteTaskList(@NonNull String id) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmTaskList object = realm.where(RealmTaskList.class).equalTo("listId", id).findFirst();
        if (object != null) {
            object.deleteFromRealm();
        }
        realm.commitTransaction();
        return true;
    }

    private void saveTaskList(@NonNull TaskListItem item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(new RealmTaskList(item));
        realm.commitTransaction();
    }

    public void setDefault(@NonNull String id) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmTaskList object = realm.where(RealmTaskList.class).equalTo("listId", id).findFirst();
        object.setDef(1);
        realm.commitTransaction();
    }

    public void setSystemDefault(@NonNull String id) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmTaskList object = realm.where(RealmTaskList.class).equalTo("listId", id).findFirst();
        object.setSystemDefault(1);
        realm.commitTransaction();
    }

    public void setSimple(@NonNull String id) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmTaskList object = realm.where(RealmTaskList.class).equalTo("listId", id).findFirst();
        object.setDef(0);
        realm.commitTransaction();
    }

    public void setStatus(@NonNull String id, boolean status) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmTask object = realm.where(RealmTask.class).equalTo("taskId", id).findFirst();
        if (status) {
            object.setStatus(Google.TASKS_COMPLETE);
            object.setCompleteDate(System.currentTimeMillis());
        } else {
            object.setStatus(Google.TASKS_NEED_ACTION);
            object.setCompleteDate(0);
        }
        realm.commitTransaction();
    }

    @Nullable
    public Reminder getReminder(@NonNull String id) {
        Realm realm = getRealm();
        RealmReminder object = realm.where(RealmReminder.class).equalTo("uuId", id).findFirst();
        if (object == null) return null;
        else return new Reminder(object);
    }

    @Nullable
    public Reminder getReminderByNote(@NonNull String id) {
        Realm realm = getRealm();
        RealmReminder object = realm.where(RealmReminder.class).equalTo("noteId", id).findFirst();
        if (object == null) return null;
        else return new Reminder(object);
    }

    public void changeReminderGroup(@NonNull String id, @NonNull String groupId) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmReminder object = realm.where(RealmReminder.class).equalTo("uuId", id).findFirst();
        object.setGroupUuId(groupId);
        realm.commitTransaction();
    }

    public boolean deleteReminder(@NonNull String id) {
        Realm realm = getRealm();
        realm.beginTransaction();
        RealmReminder object = realm.where(RealmReminder.class).equalTo("uuId", id).findFirst();
        if (object != null) {
            object.deleteFromRealm();
        }
        realm.commitTransaction();
        return true;
    }

    @NonNull
    public List<String> clearReminderTrash() {
        Realm realm = getRealm();
        realm.beginTransaction();
        String[] fields = new String[]{"eventTime"};
        Sort[] orders = new Sort[]{Sort.ASCENDING};
        RealmQuery<RealmReminder> query = realm.where(RealmReminder.class);
        query.equalTo("isRemoved", true);
        RealmResults<RealmReminder> list = query.findAllSorted(fields, orders);
        List<String> uids = new ArrayList<>();
        if (list != null) {
            for (RealmReminder reminder : list) {
                if (reminder != null) uids.add(reminder.getUuId());
            }
            list.deleteAllFromRealm();
        }
        realm.commitTransaction();
        return uids;
    }

    private void saveReminder(@NonNull Reminder item) {
        Realm realm = getRealm();
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(new RealmReminder(item));
        realm.commitTransaction();
    }

    void getActiveReminders(@NonNull RealmCallback<List<Reminder>> callback) {
        new Thread(() -> {
            Realm realm = getRealm();
            String[] fields = new String[]{"isActive", "eventTime"};
            Sort[] orders = new Sort[]{Sort.DESCENDING, Sort.ASCENDING};
            List<RealmReminder> list = realm.where(RealmReminder.class).equalTo("isRemoved", false).findAllSorted(fields, orders);
            List<Reminder> items = new ArrayList<>();
            for (RealmReminder object : list) {
                WeakReference<Reminder> reference = new WeakReference<>(new Reminder(object));
                items.add(reference.get());
            }
            callback.onDataLoaded(items);
        }).start();
    }

    @NonNull
    public List<Reminder> getEnabledReminders() {
        Realm realm = getRealm();
        String[] fields = new String[]{"isActive", "eventTime"};
        Sort[] orders = new Sort[]{Sort.DESCENDING, Sort.ASCENDING};
        List<RealmReminder> list = realm.where(RealmReminder.class)
                .equalTo("isActive", true)
                .equalTo("isRemoved", false)
                .findAllSorted(fields, orders);
        List<Reminder> items = new ArrayList<>();
        for (RealmReminder object : list) {
            WeakReference<Reminder> reference = new WeakReference<>(new Reminder(object));
            items.add(reference.get());
        }
        return items;
    }

    @NonNull
    public List<Reminder> getGpsReminders() {
        Realm realm = getRealm();
        List<RealmReminder> list = realm.where(RealmReminder.class).equalTo("isActive", true).equalTo("isRemoved", false).findAll();
        List<Reminder> items = new ArrayList<>();
        for (RealmReminder object : list) {
            if (Reminder.isGpsType(object.getType())) {
                WeakReference<Reminder> reference = new WeakReference<>(new Reminder(object));
                items.add(reference.get());
            }
        }
        return items;
    }

    @NonNull
    public List<Reminder> getActiveReminders() {
        Realm realm = getRealm();
        List<RealmReminder> list = realm.where(RealmReminder.class).equalTo("isRemoved", false).findAll();
        List<Reminder> items = new ArrayList<>();
        for (RealmReminder object : list) {
            WeakReference<Reminder> reference = new WeakReference<>(new Reminder(object));
            items.add(reference.get());
        }
        return items;
    }

    void getActiveReminders(@Nullable String groupId, int type, int active, @NonNull RealmCallback<List<Reminder>> callback) {
        new Thread(() -> {
            Realm realm = getRealm();
            String[] fields = new String[]{"isActive", "eventTime"};
            Sort[] orders = new Sort[]{Sort.DESCENDING, Sort.ASCENDING};
            RealmQuery<RealmReminder> query = realm.where(RealmReminder.class);
            query.equalTo("isRemoved", false);
            if (groupId != null) {
                query.equalTo("groupUuId", groupId);
            }
            if (type != 0) {
                query.equalTo("type", type);
            }
            if (active != 0) {
                query.equalTo("isActive", active == 1);
            }
            RealmResults<RealmReminder> list = query.findAllSorted(fields, orders);
            List<Reminder> items = new ArrayList<>();
            for (RealmReminder object : list) {
                WeakReference<Reminder> reference = new WeakReference<>(new Reminder(object));
                items.add(reference.get());
            }
            callback.onDataLoaded(items);
        }).start();
    }

    void getArchivedReminders(@Nullable String groupId, int type, @NonNull RealmCallback<List<Reminder>> callback) {
        new Thread(() -> {
            Realm realm = getRealm();
            String[] fields = new String[]{"eventTime"};
            Sort[] orders = new Sort[]{Sort.ASCENDING};
            RealmQuery<RealmReminder> query = realm.where(RealmReminder.class);
            query.equalTo("isRemoved", true);
            if (groupId != null) {
                query.equalTo("groupUuId", groupId);
            }
            if (type != 0) {
                query.equalTo("type", type);
            }
            RealmResults<RealmReminder> list = query.findAllSorted(fields, orders);
            List<Reminder> items = new ArrayList<>();
            for (RealmReminder object : list) {
                WeakReference<Reminder> reference = new WeakReference<>(new Reminder(object));
                items.add(reference.get());
            }
            callback.onDataLoaded(items);
        }).start();
    }

    void getArchivedReminders(@NonNull RealmCallback<List<Reminder>> callback) {
        new Thread(() -> {
            Realm realm = getRealm();
            String[] fields = new String[]{"eventTime"};
            Sort[] orders = new Sort[]{Sort.ASCENDING};
            List<RealmReminder> list = realm.where(RealmReminder.class).equalTo("isRemoved", true).findAllSorted(fields, orders);
            List<Reminder> items = new ArrayList<>();
            for (RealmReminder object : list) {
                WeakReference<Reminder> reference = new WeakReference<>(new Reminder(object));
                items.add(reference.get());
            }
            callback.onDataLoaded(items);
        }).start();
    }

    @NonNull
    public List<Reminder> getAllReminders() {
        Realm realm = getRealm();
        List<RealmReminder> list = realm.where(RealmReminder.class).findAll();
        List<Reminder> items = new ArrayList<>();
        for (RealmReminder object : list) {
            WeakReference<Reminder> reference = new WeakReference<>(new Reminder(object));
            items.add(reference.get());
        }
        return items;
    }
}