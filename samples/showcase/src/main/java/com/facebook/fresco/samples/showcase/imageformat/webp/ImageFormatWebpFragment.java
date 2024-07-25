/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.imageformat.webp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import com.facebook.common.webp.WebpSupportStatus;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.CheckerBoardDrawable;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.view.VitoView;

/**
 * This fragment displays different WebP images.
 *
 * <p>For being able to do this in your applications, you need to add the following dependencies to
 * your build.gradle file (where X.X.X matches the used Fresco version): - implementation
 * 'com.facebook.fresco:animated-webp:X.X.X' - implementation
 * 'com.facebook.fresco:webpsupport:X.X.X'
 */
public class ImageFormatWebpFragment extends BaseShowcaseFragment {

  private static final String CALLER_CONTEXT = "ImageFormatWebpFragment";
  private static final ImageOptions IMAGE_OPTIONS = ImageOptions.create().autoPlay(true).build();

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_format_webp, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final ImageView imageWebPStatic = view.findViewById(R.id.image_view_webp_static);
    VitoView.show(sampleUris().createWebpStaticUri(), CALLER_CONTEXT, imageWebPStatic);

    final ImageView imageWebPTranslucent = view.findViewById(R.id.image_view_webp_translucent);
    VitoView.show(sampleUris().createWebpTranslucentUri(), CALLER_CONTEXT, imageWebPTranslucent);

    final SwitchCompat switchBackground = view.findViewById(R.id.switch_background);
    switchBackground.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            imageWebPTranslucent.setBackground(
                isChecked ? new CheckerBoardDrawable(getResources()) : null);
          }
        });

    final ImageView imageWebpAnimated = view.findViewById(R.id.image_view_webp_animated);
    VitoView.show(
        sampleUris().createWebpAnimatedUri(), IMAGE_OPTIONS, CALLER_CONTEXT, imageWebpAnimated);

    final TextView supportStatusTextView = view.findViewById(R.id.text_webp_support_status);
    final StringBuilder sb = new StringBuilder();
    sb.append("WebpSupportStatus.sIsSimpleWebpSupported = ")
        .append(WebpSupportStatus.sIsSimpleWebpSupported)
        .append('\n');
    sb.append("WebpSupportStatus.sIsExtendedWebpSupported = ")
        .append(WebpSupportStatus.sIsExtendedWebpSupported)
        .append('\n');
    supportStatusTextView.setText(sb.toString());
  }
}
