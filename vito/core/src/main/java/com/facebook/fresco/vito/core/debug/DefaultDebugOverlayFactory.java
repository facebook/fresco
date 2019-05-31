/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.debug;

import android.graphics.drawable.Drawable;
import com.facebook.common.internal.Supplier;
import com.facebook.fresco.vito.core.FrescoState;
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
    DebugOverlayDrawable drawable = new DebugOverlayDrawable();
    drawable.addDebugData("URI", "" + frescoState.getUri());
    return drawable;
  }
}
