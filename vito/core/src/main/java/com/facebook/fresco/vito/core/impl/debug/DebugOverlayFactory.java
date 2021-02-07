/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug;

import android.graphics.drawable.Drawable;
import com.facebook.fresco.vito.core.FrescoState;
import javax.annotation.Nullable;

public interface DebugOverlayFactory {

  /**
   * Create a new debug overlay for the given FrescoState. Returns null when debug overlays are
   * disabled.
   *
   * @param frescoState the state to create the overlay for
   * @return the debug overlay drawable to be used
   */
  @Nullable
  Drawable create(FrescoState frescoState);
}
