/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.adapters;

import android.content.Context;
import android.view.ViewGroup;
import com.facebook.drawee.backends.volley.VolleyDraweeControllerBuilderSupplier;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.samples.comparison.Drawables;
import com.facebook.samples.comparison.configs.volley.SampleVolleyFactory;
import com.facebook.samples.comparison.holders.VolleyDraweeHolder;
import com.facebook.samples.comparison.instrumentation.InstrumentedDraweeView;
import com.facebook.samples.comparison.instrumentation.PerfListener;

/** RecyclerView Adapter for Volley using Drawee */
public class VolleyDraweeAdapter extends ImageListAdapter {

  public VolleyDraweeAdapter(Context context, PerfListener perfListener) {
    super(context, perfListener);
    final VolleyDraweeControllerBuilderSupplier supplier =
        new VolleyDraweeControllerBuilderSupplier(
            context, SampleVolleyFactory.getImageLoader(context));
    InstrumentedDraweeView.initialize(supplier);
  }

  @Override
  public VolleyDraweeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    GenericDraweeHierarchy gdh =
        new GenericDraweeHierarchyBuilder(getContext().getResources())
            .setPlaceholderImage(Drawables.sPlaceholderDrawable)
            .setFailureImage(Drawables.sErrorDrawable)
            .build();
    InstrumentedDraweeView view = new InstrumentedDraweeView(getContext());
    view.setHierarchy(gdh);
    return new VolleyDraweeHolder(getContext(), parent, view, getPerfListener());
  }

  @Override
  public void shutDown() {
    super.clear();
    InstrumentedDraweeView.shutDown();
    SampleVolleyFactory.getMemoryCache().clear();
  }
}
