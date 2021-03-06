package com.elementary.tasks.notes;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.elementary.tasks.R;
import com.elementary.tasks.core.ThemedActivity;
import com.elementary.tasks.core.app_widgets.UpdatesHelper;
import com.elementary.tasks.core.controller.EventControl;
import com.elementary.tasks.core.controller.EventControlFactory;
import com.elementary.tasks.core.utils.AssetsUtil;
import com.elementary.tasks.core.utils.BackupTool;
import com.elementary.tasks.core.utils.BitmapUtils;
import com.elementary.tasks.core.utils.Constants;
import com.elementary.tasks.core.utils.Dialogues;
import com.elementary.tasks.core.utils.LogUtil;
import com.elementary.tasks.core.utils.Module;
import com.elementary.tasks.core.utils.Permissions;
import com.elementary.tasks.core.utils.RealmDb;
import com.elementary.tasks.core.utils.SuperUtil;
import com.elementary.tasks.core.utils.TelephonyUtil;
import com.elementary.tasks.core.utils.TimeCount;
import com.elementary.tasks.core.utils.TimeUtil;
import com.elementary.tasks.core.utils.ViewUtils;
import com.elementary.tasks.core.views.ColorPickerView;
import com.elementary.tasks.core.views.roboto.RoboTextView;
import com.elementary.tasks.databinding.ActivityCreateNoteBinding;
import com.elementary.tasks.databinding.DialogColorPickerLayoutBinding;
import com.elementary.tasks.groups.GroupItem;
import com.elementary.tasks.navigation.settings.images.GridMarginDecoration;
import com.elementary.tasks.notes.editor.ImageEditActivity;
import com.elementary.tasks.reminder.models.Reminder;
import com.tapadoo.alerter.Alert;
import com.tapadoo.alerter.Alerter;

import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

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

public class CreateNoteActivity extends ThemedActivity {

    private static final String TAG = "CreateNoteActivity";
    public static final int MENU_ITEM_DELETE = 12;
    private static final int REQUEST_SD_CARD = 1112;
    private static final int EDIT_CODE = 11223;
    private static final int AUDIO_CODE = 255000;

    private int mHour = 0;
    private int mMinute = 0;
    private int mYear = 0;
    private int mMonth = 0;
    private int mDay = 1;
    private int mColor = 0;
    private int mFontStyle = 9;
    private Uri mImageUri;
    private int mEditPosition = -1;
    private float mLastX = -1;

    private RelativeLayout layoutContainer;
    private LinearLayout remindContainer;
    private RoboTextView remindDate, remindTime;

    private ActivityCreateNoteBinding binding;
    @Nullable
    private ImagesGridAdapter mAdapter;
    @Nullable
    private ProgressDialog mProgress;

    @Nullable
    private NoteItem mItem;
    @Nullable
    private Reminder mReminder;
    private AppBarLayout toolbar;
    private EditText taskField;

    @Nullable
    private SpeechRecognizer speech = null;
    @Nullable
    private Alert mAlerter;

    private RecognitionListener mRecognitionListener = new RecognitionListener() {
        @Override
        public void onReadyForSpeech(Bundle bundle) {
            LogUtil.d(TAG, "onReadyForSpeech: ");
        }

        @Override
        public void onBeginningOfSpeech() {
            LogUtil.d(TAG, "onBeginningOfSpeech: ");
            showRecording();
        }

        @Override
        public void onRmsChanged(float v) {
            v = v * 2000;
            double db = 0;
            if (v > 1) {
                db = 20 * Math.log10(v);
            }
            binding.recordingView.setVolume((float) db);
        }

        @Override
        public void onBufferReceived(byte[] bytes) {
            LogUtil.d(TAG, "onBufferReceived: " + Arrays.toString(bytes));
        }

        @Override
        public void onEndOfSpeech() {
            hideRecording();
            LogUtil.d(TAG, "onEndOfSpeech: ");
        }

        @Override
        public void onError(int i) {
            LogUtil.d(TAG, "onError: " + i);
            releaseSpeech();
            hideRecording();
        }

        @Override
        public void onResults(Bundle bundle) {
            ArrayList res = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (res != null && res.size() > 0) {
                setText(StringUtils.capitalize(res.get(0).toString().toLowerCase()));
            }
            LogUtil.d(TAG, "onResults: " + res);
            releaseSpeech();
        }

        @Override
        public void onPartialResults(Bundle bundle) {
            ArrayList res = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (res != null && res.size() > 0) {
                setText(res.get(0).toString().toLowerCase());
            }
            LogUtil.d(TAG, "onPartialResults: " + res);
        }

        @Override
        public void onEvent(int i, Bundle bundle) {
            LogUtil.d(TAG, "onEvent: ");
        }
    };

