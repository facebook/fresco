/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug

import com.facebook.fresco.ui.common.ControllerListener2.Extras
import com.facebook.fresco.vito.core.impl.FrescoDrawable2

class NoOpDebugOverlayFactory2 : DebugOverlayFactory2 {
  override fun update(drawable: FrescoDrawable2, extras: Extras?) {
    // no-op
  }
}
