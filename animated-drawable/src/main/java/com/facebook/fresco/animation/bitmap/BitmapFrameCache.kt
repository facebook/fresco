/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend.FrameType

/** Bitmap frame cache that is used for animated images. */
interface BitmapFrameCache {

  interface FrameCacheListener {
    /**
     * Called when the frame for the given frame number has been put in the frame cache.
     *
     * @param bitmapFrameCache the frame cache that holds the frame
     * @param frameNumber the cached frame number
     */
    fun onFrameCached(bitmapFrameCache: BitmapFrameCache, frameNumber: Int)

    /**
     * Called when the frame for the given frame number has been evicted from the frame cache.
     *
     * @param bitmapFrameCache the frame cache that evicted the frame
     * @param frameNumber the frame number of the evicted frame
     */
    fun onFrameEvicted(bitmapFrameCache: BitmapFrameCache, frameNumber: Int)
  }

  /**
   * Get the cached frame for the given frame number.
   *
   * @param frameNumber the frame number to get the cached frame for
   * @return the cached frame or null if not cached
   */
  fun getCachedFrame(frameNumber: Int): CloseableReference<Bitmap>?

  /**
   * Get a fallback frame for the given frame number. This method is called if all other attempts to
   * draw a frame failed. The bitmap returned could for example be the last drawn frame (if any).
   *
   * @param frameNumber the frame number to get the fallback
   * @return the fallback frame or null if not cached
   */
  fun getFallbackFrame(frameNumber: Int): CloseableReference<Bitmap>?

  /**
   * Return a reusable bitmap that should be used to render the given frame.
   *
   * @param frameNumber the frame number to be rendered
   * @param width the width of the target bitmap
   * @param height the height of the target bitmap
   * @return the reusable bitmap or null if no reusable bitmaps available
   */
  fun getBitmapToReuseForFrame(
      frameNumber: Int,
      width: Int,
      height: Int
  ): CloseableReference<Bitmap>?

  /**
   * Check whether the cache contains a certain frame.
   *
   * @param frameNumber the frame number to check
   * @return true if the frame is cached
   */
  operator fun contains(frameNumber: Int): Boolean

  /** @return the size in bytes of all cached data */
  val sizeInBytes: Int

  /** Send the list of frames when the animation frames are loaded */
  fun onAnimationPrepared(frameBitmaps: Map<Int, CloseableReference<Bitmap>>): Boolean = true

  /** Clear the cache. */
  fun clear()

  /** Indicates if animation is loaded in cache and ready for usage */
  fun isAnimationReady(): Boolean = false

  /**
   * Callback when the given bitmap has been drawn to a canvas. This bitmap can either be a reused
   * bitmap returned by [getBitmapToReuseForFrame(int, int, int)] or a new bitmap.
   *
   * Note: the implementation of this interface must manually clone the given bitmap reference if it
   * wants to hold on to the bitmap. The original reference will be automatically closed after this
   * call.
   *
   * @param frameNumber the frame number that has been rendered
   * @param bitmapReference the bitmap reference that has been rendered
   * @param frameType the frame type that has been rendered
   */
  fun onFrameRendered(
      frameNumber: Int,
      bitmapReference: CloseableReference<Bitmap>,
      @FrameType frameType: Int
  )

  /**
   * Callback when a bitmap reference for a given frame has been prepared for future rendering.
   *
   * This method is called ahead of render time (i.e. when future frames have been prepared in the
   * background), whereas [onFrameRendered(int, CloseableReference, int)] is invoked when the actual
   * frame has been drawn on a Canvas.
   *
   * The supplied bitmap reference can either hold a reused bitmap returned by
   * [getBitmapToReuseForFrame(int, int, int)] or a new bitmap as indicated by the frame type
   * parameter.
   *
   * Note: the implementation of this interface must manually clone the given bitmap reference if it
   * wants to hold on to the bitmap. The original reference will be automatically closed after this
   * call.
   *
   * @param frameNumber the frame number of the passed bitmapReference
   * @param bitmapReference the bitmap reference that has been prepared for future rendering
   * @param frameType the frame type of the prepared frame
   * @return true if the frame has been successfully cached
   */
  fun onFramePrepared(
      frameNumber: Int,
      bitmapReference: CloseableReference<Bitmap>,
      @FrameType frameType: Int
  )

  /**
   * Set a frame cache listener that gets notified about caching events.
   *
   * @param frameCacheListener the listener to use
   */
  fun setFrameCacheListener(frameCacheListener: FrameCacheListener?)
}
