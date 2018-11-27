/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.factory;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Decoder for animated images.
 */
public class AnimatedImageFactoryImpl implements AnimatedImageFactory {

  private final AnimatedDrawableBackendProvider mAnimatedDrawableBackendProvider;
  private final PlatformBitmapFactory mBitmapFactory;

  static AnimatedImageDecoder sGifAnimatedImageDecoder = null;
  static AnimatedImageDecoder sWebpAnimatedImageDecoder = null;

  private static @Nullable AnimatedImageDecoder loadIfPresent(final String className) {
    try {
      Class<?> clazz = Class.forName(className);
      return (AnimatedImageDecoder) clazz.newInstance();
    } catch (Throwable e) {
      return null;
    }
  }

  static {
    sGifAnimatedImageDecoder = loadIfPresent("com.facebook.animated.gif.GifImage");
    sWebpAnimatedImageDecoder = loadIfPresent("com.facebook.animated.webp.WebPImage");
  }

  public AnimatedImageFactoryImpl(
      AnimatedDrawableBackendProvider animatedDrawableBackendProvider,
      PlatformBitmapFactory bitmapFactory) {
    mAnimatedDrawableBackendProvider = animatedDrawableBackendProvider;
    mBitmapFactory = bitmapFactory;
  }

  /**
   * Decodes a GIF into a CloseableImage.
   * @param encodedImage encoded image (native byte array holding the encoded bytes and meta data)
   * @param options the options for the decode
   * @param bitmapConfig the Bitmap.Config used to generate the output bitmaps
   * @return a {@link CloseableImage} for the GIF image
   */
  public CloseableImage decodeGif(
      final EncodedImage encodedImage,
      final ImageDecodeOptions options,
      final Bitmap.Config bitmapConfig) {
    if (sGifAnimatedImageDecoder == null) {
      throw new UnsupportedOperationException("To encode animated gif please add the dependency " +
          "to the animated-gif module");
    }
    final CloseableReference<PooledByteBuffer> bytesRef = encodedImage.getByteBufferRef();
    Preconditions.checkNotNull(bytesRef);
    try {
      final PooledByteBuffer input = bytesRef.get();
      AnimatedImage gifImage;
      if (input.getByteBuffer() != null) {
        gifImage = sGifAnimatedImageDecoder.decode(input.getByteBuffer());
      } else {
        gifImage = sGifAnimatedImageDecoder.decode(input.getNativePtr(), input.size());
      }
      return getCloseableImage(options, gifImage, bitmapConfig);
    } finally {
      CloseableReference.closeSafely(bytesRef);
    }
  }

  /**
   * Decode a WebP into a CloseableImage.
   * @param encodedImage encoded image (native byte array holding the encoded bytes and meta data)
   * @param options the options for the decode
   * @param bitmapConfig the Bitmap.Config used to generate the output bitmaps
   * @return a {@link CloseableImage} for the WebP image
   */
  public CloseableImage decodeWebP(
      final EncodedImage encodedImage,
      final ImageDecodeOptions options,
      final Bitmap.Config bitmapConfig) {
    if (sWebpAnimatedImageDecoder == null) {
      throw new UnsupportedOperationException("To encode animated webp please add the dependency " +
          "to the animated-webp module");
    }
    final CloseableReference<PooledByteBuffer> bytesRef = encodedImage.getByteBufferRef();
    Preconditions.checkNotNull(bytesRef);
    try {
      final PooledByteBuffer input = bytesRef.get();
      AnimatedImage webPImage;
      if (input.getByteBuffer() != null) {
        webPImage = sWebpAnimatedImageDecoder.decode(input.getByteBuffer());
      } else {
        webPImage = sWebpAnimatedImageDecoder.decode(input.getNativePtr(), input.size());
      }
      return getCloseableImage(options, webPImage, bitmapConfig);
    } finally {
      CloseableReference.closeSafely(bytesRef);
    }
  }

  private CloseableImage getCloseableImage(
      ImageDecodeOptions options,
      AnimatedImage image,
      Bitmap.Config bitmapConfig) {
    List<CloseableReference<Bitmap>> decodedFrames = null;
    CloseableReference<Bitmap> previewBitmap = null;
    try {
      final int frameForPreview = options.useLastFrameForPreview ? image.getFrameCount() - 1 : 0;
      if (options.forceStaticImage) {
        return new CloseableStaticBitmap(
            createPreviewBitmap(image, bitmapConfig, frameForPreview),
            ImmutableQualityInfo.FULL_QUALITY,
            0);
      }

      if (options.decodeAllFrames) {
        decodedFrames = decodeAllFrames(image, bitmapConfig);
        previewBitmap = CloseableReference.cloneOrNull(decodedFrames.get(frameForPreview));
      }

      if (options.decodePreviewFrame && previewBitmap == null) {
        previewBitmap = createPreviewBitmap(image, bitmapConfig, frameForPreview);
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
      Bitmap.Config bitmapConfig,
      int frameForPreview) {
    CloseableReference<Bitmap> bitmap = createBitmap(
        image.getWidth(),
        image.getHeight(),
        bitmapConfig);
    AnimatedImageResult tempResult = AnimatedImageResult.forAnimatedImage(image);
    AnimatedDrawableBackend drawableBackend =
        mAnimatedDrawableBackendProvider.get(tempResult, null);
    AnimatedImageCompositor animatedImageCompositor =
        new AnimatedImageCompositor(
            drawableBackend,
            new AnimatedImageCompositor.Callback() {
              @Override
              public void onIntermediateResult(int frameNumber, Bitmap bitmap) {
                // Don't care.
              }

              @Override
              public @Nullable CloseableReference<Bitmap> getCachedBitmap(int frameNumber) {
                return null;
              }
            });
    animatedImageCompositor.renderFrame(frameForPreview, bitmap.get());
    return bitmap;
  }

  private List<CloseableReference<Bitmap>> decodeAllFrames(
      AnimatedImage image,
      Bitmap.Config bitmapConfig) {
    AnimatedImageResult tempResult = AnimatedImageResult.forAnimatedImage(image);
    AnimatedDrawableBackend drawableBackend =
        mAnimatedDrawableBackendProvider.get(tempResult, null);
    final List<CloseableReference<Bitmap>> bitmaps =
            new ArrayList<>(drawableBackend.getFrameCount());
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
          drawableBackend.getWidth(),
          drawableBackend.getHeight(),
          bitmapConfig);
      animatedImageCompositor.renderFrame(i, bitmap.get());
      bitmaps.add(bitmap);
    }
    return bitmaps;
  }

  @SuppressLint("NewApi")
  private CloseableReference<Bitmap> createBitmap(
      int width,
      int height,
      Bitmap.Config bitmapConfig) {
    CloseableReference<Bitmap> bitmap =
        mBitmapFactory.createBitmapInternal(width, height, bitmapConfig);
    bitmap.get().eraseColor(Color.TRANSPARENT);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
      bitmap.get().setHasAlpha(true);
    }
    return bitmap;
  }
}
