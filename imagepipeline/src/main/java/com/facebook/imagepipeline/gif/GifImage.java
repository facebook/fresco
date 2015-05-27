/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.gif;

import javax.annotation.concurrent.ThreadSafe;

import java.nio.ByteBuffer;

import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.soloader.SoLoaderShim;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo;
import com.facebook.imagepipeline.animated.base.AnimatedImage;

/**
 * A representation of a GIF image. An instance of this class will hold a copy of the encoded
 * data in memory along with the parsed header data. Frames are decoded on demand via
 * {@link GifFrame}.
 */
@ThreadSafe
public class GifImage implements AnimatedImage {

  private volatile static boolean sInitialized;

  // Accessed by native methods
  @SuppressWarnings("unused")
  @DoNotStrip
  private long mNativeContext;

  private static synchronized void ensure() {
    if (!sInitialized) {
      sInitialized = true;
      SoLoaderShim.loadLibrary("gifimage");
    }
  }

  /**
   * Creates a {@link GifImage} from the specified encoded data. This will throw if it fails
   * to create. This is meant to be called on a worker thread.
   *
   * @param source the data to the image (a copy will be made)
   */
  public static GifImage create(byte[] source) {
    ensure();
    Preconditions.checkNotNull(source);

    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(source.length);
    byteBuffer.put(source);
    byteBuffer.rewind();

    return nativeCreateFromDirectByteBuffer(byteBuffer);
  }

  public static GifImage create(long nativePtr, int sizeInBytes) {
    ensure();
    Preconditions.checkArgument(nativePtr != 0);
    return nativeCreateFromNativeMemory(nativePtr, sizeInBytes);
  }

  /**
   * Constructs the image with the native pointer. This is called by native code.
   *
   * @param nativeContext the native pointer
   */
  @DoNotStrip
  GifImage(long nativeContext) {
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
  public GifFrame getFrame(int frameNumber) {
    return nativeGetFrame(frameNumber);
  }

  @Override
  public boolean doesRenderSupportScaling() {
    return false;
  }

  @Override
  public int getSizeInBytes() {
    return nativeGetSizeInBytes();
  }

  @Override
  public AnimatedDrawableFrameInfo getFrameInfo(int frameNumber) {
    GifFrame frame = getFrame(frameNumber);
    try {
      return new AnimatedDrawableFrameInfo(
          frameNumber,
          frame.getXOffset(),
          frame.getYOffset(),
          frame.getWidth(),
          frame.getHeight(),
          true,
          fromGifDisposalMethod(frame.getDisposalMode()));
    } finally {
      frame.dispose();
    }
  }

  private static AnimatedDrawableFrameInfo.DisposalMethod fromGifDisposalMethod(int disposalMode) {
    if (disposalMode == 0 /* DISPOSAL_UNSPECIFIED */) {
      return AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_DO_NOT;
    } else if (disposalMode == 1 /* DISPOSE_DO_NOT */) {
      return AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_DO_NOT;
    } else if (disposalMode == 2 /* DISPOSE_BACKGROUND */) {
      return AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_TO_BACKGROUND;
    } else if (disposalMode == 3 /* DISPOSE_PREVIOUS */) {
      return AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_TO_PREVIOUS;
    } else {
      return AnimatedDrawableFrameInfo.DisposalMethod.DISPOSE_DO_NOT;
    }
  }

  private static native GifImage nativeCreateFromDirectByteBuffer(ByteBuffer buffer);
  private static native GifImage nativeCreateFromNativeMemory(long nativePtr, int sizeInBytes);
  private native int nativeGetWidth();
  private native int nativeGetHeight();
  private native int nativeGetDuration();
  private native int nativeGetFrameCount();
  private native int[] nativeGetFrameDurations();
  private native int nativeGetLoopCount();
  private native GifFrame nativeGetFrame(int frameNumber);
  private native int nativeGetSizeInBytes();
  private native void nativeDispose();
  private native void nativeFinalize();
}
