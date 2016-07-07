/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.base;

import javax.annotation.Nullable;

import java.util.List;

import android.graphics.Bitmap;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;

/**
 * The result of decoding an animated image. Contains the {@link AnimatedImage} as well as
 * additional data.
 */
public class AnimatedImageResult {

  private final AnimatedImage mImage;
  private final int mFrameForPreview;
  private @Nullable CloseableReference<Bitmap> mPreviewBitmap;
  private @Nullable List<CloseableReference<Bitmap>> mDecodedFrames;

  AnimatedImageResult(AnimatedImageResultBuilder builder) {
    mImage = Preconditions.checkNotNull(builder.getImage());
    mFrameForPreview = builder.getFrameForPreview();
    mPreviewBitmap = builder.getPreviewBitmap();
    mDecodedFrames = builder.getDecodedFrames();
  }

  private AnimatedImageResult(AnimatedImage image) {
    mImage = Preconditions.checkNotNull(image);
    mFrameForPreview = 0;
  }

  /**
   * Creates an {@link AnimatedImageResult} with no additional options.
   *
   * @param image the image
   * @return the result
   */
  public static AnimatedImageResult forAnimatedImage(AnimatedImage image) {
    return new AnimatedImageResult(image);
  }

  /**
   * Creates an {@link AnimatedImageResultBuilder} for creating an {@link AnimatedImageResult}.
   *
   * @param image the image
   * @return the builder
   */
  public static AnimatedImageResultBuilder newBuilder(AnimatedImage image) {
    return new AnimatedImageResultBuilder(image);
  }

  /**
   * Gets the underlying image.
   *
   * @return the underlying image
   */
  public AnimatedImage getImage() {
    return mImage;
  }

  /**
   * Gets the frame that should be used for the preview image. If the preview bitmap was fetched,
   * this is the frame that it's for.
   *
   * @return the frame that should be used for the preview image
   */
  public int getFrameForPreview() {
    return mFrameForPreview;
  }

  /**
   * Gets a decoded frame. This will only return non-null if the {@code ImageDecodeOptions}
   * were configured to decode all frames at decode time.
   *
   * @param index the index of the frame to get
   * @return a reference to the preview bitmap which must be released by the caller when done or
   *     null if there is no preview bitmap set
   */
  public synchronized @Nullable CloseableReference<Bitmap> getDecodedFrame(int index) {
    if (mDecodedFrames != null) {
      return CloseableReference.cloneOrNull(mDecodedFrames.get(index));
    }
    return null;
  }

  /**
   * Gets whether it has the decoded frame. This will only return true if the
   * {@code ImageDecodeOptions} were configured to decode all frames at decode time.
   *
   * @param index the index of the frame to get
   * @return true if the result has the decoded frame
   */
  public synchronized boolean hasDecodedFrame(int index) {
    return mDecodedFrames != null && mDecodedFrames.get(index) != null;
  }

  /**
   * Gets the bitmap for the preview frame. This will only return non-null if the
   * {@code ImageDecodeOptions} were configured to decode the preview frame.
   *
   * @return a reference to the preview bitmap which must be released by the caller when done or
   *     null if there is no preview bitmap set
   */
  public synchronized CloseableReference<Bitmap> getPreviewBitmap() {
    return CloseableReference.cloneOrNull(mPreviewBitmap);
  }

  /**
   * Disposes the result, which releases the reference to any bitmaps.
   */
  public synchronized void dispose() {
    CloseableReference.closeSafely(mPreviewBitmap);
    mPreviewBitmap = null;
    CloseableReference.closeSafely(mDecodedFrames);
    mDecodedFrames = null;
  }
}
