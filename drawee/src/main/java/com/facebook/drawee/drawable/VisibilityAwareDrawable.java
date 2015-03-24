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
 * Interface that enables setting a visibility callback.
 */
public interface VisibilityAwareDrawable {

  /**
   * Sets a visibility callback.
   *
   * @param visibilityCallback the visibility callback to be set
   */
  public void setVisibilityCallback(VisibilityCallback visibilityCallback);
}
