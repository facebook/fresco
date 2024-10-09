/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.cache

import android.graphics.Bitmap
import android.util.SparseArray
import androidx.annotation.VisibleForTesting
import com.facebook.common.logging.FLog
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend.FrameType
import com.facebook.fresco.animation.bitmap.BitmapFrameCache
import com.facebook.fresco.animation.bitmap.BitmapFrameCache.FrameCacheListener
import com.facebook.imagepipeline.animated.impl.AnimatedFrameCache
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import com.facebook.imagepipeline.image.ImmutableQualityInfo
import com.facebook.imageutils.BitmapUtil
import javax.annotation.concurrent.GuardedBy

/** Bitmap frame cache that uses Fresco's [AnimatedFrameCache] to cache frames. */
class FrescoFrameCache(
    private val animatedFrameCache: AnimatedFrameCache,
    private val enableBitmapReusing: Boolean
) : BitmapFrameCache {

  @GuardedBy("this")
  private val preparedPendingFrames = SparseArray<CloseableReference<CloseableImage>?>()

  @GuardedBy("this") private var lastRenderedItem: CloseableReference<CloseableImage>? = null

  @Synchronized
  override fun getCachedFrame(frameNumber: Int): CloseableReference<Bitmap>? =
      convertToBitmapReferenceAndClose(animatedFrameCache[frameNumber])

  @Synchronized
  override fun getFallbackFrame(frameNumber: Int): CloseableReference<Bitmap>? =
      convertToBitmapReferenceAndClose(CloseableReference.cloneOrNull(lastRenderedItem))

  @Synchronized
  override fun getBitmapToReuseForFrame(
      frameNumber: Int,
      width: Int,
      height: Int
  ): CloseableReference<Bitmap>? {
    if (!enableBitmapReusing) {
      return null
    }
    return convertToBitmapReferenceAndClose(animatedFrameCache.forReuse)
  }

  @Synchronized
  override fun contains(frameNumber: Int): Boolean = animatedFrameCache.contains(frameNumber)

  @get:Synchronized
  override val sizeInBytes: Int
    get() = // This currently does not include the size of the animated frame cache
    getBitmapSizeBytes(lastRenderedItem) + preparedPendingFramesSizeBytes

  @Synchronized
  override fun clear() {
    CloseableReference.closeSafely(lastRenderedItem)
    lastRenderedItem = null
    for (i in 0 until preparedPendingFrames.size()) {
      CloseableReference.closeSafely(preparedPendingFrames.valueAt(i))
    }
    preparedPendingFrames.clear()
    // The frame cache will free items when needed
  }

  @Synchronized
  override fun onFrameRendered(
      frameNumber: Int,
      bitmapReference: CloseableReference<Bitmap>,
      @BitmapAnimationBackend.FrameType frameType: Int
  ) {
    checkNotNull(bitmapReference)

    // Close up prepared references.
    removePreparedReference(frameNumber)

    // Create the new image reference and cache it.
    var closableReference: CloseableReference<CloseableImage?>? = null
    try {
      closableReference = createImageReference(bitmapReference)
      if (closableReference != null) {
        CloseableReference.closeSafely(lastRenderedItem)
        lastRenderedItem = animatedFrameCache.cache(frameNumber, closableReference)
      }
    } finally {
      CloseableReference.closeSafely(closableReference)
    }
  }

  @Synchronized
  override fun onFramePrepared(
      frameNumber: Int,
      bitmapReference: CloseableReference<Bitmap>,
      @BitmapAnimationBackend.FrameType frameType: Int
  ) {
    checkNotNull(bitmapReference)
    var closableReference: CloseableReference<CloseableImage?>? = null
    try {
      closableReference = createImageReference(bitmapReference)
      if (closableReference == null) {
        return
      }
      val newReference = animatedFrameCache.cache(frameNumber, closableReference)
      if (CloseableReference.isValid(newReference)) {
        val oldReference = preparedPendingFrames[frameNumber]
        CloseableReference.closeSafely(oldReference)
        // For performance reasons, we don't clone the reference and close the original one
        // but cache the reference directly.
        preparedPendingFrames.put(frameNumber, newReference)
        FLog.v(
            TAG,
            "cachePreparedFrame(%d) cached. Pending frames: %s",
            frameNumber,
            preparedPendingFrames)
      }
    } finally {
      CloseableReference.closeSafely(closableReference)
    }
  }

  override fun setFrameCacheListener(frameCacheListener: FrameCacheListener?) {
    // TODO (t15557326) Not supported for now
  }

  @get:Synchronized
  private val preparedPendingFramesSizeBytes: Int
    get() {
      var size = 0
      for (i in 0 until preparedPendingFrames.size()) {
        size += getBitmapSizeBytes(preparedPendingFrames.valueAt(i))
      }
      return size
    }

  @Synchronized
  private fun removePreparedReference(frameNumber: Int) {
    val existingPendingReference = preparedPendingFrames[frameNumber]
    if (existingPendingReference != null) {
      preparedPendingFrames.delete(frameNumber)
      CloseableReference.closeSafely(existingPendingReference)
      FLog.v(
          TAG,
          "removePreparedReference(%d) removed. Pending frames: %s",
          frameNumber,
          preparedPendingFrames)
    }
  }

  override fun onAnimationPrepared(frameBitmaps: Map<Int, CloseableReference<Bitmap>>): Boolean =
      true

  override fun isAnimationReady(): Boolean = false

  companion object {
    private val TAG: Class<*> = FrescoFrameCache::class.java

    /**
     * Converts the given image reference to a bitmap reference and closes the original image
     * reference.
     *
     * @param closeableImage the image to convert. It will be closed afterwards and will be invalid
     * @return the closeable bitmap reference to be used
     */
    @JvmStatic
    @VisibleForTesting
    fun convertToBitmapReferenceAndClose(
        closeableImage: CloseableReference<CloseableImage>?
    ): CloseableReference<Bitmap>? {
      try {
        if (CloseableReference.isValid(closeableImage) &&
            closeableImage!!.get() is CloseableStaticBitmap) {
          val closeableStaticBitmap = closeableImage.get() as CloseableStaticBitmap
          if (closeableStaticBitmap != null) {
            // We return a clone of the underlying bitmap reference that has to be manually closed
            // and then close the passed CloseableStaticBitmap in order to preserve correct
            // cache size calculations.
            return closeableStaticBitmap.cloneUnderlyingBitmapReference()
          }
        }
        // Not a bitmap reference, so we return null
        return null
      } finally {
        CloseableReference.closeSafely(closeableImage)
      }
    }

    private fun getBitmapSizeBytes(imageReference: CloseableReference<CloseableImage>?): Int {
      if (!CloseableReference.isValid(imageReference)) {
        return 0
      }
      return getBitmapSizeBytes(imageReference!!.get())
    }

    private fun getBitmapSizeBytes(image: CloseableImage?): Int {
      if (image !is CloseableBitmap) {
        return 0
      }
      return BitmapUtil.getSizeInBytes(image.underlyingBitmap)
    }

    private fun createImageReference(
        bitmapReference: CloseableReference<Bitmap>
    ): CloseableReference<CloseableImage?>? {
      // The given CloseableStaticBitmap will be cached and then released by the resource releaser
      // of the closeable reference
      val closeableImage =
          CloseableStaticBitmap.of(bitmapReference, ImmutableQualityInfo.FULL_QUALITY, 0)
      return CloseableReference.of(closeableImage)
    }
  }
}
