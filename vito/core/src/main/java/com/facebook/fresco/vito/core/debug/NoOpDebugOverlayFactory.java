/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.debug;

import android.graphics.drawable.Drawable;
import com.facebook.fresco.vito.core.FrescoState;
import javax.annotation.Nullable;

public class NoOpDebugOverlayFactory implements DebugOverlayFactory {

  @Nullable
  @Override
  public Drawable create(FrescoState frescoState) {
    return null;
  }
}
