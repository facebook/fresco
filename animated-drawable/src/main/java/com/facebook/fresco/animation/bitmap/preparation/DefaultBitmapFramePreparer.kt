/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.preparation

import android.graphics.Bitmap
import android.util.SparseArray
import com.facebook.common.logging.FLog
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.animation.backend.AnimationBackend
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend.FrameType
import com.facebook.fresco.animation.bitmap.BitmapFrameCache
import com.facebook.fresco.animation.bitmap.BitmapFrameRenderer
import com.facebook.fresco.animation.bitmap.preparation.DefaultBitmapFramePreparer.FrameDecodeRunnable
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import java.lang.RuntimeException
import java.util.concurrent.ExecutorService

/**
 * Default bitmap frame preparer that uses the given [ExecutorService] to schedule jobs. An instance
 * of this class can be shared between multiple animated images.
 */
class DefaultBitmapFramePreparer(
    private val platformBitmapFactory: PlatformBitmapFactory,
    private val bitmapFrameRenderer: BitmapFrameRenderer,
    private val bitmapConfig: Bitmap.Config,
    private val executorService: ExecutorService
) : BitmapFramePreparer {

  private val pendingFrameDecodeJobs: SparseArray<Runnable?> = SparseArray()

  override fun prepareFrame(
      bitmapFrameCache: BitmapFrameCache,
      animationBackend: AnimationBackend,
      frameNumber: Int
  ): Boolean {
    // Create a unique ID to identify the frame for the given backend.
    val frameId = getUniqueId(animationBackend, frameNumber)
    synchronized(pendingFrameDecodeJobs) {

      // Check if already scheduled.
      if (pendingFrameDecodeJobs[frameId] != null) {
        FLog.v(TAG, "Already scheduled decode job for frame %d", frameNumber)
        return true
      }
      // Check if already cached.
      if (bitmapFrameCache.contains(frameNumber)) {
        FLog.v(TAG, "Frame %d is cached already.", frameNumber)
        return true
      }
      val frameDecodeRunnable =
          FrameDecodeRunnable(animationBackend, bitmapFrameCache, frameNumber, frameId)
      pendingFrameDecodeJobs.put(frameId, frameDecodeRunnable)
      executorService.execute(frameDecodeRunnable)
    }
    return true
  }

  private inner class FrameDecodeRunnable(
      private val animationBackend: AnimationBackend,
      private val bitmapFrameCache: BitmapFrameCache,
      private val frameNumber: Int,
      private val hashCode: Int
  ) : Runnable {
    override fun run() {
      try {
        // If we have a cached frame already, we don't need to do anything.
        if (bitmapFrameCache.contains(frameNumber)) {
          FLog.v(TAG, "Frame %d is cached already.", frameNumber)
          return
        }

        // Prepare the frame.
        if (prepareFrameAndCache(frameNumber, BitmapAnimationBackend.Companion.FRAME_TYPE_REUSED)) {
          FLog.v(TAG, "Prepared frame frame %d.", frameNumber)
        } else {
          FLog.e(TAG, "Could not prepare frame %d.", frameNumber)
        }
      } finally {
        synchronized(pendingFrameDecodeJobs) { pendingFrameDecodeJobs.remove(hashCode) }
      }
    }

    private fun prepareFrameAndCache(frameNumber: Int, @FrameType frameType: Int): Boolean {
      var bitmapReference: CloseableReference<Bitmap?>? = null
      val created: Boolean
      val nextFrameType: Int
      try {
        when (frameType) {
          BitmapAnimationBackend.Companion.FRAME_TYPE_REUSED -> {
            bitmapReference =
                bitmapFrameCache.getBitmapToReuseForFrame(
                    frameNumber, animationBackend.intrinsicWidth, animationBackend.intrinsicHeight)
            nextFrameType = BitmapAnimationBackend.Companion.FRAME_TYPE_CREATED
          }
          BitmapAnimationBackend.Companion.FRAME_TYPE_CREATED -> {
            bitmapReference =
                try {
                  platformBitmapFactory.createBitmap(
                      animationBackend.intrinsicWidth,
                      animationBackend.intrinsicHeight,
                      bitmapConfig)
                } catch (e: RuntimeException) {
                  // Failed to create the bitmap for the frame, return and report that we could not

                  // prepare the frame.
                  FLog.w(TAG, "Failed to create frame bitmap", e)
                  return false
                }
            nextFrameType = BitmapAnimationBackend.Companion.FRAME_TYPE_UNKNOWN
          }
          else -> return false
        }
        // Try to render and cache the frame
        created = renderFrameAndCache(frameNumber, bitmapReference, frameType)
      } finally {
        CloseableReference.closeSafely(bitmapReference)
      }
      return if (created || nextFrameType == BitmapAnimationBackend.Companion.FRAME_TYPE_UNKNOWN) {
        created
      } else {
        prepareFrameAndCache(frameNumber, nextFrameType)
      }
    }

    private fun renderFrameAndCache(
        frameNumber: Int,
        bitmapReference: CloseableReference<Bitmap?>?,
        @FrameType frameType: Int
    ): Boolean {
      // Check if the bitmap is valid
      if (!CloseableReference.isValid(bitmapReference)) {
        return false
      }
      // Try to render the frame
      if (!bitmapFrameRenderer.renderFrame(frameNumber, bitmapReference!!.get())) {
        return false
      }
      FLog.v(TAG, "Frame %d ready.", this.frameNumber)
      // Cache the frame
      synchronized(pendingFrameDecodeJobs) {
        bitmapFrameCache.onFramePrepared(this.frameNumber, bitmapReference, frameType)
      }
      return true
    }
  }

  companion object {
    private val TAG: Class<*> = DefaultBitmapFramePreparer::class.java
    private fun getUniqueId(backend: AnimationBackend, frameNumber: Int): Int {
      var result = backend.hashCode()
      result = 31 * result + frameNumber
      return result
    }
  }
}
