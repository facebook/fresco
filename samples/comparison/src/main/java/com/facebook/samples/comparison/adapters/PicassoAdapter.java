/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.facebook.samples.comparison.configs.picasso.SamplePicassoFactory;
import com.facebook.samples.comparison.holders.PicassoHolder;
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;
import com.squareup.picasso.Picasso;

/** RecyclerView Adapter for Picasso */
public class PicassoAdapter extends ImageListAdapter {

  private final Picasso mPicasso;

  public PicassoAdapter(Context context, PerfListener perfListener) {
    super(context, perfListener);
    mPicasso = SamplePicassoFactory.getPicasso(context);
  }

  @Override
  public PicassoHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final InstrumentedImageView instrImageView = new InstrumentedImageView(getContext());
    return new PicassoHolder(getContext(), mPicasso, parent, instrImageView, getPerfListener());
  }

  @Override
  public void shutDown() {
    for (int i = 0; i < getItemCount(); i++) {
      String uri = getItem(i);
      mPicasso.invalidate(uri);
    }
    super.clear();
  }
}
