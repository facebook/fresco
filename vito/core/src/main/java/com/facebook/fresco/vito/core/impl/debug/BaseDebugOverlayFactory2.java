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
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.infer.annotation.OkToExtend;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
@OkToExtend
public abstract class BaseDebugOverlayFactory2 implements DebugOverlayFactory2 {

  private final Supplier<Boolean> mDebugOverlayEnabled;

  public BaseDebugOverlayFactory2(Supplier<Boolean> debugOverlayEnabled) {
    mDebugOverlayEnabled = debugOverlayEnabled;
  }

  @Override
  public void update(FrescoDrawable2 drawable, @Nullable ControllerListener2.Extras extras) {
    if (!mDebugOverlayEnabled.get()) {
      return;
    }
    DebugOverlayDrawable overlay = extractOrCreate(drawable);
    overlay.reset();
    setData(overlay, drawable, extras);
  }

  protected abstract void setData(
      DebugOverlayDrawable overlay,
      FrescoDrawable2 drawable,
      @Nullable ControllerListener2.Extras extras);

  private static DebugOverlayDrawable extractOrCreate(FrescoDrawable2 drawable) {
    Drawable existingOverlay = drawable.getOverlayDrawable();
    if (existingOverlay instanceof DebugOverlayDrawable) {
      return (DebugOverlayDrawable) existingOverlay;
    } else if (existingOverlay instanceof DebugOverlayDrawableWrapper) {
      DebugOverlayDrawableWrapper wrapper = (DebugOverlayDrawableWrapper) existingOverlay;
      return wrapper.getDebugOverlayDrawable();
    }

    DebugOverlayDrawable debugOverlay = new DebugOverlayDrawable("v2");
    if (existingOverlay != null) {
      drawable.setOverlayDrawable(new DebugOverlayDrawableWrapper(existingOverlay, debugOverlay));
    } else {
      drawable.setOverlayDrawable(debugOverlay);
    }
    drawable.showOverlayImmediately();
    return debugOverlay;
  }

  private static class DebugOverlayDrawableWrapper extends LayerDrawable {
    private DebugOverlayDrawable mDebugOverlayDrawable;

    public DebugOverlayDrawableWrapper(
        Drawable existingOverlayDrawable, DebugOverlayDrawable debugOverlayDrawable) {
      super(new Drawable[] {existingOverlayDrawable, debugOverlayDrawable});
      mDebugOverlayDrawable = debugOverlayDrawable;
    }

    public DebugOverlayDrawable getDebugOverlayDrawable() {
      return mDebugOverlayDrawable;
    }
  }
}
