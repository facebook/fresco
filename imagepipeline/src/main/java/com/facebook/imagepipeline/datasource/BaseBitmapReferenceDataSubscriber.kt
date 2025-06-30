/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.datasource

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.BaseDataSubscriber
import com.facebook.datasource.DataSource
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap

/**
 * Implementation of [DataSubscriber] for cases where the client wants access to a bitmap reference.
 *
 * Sample usage:
 * <pre>
 * `
 * dataSource.subscribe(
 * new BaseBitmapReferenceDataSubscriber() {
 * @Override
 * public void onNewResultImpl(@Nullable CloseableReference<Bitmap> bitmapReference) {
 * // Pass bitmap to another component, which clones the bitmap reference if it needs to
 * // hold on to the bitmap.
 * doSomething(bitmapReference);
 * // No need to do any cleanup.
 * }
 *
 * @Override
 * public void onFailureImpl(DataSource dataSource) {
 * // No cleanup required here.
 * }
 * });
 * </Bitmap>` *
 * </pre> *
 */
abstract class BaseBitmapReferenceDataSubscriber :
    BaseDataSubscriber<CloseableReference<CloseableImage>>() {
  override fun onNewResultImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
    if (!dataSource.isFinished) {
      return
    }

    val closeableImageRef = dataSource.result
    var bitmapReference: CloseableReference<Bitmap>? = null
    if (closeableImageRef != null && closeableImageRef.get() is CloseableStaticBitmap) {
      bitmapReference =
          (closeableImageRef.get() as CloseableStaticBitmap).cloneUnderlyingBitmapReference()
    }

    try {
      onNewResultImpl(bitmapReference)
    } finally {
      CloseableReference.closeSafely(bitmapReference)
      CloseableReference.closeSafely(closeableImageRef)
    }
  }

  /**
   * The bitmap reference will be closed immediately after this method is called. Clone the
   * reference if you need to hold on to the bitmap and close once no longer needed.
   *
   * @param bitmapReference the bitmap reference or null if not a bitmap-based image
   */
  protected abstract fun onNewResultImpl(bitmapReference: CloseableReference<Bitmap>?)
}
