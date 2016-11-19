package com.elementary.tasks.notes;

import android.content.Context;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.elementary.tasks.core.file_explorer.FilterCallback;
import com.elementary.tasks.core.interfaces.SimpleListener;
import com.elementary.tasks.core.utils.AssetsUtil;
import com.elementary.tasks.core.utils.Configs;
import com.elementary.tasks.core.utils.Module;
import com.elementary.tasks.core.utils.Prefs;
import com.elementary.tasks.core.utils.ThemeUtil;
import com.elementary.tasks.databinding.NoteListItemBinding;

import java.util.ArrayList;
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

public class NotesRecyclerAdapter extends RecyclerView.Adapter<NotesRecyclerAdapter.ViewHolder> {

    private List<NoteItem> mDataList;
    private SimpleListener mEventListener;
    private FilterCallback mCallback;


    public NotesRecyclerAdapter(Context context, List<NoteItem> list, FilterCallback callback) {
        this.mDataList = list;
        this.mCallback = callback;
        setHasStableIds(true);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public NoteListItemBinding binding;

        public ViewHolder(View v) {
            super(v);
            binding = DataBindingUtil.bind(v);
            binding.setClick(v1 -> {
                if (mEventListener != null) {
                    mEventListener.onItemClicked(getAdapterPosition(), v1);
                }
            });
            binding.noteClick.setOnLongClickListener(view -> {
                if (mEventListener != null) {
                    mEventListener.onItemLongClicked(getAdapterPosition(), view);
                }
                return false;
            });
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(NoteListItemBinding.inflate(inflater, parent, false).getRoot());
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final NoteItem item = mDataList.get(position);
        holder.binding.setNote(item);
    }

    public NoteItem getItem(int position) {
        return mDataList.get(position);
    }

    public void filter(String q, List<NoteItem> list) {
        List<NoteItem> res = filter(list, q);
        animateTo(res);
        if (mCallback != null) mCallback.filter(res.size());
    }

    private List<NoteItem> filter(List<NoteItem> mData, String q) {
        q = q.toLowerCase();
        if (mData == null) mData = new ArrayList<>();
        List<NoteItem> filteredModelList = new ArrayList<>();
        if (q.matches("")) {
            filteredModelList = new ArrayList<>(mData);
        } else {
            filteredModelList.addAll(getFiltered(mData, q));
        }
        return filteredModelList;
    }

    private List<NoteItem> getFiltered(List<NoteItem> models, String query) {
        List<NoteItem> list = new ArrayList<>();
        for (NoteItem model : models) {
            String text = model.getSummary();
            if (text.toLowerCase().contains(query)) {
                list.add(model);
            }
        }
        return list;
    }

    public NoteItem remove(int position) {
        final NoteItem model = mDataList.remove(position);
        notifyItemRemoved(position);
        return model;
    }

    public void addItem(int position, NoteItem model) {
        mDataList.add(position, model);
        notifyItemInserted(position);
    }

    public void moveItem(int fromPosition, int toPosition) {
        final NoteItem model = mDataList.remove(fromPosition);
        mDataList.add(toPosition, model);
        notifyItemMoved(fromPosition, toPosition);
    }

    public void animateTo(List<NoteItem> models) {
        applyAndAnimateRemovals(models);
        applyAndAnimateAdditions(models);
        applyAndAnimateMovedItems(models);
    }

    private void applyAndAnimateRemovals(List<NoteItem> newModels) {
        for (int i = mDataList.size() - 1; i >= 0; i--) {
            final NoteItem model = mDataList.get(i);
            if (!newModels.contains(model)) {
                remove(i);
            }
        }
    }

    private void applyAndAnimateAdditions(List<NoteItem> newModels) {
        for (int i = 0, count = newModels.size(); i < count; i++) {
            final NoteItem model = newModels.get(i);
            if (!mDataList.contains(model)) {
                addItem(i, model);
            }
        }
    }

    private void applyAndAnimateMovedItems(List<NoteItem> newModels) {
        for (int toPosition = newModels.size() - 1; toPosition >= 0; toPosition--) {
            final NoteItem model = newModels.get(toPosition);
            final int fromPosition = mDataList.indexOf(model);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    @BindingAdapter({"loadNote"})
    public static void loadNote(TextView textView, NoteItem note) {
        String title = note.getSummary();
        Context context = textView.getContext();
        if (title.length() > 500) {
            String substring = title.substring(0, 500);
            title = substring + "...";
        }
        textView.setText(title);
        textView.setTypeface(AssetsUtil.getTypeface(context, note.getStyle()));
        textView.setTextSize(Prefs.getInstance(context).getNoteTextSize() + 12);
    }

    @BindingAdapter({"loadNoteCard"})
    public static void loadNoteCard(CardView cardView, int color) {
        cardView.setCardBackgroundColor(ThemeUtil.getInstance(cardView.getContext()).getNoteLightColor(color));
        if (Module.isLollipop()) {
            cardView.setCardElevation(Configs.CARD_ELEVATION);
        }
    }

    @BindingAdapter({"loadImage"})
    public static void loadImage(ImageView imageView, byte[] image) {
        if (image != null) {
            Bitmap photo = BitmapFactory.decodeByteArray(image, 0, image.length);
            if (photo != null) {
                imageView.setImageBitmap(photo);
            } else {
                imageView.setImageDrawable(null);
            }
        } else {
            imageView.setImageDrawable(null);
        }
    }

    public void setEventListener(SimpleListener eventListener) {
        mEventListener = eventListener;
    }
}