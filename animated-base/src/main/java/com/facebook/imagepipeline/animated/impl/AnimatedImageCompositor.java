/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.impl;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo.DisposalMethod;
import com.facebook.imagepipeline.animated.base.AnimatedImage;

/**
 * Contains the logic for compositing the frames of an {@link AnimatedImage}. Animated image
 * formats like GIF and WebP support inter-frame compression where a subsequent frame may require
 * being blended on a previous frame in order to render the full frame. This class encapsulates
 * the behavior to be able to render any frame of the image. Designed to work with a cache
 * via a Callback.
 */
public class AnimatedImageCompositor {

  /**
   * Callback for caching.
   */
  public interface Callback {

    /**
     * Called from within {@link #renderFrame} to let the caller know that while trying generate
     * the requested frame, an earlier frame was generated. This allows the caller to optionally
     * cache the intermediate result. The caller must copy the Bitmap if it wishes to cache it
     * as {@link #renderFrame} will continue using it generate the requested frame.
     *
     * @param frameNumber the frame number of the intermediate result
     * @param bitmap the bitmap which must not be modified or directly cached
     */
    void onIntermediateResult(int frameNumber, Bitmap bitmap);

    /**
     * Called from within {@link #renderFrame} to ask the caller for a cached bitmap for the
     * specified frame number. If the caller has the bitmap cached, it can greatly reduce the
     * work required to render the requested frame.
     *
     * @param frameNumber the frame number to get
     * @return a reference to the bitmap. The ownership of the reference is passed to the caller
     *    who must close it.
     */
    CloseableReference<Bitmap> getCachedBitmap(int frameNumber);
  }

  private final AnimatedDrawableBackend mAnimatedDrawableBackend;
  private final Callback mCallback;
  private final Paint mTransparentFillPaint;

