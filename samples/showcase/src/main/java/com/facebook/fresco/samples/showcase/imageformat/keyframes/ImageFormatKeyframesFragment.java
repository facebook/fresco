/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.imageformat.keyframes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.CustomImageFormatConfigurator;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.CheckerBoardDrawable;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.view.VitoView;

/** Fragment using a ImageView to display a Keyframes animation */
public class ImageFormatKeyframesFragment extends BaseShowcaseFragment {

  private static final String CALLER_CONTEXT = "ImageFormatKeyframesFragment";
  private static final ImageOptions IMAGE_OPTIONS = ImageOptions.create().autoPlay(true).build();
  private ImageView mImageView;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_format_keyframes, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    if (CustomImageFormatConfigurator.isKeyframesEnabled()) {
      initAnimation(view);
    }
  }

  private void initAnimation(View view) {
    mImageView = view.findViewById(R.id.image);
    mImageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    VitoView.show(sampleUris().createKeyframesUri(), IMAGE_OPTIONS, CALLER_CONTEXT, mImageView);

    final SwitchCompat switchBackground = (SwitchCompat) view.findViewById(R.id.switch_background);
    switchBackground.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mImageView.setBackground(isChecked ? new CheckerBoardDrawable(getResources()) : null);
          }
        });
  }
}
