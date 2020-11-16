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
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.vito.litho.slideshow.FrescoVitoSlideshowComponent;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.options.RoundingOptions;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.LithoView;

/** Fresco Vito fragment that displays a slideshow. */
public class FrescoVitoLithoSlideshowFragment extends BaseShowcaseFragment {

  private static final ImageOptions IMAGE_OPTIONS =
      ImageOptions.create()
          .placeholderRes(R.drawable.logo)
          .round(RoundingOptions.asCircle())
          .fadeDurationMs(0)
          .build();

  @Nullable
  @Override
  public View onCreateView(
      @Nullable LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_vito_slideshow, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    final ComponentContext componentContext = new ComponentContext(getContext());

    FrameLayout container = view.findViewById(R.id.container);
    container.addView(
        LithoView.create(componentContext, createSlideshowComponent(componentContext)));
  }

  public Component createSlideshowComponent(ComponentContext c) {
    return FrescoVitoSlideshowComponent.create(c)
        .fadeTransitionMs(1000)
        .photoTransitionMs(4000)
        .isPlaying(true)
        .uris(sampleUris().getRandomSampleUris(ImageUriProvider.ImageSize.M, 5))
        .imageOptions(IMAGE_OPTIONS)
        .build();
  }
}
