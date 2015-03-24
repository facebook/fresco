/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.factory;

import android.graphics.Bitmap;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.gif.GifImage;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.webp.WebPImage;

/**
 * Decoder for animated images.
 */
public class AnimatedImageFactory {

  private final AnimatedDrawableBackendProvider mAnimatedDrawableBackendProvider;

  public AnimatedImageFactory(
      AnimatedDrawableBackendProvider animatedDrawableBackendProvider) {
    mAnimatedDrawableBackendProvider = animatedDrawableBackendProvider;
  }

  /**
   * Decodes a GIF into a CloseableImage.
   *
   * @param pooledByteBufferRef native byte array holding the encoded bytes
   * @param options the options for the decode
   * @return a {@link CloseableImage} for the GIF image
   */
  public CloseableImage decodeGif(
      final CloseableReference<PooledByteBuffer> pooledByteBufferRef,
      final ImageDecodeOptions options) {
    Preconditions.checkState(!options.forceOldAnimationCode);
    final PooledByteBuffer input = pooledByteBufferRef.get();
    GifImage gifImage = GifImage.create(input.getNativePtr(), input.size());

    return getCloseableImage(options, gifImage);
  }

  /**
   * Decode a WebP into a CloseableImage.
   *
   * @param pooledByteBufferRef native byte array holding the encoded bytes
   * @param options the options for the decode
   * @return a {@link CloseableImage} for the WebP image
   */
  public CloseableImage decodeWebP(
      final CloseableReference<PooledByteBuffer> pooledByteBufferRef,
      final ImageDecodeOptions options) {
    Preconditions.checkArgument(!options.forceOldAnimationCode);
    final PooledByteBuffer input = pooledByteBufferRef.get();
    WebPImage webPImage = WebPImage.create(input.getNativePtr(), input.size());
    return getCloseableImage(options, webPImage);
  }

  private CloseableImage getCloseableImage(ImageDecodeOptions options, AnimatedImage image) {
    int frameForPreview = options.useLastFrameForPreview ? image.getFrameCount() - 1 : 0;
    CloseableReference<Bitmap> previewBitmap = null;
    if (options.decodePreviewFrame) {
      previewBitmap = createPreviewBitmap(image, frameForPreview);
    }
    try {
      AnimatedImageResult animatedImageResult = AnimatedImageResult.newBuilder(image)
          .setPreviewBitmap(previewBitmap)
          .setFrameForPreview(frameForPreview)
          .build();
      return new CloseableAnimatedImage(animatedImageResult);
    } finally {
      CloseableReference.closeSafely(previewBitmap);
    }
  }

  private CloseableReference<Bitmap> createPreviewBitmap(
      AnimatedImage image,
      int frameForPreview) {
    Bitmap previewBitmap;
    previewBitmap = Bitmap.createBitmap(
        image.getWidth(), image.getHeight(), Bitmap.Config.ARGB_8888);
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
    animatedImageCompositor.renderFrame(frameForPreview, previewBitmap);
    return CloseableReference.of(
        previewBitmap,
        new ResourceReleaser<Bitmap>() {
          @Override
          public void release(Bitmap value) {
            value.recycle();
          }
        });
  }
}
