/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.datasource;

import android.graphics.Bitmap;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import javax.annotation.Nullable;

/**
 * Implementation of {@link DataSubscriber} for cases where the client wants access to a bitmap
 * reference.
 *
 * <p>Sample usage:
 *
 * <pre>
 * <code>
 * dataSource.subscribe(
 *   new BaseBitmapReferenceDataSubscriber() {
 *     {@literal @}Override
 *     public void onNewResultImpl(@Nullable CloseableReference<Bitmap> bitmapReference) {
 *       // Pass bitmap to another component, which clones the bitmap reference if it needs to
 *       // hold on to the bitmap.
 *       doSomething(bitmapReference);
 *       // No need to do any cleanup.
 *     }
 *
 *     {@literal @}Override
 *     public void onFailureImpl(DataSource dataSource) {
 *       // No cleanup required here.
 *     }
 *   });
 * </code>
 * </pre>
 */
public abstract class BaseBitmapReferenceDataSubscriber
    extends BaseDataSubscriber<CloseableReference<CloseableImage>> {

  @Override
  public void onNewResultImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
    if (!dataSource.isFinished()) {
      return;
    }

    CloseableReference<CloseableImage> closeableImageRef = dataSource.getResult();
    CloseableReference<Bitmap> bitmapReference = null;
    if (closeableImageRef != null && closeableImageRef.get() instanceof CloseableStaticBitmap) {
      bitmapReference =
          ((CloseableStaticBitmap) closeableImageRef.get()).cloneUnderlyingBitmapReference();
    }

    try {
      onNewResultImpl(bitmapReference);
    } finally {
      CloseableReference.closeSafely(bitmapReference);
      CloseableReference.closeSafely(closeableImageRef);
    }
  }

  /**
   * The bitmap reference will be closed immediately after this method is called. Clone the
   * reference if you need to hold on to the bitmap and close once no longer needed.
   *
   * @param bitmapReference the bitmap reference or null if not a bitmap-based image
   */
  protected abstract void onNewResultImpl(@Nullable CloseableReference<Bitmap> bitmapReference);
}
