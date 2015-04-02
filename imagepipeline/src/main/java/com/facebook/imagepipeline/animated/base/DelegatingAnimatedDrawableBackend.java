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
import android.graphics.Canvas;

import com.facebook.common.references.CloseableReference;

/**
 * A convenience base class that implements {@link AnimatedDrawableBackend} and delegates
 * to another class.
 */
public abstract class DelegatingAnimatedDrawableBackend implements AnimatedDrawableBackend {

  private final AnimatedDrawableBackend mAnimatedDrawableBackend;

  public DelegatingAnimatedDrawableBackend(AnimatedDrawableBackend animatedDrawableBackend) {
    mAnimatedDrawableBackend = animatedDrawableBackend;
  }

  protected AnimatedDrawableBackend getDelegate() {
    return mAnimatedDrawableBackend;
  }

  @Override
  public AnimatedImageResult getAnimatedImageResult() {
    return mAnimatedDrawableBackend.getAnimatedImageResult();
  }

  @Override
  public int getDurationMs() {
    return mAnimatedDrawableBackend.getDurationMs();
  }

  @Override
  public int getFrameCount() {
    return mAnimatedDrawableBackend.getFrameCount();
  }

  @Override
  public int getLoopCount() {
    return mAnimatedDrawableBackend.getLoopCount();
  }

  @Override
  public int getWidth() {
    return mAnimatedDrawableBackend.getWidth();
  }

  @Override
  public int getHeight() {
    return mAnimatedDrawableBackend.getHeight();
  }

  @Override
  public int getRenderedWidth() {
    return mAnimatedDrawableBackend.getRenderedWidth();
  }

  @Override
  public int getRenderedHeight() {
    return mAnimatedDrawableBackend.getRenderedHeight();
  }

  @Override
  public AnimatedDrawableFrameInfo getFrameInfo(int frameNumber) {
    return mAnimatedDrawableBackend.getFrameInfo(frameNumber);
  }

  @Override
  public void renderFrame(int frameNumber, Canvas canvas) {
    mAnimatedDrawableBackend.renderFrame(frameNumber, canvas);
  }

  @Override
  public int getFrameForTimestampMs(int timestampMs) {
    return mAnimatedDrawableBackend.getFrameForTimestampMs(timestampMs);
  }

  @Override
  public int getTimestampMsForFrame(int frameNumber) {
    return mAnimatedDrawableBackend.getTimestampMsForFrame(frameNumber);
  }

  @Override
  public int getDurationMsForFrame(int frameNumber) {
    return mAnimatedDrawableBackend.getDurationMsForFrame(frameNumber);
  }

  @Override
  public int getFrameForPreview() {
    return mAnimatedDrawableBackend.getFrameForPreview();
  }

  @Override
  public int getMemoryUsage() {
    return mAnimatedDrawableBackend.getMemoryUsage();
  }

  @Override
  public CloseableReference<Bitmap> getPreDecodedFrame(int frameNumber) {
    return mAnimatedDrawableBackend.getPreDecodedFrame(frameNumber);
  }

  @Override
  public boolean hasPreDecodedFrame(int frameNumber) {
    return mAnimatedDrawableBackend.hasPreDecodedFrame(frameNumber);
  }

  @Override
  public void dropCaches() {
    mAnimatedDrawableBackend.dropCaches();
  }
}
