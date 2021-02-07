/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.facebook.samples.comparison.configs.uil.SampleUilFactory;
import com.facebook.samples.comparison.holders.UilHolder;
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;
import com.nostra13.universalimageloader.core.ImageLoader;

/** RecyclerView Adapter for Universal ImageLoader */
public class UilAdapter extends ImageListAdapter {

  private final ImageLoader mImageLoader;

  public UilAdapter(Context context, PerfListener perfListener) {
    super(context, perfListener);
    mImageLoader = SampleUilFactory.getImageLoader(context);
  }

  @Override
  public UilHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final InstrumentedImageView instrumentedImageView = new InstrumentedImageView(getContext());
    return new UilHolder(
        getContext(), mImageLoader, parent, instrumentedImageView, getPerfListener());
  }

  @Override
  public void shutDown() {
    super.clear();
    mImageLoader.clearMemoryCache();
  }
}
