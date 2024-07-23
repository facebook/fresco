/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.fragments.recycler;

import android.net.Uri;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.instrumentation.InstrumentedVitoView;
import com.facebook.samples.scrollperf.instrumentation.PerfListener;
import com.facebook.samples.scrollperf.util.PipelineUtil;
import com.facebook.samples.scrollperf.util.SizeUtil;

/** This is the ViewHolder for the RecyclerView in order to contain the VitoView */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class VitoViewHolder extends RecyclerView.ViewHolder {

  private final View mParentView;

  private final InstrumentedVitoView mVitoView;

  private final Config mConfig;

  private final PerfListener mPerfListener;

  public VitoViewHolder(
      View parentView,
      InstrumentedVitoView simpleVitoView,
      Config config,
      PerfListener perfListener) {
    super(simpleVitoView);
    mParentView = parentView;
    mVitoView = simpleVitoView;
    mConfig = config;
    SizeUtil.setConfiguredSize(mParentView, mVitoView, config);
    mPerfListener = perfListener;
  }

  /**
   * @param uri The Uri to show into the VitoView for this Holder
   */
  public void bind(Uri uri) {
    mVitoView.initInstrumentation(uri.toString(), mPerfListener);
    PipelineUtil.addOptionalFeatures(
        mVitoView
            .getImageOptionsBuilder()
            .resize(
                new ResizeOptions(
                    mVitoView.getLayoutParams().width, mVitoView.getLayoutParams().height)),
        mConfig);
    mVitoView.setImageURI(uri, "VitoViewHolder");
  }
}
