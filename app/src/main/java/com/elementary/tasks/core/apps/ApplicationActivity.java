package com.elementary.tasks.core.apps;

import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;

import com.elementary.tasks.R;
import com.elementary.tasks.core.ThemedActivity;
import com.elementary.tasks.core.adapter.FilterableAdapter;
import com.elementary.tasks.core.file_explorer.RecyclerClickListener;
import com.elementary.tasks.core.utils.Constants;
import com.elementary.tasks.databinding.ActivityApplicationListBinding;

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

public class ApplicationActivity extends ThemedActivity implements LoadListener, RecyclerClickListener {

    private ActivityApplicationListBinding binding;
    private AppsRecyclerAdapter mAdapter;

    private RecyclerView mRecyclerView;
    private FilterableAdapter.Filter<ApplicationItem, String> mFilter = new FilterableAdapter.Filter<ApplicationItem, String>() {
        @Override
        public boolean filter(ApplicationItem applicationItem, String query) {
            String text = applicationItem.getName().toLowerCase();
            return text.contains(query.toLowerCase());
        }

        @Override
        public void onFilterEnd(List<ApplicationItem> list, int size, String query) {
            mRecyclerView.scrollToPosition(0);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_application_list);
        initActionBar();
        initSearchView();
        initRecyclerView();
        new AppsAsync(this, this).execute();
    }

    private void initRecyclerView() {
        mRecyclerView = binding.contactsList;
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setHasFixedSize(true);
    }

    private void initSearchView() {
        binding.searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mAdapter != null) {
                    mAdapter.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private void initActionBar() {
        Toolbar toolbar = binding.toolbar;
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
        if (toolbar != null) {
            toolbar.setTitle(getString(R.string.choose_application));
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(binding.searchField.getWindowToken(), 0);
    }

    @Override
    public void onLoaded(List<ApplicationItem> list) {
        mAdapter = new AppsRecyclerAdapter(list, this, mFilter);
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onItemClick(int position) {
        Intent intent = new Intent();
        String packageName = mAdapter.getItem(position).getPackageName();
        intent.putExtra(Constants.SELECTED_APPLICATION, packageName);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent();
            setResult(RESULT_CANCELED, intent);
            finish();
        }
        return true;
    }
}
