/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.webp;

import javax.annotation.concurrent.ThreadSafe;

import java.nio.ByteBuffer;

import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.soloader.SoLoaderShim;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo.DisposalMethod;
import com.facebook.imagepipeline.animated.base.AnimatedImage;

/**
 * A representation of a WebP image. An instance of this class will hold a copy of the encoded
 * data in memory along with the parsed header data. Frames are decoded on demand via
 * {@link WebPFrame}.
 */
@ThreadSafe
public class WebPImage implements AnimatedImage {

  private volatile static boolean sInitialized;

  // Accessed by native methods
  @SuppressWarnings("unused")
  @DoNotStrip
  private long mNativeContext;

  private static synchronized void ensure() {
    if (!sInitialized) {
      sInitialized = true;
      SoLoaderShim.loadLibrary("webp");
      SoLoaderShim.loadLibrary("webpimage");
    }
  }

  /**
   * Constructs the image with the native pointer. This is called by native code.
   *
   * @param nativeContext the native pointer
   */
  @DoNotStrip
  WebPImage(long nativeContext) {
    mNativeContext = nativeContext;
  }

  @Override
  protected void finalize() {
    nativeFinalize();
  }

  @Override
  public void dispose() {
    nativeDispose();
  }

  /**
   * Creates a {@link WebPImage} from the specified encoded data. This will throw if it fails
   * to create. This is meant to be called on a worker thread.
   *
   * @param source the data to the image (a copy will be made)
   */
  public static WebPImage create(byte[] source) {
    ensure();
    Preconditions.checkNotNull(source);

    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(source.length);
    byteBuffer.put(source);
    byteBuffer.rewind();

    return nativeCreateFromDirectByteBuffer(byteBuffer);
  }

  public static WebPImage create(long nativePtr, int sizeInBytes) {
    ensure();
    Preconditions.checkArgument(nativePtr != 0);
    return nativeCreateFromNativeMemory(nativePtr, sizeInBytes);
  }

  @Override
  public int getWidth() {
    return nativeGetWidth();
  }

  @Override
  public int getHeight() {
    return nativeGetHeight();
  }

  @Override
  public int getFrameCount() {
    return nativeGetFrameCount();
  }

  @Override
  public int getDuration() {
    return nativeGetDuration();
  }

  @Override
  public int[] getFrameDurations() {
    return nativeGetFrameDurations();
  }

  @Override
  public int getLoopCount() {
    return nativeGetLoopCount();
  }

  @Override
  public WebPFrame getFrame(int frameNumber) {
    return nativeGetFrame(frameNumber);
  }

  @Override
  public int getSizeInBytes() {
    return nativeGetSizeInBytes();
  }

  @Override
  public boolean doesRenderSupportScaling() {
    return true;
  }

  @Override
  public AnimatedDrawableFrameInfo getFrameInfo(int frameNumber) {
    WebPFrame frame = getFrame(frameNumber);
    try {
      return new AnimatedDrawableFrameInfo(
          frameNumber,
          frame.getXOffset(),
          frame.getYOffset(),
          frame.getWidth(),
          frame.getHeight(),
          frame.shouldBlendWithPreviousFrame(),
          frame.shouldDisposeToBackgroundColor() ?
              DisposalMethod.DISPOSE_TO_BACKGROUND :
              DisposalMethod.DISPOSE_DO_NOT);
    } finally {
      frame.dispose();
    }
  }

  private static native WebPImage nativeCreateFromDirectByteBuffer(ByteBuffer buffer);
  private static native WebPImage nativeCreateFromNativeMemory(long nativePtr, int sizeInBytes);
  private native int nativeGetWidth();
  private native int nativeGetHeight();
  private native int nativeGetDuration();
  private native int nativeGetFrameCount();
  private native int[] nativeGetFrameDurations();
  private native int nativeGetLoopCount();
  private native WebPFrame nativeGetFrame(int frameNumber);
  private native int nativeGetSizeInBytes();
  private native void nativeDispose();
  private native void nativeFinalize();
}
