/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import javax.annotation.Nullable;

/**
 * Encapsulates the data needed in order for {@code AnimatedDrawable} to render a
 * {@code AnimatedImage}.
 */
public class CloseableAnimatedImage extends CloseableImage {

  private AnimatedImageResult mImageResult;

  public CloseableAnimatedImage(AnimatedImageResult imageResult) {
    mImageResult = imageResult;
  }

  @Override
  public synchronized int getWidth() {
    return isClosed() ? 0 : mImageResult.getImage().getWidth();
  }

  @Override
  public synchronized int getHeight() {
    return isClosed() ? 0 : mImageResult.getImage().getHeight();
  }

  @Override
  public void close() {
    AnimatedImageResult imageResult;
    synchronized (this) {
      if (mImageResult == null) {
        return;
      }
      imageResult = mImageResult;
      mImageResult = null;
    }
    imageResult.dispose();
  }

  @Override
  public synchronized boolean isClosed() {
    return mImageResult == null;
  }

  @Override
  public synchronized int getSizeInBytes() {
    return isClosed() ? 0 : mImageResult.getImage().getSizeInBytes();
  }

  @Override
  public boolean isStateful() {
    return true;
  }

  public synchronized AnimatedImageResult getImageResult() {
    return mImageResult;
  }

  public synchronized @Nullable AnimatedImage getImage() {
    return isClosed() ? null : mImageResult.getImage();
  }
}
