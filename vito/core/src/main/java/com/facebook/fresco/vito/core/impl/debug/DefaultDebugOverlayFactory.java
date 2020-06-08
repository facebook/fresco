/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug;

import android.graphics.drawable.Drawable;
import com.facebook.common.internal.Supplier;
import com.facebook.drawee.backends.pipeline.debug.DebugOverlayImageOriginColor;
import com.facebook.drawee.backends.pipeline.info.ImageOriginUtils;
import com.facebook.fresco.vito.core.FrescoState;
import java.util.Locale;
import javax.annotation.Nullable;

public class DefaultDebugOverlayFactory implements DebugOverlayFactory {

  private final Supplier<Boolean> mDebugOverlayEnabled;

  public DefaultDebugOverlayFactory(Supplier<Boolean> debugOverlayEnabled) {
    mDebugOverlayEnabled = debugOverlayEnabled;
  }

  @Override
  @Nullable
  public Drawable create(FrescoState frescoState) {
    if (!mDebugOverlayEnabled.get()) {
      return null;
    }
    DebugOverlayDrawable drawable = new DebugOverlayDrawable("v1");
    drawable.addDebugData("ID", "" + frescoState.getStringId());
    final int origin = frescoState.getImageOrigin();
    drawable.addDebugData(
        "origin",
        ImageOriginUtils.toString(origin),
        DebugOverlayImageOriginColor.getImageOriginColor(origin));
    drawable.addDebugData("URI", "" + frescoState.getUri());
    drawable.addDebugData(
        "D",
        String.format(
            Locale.US, "%dx%d", frescoState.getTargetHeightPx(), frescoState.getTargetWidthPx()));

    if (frescoState.getImageOptions().getActualImageScaleType() != null) {
      drawable.addDebugData(
          "scale", String.valueOf(frescoState.getImageOptions().getActualImageScaleType()));
    }

    return drawable;
  }
}
