/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
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
  void setVisibilityCallback(VisibilityCallback visibilityCallback);
}
