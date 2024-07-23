/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.fragments.recycler;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.samples.scrollperf.R;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.data.SimpleAdapter;
import com.facebook.samples.scrollperf.instrumentation.InstrumentedVitoView;
import com.facebook.samples.scrollperf.instrumentation.PerfListener;
import com.facebook.samples.scrollperf.util.PipelineUtil;
import com.facebook.samples.scrollperf.util.SizeUtil;
import com.facebook.samples.scrollperf.util.VitoUtil;

/** This is the implementation of the Adapter for the ListView */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class VitoViewListAdapter extends BaseAdapter {

  private final SimpleAdapter<Uri> mSimpleAdapter;

  private final Config mConfig;

  private final int mPaddingPx;

  private final PerfListener mPerfListener;

  public VitoViewListAdapter(
      Context context, SimpleAdapter<Uri> simpleAdapter, Config config, PerfListener perfListener) {
    this.mSimpleAdapter = simpleAdapter;
    this.mConfig = config;
    this.mPaddingPx = context.getResources().getDimensionPixelSize(R.dimen.vito_padding);
    this.mPerfListener = perfListener;
  }

  @Override
  public int getCount() {
    return mSimpleAdapter.getSize();
  }

  @Override
  public Uri getItem(int position) {
    return mSimpleAdapter.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    InstrumentedVitoView vitoView;
    if (convertView == null) {
      final Context context = parent.getContext();
      vitoView =
          new InstrumentedVitoView(context, VitoUtil.createImageOptions(context, mConfig), mConfig);
      SizeUtil.setConfiguredSize(parent, vitoView, mConfig);
      vitoView.setPadding(mPaddingPx, mPaddingPx, mPaddingPx, mPaddingPx);
    } else {
      vitoView = (InstrumentedVitoView) convertView;
    }
    final Uri uri = getItem(position);
    vitoView.initInstrumentation(uri.toString(), mPerfListener);
    PipelineUtil.addOptionalFeatures(
        vitoView
            .getImageOptionsBuilder()
            .resize(
                new ResizeOptions(
                    vitoView.getLayoutParams().width, vitoView.getLayoutParams().height)),
        mConfig);
    vitoView.setImageURI(uri, "VitoViewListAdapter");
    return vitoView;
  }
}
