/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug;

import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class NoOpDebugOverlayFactory2 implements DebugOverlayFactory2 {

  @Override
  public void update(FrescoDrawable2 drawable, @Nullable ControllerListener2.Extras extras) {
    // no-op
  }
}
