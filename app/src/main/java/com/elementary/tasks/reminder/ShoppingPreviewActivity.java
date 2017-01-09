package com.elementary.tasks.reminder;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.elementary.tasks.R;
import com.elementary.tasks.core.ThemedActivity;
import com.elementary.tasks.core.controller.EventControl;
import com.elementary.tasks.core.controller.EventControlImpl;
import com.elementary.tasks.core.utils.Constants;
import com.elementary.tasks.core.utils.Module;
import com.elementary.tasks.core.utils.RealmDb;
import com.elementary.tasks.creators.CreateReminderActivity;
import com.elementary.tasks.databinding.ActivityShoppingPreviewBinding;
import com.elementary.tasks.groups.GroupItem;
import com.elementary.tasks.reminder.models.Reminder;
import com.elementary.tasks.reminder.models.ShopItem;

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

public class ShoppingPreviewActivity extends ThemedActivity {

    private ActivityShoppingPreviewBinding binding;

    private ShopListRecyclerAdapter shoppingAdapter;
    private Reminder mReminder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String id = getIntent().getStringExtra(Constants.INTENT_ID);
        mReminder = RealmDb.getInstance().getReminder(id);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_shopping_preview);
        initActionBar();
        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInfo();
    }

    private void loadInfo() {
        if (mReminder != null) {
            binding.statusSwitch.setChecked(mReminder.isActive());
            if (!mReminder.isActive()) {
                binding.statusText.setText(R.string.disabled);
            } else {
                binding.statusText.setText(R.string.enabled4);
            }
            if (TextUtils.isEmpty(mReminder.getEventTime())) {
                binding.switchWrapper.setVisibility(View.GONE);
            } else {
                binding.switchWrapper.setVisibility(View.VISIBLE);
            }
            binding.toolbar.setTitle(mReminder.getSummary());
            int catColor = 0;
            GroupItem group = RealmDb.getInstance().getGroup(mReminder.getGroupUuId());
            if (group != null) {
                catColor = group.getColor();
            }
            int mColor = themeUtil.getColor(themeUtil.getCategoryColor(catColor));
            binding.toolbar.setBackgroundColor(mColor);
            if (Module.isLollipop()) {
                getWindow().setStatusBarColor(themeUtil.getNoteDarkColor(catColor));
            }
            loadData();
        }
    }

    private void loadData() {
        shoppingAdapter = new ShopListRecyclerAdapter(this, mReminder.getShoppings(),
                new ShopListRecyclerAdapter.ActionListener() {
                    @Override
                    public void onItemCheck(int position, boolean isChecked) {
                        ShopItem item = shoppingAdapter.getItem(position);
                        item.setChecked(!item.isChecked());
                        shoppingAdapter.updateData();
                        RealmDb.getInstance().saveObject(mReminder.setShoppings(shoppingAdapter.getData()));
                    }

                    @Override
                    public void onItemDelete(int position) {
                        shoppingAdapter.delete(position);
                        RealmDb.getInstance().saveObject(mReminder.setShoppings(shoppingAdapter.getData()));
                    }
                });
        binding.todoList.setLayoutManager(new LinearLayoutManager(this));
        binding.todoList.setAdapter(shoppingAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_reminder_preview, menu);
        menu.getItem(1).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int ids = item.getItemId();
        switch (ids) {
            case R.id.action_delete:
                removeReminder();
                return true;
            case android.R.id.home:
                closeWindow();
                return true;
            case R.id.action_edit:
                editReminder();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void editReminder() {
        startActivity(new Intent(this, CreateReminderActivity.class).putExtra(Constants.INTENT_ID, mReminder.getUuId()));
    }

    private void removeReminder() {
        if (RealmDb.getInstance().moveToTrash(mReminder.getUuId())) {
            EventControl control = EventControlImpl.getController(this, mReminder.setRemoved(true));
            control.stop();
        }
        closeWindow();
    }

    private void closeWindow() {
        if (Module.isLollipop()) {
            finishAfterTransition();
        } else {
            finish();
        }
    }

    private void initViews() {
        binding.switchWrapper.setOnClickListener(v -> switchClick());
    }

    private void switchClick() {
        EventControl control = EventControlImpl.getController(this, mReminder);
        if (!control.onOff()) {
            Toast.makeText(this, R.string.reminder_is_outdated, Toast.LENGTH_SHORT).show();
        }
        mReminder = RealmDb.getInstance().getReminder(getIntent().getStringExtra(Constants.INTENT_ID));
        loadInfo();
    }

    private void initActionBar() {
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        binding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}