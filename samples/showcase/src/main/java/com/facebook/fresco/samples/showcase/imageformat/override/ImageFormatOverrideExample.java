/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import androidx.annotation.Nullable;
import com.facebook.drawee.backends.pipeline.DraweeConfig;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.CustomImageFormatConfigurator;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.imageformat.color.ColorImageExample;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ImageDecodeOptionsBuilder;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/**
 * Example that overrides the decoder for a given image request.
 *
 * <p>If your decoder needs a custom {@link DrawableFactory} to render the image, don't forget to
 * add it when you initialize Fresco. For this color example, we add this factory in {@link
 * CustomImageFormatConfigurator#addCustomDrawableFactories(Context, DraweeConfig.Builder)}.
 */
public class ImageFormatOverrideExample extends BaseShowcaseFragment {

  private static final ImageDecoder CUSTOM_COLOR_DECODER = ColorImageExample.createDecoder();

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_format_override, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    SimpleDraweeView simpleDraweeView = (SimpleDraweeView) view.findViewById(R.id.drawee_view);

    ImageDecodeOptions imageDecodeOptionsWithCustomDecoder =
        new ImageDecodeOptionsBuilder().setCustomImageDecoder(CUSTOM_COLOR_DECODER).build();

    AbstractDraweeController controller =
        Fresco.newDraweeControllerBuilder()
            .setImageRequest(
                ImageRequestBuilder.newBuilderWithResourceId(R.raw.custom_color1)
                    .setImageDecodeOptions(imageDecodeOptionsWithCustomDecoder)
                    .build())
            .build();
    simpleDraweeView.setController(controller);
  }
}
