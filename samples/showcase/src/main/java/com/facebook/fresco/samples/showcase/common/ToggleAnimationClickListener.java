/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.common;

import android.graphics.drawable.Animatable;
import android.view.View;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.GenericDraweeView;

/**
 * Simple View click listener that toggles a DraweeView's animation if an animated image is
 * currently displayed.
 */
public class ToggleAnimationClickListener implements View.OnClickListener {

  private final GenericDraweeView mDraweeView;

  public ToggleAnimationClickListener(GenericDraweeView draweeView) {
    mDraweeView = draweeView;
  }

  @Override
  public void onClick(View v) {
    DraweeController controller = mDraweeView.getController();
    if (controller == null) {
      return;
    }
    Animatable animatable = controller.getAnimatable();
    if (animatable == null) {
      return;
    }
    if (animatable.isRunning()) {
      animatable.stop();
    } else {
      animatable.start();
    }
  }
}
