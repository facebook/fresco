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

import com.facebook.common.references.CloseableReference;

/**
 * Builder for {@link AnimatedImageResult}.
 */
public class AnimatedImageResultBuilder {

  private final AnimatedImage mImage;
  private CloseableReference<Bitmap> mPreviewBitmap;
  private int mFrameForPreview;

  AnimatedImageResultBuilder(AnimatedImage image) {
    mImage = image;
  }

  /**
   * Gets the image for the result.
   *
   * @return the image
   */
  public AnimatedImage getImage() {
    return mImage;
  }

  /**
   * Gets the preview bitmap. This method returns a new reference. The caller must close it.
   *
   * @return the reference to the preview bitmap or null if none was set.
   */
  public CloseableReference<Bitmap> getPreviewBitmap() {
    return CloseableReference.cloneOrNull(mPreviewBitmap);
  }

  /**
   * Sets a preview bitmap.
   *
   * @param previewBitmap the preview. Them method clones the reference.
   * @return this builder
   */
  public AnimatedImageResultBuilder setPreviewBitmap(CloseableReference<Bitmap> previewBitmap) {
    mPreviewBitmap = CloseableReference.cloneOrNull(previewBitmap);
    return this;
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
   * Sets the frame that should be used for the preview image. If the preview bitmap was fetched,
   * this is the frame that it's for.
   *
   * @return the frame that should be used for the preview image
   */
  public AnimatedImageResultBuilder setFrameForPreview(int frameForPreview) {
    mFrameForPreview = frameForPreview;
    return this;
  }

  /**
   * Builds the {@link AnimatedImageResult}. The preview bitmap is closed after build is called, so
   * this should not be called more than once or the preview will be lost after the first call.
   *
   * @return the result
   */
  public AnimatedImageResult build() {
    try {
      return new AnimatedImageResult(this);
    } finally {
      CloseableReference.closeSafely(mPreviewBitmap);
      mPreviewBitmap = null;
    }
  }
}
