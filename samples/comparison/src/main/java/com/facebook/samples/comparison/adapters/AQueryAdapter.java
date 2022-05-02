/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.androidquery.AQuery;
import com.facebook.samples.comparison.holders.AQueryHolder;
import com.facebook.samples.comparison.instrumentation.InstrumentedImageView;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/** RecyclerView Adapter for Android Query */
public class AQueryAdapter extends ImageListAdapter {

  private AQuery mAQuery;

  public AQueryAdapter(Context context, PerfListener perfListener) {
    super(context, perfListener);
    mAQuery = new AQuery(context);
  }

  @Override
  public AQueryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final InstrumentedImageView instrImageView = new InstrumentedImageView(getContext());
    return new AQueryHolder(getContext(), mAQuery, parent, instrImageView, getPerfListener());
  }

  @Override
  public void shutDown() {
    for (int i = 0; i < getItemCount(); i++) {
      String uri = getItem(i);
      mAQuery.invalidate(uri);
    }
    super.clear();
  }
}
