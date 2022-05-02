/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.impl;

import android.graphics.Rect;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/** Assisted provider for {@link AnimatedDrawableBackend}. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface AnimatedDrawableBackendProvider {

  /**
   * Creates a new {@link AnimatedDrawableBackend}.
   *
   * @param animatedImageResult the image result.
   * @param bounds the initial bounds for the drawable
   * @return a new {@link AnimatedDrawableBackend}
   */
  AnimatedDrawableBackend get(AnimatedImageResult animatedImageResult, @Nullable Rect bounds);
}
