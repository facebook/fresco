/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

/**
 * Callback used to notify about drawable's visibility changes.
 */
public interface VisibilityCallback {

  /**
   * Called when the drawable's visibility changes.
   *
   * @param visible whether or not the drawable is visible
   */
  void onVisibilityChange(boolean visible);

  /**
   * Called when the drawable gets drawn.
   */
  void onDraw();
}
