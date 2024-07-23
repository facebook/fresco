/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.fragments.recycler;

import android.content.Context;
import android.net.Uri;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.samples.scrollperf.R;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.data.SimpleAdapter;
import com.facebook.samples.scrollperf.instrumentation.InstrumentedVitoView;
import com.facebook.samples.scrollperf.instrumentation.PerfListener;
import com.facebook.samples.scrollperf.util.VitoUtil;

/** The RecyclerView.Adapter for the VitoView */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class VitoViewAdapter extends RecyclerView.Adapter<VitoViewHolder> {

  private final SimpleAdapter<Uri> mSimpleAdapter;

  private final Context mContext;

  private final Config mConfig;

  private final int mPaddingPx;

  private final PerfListener mPerfListener;

  public VitoViewAdapter(
      Context context, SimpleAdapter<Uri> simpleAdapter, Config config, PerfListener perfListener) {
    this.mContext = context;
    this.mSimpleAdapter = simpleAdapter;
    this.mConfig = config;
    this.mPaddingPx = context.getResources().getDimensionPixelSize(R.dimen.vito_padding);
    mPerfListener = perfListener;
  }

  @Override
  public VitoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final InstrumentedVitoView vitoViewHolder =
        new InstrumentedVitoView(mContext, VitoUtil.createImageOptions(mContext, mConfig), mConfig);
    vitoViewHolder.setPadding(mPaddingPx, mPaddingPx, mPaddingPx, mPaddingPx);
    return new VitoViewHolder(parent, vitoViewHolder, mConfig, mPerfListener);
  }

  @Override
  public void onBindViewHolder(VitoViewHolder holder, int position) {
    holder.bind(mSimpleAdapter.get(position));
  }

  @Override
  public int getItemCount() {
    return mSimpleAdapter.getSize();
  }
}
