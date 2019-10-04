/*
 * Copyright (c) Facebook, Inc. and its affiliates.
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
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import com.facebook.common.webp.WebpSupportStatus;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.CheckerBoardDrawable;

/**
 * This fragment displays different WebP images.
 *
 * <p>For being able to do this in your applications, you need to add the following dependencies to
 * your build.gradle file (where X.X.X matches the used Fresco version): - implementation
 * 'com.facebook.fresco:animated-webp:X.X.X' - implementation
 * 'com.facebook.fresco:webpsupport:X.X.X'
 */
public class ImageFormatWebpFragment extends BaseShowcaseFragment {

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_format_webp, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    final SimpleDraweeView draweeWebpStatic = view.findViewById(R.id.drawee_view_webp_static);
    draweeWebpStatic.setImageURI(sampleUris().createWebpStaticUri());

    final SimpleDraweeView draweeWebpTranslucent =
        view.findViewById(R.id.drawee_view_webp_translucent);
    draweeWebpTranslucent.setImageURI(sampleUris().createWebpTranslucentUri());

    final SwitchCompat switchBackground = view.findViewById(R.id.switch_background);
    switchBackground.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            draweeWebpTranslucent
                .getHierarchy()
                .setBackgroundImage(isChecked ? new CheckerBoardDrawable(getResources()) : null);
          }
        });

    final SimpleDraweeView draweeWebpAnimated = view.findViewById(R.id.drawee_view_webp_animated);
    draweeWebpAnimated.setController(
        Fresco.newDraweeControllerBuilder()
            .setAutoPlayAnimations(true)
            .setOldController(draweeWebpAnimated.getController())
            .setUri(sampleUris().createWebpAnimatedUri())
            .build());

    final TextView supportStatusTextView = view.findViewById(R.id.text_webp_support_status);
    final StringBuilder sb = new StringBuilder();
    sb.append("WebpSupportStatus.sIsSimpleWebpSupported = ")
        .append(WebpSupportStatus.sIsSimpleWebpSupported)
        .append('\n');
    sb.append("WebpSupportStatus.sIsExtendedWebpSupported = ")
        .append(WebpSupportStatus.sIsExtendedWebpSupported)
        .append('\n');
    sb.append("WebpSupportStatus.sIsWebpSupportRequired = ")
        .append(WebpSupportStatus.sIsWebpSupportRequired)
        .append('\n');
    supportStatusTextView.setText(sb.toString());
  }

  @Override
  public int getTitleId() {
    return R.string.format_webp_title;
  }
}
