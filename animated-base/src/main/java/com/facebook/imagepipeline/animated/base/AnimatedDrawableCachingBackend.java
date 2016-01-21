/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.base;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.facebook.common.references.CloseableReference;

/**
 * A specialized version of {@link AnimatedDrawableBackend} that adds caching and prefetching.
 */
public interface AnimatedDrawableCachingBackend extends AnimatedDrawableBackend {

  /**
   * Gets the bitmap for the specified frame number. The bitmap should be the size of the
   * rendered image according to {@link #getRenderedWidth()} and {@link #getRenderedHeight()} and
   * ready to be drawn in the Drawable's draw method.
   *
   * @param frameNumber the frame number (0-based)
   * @return a reference to the bitmap which must be released by the caller when done or null
   *    to indicate to the caller that the bitmap is not ready and it should try again later
   */
  CloseableReference<Bitmap> getBitmapForFrame(int frameNumber);

  /**
   * Gets the bitmap for the preview frame. This will only return non-null if the
   * {@code ImageDecodeOptions} were configured to decode the preview frame.
   *
   * @return a reference to the preview bitmap which must be released by the caller when done or
   *    null if there is no preview bitmap set
   */
  CloseableReference<Bitmap> getPreviewBitmap();

  /**
   * Appends a string about the state of the backend that might be useful for debugging.
   *
   * @param sb the builder to append to
   */
  void appendDebugOptionString(StringBuilder sb);

  // Overridden to restrict the return type.
  @Override
  AnimatedDrawableCachingBackend forNewBounds(Rect bounds);
}
