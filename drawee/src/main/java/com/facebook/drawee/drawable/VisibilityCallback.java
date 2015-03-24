/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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
  public void onVisibilityChange(boolean visible);

  /**
   * Called when the drawable gets drawn.
   */
  public void onDraw();
}
