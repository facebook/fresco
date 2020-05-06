/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug;

import com.facebook.fresco.vito.core.FrescoDrawable2;

public interface DebugOverlayFactory2 {

  /**
   * Create a new debug overlay for the given FrescoState. Returns null when debug overlays are
   * disabled.
   *
   * @param drawable the drawable to update the overlay for
   */
  void update(FrescoDrawable2 drawable);
}
