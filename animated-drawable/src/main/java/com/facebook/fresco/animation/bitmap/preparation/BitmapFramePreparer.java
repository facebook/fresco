/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.bitmap.preparation;

import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.bitmap.BitmapFrameCache;

/**
 * Prepare frames for animated images ahead of time.
 */
public interface BitmapFramePreparer {

  /**
   * Prepare the frame with the given frame number and notify the supplied bitmap frame cache
   * once the frame is ready by calling
   * {@link BitmapFrameCache#onFramePrepared(int, CloseableReference, int)}
   *
   * @param bitmapFrameCache the cache to notify for prepared frames
   * @param animationBackend the backend to prepare frames for
   * @param frameNumber
   * @return
   */
  boolean prepareFrame(
      BitmapFrameCache bitmapFrameCache,
      AnimationBackend animationBackend,
      int frameNumber);
}
