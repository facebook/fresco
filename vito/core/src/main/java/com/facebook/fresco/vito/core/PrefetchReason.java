/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.STRICT)
public enum PrefetchReason {
  /** the image is needed right away to show on the viewport */
  ON_SCREEN,

  /** the image will be automatically become visible on the viewport after several seconds */
  ON_TIMEOUT,

  /**
   * the image will be visible when user scroll down to reveal it, highly likely if people stay on
   * the same surface
   */
  ON_SCROLL,

  /**
   * the image will not be visible until people tap on something to show a different surface than
   * the current one
   */
  ON_TAP,
}
