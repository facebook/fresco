/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;
import com.facebook.litho.sections.SectionContext;
import com.facebook.litho.sections.widget.RecyclerCollectionComponent;

/** Simple experimental Fresco Vito fragment that displays a list of photos. */
public class FrescoVitoLithoSectionsFragment extends BaseShowcaseFragment {

  @Nullable
  @Override
  public View onCreateView(
      @Nullable LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    final ComponentContext c = new ComponentContext(getContext());
    return LithoView.create(
        c,
        RecyclerCollectionComponent.create(c)
            .section(SimpleListSection.create(new SectionContext(c)).build())
            .disablePTR(true)
            .build());
  }

  @Override
  public int getTitleId() {
    return R.string.vito_litho_sections;
  }
}
