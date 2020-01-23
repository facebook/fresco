/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.base;

import android.graphics.Bitmap;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.transformation.BitmapTransformation;
import java.util.List;
import javax.annotation.Nullable;

/** Builder for {@link AnimatedImageResult}. */
public class AnimatedImageResultBuilder {

  private final AnimatedImage mImage;
  private CloseableReference<Bitmap> mPreviewBitmap;
  private List<CloseableReference<Bitmap>> mDecodedFrames;
  private int mFrameForPreview;
  private @Nullable BitmapTransformation mBitmapTransformation;

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
   * @return the reference to the preview bitmap or null if none was set. This returns a reference
   *     that must be released by the caller
   */
  public CloseableReference<Bitmap> getPreviewBitmap() {
    return CloseableReference.cloneOrNull(mPreviewBitmap);
  }

  /**
   * Sets a preview bitmap.
   *
   * @param previewBitmap the preview. The method clones the reference.
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
   * Gets the decoded frames. Only used if the {@code ImageDecodeOptions} were configured to decode
   * all frames at decode time.
   *
   * @return the references to the decoded frames or null if none was set. This returns references
   *     that must be released by the caller
   */
  public List<CloseableReference<Bitmap>> getDecodedFrames() {
    return CloseableReference.cloneOrNull(mDecodedFrames);
  }

  /**
   * Sets the decoded frames. Only used if the {@code ImageDecodeOptions} were configured to decode
   * all frames at decode time.
   *
   * @param decodedFrames the decoded frames. The method clones the references.
   */
  public AnimatedImageResultBuilder setDecodedFrames(
      List<CloseableReference<Bitmap>> decodedFrames) {
    mDecodedFrames = CloseableReference.cloneOrNull(decodedFrames);
    return this;
  }

  /**
   * Gets the transformation that is to be applied to the image, or null if none.
   *
   * @return the transformation that is to be applied to the image, or null if none
   */
  @Nullable
  public BitmapTransformation getBitmapTransformation() {
    return mBitmapTransformation;
  }

  /**
   * Sets the transformation that is to be applied to the image.
   *
   * @param bitmapTransformation the transformation that is to be applied to the image
   */
  public AnimatedImageResultBuilder setBitmapTransformation(
      @Nullable BitmapTransformation bitmapTransformation) {
    mBitmapTransformation = bitmapTransformation;
    return this;
  }

  /**
   * Builds the {@link AnimatedImageResult}. The preview bitmap and the decoded frames are closed
   * after build is called, so this should not be called more than once or those fields will be lost
   * after the first call.
   *
   * @return the result
   */
  public AnimatedImageResult build() {
    try {
      return new AnimatedImageResult(this);
    } finally {
      CloseableReference.closeSafely(mPreviewBitmap);
      mPreviewBitmap = null;
      CloseableReference.closeSafely(mDecodedFrames);
      mDecodedFrames = null;
    }
  }
}
