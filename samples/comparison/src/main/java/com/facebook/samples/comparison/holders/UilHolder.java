/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.holders;

import android.content.Context;
import android.view.View;
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;
import com.nostra13.universalimageloader.core.ImageLoader;

/** This is the Holder class for the RecycleView to use with Universal Image Loader */
public class UilHolder extends BaseViewHolder<InstrumentedImageView> {

  private final ImageLoader mImageLoader;

  public UilHolder(
      Context context,
      ImageLoader imageLoader,
      View layoutView,
      InstrumentedImageView view,
      PerfListener perfListener) {
    super(context, layoutView, view, perfListener);
    this.mImageLoader = imageLoader;
  }

  @Override
  protected void onBind(String uri) {
    mImageLoader.displayImage(uri, mImageView);
  }
}
