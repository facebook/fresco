/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.fragments.recycler;

import android.content.Context;
import android.net.Uri;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.samples.scrollperf.R;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.data.SimpleAdapter;
import com.facebook.samples.scrollperf.instrumentation.InstrumentedDraweeView;
import com.facebook.samples.scrollperf.instrumentation.PerfListener;
import com.facebook.samples.scrollperf.util.DraweeUtil;

/** The RecyclerView.Adapter for the DraweeView */
public class DraweeViewAdapter extends RecyclerView.Adapter<DraweeViewHolder> {

  private final SimpleAdapter<Uri> mSimpleAdapter;

  private final Context mContext;

  private final Config mConfig;

  private final int mPaddingPx;

  private final PerfListener mPerfListener;

  public DraweeViewAdapter(
      Context context, SimpleAdapter<Uri> simpleAdapter, Config config, PerfListener perfListener) {
    this.mContext = context;
    this.mSimpleAdapter = simpleAdapter;
    this.mConfig = config;
    this.mPaddingPx = context.getResources().getDimensionPixelSize(R.dimen.drawee_padding);
    mPerfListener = perfListener;
  }

  @Override
  public DraweeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    GenericDraweeHierarchy gdh = DraweeUtil.createDraweeHierarchy(mContext, mConfig);
    final InstrumentedDraweeView simpleDraweeView =
        new InstrumentedDraweeView(mContext, gdh, mConfig);
    simpleDraweeView.setPadding(mPaddingPx, mPaddingPx, mPaddingPx, mPaddingPx);
    return new DraweeViewHolder(parent, simpleDraweeView, mConfig, mPerfListener);
  }

  @Override
  public void onBindViewHolder(DraweeViewHolder holder, int position) {
    holder.bind(mSimpleAdapter.get(position));
  }

  @Override
  public int getItemCount() {
    return mSimpleAdapter.getSize();
  }
}
