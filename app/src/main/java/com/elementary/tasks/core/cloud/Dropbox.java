package com.elementary.tasks.core.cloud;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;
import com.elementary.tasks.core.controller.EventControl;
import com.elementary.tasks.core.controller.EventControlFactory;
import com.elementary.tasks.core.utils.BackupTool;
import com.elementary.tasks.core.utils.LogUtil;
import com.elementary.tasks.core.utils.MemoryUtil;
import com.elementary.tasks.core.utils.Prefs;
import com.elementary.tasks.core.utils.RealmDb;
import com.elementary.tasks.reminder.models.Reminder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

public class Dropbox {

    private static final String TAG = "Dropbox";
    private static final String APP_KEY = "4zi1d414h0v8sxe";
    private static final String APP_SECRET = "aopehxo80oq8g5o";
    private static final String ACCOUNT_PREFS_NAME = "prefs";
    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";

    private Context mContext;

    private String dbxFolder = "Reminders/";
    private String dbxNoteFolder = "Notes/";
    private String dbxGroupFolder = "Groups/";
    private String dbxBirthFolder = "Birthdays/";
    private String dbxPlacesFolder = "Places/";
    private String dbxTemplatesFolder = "Templates/";
    private String dbxSettingsFolder = "Settings/";

    private DropboxAPI<AndroidAuthSession> mDBApi;
    private DropboxAPI.Entry newEntry;

    public Dropbox(Context context) {
        this.mContext = context;
    }

    /**
     * Start connection to Dropbox.
     */
    public void startSession() {
        AndroidAuthSession session = buildSession();
        mDBApi = new DropboxAPI<>(session);
        checkAppKeySetup();
    }

    /**
     * Check if user has already connected to Dropbox from this application.
     *
     * @return Boolean
     */
    public boolean isLinked() {
        return mDBApi != null && mDBApi.getSession().isLinked();
    }

    /**
     * Holder Dropbox user name.
     *
     * @return String user name
     */
    public String userName() {
        DropboxAPI.Account account = null;
        try {
            account = mDBApi.accountInfo();
        } catch (DropboxException e) {
            LogUtil.e(TAG, "userName: ", e);
        }
        return account != null ? account.displayName : null;
    }

    /**
     * Holder user all apace on Dropbox.
     *
     * @return Long - user quota
     */
    public long userQuota() {
        DropboxAPI.Account account = null;
        try {
            account = mDBApi.accountInfo();
        } catch (DropboxException e) {
            LogUtil.e(TAG, "userQuota: ", e);
        }
        return account != null ? account.quota : 0;
    }

    public long userQuotaNormal() {
        DropboxAPI.Account account = null;
        try {
            account = mDBApi.accountInfo();
        } catch (DropboxException e) {
            LogUtil.e(TAG, "userQuotaNormal: ", e);
        }
        return account != null ? (account.quotaNormal + account.quotaShared) : 0;
    }

    public boolean checkLink() {
        boolean isLogged = false;
        startSession();
        AndroidAuthSession session = mDBApi.getSession();
        if (session.authenticationSuccessful()) {
            try {
                session.finishAuthentication();
                storeAuth(session);
                isLogged = true;
            } catch (IllegalStateException e) {
                Toast.makeText(mContext, "Couldn't authenticate with Dropbox:" + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
            }
        }
        return isLogged;
    }

    public boolean startLink() {
        mDBApi.getSession().startOAuth2Authentication(mContext);
        return mDBApi.getSession().isLinked();
    }

    public boolean unlink() {
        boolean is = false;
        if (logOut()) {
            is = true;
        }
        return is;
    }

    public void checkAppKeySetup() {
        if (APP_KEY.startsWith("CHANGE") ||
                APP_SECRET.startsWith("CHANGE")) {
            Toast.makeText(mContext, "You must apply for an app key and secret from developers.dropbox.com, " +
                    "and add them to the DBRoulette ap before trying it.", Toast.LENGTH_SHORT).show();
            ((Activity) mContext).finish();
            return;
        }
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        String scheme = "db-" + APP_KEY;
        String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
        testIntent.setData(Uri.parse(uri));
        PackageManager pm = mContext.getPackageManager();
        if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
            Toast.makeText(mContext, "URL scheme in your app's " +
                    "manifest is not set up correctly. You should have a " +
                    "com.dropbox.client2.android.AuthActivity with the " +
                    "scheme: " + scheme, Toast.LENGTH_SHORT).show();
            ((Activity) mContext).finish();
        }
    }

