/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.samples.apps.iosched.myschedule;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.model.TagMetadata.Tag;
import com.google.samples.apps.iosched.myschedule.SessionsFilterAdapter.OnFiltersChangedListener;

public class ScheduleFilterFragment extends Fragment implements LoaderCallbacks<Cursor> {

    public static final String FILTER_TAG = "com.google.samples.apps.iosched.FILTER_TAG";
    public static final String SHOW_LIVE_STREAMED_ONLY =
            "com.google.samples.apps.iosched.SHOW_LIVE_STREAMED_ONLY";

    private static final int TAG_METADATA_TOKEN = 0x8;

    private RecyclerView mRecyclerView;
    private SessionsFilterAdapter mAdapter;
    private Button mClearFilters;

    private ScheduleFiltersFragmentListener mListener;

    private TagMetadata mTagMetadata;
    // We may have to hang onto this while TagMetadata is loading.
    private String mFilterTag;

    interface ScheduleFiltersFragmentListener {
        void onFiltersChanged(TagFilterHolder filterHolder);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.my_schedule_filter_drawer, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.filters);
        mClearFilters = (Button) view.findViewById(R.id.clear_filters);
        mClearFilters.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mAdapter.clearAllFilters();
            }
        });
        final Context context = view.getContext();
        TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(mClearFilters, null, null,
                AppCompatResources.getDrawable(context, R.drawable.ic_clear_all), null);

        mRecyclerView.addItemDecoration(new DividerItemDecoration(context,
                DividerItemDecoration.VERTICAL));
        mAdapter = new SessionsFilterAdapter(context, null, savedInstanceState);
        mAdapter.setSessionFilterAdapterListener(new OnFiltersChangedListener() {
            @Override
            public void onFiltersChanged(TagFilterHolder filterHolder) {
                mClearFilters.setVisibility(
                        filterHolder.hasAnyFilters() ? View.VISIBLE : View.GONE);
                if (mListener != null) {
                    mListener.onFiltersChanged(filterHolder);
                }
            }
        });
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(TAG_METADATA_TOKEN, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAdapter.onSaveInstanceState(outState);
    }

    public void initWithArguments(Bundle args) {
        if (args != null && mAdapter != null) {
            mAdapter.setShowLiveStreamedOnly(args.getBoolean(SHOW_LIVE_STREAMED_ONLY, false));
            mFilterTag = args.getString(FILTER_TAG);
            maybeApplyPendingFilterTag();
        }
    }

    public void setListener(ScheduleFiltersFragmentListener listener) {
        mListener = listener;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == TAG_METADATA_TOKEN) {
            if (getContext() != null) {
                return TagMetadata.createCursorLoader(getContext());
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == TAG_METADATA_TOKEN) {
            TagMetadata tagMetadata = new TagMetadata(cursor);
            onTagMetadataLoaded(tagMetadata);
        } else {
            cursor.close();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private void onTagMetadataLoaded(TagMetadata tagMetadata) {
        mTagMetadata = tagMetadata;
        maybeApplyPendingFilterTag();
        mAdapter.setTagMetadata(tagMetadata);
    }

    private void maybeApplyPendingFilterTag() {
        if (mTagMetadata != null && mFilterTag != null) {
            Tag tag = mTagMetadata.getTag(mFilterTag);
            if (tag != null) {
                mAdapter.addTag(tag);
                mFilterTag = null; // we don't have to run this again
            }
        }
    }
}
