/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.android.volley.toolbox.ImageLoader;
import com.facebook.samples.comparison.R;
import com.facebook.samples.comparison.configs.volley.SampleVolleyFactory;
import com.facebook.samples.comparison.holders.VolleyHolder;
import com.facebook.samples.comparison.instrumentation.InstrumentedNetworkImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/** RecyclerView Adapter for Volley */
public class VolleyAdapter extends ImageListAdapter {

  private final ImageLoader mImageLoader;

  public VolleyAdapter(Context context, PerfListener perfListener) {
    super(context, perfListener);
    mImageLoader = SampleVolleyFactory.getImageLoader(context);
  }

  @Override
  public VolleyHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    InstrumentedNetworkImageView view = new InstrumentedNetworkImageView(getContext());
    view.setDefaultImageResId(R.color.placeholder);
    view.setErrorImageResId(R.color.error);
    return new VolleyHolder(getContext(), mImageLoader, parent, view, getPerfListener());
  }

  @Override
  public void shutDown() {
    super.clear();
    SampleVolleyFactory.getMemoryCache().clear();
  }
}
