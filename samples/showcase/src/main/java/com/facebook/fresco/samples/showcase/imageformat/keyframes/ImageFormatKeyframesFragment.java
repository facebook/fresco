/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.imageformat.keyframes;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.SwitchCompat;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.CustomImageFormatConfigurator;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.CheckerBoardDrawable;

/** Fragment using a SimpleDraweeView to display a Keyframes animation */
public class ImageFormatKeyframesFragment extends BaseShowcaseFragment {

  private SimpleDraweeView mSimpleDraweeView;

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

  @Override
  public int getTitleId() {
    return R.string.format_keyframes_title;
  }

  @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
  private void initAnimation(View view) {
    mSimpleDraweeView = (SimpleDraweeView) view.findViewById(R.id.drawee_view);
    mSimpleDraweeView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    DraweeController controller =
        Fresco.newDraweeControllerBuilder()
            .setOldController(mSimpleDraweeView.getController())
            .setUri(sampleUris().createKeyframesUri())
            .setAutoPlayAnimations(true)
            .build();
    mSimpleDraweeView.setController(controller);

    final SwitchCompat switchBackground = (SwitchCompat) view.findViewById(R.id.switch_background);
    switchBackground.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mSimpleDraweeView
                .getHierarchy()
                .setBackgroundImage(isChecked ? new CheckerBoardDrawable(getResources()) : null);
          }
        });
  }
}
