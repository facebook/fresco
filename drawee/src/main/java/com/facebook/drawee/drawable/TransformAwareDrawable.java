/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

/**
 * Interface that enables setting a transform callback.
 */
public interface TransformAwareDrawable {

  /**
   * Sets a transform callback.
   *
   * @param transformCallback the transform callback to be set
   */
  void setTransformCallback(TransformCallback transformCallback);
}