  public AnimatedImageCompositor(
      AnimatedDrawableBackend animatedDrawableBackend,
      Callback callback) {
    mAnimatedDrawableBackend = animatedDrawableBackend;
    mCallback = callback;
    mTransparentFillPaint = new Paint();
    mTransparentFillPaint.setColor(Color.TRANSPARENT);
    mTransparentFillPaint.setStyle(Paint.Style.FILL);
    mTransparentFillPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));
  }

  /**
   * Renders the specified frame. Only should be called on the rendering thread.
   *
   * @param frameNumber the frame to render
   * @param bitmap the bitmap to render into
   */
  public void renderFrame(int frameNumber, Bitmap bitmap) {
    Canvas canvas = new Canvas(bitmap);
    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.SRC);

    // If blending is required, prepare the canvas with the nearest cached frame.
    int nextIndex;
    AnimatedDrawableFrameInfo frameInfo = mAnimatedDrawableBackend.getFrameInfo(frameNumber);
    if (frameInfo.shouldBlendWithPreviousFrame && frameNumber > 0) {
      // Blending is required. nextIndex points to the next index to render onto the canvas.
      nextIndex = prepareCanvasWithClosestCachedFrame(frameNumber - 1, canvas);
    } else {
      // Blending isn't required. Start at the frame we're trying to render.
      nextIndex = frameNumber;
    }

    // Iterate from nextIndex to the frame number just preceding the one we're trying to render
    // and composite them in order according to the Disposal Method.
    for (int index = nextIndex; index < frameNumber; index++) {
      frameInfo = mAnimatedDrawableBackend.getFrameInfo(index);
      DisposalMethod disposalMethod = frameInfo.disposalMethod;
      if (disposalMethod == DisposalMethod.DISPOSE_TO_PREVIOUS) {
        continue;
      }
      mAnimatedDrawableBackend.renderFrame(index, canvas);
      mCallback.onIntermediateResult(index, bitmap);
      if (disposalMethod == DisposalMethod.DISPOSE_TO_BACKGROUND) {
        disposeToBackground(canvas, frameInfo);
      }
    }

    // Finally, we render the current frame. We don't dispose it.
    mAnimatedDrawableBackend.renderFrame(frameNumber, canvas);
  }

  /**
   * Return value for {@link #isFrameNeededForRendering} used in the compositing logic.
   */
  private enum FrameNeededResult {
    /** The frame is required to render the next frame */
    REQUIRED,

    /** The frame is not required to render the next frame. */
    NOT_REQUIRED,

    /** Skip this frame and keep going. Used for GIF's DISPOSE_TO_PREVIOUS */
    SKIP,

    /** Stop processing at this frame. This means the image didn't specify the disposal method */
    ABORT
  }

  /**
   * Given a frame number, prepares the canvas to render based on the nearest cached frame
   * at or before the frame. On return the canvas will be prepared as if the nearest cached
   * frame had been rendered and disposed. The returned index is the next frame that needs to be
   * composited onto the canvas.
   *
   * @param previousFrameNumber the frame number that is ones less than the one we're rendering
   * @param canvas the canvas to prepare
   * @return the index of the the next frame to process
   */
  private int prepareCanvasWithClosestCachedFrame(int previousFrameNumber, Canvas canvas) {
    for (int index = previousFrameNumber; index >= 0; index--) {
      FrameNeededResult neededResult = isFrameNeededForRendering(index);
      switch (neededResult) {
        case REQUIRED:
          AnimatedDrawableFrameInfo frameInfo = mAnimatedDrawableBackend.getFrameInfo(index);
          CloseableReference<Bitmap> startBitmap = mCallback.getCachedBitmap(index);
          if (startBitmap != null) {
            try {
              canvas.drawBitmap(startBitmap.get(), 0, 0, null);
              if (frameInfo.disposalMethod == DisposalMethod.DISPOSE_TO_BACKGROUND) {
                disposeToBackground(canvas, frameInfo);
              }
              return index + 1;
            } finally {
              startBitmap.close();
            }
          } else {
            if (!frameInfo.shouldBlendWithPreviousFrame) {
              return index;
            } else {
              // Keep going.
              break;
            }
          }
        case NOT_REQUIRED:
          return index + 1;
        case ABORT:
          return index;
        case SKIP:
        default:
          // Keep going.
      }
    }
    return 0;
  }

  private void disposeToBackground(Canvas canvas, AnimatedDrawableFrameInfo frameInfo) {
    canvas.drawRect(
        frameInfo.xOffset,
        frameInfo.yOffset,
        frameInfo.xOffset + frameInfo.width,
        frameInfo.yOffset + frameInfo.height,
        mTransparentFillPaint);
  }

  /**
   * Returns whether the specified frame is needed for rendering the next frame. This is part of
   * the compositing logic. See {@link FrameNeededResult} for more info about the results.
   *
   * @param index the frame to check
   * @return whether the frame is required taking into account special conditions
   */
  private FrameNeededResult isFrameNeededForRendering(int index) {
    AnimatedDrawableFrameInfo frameInfo = mAnimatedDrawableBackend.getFrameInfo(index);
    DisposalMethod disposalMethod = frameInfo.disposalMethod;
    if (disposalMethod == DisposalMethod.DISPOSE_DO_NOT) {
      // Need this frame so keep going.
      return FrameNeededResult.REQUIRED;
    } else if (disposalMethod == DisposalMethod.DISPOSE_TO_BACKGROUND) {
      if (frameInfo.xOffset == 0 &&
          frameInfo.yOffset == 0 &&
          frameInfo.width == mAnimatedDrawableBackend.getRenderedWidth() &&
          frameInfo.height == mAnimatedDrawableBackend.getRenderedHeight()) {
        // The frame covered the whole image and we're disposing to background,
        // so we don't even need to draw this frame.
        return FrameNeededResult.NOT_REQUIRED;
      } else {
        // We need to draw the image. Then erase the part the previous frame covered.
        // So keep going.
        return FrameNeededResult.REQUIRED;
      }
    } else if (disposalMethod == DisposalMethod.DISPOSE_TO_PREVIOUS) {
      return FrameNeededResult.SKIP;
    } else {
      return FrameNeededResult.ABORT;
    }
  }
}
