/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.imageformat.override;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import com.facebook.common.util.UriUtil;
import com.facebook.drawee.backends.pipeline.DraweeConfig;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.CustomImageFormatConfigurator;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.imageformat.color.ColorImageExample;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.view.VitoView;
import com.facebook.imagepipeline.common.ImageDecodeOptionsBuilder;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.drawable.DrawableFactory;

/**
 * Example that overrides the decoder for a given image request.
 *
 * <p>If your decoder needs a custom {@link DrawableFactory} to render the image, don't forget to
 * add it when you initialize Fresco. For this color example, we add this factory in {@link
 * CustomImageFormatConfigurator#addCustomDrawableFactories(Context, DraweeConfig.Builder)}.
 */
public class ImageFormatOverrideExample extends BaseShowcaseFragment {

  private static final String CALLER_CONTEXT = "ImageFormatOverrideExample";

  private static final ImageDecoder CUSTOM_COLOR_DECODER = ColorImageExample.createDecoder();
  private static final ImageOptions IMAGE_OPTIONS =
      ImageOptions.create()
          .imageDecodeOptions(
              new ImageDecodeOptionsBuilder().setCustomImageDecoder(CUSTOM_COLOR_DECODER).build())
          .build();

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_format_override, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    ImageView imageView = view.findViewById(R.id.image);

    VitoView.show(
        UriUtil.getUriForResourceId(R.raw.custom_color1), IMAGE_OPTIONS, CALLER_CONTEXT, imageView);
  }
}
