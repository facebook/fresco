/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.flipper.plugins.fresco;

public interface FrescoFlipperDebugPrefHelper {

  interface Listener {
    void onEnabledStatusChanged(boolean enabled);
  }

  void setDebugOverlayEnabled(boolean enabled);

  boolean isDebugOverlayEnabled();

  void setDebugOverlayEnabledListener(Listener l);
}
