/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.request;

import javax.annotation.Nullable;

import android.graphics.Bitmap;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.nativecode.Bitmaps;

/**
 * Base implementation of {@link Postprocessor} interface.
 *
 * <p> Clients should override exactly one of the three provided {@link #process} methods.
 */
public abstract class BasePostprocessor implements Postprocessor {

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
    CloseableReference<Bitmap> destBitmapRef =
        bitmapFactory.createBitmap(sourceBitmap.getWidth(), sourceBitmap.getHeight());
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
    Bitmaps.copyBitmap(destBitmap, sourceBitmap);
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
}
