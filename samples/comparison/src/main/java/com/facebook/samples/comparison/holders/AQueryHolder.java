/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.holders;

import android.content.Context;
import android.view.View;
import com.androidquery.AQuery;
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/** This is the Holder class for the RecycleView to use with Android Query */
public class AQueryHolder extends BaseViewHolder<InstrumentedImageView> {

  private final AQuery mAQuery;

  public AQueryHolder(
      Context context,
      AQuery aQuery,
      View parentView,
      InstrumentedImageView instrumentedImageView,
      PerfListener perfListener) {
    super(context, parentView, instrumentedImageView, perfListener);
    mAQuery = aQuery;
  }

  @Override
  protected void onBind(String uri) {
    mAQuery.id(mImageView).image(uri);
  }
}
