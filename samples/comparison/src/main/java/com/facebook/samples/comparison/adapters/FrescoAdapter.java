/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.adapters;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.samples.comparison.holders.FrescoHolder;
import com.facebook.samples.comparison.instrumentation.InstrumentedDraweeView;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/** RecyclerView Adapter for Fresco */
public class FrescoAdapter extends ImageListAdapter {

  public FrescoAdapter(
      Context context, PerfListener perfListener, ImagePipelineConfig imagePipelineConfig) {
    super(context, perfListener);
    Fresco.initialize(context, imagePipelineConfig);
  }

  @Override
  public FrescoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    ImageOptions imageOptions =
        ImageOptions.create()
            .placeholderRes(android.R.drawable.ic_menu_report_image)
            .errorRes(android.R.drawable.ic_menu_report_image)
            .scale(ScalingUtils.ScaleType.FIT_CENTER)
            .build();
    final InstrumentedDraweeView instrView = new InstrumentedDraweeView(getContext(), imageOptions);
    instrView.setScaleType(ImageView.ScaleType.FIT_CENTER);

    return new FrescoHolder(getContext(), parent, instrView, getPerfListener());
  }

  @Override
  public void shutDown() {
    Fresco.shutDown();
  }
}
