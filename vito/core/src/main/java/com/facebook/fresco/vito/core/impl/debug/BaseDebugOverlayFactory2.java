/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import com.facebook.common.internal.Supplier;
import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.infer.annotation.OkToExtend;

@OkToExtend
public abstract class BaseDebugOverlayFactory2 implements DebugOverlayFactory2 {

  private static final int NUMBER_OF_DEBUG_OVERLAY_WRAPPER_LAYERS = 2;
  private static final int DEBUG_OVERLAY_DRAWABLE_INDEX = 1;

  private final Supplier<Boolean> mDebugOverlayEnabled;

  public BaseDebugOverlayFactory2(Supplier<Boolean> debugOverlayEnabled) {
    mDebugOverlayEnabled = debugOverlayEnabled;
  }

  @Override
  public void update(FrescoDrawable2 drawable) {
    if (!mDebugOverlayEnabled.get()) {
      return;
    }
    DebugOverlayDrawable overlay = extractOrCreate(drawable);
    overlay.reset();
    setData(overlay, drawable);
  }

  protected abstract void setData(DebugOverlayDrawable overlay, FrescoDrawable2 drawable);

  private DebugOverlayDrawable extractOrCreate(FrescoDrawable2 drawable) {
    Drawable existingOverlay = drawable.getOverlayDrawable();
    DebugOverlayDrawable overlay = null;
    if (existingOverlay instanceof LayerDrawable) {
      LayerDrawable layers = (LayerDrawable) existingOverlay;
      if (layers.getNumberOfLayers() == NUMBER_OF_DEBUG_OVERLAY_WRAPPER_LAYERS
          && layers.getDrawable(DEBUG_OVERLAY_DRAWABLE_INDEX) instanceof DebugOverlayDrawable) {
        overlay = (DebugOverlayDrawable) layers.getDrawable(DEBUG_OVERLAY_DRAWABLE_INDEX);
      }
    }
    if (overlay == null) {
      overlay = new DebugOverlayDrawable("V2");
      drawable.setOverlayDrawable(new LayerDrawable(new Drawable[] {existingOverlay, overlay}));
      drawable.showOverlayImmediately();
    }
    return overlay;
  }
}
