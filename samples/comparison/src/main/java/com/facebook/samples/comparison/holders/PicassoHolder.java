/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.holders;

import android.content.Context;
import android.view.View;
import com.facebook.samples.comparison.Drawables;
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;
import com.squareup.picasso.Picasso;

/** This is the Holder class for the RecycleView to use with Picasso */
public class PicassoHolder extends BaseViewHolder<InstrumentedImageView> {

  private final Picasso mPicasso;

  public PicassoHolder(
      Context context,
      Picasso picasso,
      View parent,
      InstrumentedImageView view,
      PerfListener perfListener) {
    super(context, parent, view, perfListener);
    mPicasso = picasso;
  }

  @Override
  protected void onBind(String uri) {
    mPicasso
        .load(uri)
        .placeholder(Drawables.sPlaceholderDrawable)
        .error(Drawables.sErrorDrawable)
        .fit()
        .into(mImageView);
  }
}
