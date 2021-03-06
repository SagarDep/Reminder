package com.elementary.tasks.google_tasks;

import com.elementary.tasks.core.utils.SuperUtil;
import com.google.api.client.util.DateTime;
import com.google.api.services.tasks.model.Task;
import com.google.gson.annotations.SerializedName;

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

public class TaskItem {

    @SerializedName("title")
    private String title;
    @SerializedName("taskId")
    private String taskId;
    @SerializedName("completeDate")
    private long completeDate;
    @SerializedName("del")
    private int del;
    @SerializedName("dueDate")
    private long dueDate;
    @SerializedName("eTag")
    private String eTag;
    @SerializedName("kind")
    private String kind;
    @SerializedName("notes")
    private String notes;
    @SerializedName("parent")
    private String parent;
    @SerializedName("position")
    private String position;
    @SerializedName("selfLink")
    private String selfLink;
    @SerializedName("updateDate")
    private long updateDate;
    @SerializedName("listId")
    private String listId;
    @SerializedName("status")
    private String status;
    @SerializedName("uuId")
    private String uuId;
    @SerializedName("hidden")
    private int hidden;

    public TaskItem() {
    }

    public TaskItem(RealmTask item) {
        this.listId = item.getListId();
        this.selfLink = item.getSelfLink();
        this.kind = item.getKind();
        this.eTag = item.geteTag();
        this.title = item.getTitle();
        this.taskId = item.getTaskId();
        this.completeDate = item.getCompleteDate();
        this.del = item.getDel();
        this.hidden = item.getHidden();
        this.dueDate = item.getDueDate();
        this.notes = item.getNotes();
        this.parent = item.getParent();
        this.position = item.getPosition();
        this.updateDate = item.getUpdateDate();
        this.status = item.getStatus();
        this.uuId = item.getUuId();
    }

    public TaskItem(TaskItem item) {
        this.listId = item.getListId();
        this.selfLink = item.getSelfLink();
        this.kind = item.getKind();
        this.eTag = item.geteTag();
        this.title = item.getTitle();
        this.taskId = item.getTaskId();
        this.completeDate = item.getCompleteDate();
        this.del = item.getDel();
        this.hidden = item.getHidden();
        this.dueDate = item.getDueDate();
        this.notes = item.getNotes();
        this.parent = item.getParent();
        this.position = item.getPosition();
        this.updateDate = item.getUpdateDate();
        this.status = item.getStatus();
        this.uuId = item.getUuId();
    }

    public TaskItem(Task task, String listId) {
        this.listId = listId;
        DateTime dueDate = task.getDue();
        long due = dueDate != null ? dueDate.getValue() : 0;
        DateTime completeDate = task.getCompleted();
        long complete = completeDate != null ? completeDate.getValue() : 0;
        DateTime updateDate = task.getUpdated();
        long update = updateDate != null ? updateDate.getValue() : 0;
        String taskId = task.getId();
        boolean isDeleted = false;
        try {
            isDeleted = task.getDeleted();
        } catch (NullPointerException e) {
        }
        boolean isHidden = false;
        try {
            isHidden = task.getHidden();
        } catch (NullPointerException e) {
        }
        this.selfLink = task.getSelfLink();
        this.kind = task.getKind();
        this.eTag = task.getEtag();
        this.title = task.getTitle();
        this.taskId = taskId;
        this.completeDate = complete;
        this.del = isDeleted ? 1 : 0;
        this.hidden = isHidden ? 1 : 0;
        this.dueDate = due;
        this.notes = task.getNotes();
        this.parent = task.getParent();
        this.position = task.getPosition();
        this.updateDate = update;
        this.status = task.getStatus();
    }

    public String getUuId() {
        return uuId;
    }

    public void setUuId(String uuId) {
        this.uuId = uuId;
    }

    public void update(Task task) {
        DateTime dueDate = task.getDue();
        long due = dueDate != null ? dueDate.getValue() : 0;
        DateTime completeDate = task.getCompleted();
        long complete = completeDate != null ? completeDate.getValue() : 0;
        DateTime updateDate = task.getUpdated();
        long update = updateDate != null ? updateDate.getValue() : 0;
        boolean isDeleted = false;
        try {
            isDeleted = task.getDeleted();
        } catch (NullPointerException e) {
        }
        boolean isHidden = false;
        try {
            isHidden = task.getHidden();
        } catch (NullPointerException e) {
        }
        this.selfLink = task.getSelfLink();
        this.kind = task.getKind();
        this.eTag = task.getEtag();
        this.title = task.getTitle();
        this.taskId = task.getId();
        this.completeDate = complete;
        this.del = isDeleted ? 1 : 0;
        this.hidden = isHidden ? 1 : 0;
        this.dueDate = due;
        this.notes = task.getNotes();
        this.parent = task.getParent();
        this.position = task.getPosition();
        this.updateDate = update;
        this.status = task.getStatus();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public long getCompleteDate() {
        return completeDate;
    }

    public void setCompleteDate(long completeDate) {
        this.completeDate = completeDate;
    }

    public int getDel() {
        return del;
    }

    public void setDel(int del) {
        this.del = del;
    }

    public long getDueDate() {
        return dueDate;
    }

    public void setDueDate(long dueDate) {
        this.dueDate = dueDate;
    }

    public String geteTag() {
        return eTag;
    }

    public void seteTag(String eTag) {
        this.eTag = eTag;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getParent() {
        return parent;
    }

    public void setParent(String parent) {
        this.parent = parent;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getSelfLink() {
        return selfLink;
    }

    public void setSelfLink(String selfLink) {
        this.selfLink = selfLink;
    }

    public long getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(long updateDate) {
        this.updateDate = updateDate;
    }

    public String getListId() {
        return listId;
    }

    public TaskItem setListId(String listId) {
        this.listId = listId;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getHidden() {
        return hidden;
    }

    public void setHidden(int hidden) {
        this.hidden = hidden;
    }

    @Override
    public String toString() {
        return SuperUtil.getObjectPrint(this, TaskItem.class);
    }
}
