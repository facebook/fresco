/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.request;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.nativecode.Bitmaps;
import javax.annotation.Nullable;

/**
 * Base implementation of {@link Postprocessor} interface.
 *
 * <p> Clients should override exactly one of the three provided {@link #process} methods.
 */
public abstract class BasePostprocessor implements Postprocessor {

  /**
   * The fallback bitmap configuration is used for creating a new destination bitmap when the
   * source bitmap has <code>config==null</code>. This is the case for preview images for GIF
   * animations.
   */
  public static final Bitmap.Config FALLBACK_BITMAP_CONFIGURATION = Bitmap.Config.ARGB_8888;

  @Override
  public String getName() {
    return "Unknown postprocessor";
  }

  /**
   * Clients should override this method only if the post-processed bitmap has to be of a different
   * size than the source bitmap. If the post-processed bitmap is of the same size, clients should
   * override one of the other two methods.
   *
   * <p> The source bitmap must not be modified as it may be shared by the other clients. The
   * implementation must create a new bitmap that is safe to be modified and return a reference
   * to it. Clients should use <code>bitmapFactory</code> to create a new bitmap.
   *
   * @param sourceBitmap The source bitmap.
   * @param bitmapFactory The factory to create a destination bitmap.
   * @return a reference to the newly created bitmap
   */
  @Override
  public CloseableReference<Bitmap> process(
      Bitmap sourceBitmap,
      PlatformBitmapFactory bitmapFactory) {
    final Bitmap.Config sourceBitmapConfig = sourceBitmap.getConfig();
    CloseableReference<Bitmap> destBitmapRef =
        bitmapFactory.createBitmapInternal(
            sourceBitmap.getWidth(),
            sourceBitmap.getHeight(),
            sourceBitmapConfig != null ? sourceBitmapConfig : FALLBACK_BITMAP_CONFIGURATION);
    try {
      process(destBitmapRef.get(), sourceBitmap);
      return CloseableReference.cloneOrNull(destBitmapRef);
    } finally {
      CloseableReference.closeSafely(destBitmapRef);
    }
  }

  /**
   * Clients should override this method if the post-processing cannot be done in place. If the
   * post-processing can be done in place, clients should override the {@link #process(Bitmap)}
   * method.
   *
   * <p> The provided destination bitmap is of the same size as the source bitmap. There are no
   * guarantees on the initial content of the destination bitmap, so the implementation has to make
   * sure that it properly populates it.
   *
   * <p> The source bitmap must not be modified as it may be shared by the other clients.
   * The implementation must use the provided destination bitmap as its output.
   *
   * @param destBitmap the destination bitmap to be used as output
   * @param sourceBitmap the source bitmap to be used as input
   */
  public void process(Bitmap destBitmap, Bitmap sourceBitmap) {
    internalCopyBitmap(destBitmap, sourceBitmap);
    process(destBitmap);
  }

  /**
   * Clients should override this method if the post-processing can be done in place.
   *
   * <p> The provided bitmap is a copy of the source bitmap and the implementation is free to
   * modify it.
   *
   * @param bitmap the bitmap to be used both as input and as output
   */
  public void process(Bitmap bitmap) {
  }

  /**
   * The default implementation of the CacheKey for a Postprocessor is null
   * @return The CacheKey to use for caching. Not used if null
   */
  @Override
  @Nullable
  public CacheKey getPostprocessorCacheKey() {
    return null;
  }

  /**
   * Copies the content of {@code sourceBitmap} to {@code destBitmap}. Both bitmaps must have the
   * same width and height. If their {@link Bitmap.Config} are identical, the memory is directly
   * copied. Otherwise, the {@code sourceBitmap} is drawn into {@code destBitmap}.
   */
  private static void internalCopyBitmap(Bitmap destBitmap, Bitmap sourceBitmap) {
    if (destBitmap.getConfig() == sourceBitmap.getConfig()) {
      Bitmaps.copyBitmap(destBitmap, sourceBitmap);
    } else {
      // The bitmap configurations might be different when the source bitmap's configuration is
      // null, because it uses an internal configuration and the destination bitmap's configuration
      // is the FALLBACK_BITMAP_CONFIGURATION. This is the case for static images for animated GIFs.
      Canvas canvas = new Canvas(destBitmap);
      canvas.drawBitmap(sourceBitmap, 0, 0, null);
    }
  }
}