    public void loadAuth(AndroidAuthSession session) {
        SharedPreferences prefs = mContext.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key == null || secret == null || key.length() == 0 || secret.length() == 0) {
            return;
        }
        if (key.equals("oauth2:")) {
            session.setOAuth2AccessToken(secret);
        } else {
            session.setAccessTokenPair(new AccessTokenPair(key, secret));
        }
    }

    public void storeAuth(AndroidAuthSession session) {
        String oauth2AccessToken = session.getOAuth2AccessToken();
        if (oauth2AccessToken != null) {
            SharedPreferences prefs = mContext.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, "oauth2:");
            edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
            edit.apply();
            return;
        }
        AccessTokenPair oauth1AccessToken = session.getAccessTokenPair();
        if (oauth1AccessToken != null) {
            SharedPreferences prefs = mContext.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, oauth1AccessToken.key);
            edit.putString(ACCESS_SECRET_NAME, oauth1AccessToken.secret);
            edit.apply();
        }
    }

    public AndroidAuthSession buildSession() {
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);
        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        loadAuth(session);
        return session;
    }

    private boolean logOut() {
        mDBApi.getSession().unlink();
        clearKeys();
        return true;
    }

    private void clearKeys() {
        SharedPreferences prefs = mContext.getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.apply();
    }

    /**
     * Upload to Dropbox folder backup files from selected folder on SD Card.
     *
     * @param path name of folder to upload.
     */
    private void upload(String path) {
        startSession();
        if (!isLinked()) {
            return;
        }
        File sdPath = Environment.getExternalStorageDirectory();
        File sdPathDr = new File(sdPath.toString() + "/JustReminder/" + path);
        File[] files = sdPathDr.listFiles();
        String fileLoc = sdPathDr.toString();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String fileLoopName = file.getName();
            File tmpFile = new File(fileLoc, fileLoopName);
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(tmpFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            String folder;
            if (path.matches(MemoryUtil.DIR_NOTES_SD)) {
                folder = dbxNoteFolder;
            } else if (path.matches(MemoryUtil.DIR_GROUP_SD)) {
                folder = dbxGroupFolder;
            } else if (path.matches(MemoryUtil.DIR_BIRTHDAY_SD)) {
                folder = dbxBirthFolder;
            } else if (path.matches(MemoryUtil.DIR_PLACES_SD)) {
                folder = dbxPlacesFolder;
            } else if (path.matches(MemoryUtil.DIR_TEMPLATES_SD)) {
                folder = dbxTemplatesFolder;
            } else {
                folder = dbxFolder;
            }
            try {
                newEntry = mDBApi.putFileOverwrite(folder + fileLoopName, fis, tmpFile.length(), null);
            } catch (DropboxUnlinkedException e) {
                LogUtil.e(TAG, "User has unlinked.", e);
            } catch (DropboxException e) {
                LogUtil.e(TAG, "Something went wrong while uploading.", e);
            }
        }
    }

    /**
     * Upload reminder backup files or selected file to Dropbox folder.
     *
     * @param fileName file name.
     */
    public void uploadReminder(final String fileName) {
        File dir = MemoryUtil.getRemindersDir();
        if (dir == null) {
            return;
        }
        startSession();
        if (!isLinked()) {
            return;
        }
        if (fileName != null) {
            File tmpFile = new File(dir.toString(), fileName);
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(tmpFile);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                newEntry = mDBApi.putFileOverwrite(dbxFolder + fileName, fis, tmpFile.length(), null);
            } catch (DropboxUnlinkedException e) {
                LogUtil.e(TAG, "User has unlinked.", e);
            } catch (DropboxException e) {
                LogUtil.e(TAG, "Something went wrong while uploading.", e);
            }
        } else {
            upload(MemoryUtil.DIR_SD);
        }
    }

    /**
     * Upload all note backup files to Dropbox folder.
     */
    public void uploadNotes() {
        upload(MemoryUtil.DIR_NOTES_SD);
    }

    /**
     * Upload all group backup files to Dropbox folder.
     */
    public void uploadGroups() {
        upload(MemoryUtil.DIR_GROUP_SD);
    }

    /**
     * Upload all birthday backup files to Dropbox folder.
     */
    public void uploadBirthdays() {
        upload(MemoryUtil.DIR_BIRTHDAY_SD);
    }

    /**
     * Upload all places backup files to Dropbox folder.
     */
    public void uploadPlaces() {
        upload(MemoryUtil.DIR_PLACES_SD);
    }

    /**
     * Upload all templates backup files to Dropbox folder.
     */
    public void uploadTemplates() {
        upload(MemoryUtil.DIR_TEMPLATES_SD);
    }

    public void deleteFile(String fileName) {
        if (fileName.endsWith(FileConfig.FILE_NAME_REMINDER)) {
            deleteReminder(fileName);
        } else if (fileName.endsWith(FileConfig.FILE_NAME_NOTE)) {
            deleteNote(fileName);
        } else if (fileName.endsWith(FileConfig.FILE_NAME_GROUP)) {
            deleteGroup(fileName);
        } else if (fileName.endsWith(FileConfig.FILE_NAME_BIRTHDAY)) {
            deleteBirthday(fileName);
        } else if (fileName.endsWith(FileConfig.FILE_NAME_PLACE)) {
            deletePlace(fileName);
        } else if (fileName.endsWith(FileConfig.FILE_NAME_TEMPLATE)) {
            deleteTemplate(fileName);
        } else if (fileName.endsWith(FileConfig.FILE_NAME_SETTINGS)) {
            deleteSettings(fileName);
        }
    }

    /**
     * Delete reminder backup file from Dropbox folder.
     *
     * @param name file name.
     */
    public void deleteReminder(String name) {
        startSession();
        if (!isLinked()) {
            return;
        }
        try {
            mDBApi.delete(dbxFolder + name);
        } catch (DropboxException e) {
            LogUtil.e(TAG, "deleteReminder: ", e);
        }
    }

    /**
     * Delete note backup file from Dropbox folder.
     *
     * @param name file name.
     */
    public void deleteNote(String name) {
        startSession();
        if (!isLinked()) {
            return;
        }
        try {
            mDBApi.delete(dbxNoteFolder + name);
        } catch (DropboxException e) {
            LogUtil.e(TAG, "deleteNote: ", e);
        }
    }

    /**
     * Delete group backup file from Dropbox folder.
     *
     * @param name file name.
     */
    public void deleteGroup(String name) {
        LogUtil.d(TAG, "deleteGroup: " + name);
        startSession();
        if (!isLinked()) {
            return;
        }
        try {
            mDBApi.delete(dbxGroupFolder + name);
        } catch (DropboxException e) {
            LogUtil.e(TAG, "deleteGroup: " + name, e);
        }
    }

    /**
     * Delete birthday backup file from Dropbox folder.
     *
     * @param name file name
     */
    public void deleteBirthday(String name) {
        startSession();
        if (!isLinked()) {
            return;
        }
        try {
            mDBApi.delete(dbxBirthFolder + name);
        } catch (DropboxException e) {
            LogUtil.e(TAG, "deleteBirthday: ", e);
        }
    }

    /**
     * Delete place backup file from Dropbox folder.
     *
     * @param name file name
     */
    public void deletePlace(String name) {
        startSession();
        if (!isLinked()) {
            return;
        }
        try {
            mDBApi.delete(dbxPlacesFolder + name);
        } catch (DropboxException e) {
            LogUtil.e(TAG, "deletePlace: ", e);
        }
    }

    /**
     * Delete place backup file from Dropbox folder.
     *
     * @param name file name
     */
    public void deleteTemplate(String name) {
        startSession();
        if (!isLinked()) {
            return;
        }
        try {
            mDBApi.delete(dbxTemplatesFolder + name);
        } catch (DropboxException e) {
            LogUtil.e(TAG, "deleteTemplate: ", e);
        }
    }

    /**
     * Delete settings backup file from Dropbox folder.
     *
     * @param name file name
     */
    public void deleteSettings(String name) {
        startSession();
        if (!isLinked()) {
            return;
        }
        try {
            mDBApi.delete(dbxSettingsFolder + name);
        } catch (DropboxException e) {
            LogUtil.e(TAG, "deleteSettings: ", e);
        }
    }

    /**
     * Delete all folders inside application folder on Dropbox.
     */
    public void cleanFolder() {
        startSession();
        if (!isLinked()) {
            return;
        }
        deleteFolder(dbxNoteFolder);
        deleteFolder(dbxGroupFolder);
        deleteFolder(dbxBirthFolder);
        deleteFolder(dbxPlacesFolder);
        deleteFolder(dbxTemplatesFolder);
        deleteFolder(dbxSettingsFolder);
        deleteFolder(dbxFolder);
    }

    private void deleteFolder(String folder) {
        try {
            mDBApi.delete(folder);
        } catch (DropboxException e) {
            LogUtil.e(TAG, "deleteFolder: ", e);
        }
    }

    /**
     * Download on SD Card all template backup files found on Dropbox.
     */
    public void downloadTemplates(boolean deleteFile) {
        File dir = MemoryUtil.getDropboxTemplatesDir();
        if (dir == null) {
            return;
        }
        startSession();
        if (!isLinked()) {
            return;
        }
        try {
            newEntry = mDBApi.metadata("/" + dbxTemplatesFolder, 1000, null, true, null);
            if (newEntry == null) {
                return;
            }
            RealmDb realmDb = RealmDb.getInstance();
            BackupTool backupTool = BackupTool.getInstance();
            for (DropboxAPI.Entry e : newEntry.contents) {
                if (!e.isDeleted) {
                    String fileName = e.fileName();
                    File localFile = new File(dir + "/" + fileName);
                    String cloudFile = "/" + dbxTemplatesFolder + fileName;
                    downloadFile(localFile, cloudFile);
                    realmDb.saveObject(backupTool.getReminder(localFile.toString(), null));
                    if (deleteFile) {
                        if (localFile.exists()) {
                            localFile.delete();
                        }
                        mDBApi.delete(e.path);
                    }
                }
            }
        } catch (DropboxException | IOException e) {
            LogUtil.e(TAG, "downloadTemplates: ", e);
        }
    }

    /**
     * Download on SD Card all reminder backup files found on Dropbox.
     */
    public void downloadReminders(boolean deleteFile) {
        File dir = MemoryUtil.getDropboxRemindersDir();
        if (dir == null) {
            return;
        }
        startSession();
        if (!isLinked()) {
            return;
        }
        try {
            newEntry = mDBApi.metadata("/" + dbxFolder, 1000, null, true, null);
            if (newEntry == null) {
                return;
            }
            RealmDb realmDb = RealmDb.getInstance();
            BackupTool backupTool = BackupTool.getInstance();
            for (DropboxAPI.Entry e : newEntry.contents) {
                if (!e.isDeleted) {
                    String fileName = e.fileName();
                    File localFile = new File(dir + "/" + fileName);
                    String cloudFile = "/" + dbxFolder + fileName;
                    downloadFile(localFile, cloudFile);
                    Reminder reminder = backupTool.getReminder(localFile.toString(), null);
                    if (reminder.isRemoved() || !reminder.isActive()) {
                        continue;
                    }
                    realmDb.saveObject(reminder);
                    EventControl control = EventControlFactory.getController(mContext, reminder);
                    control.next();
                    if (deleteFile) {
                        if (localFile.exists()) {
                            localFile.delete();
                        }
                        mDBApi.delete(e.path);
                    }
                }
            }
        } catch (DropboxException | IOException e) {
            LogUtil.e(TAG, "downloadReminders: ", e);
        }
    }

    private void downloadFile(File localFile, String cloudFile) {
        try {
            if (!localFile.exists()) {
                localFile.createNewFile();
            }
            FileOutputStream outputStream = new FileOutputStream(localFile);
            mDBApi.getFile(cloudFile, null, outputStream, null);
        } catch (DropboxException | IOException e1) {
            LogUtil.e(TAG, "downloadFile: ", e1);
        }
    }

    /**
     * Download on SD Card all note backup files found on Dropbox.
     */
    public void downloadNotes(boolean deleteFile) {
        File dir = MemoryUtil.getDropboxNotesDir();
        if (dir == null) {
            return;
        }
        startSession();
        if (!isLinked()) {
            return;
        }
        try {
            newEntry = mDBApi.metadata("/" + dbxNoteFolder, 1000, null, true, null);
            if (newEntry == null) {
                return;
            }
            RealmDb realmDb = RealmDb.getInstance();
            BackupTool backupTool = BackupTool.getInstance();
            for (DropboxAPI.Entry e : newEntry.contents) {
                if (!e.isDeleted) {
                    String fileName = e.fileName();
                    File localFile = new File(dir + "/" + fileName);
                    String cloudFile = "/" + dbxNoteFolder + fileName;
                    downloadFile(localFile, cloudFile);
                    realmDb.saveObject(backupTool.getNote(localFile.toString(), null));
                    if (deleteFile) {
                        if (localFile.exists()) {
                            localFile.delete();
                        }
                        mDBApi.delete(e.path);
                    }
                }
            }
        } catch (DropboxException | IOException e) {
            LogUtil.e(TAG, "downloadNotes: ", e);
        }

    }

    /**
     * Download on SD Card all group backup files found on Dropbox.
     */
    public void downloadGroups(boolean deleteFile) {
        File dir = MemoryUtil.getDropboxGroupsDir();
        if (dir == null) {
            return;
        }
        startSession();
        if (!isLinked()) {
            return;
        }
        try {
            newEntry = mDBApi.metadata("/" + dbxGroupFolder, 1000, null, true, null);
            if (newEntry == null) {
                return;
            }
            RealmDb realmDb = RealmDb.getInstance();
            BackupTool backupTool = BackupTool.getInstance();
            for (DropboxAPI.Entry e : newEntry.contents) {
                if (!e.isDeleted) {
                    String fileName = e.fileName();
                    File localFile = new File(dir + "/" + fileName);
                    String cloudFile = "/" + dbxGroupFolder + fileName;
                    downloadFile(localFile, cloudFile);
                    realmDb.saveObject(backupTool.getGroup(localFile.toString(), null));
                    if (deleteFile) {
                        if (localFile.exists()) {
                            localFile.delete();
                        }
                        mDBApi.delete(e.path);
                    }
                }
            }
        } catch (DropboxException | IOException e) {
            LogUtil.e(TAG, "downloadGroups: ", e);
        }
    }

    /**
     * Download on SD Card all birthday backup files found on Dropbox.
     */
    public void downloadBirthdays(boolean deleteFile) {
        File dir = MemoryUtil.getDropboxBirthdaysDir();
        if (dir == null) {
            return;
        }
        startSession();
        if (!isLinked()) {
            return;
        }
        try {
            newEntry = mDBApi.metadata("/" + dbxBirthFolder, 1000, null, true, null);
            if (newEntry == null) {
                return;
            }
            RealmDb realmDb = RealmDb.getInstance();
            BackupTool backupTool = BackupTool.getInstance();
            for (DropboxAPI.Entry e : newEntry.contents) {
                if (!e.isDeleted) {
                    String fileName = e.fileName();
                    File localFile = new File(dir + "/" + fileName);
                    String cloudFile = "/" + dbxBirthFolder + fileName;
                    downloadFile(localFile, cloudFile);
                    realmDb.saveObject(backupTool.getBirthday(localFile.toString(), null));
                    if (deleteFile) {
                        if (localFile.exists()) {
                            localFile.delete();
                        }
                        mDBApi.delete(e.path);
                    }
                }
            }
        } catch (DropboxException | IOException e) {
            LogUtil.e(TAG, "downloadBirthdays: ", e);
        }
    }

    /**
     * Download on SD Card all places backup files found on Dropbox.
     */
    public void downloadPlaces(boolean deleteFile) {
        File dir = MemoryUtil.getDropboxPlacesDir();
        if (dir == null) {
            return;
        }
        startSession();
        if (!isLinked()) {
            return;
        }
        try {
            newEntry = mDBApi.metadata("/" + dbxPlacesFolder, 1000, null, true, null);
            if (newEntry == null) {
                return;
            }
            RealmDb realmDb = RealmDb.getInstance();
            BackupTool backupTool = BackupTool.getInstance();
            for (DropboxAPI.Entry e : newEntry.contents) {
                if (!e.isDeleted) {
                    String fileName = e.fileName();
                    File localFile = new File(dir + "/" + fileName);
                    String cloudFile = "/" + dbxPlacesFolder + fileName;
                    downloadFile(localFile, cloudFile);
                    realmDb.saveObject(backupTool.getPlace(localFile.toString(), null));
                    if (deleteFile) {
                        if (localFile.exists()) {
                            localFile.delete();
                        }
                        mDBApi.delete(e.path);
                    }
                }
            }
        } catch (DropboxException | IOException e) {
            LogUtil.e(TAG, "downloadPlaces: ", e);
        }
    }

    public void uploadSettings() {
        File dir = MemoryUtil.getPrefsDir();
        if (dir == null) {
            return;
        }
        startSession();
        if (!isLinked()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (!file.toString().endsWith(FileConfig.FILE_NAME_SETTINGS)) {
                continue;
            }
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            try {
                newEntry = mDBApi.putFileOverwrite(dbxSettingsFolder + file.getName(), fis, file.length(), null);
            } catch (DropboxUnlinkedException e) {
                LogUtil.e(TAG, "User has unlinked.", e);
            } catch (DropboxException e) {
                LogUtil.e(TAG, "Something went wrong while uploading.", e);
            }
            break;
        }
    }

    public void downloadSettings() {
        File dir = MemoryUtil.getPrefsDir();
        if (dir == null) {
            return;
        }
        startSession();
        if (!isLinked()) {
            return;
        }
        try {
            newEntry = mDBApi.metadata("/" + dbxSettingsFolder, 1000, null, true, null);
            if (newEntry == null) {
                return;
            }
            for (DropboxAPI.Entry e : newEntry.contents) {
                if (!e.isDeleted) {
                    String fileName = e.fileName();
                    if (fileName.contains(FileConfig.FILE_NAME_SETTINGS)) {
                        File localFile = new File(dir + "/" + fileName);
                        String cloudFile = "/" + dbxPlacesFolder + fileName;
                        downloadFile(localFile, cloudFile);
                        Prefs.getInstance(mContext).loadPrefsFromFile();
                        break;
                    }
                }
            }
        } catch (DropboxException e) {
            LogUtil.e(TAG, "downloadSettings: ", e);
        }
    }

    /**
     * Count all reminder backup files in Dropbox folder.
     *
     * @return number of found backup files.
     */
    public int countFiles() {
        int count = 0;
        startSession();
        if (!isLinked()) {
            return 0;
        }
        try {
            newEntry = mDBApi.metadata("/", 1000, null, true, null);
            if (newEntry == null) {
                return 0;
            }
            for (DropboxAPI.Entry e : newEntry.contents) {
                if (!e.isDeleted) {
                    if (e.isDir) {
                        DropboxAPI.Entry entry = mDBApi.metadata(e.path, 1000, null, true, null);
                        if (entry == null) {
                            continue;
                        }
                        List<DropboxAPI.Entry> list = entry.contents;
                        if (list != null) {
                            count += list.size();
                        }
                    } else {
                        count++;
                    }
                }
            }
        } catch (DropboxException e) {
            LogUtil.e(TAG, "countFiles: ", e);
        }
        return count;
    }
}
