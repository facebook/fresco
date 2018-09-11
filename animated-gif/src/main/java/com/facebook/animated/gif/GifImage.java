/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.gif;

import com.facebook.common.internal.DoNotStrip;
import com.facebook.common.internal.Preconditions;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableFrameInfo.BlendOperation;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.factory.AnimatedImageDecoder;
import com.facebook.soloader.SoLoader;
import java.nio.ByteBuffer;
import javax.annotation.concurrent.ThreadSafe;

/**
 * A representation of a GIF image. An instance of this class will hold a copy of the encoded
 * data in memory along with the parsed header data. Frames are decoded on demand via
 * {@link GifFrame}.
 */
@ThreadSafe
@DoNotStrip
public class GifImage implements AnimatedImage, AnimatedImageDecoder {

  private static final int LOOP_COUNT_FOREVER = 0;
  private static final int LOOP_COUNT_MISSING = -1;

  private volatile static boolean sInitialized;

  // Accessed by native methods
  @SuppressWarnings("unused")
  @DoNotStrip
  private long mNativeContext;

  private static synchronized void ensure() {
    if (!sInitialized) {
      sInitialized = true;
      SoLoader.loadLibrary("gifimage");
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

  /**
   * Creates a {@link GifImage} from a ByteBuffer containing the image. This will throw if it fails
   * to create.
   *
   * @param byteBuffer the ByteBuffer containing the image (a copy will be made)
   */
  public static GifImage create(ByteBuffer byteBuffer) {
    ensure();
    byteBuffer.rewind();

    return nativeCreateFromDirectByteBuffer(byteBuffer);
  }

  public static GifImage create(long nativePtr, int sizeInBytes) {
    ensure();
    Preconditions.checkArgument(nativePtr != 0);
    return nativeCreateFromNativeMemory(nativePtr, sizeInBytes);
  }

  @Override
  public AnimatedImage decode(long nativePtr, int sizeInBytes) {
    return GifImage.create(nativePtr, sizeInBytes);
  }

  @Override
  public AnimatedImage decode(ByteBuffer byteBuffer) {
    return GifImage.create(byteBuffer);
  }

  @DoNotStrip
  public GifImage() {
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
    // If a GIF image has no Netscape 2.0 loop extension, it is meant to play once and then stop. A
    // loop count of 0 indicates an endless looping of the animation. Any loop count X>0 indicates
    // that the animation shall be repeated X times, resulting in the animation to play X+1 times.
    final int loopCount = nativeGetLoopCount();
    switch (loopCount) {
      case LOOP_COUNT_FOREVER:
        return AnimatedImage.LOOP_COUNT_INFINITE;

      case LOOP_COUNT_MISSING:
        return 1;

      default:
        return loopCount + 1;
    }
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
          BlendOperation.BLEND_WITH_PREVIOUS,
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

  @DoNotStrip
  private static native GifImage nativeCreateFromDirectByteBuffer(ByteBuffer buffer);

  @DoNotStrip
  private static native GifImage nativeCreateFromNativeMemory(long nativePtr, int sizeInBytes);

  @DoNotStrip
  private native int nativeGetWidth();

  @DoNotStrip
  private native int nativeGetHeight();

  @DoNotStrip
  private native int nativeGetDuration();

  @DoNotStrip
  private native int nativeGetFrameCount();

  @DoNotStrip
  private native int[] nativeGetFrameDurations();

  @DoNotStrip
  private native int nativeGetLoopCount();

  @DoNotStrip
  private native GifFrame nativeGetFrame(int frameNumber);

  @DoNotStrip
  private native int nativeGetSizeInBytes();

  @DoNotStrip
  private native void nativeDispose();

  @DoNotStrip
  private native void nativeFinalize();
}