    private void setText(String text) {
        binding.taskMessage.setText(text);
        binding.taskMessage.setSelection(binding.taskMessage.getText().length());
    }

    private void showRecording() {
        binding.recordingView.start();
        binding.recordingView.setVisibility(View.VISIBLE);
    }

    private void hideRecording() {
        binding.recordingView.stop();
        binding.recordingView.setVisibility(View.GONE);
    }

    private DecodeImagesAsync.DecodeListener mDecodeCallback = new DecodeImagesAsync.DecodeListener() {
        @Override
        public void onDecode(List<NoteImage> result) {
            if (mAdapter != null && !result.isEmpty()) {
                mAdapter.addNextImages(result);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_create_note);
        initActionBar();
        initMenu();
        initBgContainer();
        remindContainer = binding.remindContainer;
        ViewUtils.fadeInAnimation(layoutContainer);
        remindDate = binding.remindDate;
        remindDate.setOnClickListener(v -> dateDialog());
        remindTime = binding.remindTime;
        remindTime.setOnClickListener(v -> timeDialog());
        binding.micButton.setOnClickListener(v -> micClick());
        binding.discardReminder.setOnClickListener(v -> ViewUtils.collapse(remindContainer));
        initImagesList();
        loadNote();
        if (mItem != null) {
            mColor = mItem.getColor();
            mFontStyle = mItem.getStyle();
            setText(mItem.getSummary());
            if (mAdapter != null) mAdapter.setImages(mItem.getImages());
            showReminder();
        } else {
            mColor = new Random().nextInt(16);
            if (getPrefs().isNoteColorRememberingEnabled()) {
                mColor = getPrefs().getLastNoteColor();
            }
        }
        updateBackground();
        updateTextStyle();
        showSaturationAlert();
    }

    private void initRecognizer() {
        Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(mRecognitionListener);
        speech.startListening(recognizerIntent);
    }

    private void releaseSpeech() {
        try {
            if (speech != null) {
                speech.stopListening();
                speech.cancel();
                speech.destroy();
                speech = null;
            }
        } catch (IllegalArgumentException ignored) {

        }
    }

    private void micClick() {
        if (speech != null) {
            hideRecording();
            releaseSpeech();
            return;
        }
        if (!Permissions.checkPermission(this, Permissions.RECORD_AUDIO)) {
            Permissions.requestPermission(this, AUDIO_CODE, Permissions.RECORD_AUDIO);
            return;
        }
        initRecognizer();
    }

    private void showSaturationAlert() {
        if (getPrefs().isNoteHintShowed()) {
            return;
        }
        getPrefs().setNoteHintShowed(true);
        mAlerter = Alerter.create(this)
                .setTitle(R.string.swipe_left_or_right_to_adjust_saturation)
                .setText(R.string.click_to_hide)
                .enableInfiniteDuration(true)
                .setBackgroundColorRes(getThemeUtil().colorPrimaryDark(mColor))
                .setOnClickListener(v -> {
                    if (mAlerter != null) mAlerter.hide();
                })
                .show();
    }

    private void initBgContainer() {
        layoutContainer = binding.layoutContainer;
        binding.touchView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                float x = event.getX();
                if (mLastX != -1) {
                    int currentOpacity = getPrefs().getNoteColorOpacity();
                    if (x - mLastX > 0) {
                        if (currentOpacity < 100) {
                            currentOpacity += 1;
                        }
                    } else {
                        if (currentOpacity > 0) {
                            currentOpacity -= 1;
                        }
                    }
                    getPrefs().setNoteColorOpacity(currentOpacity);
                    updateBackground();
                }
                mLastX = x;
                return true;
            }
            return false;
        });
    }

    private void initMenu() {
        binding.bottomBarView.setBackgroundColor(getThemeUtil().getBackgroundStyle());
        binding.colorButton.setOnClickListener(view -> showColorDialog());
        binding.imageButton.setOnClickListener(view -> selectImages());
        binding.reminderButton.setOnClickListener(view -> switchReminder());
        binding.fontButton.setOnClickListener(view -> showStyleDialog());
    }

    private void switchReminder() {
        if (!isReminderAttached()) {
            setDateTime(null);
            ViewUtils.expand(remindContainer);
        } else {
            ViewUtils.collapse(remindContainer);
        }
    }

    private void selectImages() {
        if (Permissions.checkPermission(this, Permissions.READ_EXTERNAL, Permissions.WRITE_EXTERNAL)) {
            getImage();
        } else {
            Permissions.requestPermission(this, REQUEST_SD_CARD, Permissions.READ_EXTERNAL, Permissions.WRITE_EXTERNAL);
        }
    }

    private void loadNote() {
        Intent intent = getIntent();
        String id = intent.getStringExtra(Constants.INTENT_ID);
        if (id != null) {
            mItem = RealmDb.getInstance().getNote(id);
        } else if (intent.getData() != null) {
            String filePath = intent.getStringExtra(Constants.FILE_PICKED);
            Uri name = intent.getData();
            loadNoteFromFile(filePath, name);
        }
    }

    private void initActionBar() {
        toolbar = binding.appBar;
        setSupportActionBar(binding.toolbar);
        taskField = binding.taskMessage;
        taskField.setTextSize(getPrefs().getNoteTextSize() + 12);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setElevation(0f);
        toolbar.setVisibility(View.VISIBLE);
    }

    private void loadNoteFromFile(String filePath, Uri name) {
        try {
            if (name != null) {
                String scheme = name.getScheme();
                if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
                    ContentResolver cr = getContentResolver();
                    mItem = BackupTool.getInstance().getNote(cr, name);
                } else {
                    mItem = BackupTool.getInstance().getNote(name.getPath(), null);
                }
            } else {
                mItem = BackupTool.getInstance().getNote(filePath, null);
            }
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void initImagesList() {
        mAdapter = new ImagesGridAdapter(this);
        binding.imagesList.setLayoutManager(new KeepLayoutManager(this, 6, mAdapter));
        binding.imagesList.addItemDecoration(new GridMarginDecoration(getResources().getDimensionPixelSize(R.dimen.grid_item_spacing)));
        binding.imagesList.setHasFixedSize(true);
        binding.imagesList.setItemAnimator(new DefaultItemAnimator());
        mAdapter.setEditable(true, this::editImage);
        binding.imagesList.setAdapter(mAdapter);
    }

    private void editImage(int position) {
        if (mAdapter != null) {
            NoteImage image = mAdapter.getItem(position);
            RealmDb.getInstance().saveImage(image);
            startActivityForResult(new Intent(this, ImageEditActivity.class), EDIT_CODE);
            this.mEditPosition = position;
        }
    }

    private void showReminder() {
        if (mItem != null) {
            mReminder = RealmDb.getInstance().getReminderByNote(mItem.getKey());
            if (mReminder != null) {
                setDateTime(mReminder.getEventTime());
                ViewUtils.expand(remindContainer);
            }
        }
    }

    private void hideProgress() {
        if (mProgress != null && mProgress.isShowing()) {
            mProgress.dismiss();
        }
    }

    private void showProgress() {
        mProgress = ProgressDialog.show(this, null, getString(R.string.please_wait), true, false);
    }

    private void shareNote() {
        createObject();
        showProgress();
        BackupTool.CreateCallback callback = this::sendNote;
        new Thread(() -> BackupTool.getInstance().createNote(mItem, callback)).start();
    }

    private void sendNote(File file) {
        hideProgress();
        if (!file.exists() || !file.canRead()) {
            Toast.makeText(this, getString(R.string.error_sending), Toast.LENGTH_SHORT).show();
            return;
        }
        if (mItem != null) {
            TelephonyUtil.sendNote(file, this, mItem.getSummary());
        }
    }

    private void setDateTime(String eventTime) {
        Calendar calendar = Calendar.getInstance();
        if (eventTime == null) calendar.setTimeInMillis(System.currentTimeMillis());
        else calendar.setTimeInMillis(TimeUtil.getDateTimeFromGmt(eventTime));
        mDay = calendar.get(Calendar.DAY_OF_MONTH);
        mMonth = calendar.get(Calendar.MONTH);
        mYear = calendar.get(Calendar.YEAR);
        mHour = calendar.get(Calendar.HOUR_OF_DAY);
        mMinute = calendar.get(Calendar.MINUTE);
        remindDate.setText(TimeUtil.getDate(calendar.getTimeInMillis()));
        remindTime.setText(TimeUtil.getTime(calendar.getTime(), getPrefs().is24HourFormatEnabled()));
    }

    private boolean isReminderAttached() {
        return remindContainer.getVisibility() == View.VISIBLE;
    }

    private boolean createObject() {
        String note = taskField.getText().toString().trim();
        List<NoteImage> images = new ArrayList<>();
        if (mAdapter != null) images = mAdapter.getImages();
        if (TextUtils.isEmpty(note) && images.isEmpty()) {
            taskField.setError(getString(R.string.must_be_not_empty));
            return false;
        }
        if (mItem == null) {
            mItem = new NoteItem();
        }
        mItem.setSummary(note);
        mItem.setDate(TimeUtil.getGmtDateTime());
        mItem.setImages(images);
        mItem.setColor(mColor);
        mItem.setStyle(mFontStyle);
        return true;
    }

    private void saveNote() {
        if (!createObject()) {
            return;
        }
        boolean hasReminder = isReminderAttached();
        if (!hasReminder && mItem != null) removeNoteFromReminder(mItem.getKey());
        RealmDb.getInstance().saveObject(mItem);
        if (hasReminder) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(mYear, mMonth, mDay, mHour, mMinute);
            createReminder(mItem.getKey(), calendar);
        }
        UpdatesHelper.getInstance(this).updateNotesWidget();
        finish();
    }

    private void createReminder(String key, Calendar calendar) {
        if (mReminder == null) {
            mReminder = new Reminder();
        }
        mReminder.setType(Reminder.BY_DATE);
        mReminder.setDelay(0);
        mReminder.setEventCount(0);
        mReminder.setUseGlobal(true);
        mReminder.setNoteId(key);
        mReminder.setActive(true);
        mReminder.setRemoved(false);
        if (mItem != null) mReminder.setSummary(mItem.getSummary());
        else mReminder.setSummary("");
        GroupItem def = RealmDb.getInstance().getDefaultGroup();
        if (def != null) {
            mReminder.setGroupUuId(def.getUuId());
        }
        long startTime = calendar.getTimeInMillis();
        mReminder.setStartTime(TimeUtil.getGmtFromDateTime(startTime));
        mReminder.setEventTime(TimeUtil.getGmtFromDateTime(startTime));
        if (!TimeCount.isCurrent(mReminder.getEventTime())) {
            Toast.makeText(this, R.string.reminder_is_outdated, Toast.LENGTH_SHORT).show();
            return;
        }
        EventControl control = EventControlFactory.getController(this, mReminder);
        if (!control.start()) {
            Toast.makeText(this, R.string.reminder_is_outdated, Toast.LENGTH_SHORT).show();
        }
    }

    private void removeNoteFromReminder(String key) {
        mReminder = RealmDb.getInstance().getReminderByNote(key);
        if (mReminder != null) {
            EventControl control = EventControlFactory.getController(this, mReminder);
            control.stop();
            RealmDb.getInstance().deleteReminder(mReminder.getUuId());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mItem != null && getPrefs().isAutoSaveEnabled()) {
            saveNote();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_share:
                shareNote();
                return true;
            case MENU_ITEM_DELETE:
                deleteDialog();
                return true;
            case R.id.action_add:
                saveNote();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showColorDialog() {
        AlertDialog.Builder builder = Dialogues.getDialog(this);
        builder.setTitle(getString(R.string.change_color));
        DialogColorPickerLayoutBinding binding = DialogColorPickerLayoutBinding.inflate(LayoutInflater.from(this));
        ColorPickerView view = binding.pickerView;
        view.setSelectedColor(mColor);
        builder.setView(binding.getRoot());
        AlertDialog dialog = builder.create();
        view.setListener(code -> {
            mColor = code;
            if (getPrefs().isNoteColorRememberingEnabled()) {
                getPrefs().setLastNoteColor(mColor);
            }
            updateBackground();
            dialog.dismiss();
        });
        dialog.show();
    }

    private void deleteDialog() {
        AlertDialog.Builder builder = Dialogues.getDialog(this);
        builder.setMessage(getString(R.string.delete_this_note));
        builder.setPositiveButton(getString(R.string.yes), (dialog, which) -> {
            dialog.dismiss();
            deleteNote();
        });
        builder.setNegativeButton(getString(R.string.no), (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void deleteNote() {
        if (mItem != null) {
            RealmDb.getInstance().deleteNote(mItem);
            new DeleteNoteFilesAsync(this).execute(mItem.getKey());
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_create_note, menu);
        if (mItem != null) {
            menu.add(Menu.NONE, MENU_ITEM_DELETE, 100, getString(R.string.delete));
        }
        return true;
    }

    private void getImage() {
        AlertDialog.Builder builder = Dialogues.getDialog(this);
        builder.setTitle(getString(R.string.image));
        builder.setItems(new CharSequence[]{getString(R.string.gallery),
                        getString(R.string.take_a_shot)},
                (dialog, which) -> {
                    switch (which) {
                        case 0: {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            if (Module.isJellyMR2()) {
                                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                            }
                            startActivityForResult(Intent.createChooser(intent, getString(R.string.image)), Constants.ACTION_REQUEST_GALLERY);
                        }
                        break;
                        case 1: {
                            ContentValues values = new ContentValues();
                            values.put(MediaStore.Images.Media.TITLE, "Picture");
                            values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
                            mImageUri = getContentResolver().insert(
                                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                            intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
                            startActivityForResult(intent, Constants.ACTION_REQUEST_CAMERA);
                        }
                        break;
                        default:
                            break;
                    }
                });
        builder.show();
    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor
                .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case Constants.ACTION_REQUEST_GALLERY:
                    getImageFromGallery(data);
                    break;
                case Constants.ACTION_REQUEST_CAMERA:
                    getImageFromCamera();
                    break;
                case EDIT_CODE:
                    if (mEditPosition != -1) updateImage();
                    break;
            }
        }
    }

    private void updateImage() {
        if (mAdapter != null) {
            NoteImage image = RealmDb.getInstance().getImage();
            mAdapter.setImage(image, mEditPosition);
        }
    }

    private void getImageFromGallery(Intent data) {
        if (data.getData() != null) {
            addImageFromUri(data.getData());
        } else if (data.getClipData() != null) {
            ClipData mClipData = data.getClipData();
            new DecodeImagesAsync(this, mDecodeCallback, mClipData.getItemCount()).execute(mClipData);
        }
    }

    private void addImageFromUri(Uri uri) {
        if (uri == null) return;
        Bitmap bitmapImage = null;
        try {
            bitmapImage = BitmapUtils.decodeUriToBitmap(this, uri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (bitmapImage != null && mAdapter != null) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            mAdapter.addImage(new NoteImage(outputStream.toByteArray()));
        }
    }

    private void getImageFromCamera() {
        addImageFromUri(mImageUri);
        if (mImageUri != null) {
            String pathFromURI = getRealPathFromURI(mImageUri);
            File file = new File(pathFromURI);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    private void updateTextStyle() {
        taskField.setTypeface(AssetsUtil.getTypeface(this, mFontStyle));
    }

    private void updateBackground() {
        layoutContainer.setBackgroundColor(getThemeUtil().getNoteLightColor(mColor));
        toolbar.setBackgroundColor(getThemeUtil().getNoteLightColor(mColor));
        if (Module.isLollipop()) {
            getWindow().setStatusBarColor(getThemeUtil().getNoteDarkColor(mColor));
        }
    }

    private void showStyleDialog() {
        AlertDialog.Builder builder = Dialogues.getDialog(this);
        builder.setTitle(getString(R.string.font_style));
        ArrayList<String> contacts = new ArrayList<>();
        contacts.clear();
        contacts.add("Black");
        contacts.add("Black Italic");
        contacts.add("Bold");
        contacts.add("Bold Italic");
        contacts.add("Italic");
        contacts.add("Light");
        contacts.add("Light Italic");
        contacts.add("Medium");
        contacts.add("Medium Italic");
        contacts.add("Regular");
        contacts.add("Thin");
        contacts.add("Thin Italic");
        final LayoutInflater inflater = LayoutInflater.from(this);
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_single_choice, contacts) {
            @NonNull
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = inflater.inflate(android.R.layout.simple_list_item_single_choice, null);
                }
                TextView textView = convertView.findViewById(android.R.id.text1);
                textView.setTypeface(getTypeface(position));
                textView.setText(contacts.get(position));
                return convertView;
            }

            private Typeface getTypeface(int position) {
                return AssetsUtil.getTypeface(CreateNoteActivity.this, position);
            }
        };
        builder.setSingleChoiceItems(adapter, mFontStyle, (dialog, which) -> {
            mFontStyle = which;
            updateTextStyle();
        });
        builder.setPositiveButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected void dateDialog() {
        TimeUtil.showDatePicker(this, myDateCallBack, mYear, mMonth, mDay);
    }

    DatePickerDialog.OnDateSetListener myDateCallBack = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear,
                              int dayOfMonth) {
            mYear = year;
            mMonth = monthOfYear;
            mDay = dayOfMonth;
            String dayStr;
            String monthStr;
            if (mDay < 10) {
                dayStr = "0" + mDay;
            } else {
                dayStr = String.valueOf(mDay);
            }
            if (mMonth < 9) {
                monthStr = "0" + (mMonth + 1);
            } else {
                monthStr = String.valueOf(mMonth + 1);
            }
            remindDate.setText(SuperUtil.appendString(dayStr, "/", monthStr, "/", String.valueOf(mYear)));
        }
    };

    protected void timeDialog() {
        TimeUtil.showTimePicker(this, myCallBack, mHour, mMinute);
    }

    TimePickerDialog.OnTimeSetListener myCallBack = new TimePickerDialog.OnTimeSetListener() {
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            mHour = hourOfDay;
            mMinute = minute;
            Calendar c = Calendar.getInstance();
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
            c.set(Calendar.MINUTE, minute);
            remindTime.setText(TimeUtil.getTime(c.getTime(), getPrefs().is24HourFormatEnabled()));
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(taskField.getWindowToken(), 0);
        releaseSpeech();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_SD_CARD:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getImage();
                }
                break;
            case AUDIO_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    micClick();
                }
                break;
        }
    }
}