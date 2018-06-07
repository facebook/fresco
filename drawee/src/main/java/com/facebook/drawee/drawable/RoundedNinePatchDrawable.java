/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.drawable;

import android.graphics.Canvas;
import android.graphics.drawable.NinePatchDrawable;
import com.facebook.common.internal.VisibleForTesting;

public class RoundedNinePatchDrawable extends RoundedDrawable {

  public RoundedNinePatchDrawable(NinePatchDrawable ninePatchDrawable) {
    super(ninePatchDrawable);
  }

  @Override
  public void draw(Canvas canvas) {
    if (!shouldRound()) {
      super.draw(canvas);
      return;
    }
    updateTransform();
    updatePath();
    canvas.clipPath(mPath);
    super.draw(canvas);
  }

}
