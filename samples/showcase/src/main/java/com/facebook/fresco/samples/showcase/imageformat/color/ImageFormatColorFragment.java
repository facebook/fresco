/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.imageformat.color;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import com.facebook.common.util.UriUtil;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.CustomImageFormatConfigurator;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.CheckerBoardDrawable;
import com.facebook.fresco.vito.view.VitoView;

/**
 * Color XML example. It has a toggle to enable / disable Color XML support and displays 1 image.
 *
 * <p>The supported XML color format is: <color>#rrggbb</color>
 */
public class ImageFormatColorFragment extends BaseShowcaseFragment {

  private static final String CALLER_CONTEXT = "ImageFormatColorFragment";

  private ImageView mImageView1;
  private ImageView mImageView2;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_format_color, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    mImageView1 = view.findViewById(R.id.image1);
    mImageView2 = view.findViewById(R.id.image2);

    // Set a simple custom color resource as the image.
    // The format of custom_color1 is <color>#rrggbb</color>
    VitoView.show(UriUtil.getUriForResourceId(R.raw.custom_color1), CALLER_CONTEXT, mImageView1);
    VitoView.show(UriUtil.getUriForResourceId(R.raw.custom_color2), CALLER_CONTEXT, mImageView2);

    final SwitchCompat switchBackground = (SwitchCompat) view.findViewById(R.id.switch_background);
    switchBackground.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mImageView1.setBackground(isChecked ? new CheckerBoardDrawable(getResources()) : null);
            mImageView2.setBackground(isChecked ? new CheckerBoardDrawable(getResources()) : null);
          }
        });

    SwitchCompat switchCompat = (SwitchCompat) view.findViewById(R.id.decoder_switch);
    switchCompat.setChecked(
        CustomImageFormatConfigurator.isGlobalColorDecoderEnabled(getContext()));
    switchCompat.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            CustomImageFormatConfigurator.setGlobalColorDecoderEnabled(getContext(), isChecked);
          }
        });
  }
}
