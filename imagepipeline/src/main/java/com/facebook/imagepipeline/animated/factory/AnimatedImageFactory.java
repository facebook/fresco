/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.factory;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.gif.GifImage;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.webp.WebPImage;

import java.util.ArrayList;
import java.util.List;

/**
 * Decoder for animated images.
 */
public class AnimatedImageFactory {

  private final AnimatedDrawableBackendProvider mAnimatedDrawableBackendProvider;
  private final PlatformBitmapFactory mBitmapFactory;

  public AnimatedImageFactory(
      AnimatedDrawableBackendProvider animatedDrawableBackendProvider,
      PlatformBitmapFactory bitmapFactory) {
    mAnimatedDrawableBackendProvider = animatedDrawableBackendProvider;
    mBitmapFactory = bitmapFactory;
  }

  /**
   * Decodes a GIF into a CloseableImage.
   *
   * @param encodedImage encoded image (native byte array holding the encoded bytes and meta data)
   * @param options the options for the decode
   * @return a {@link CloseableImage} for the GIF image
   */
  public CloseableImage decodeGif(
      final EncodedImage encodedImage,
      final ImageDecodeOptions options) {
    final CloseableReference<PooledByteBuffer> bytesRef = encodedImage.getByteBufferRef();
    Preconditions.checkNotNull(bytesRef);
    try {
      Preconditions.checkState(!options.forceOldAnimationCode);
      final PooledByteBuffer input = bytesRef.get();
      GifImage gifImage = GifImage.create(input.getNativePtr(), input.size());

      return getCloseableImage(options, gifImage);
    } finally {
      CloseableReference.closeSafely(bytesRef);
    }
  }

  /**
   * Decode a WebP into a CloseableImage.
   *
   * @param encodedImage encoded image (native byte array holding the encoded bytes and meta data)
   * @param options the options for the decode
   * @return a {@link CloseableImage} for the WebP image
   */
  public CloseableImage decodeWebP(
      final EncodedImage encodedImage,
      final ImageDecodeOptions options) {
    final CloseableReference<PooledByteBuffer> bytesRef = encodedImage.getByteBufferRef();
    Preconditions.checkNotNull(bytesRef);
    try {
      Preconditions.checkArgument(!options.forceOldAnimationCode);
      final PooledByteBuffer input = bytesRef.get();
      WebPImage webPImage = WebPImage.create(input.getNativePtr(), input.size());
      return getCloseableImage(options, webPImage);
    } finally {
      CloseableReference.closeSafely(bytesRef);
    }
  }

  private CloseableAnimatedImage getCloseableImage(
      ImageDecodeOptions options,
      AnimatedImage image) {
    List<CloseableReference<Bitmap>> decodedFrames = null;
    CloseableReference<Bitmap> previewBitmap = null;
    try {
      int frameForPreview = options.useLastFrameForPreview ? image.getFrameCount() - 1 : 0;
      if (options.decodeAllFrames) {
        decodedFrames = decodeAllFrames(image);
        previewBitmap = CloseableReference.cloneOrNull(decodedFrames.get(frameForPreview));
      }

      if (options.decodePreviewFrame && previewBitmap == null) {
        previewBitmap = createPreviewBitmap(image, frameForPreview);
      }
      AnimatedImageResult animatedImageResult = AnimatedImageResult.newBuilder(image)
          .setPreviewBitmap(previewBitmap)
          .setFrameForPreview(frameForPreview)
          .setDecodedFrames(decodedFrames)
          .build();
      return new CloseableAnimatedImage(animatedImageResult);
    } finally {
      CloseableReference.closeSafely(previewBitmap);
      CloseableReference.closeSafely(decodedFrames);
    }
  }

  private CloseableReference<Bitmap> createPreviewBitmap(
      AnimatedImage image,
      int frameForPreview) {
    CloseableReference<Bitmap> bitmap = createBitmap(image.getWidth(), image.getHeight());
    AnimatedImageResult tempResult = AnimatedImageResult.forAnimatedImage(image);
    AnimatedDrawableBackend drawableBackend =
        mAnimatedDrawableBackendProvider.get(tempResult, null);
    AnimatedImageCompositor animatedImageCompositor = new AnimatedImageCompositor(
        drawableBackend,
        new AnimatedImageCompositor.Callback() {
          @Override
          public void onIntermediateResult(int frameNumber, Bitmap bitmap) {
            // Don't care.
          }

          @Override
          public CloseableReference<Bitmap> getCachedBitmap(int frameNumber) {
            return null;
          }
        });
    animatedImageCompositor.renderFrame(frameForPreview, bitmap.get());
    return bitmap;
  }

  private List<CloseableReference<Bitmap>> decodeAllFrames(AnimatedImage image) {

    final List<CloseableReference<Bitmap>> bitmaps = new ArrayList<>();
    AnimatedImageResult tempResult = AnimatedImageResult.forAnimatedImage(image);
    AnimatedDrawableBackend drawableBackend =
        mAnimatedDrawableBackendProvider.get(tempResult, null);
    AnimatedImageCompositor animatedImageCompositor = new AnimatedImageCompositor(
        drawableBackend,
        new AnimatedImageCompositor.Callback() {
          @Override
          public void onIntermediateResult(int frameNumber, Bitmap bitmap) {
            // Don't care.
          }

          @Override
          public CloseableReference<Bitmap> getCachedBitmap(int frameNumber) {
            return CloseableReference.cloneOrNull(bitmaps.get(frameNumber));
          }
        });
    for (int i = 0; i < drawableBackend.getFrameCount(); i++) {
      CloseableReference<Bitmap> bitmap = createBitmap(
          drawableBackend.getWidth(), drawableBackend.getHeight());
      animatedImageCompositor.renderFrame(i, bitmap.get());
      bitmaps.add(bitmap);
    }
    return bitmaps;
  }

  @SuppressLint("NewApi")
  private CloseableReference<Bitmap> createBitmap(int width, int height) {
    CloseableReference<Bitmap> bitmap = mBitmapFactory.createBitmap(width, height);
    bitmap.get().eraseColor(Color.TRANSPARENT);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
      bitmap.get().setHasAlpha(true);
    }
    return bitmap;
  }
}
