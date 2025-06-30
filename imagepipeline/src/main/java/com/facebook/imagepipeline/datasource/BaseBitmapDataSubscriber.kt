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
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage

/**
 * Implementation of [DataSubscriber] for cases where the client wants access to a bitmap.
 *
 * Sample usage:
 * <pre>
 * `
 * dataSource.subscribe(
 * new BaseBitmapDataSubscriber() {
 * @Override
 * public void onNewResultImpl(@Nullable Bitmap bitmap) {
 * // Pass bitmap to system, which makes a copy of the bitmap.
 * updateStatus(bitmap);
 * // No need to do any cleanup.
 * }
 *
 * @Override
 * public void onFailureImpl(DataSource dataSource) {
 * // No cleanup required here.
 * }
 * });
 * ` *
 * </pre> *
 */
abstract class BaseBitmapDataSubscriber : BaseDataSubscriber<CloseableReference<CloseableImage>>() {
  public override fun onNewResultImpl(dataSource: DataSource<CloseableReference<CloseableImage>>) {
    if (!dataSource.isFinished) {
      return
    }

    val closeableImageRef = dataSource.result
    var bitmap: Bitmap? = null
    if (closeableImageRef != null && closeableImageRef.get() is CloseableBitmap) {
      bitmap = (closeableImageRef.get() as CloseableBitmap).underlyingBitmap
    }

    try {
      onNewResultImpl(bitmap)
    } finally {
      CloseableReference.closeSafely(closeableImageRef)
    }
  }

  /**
   * The bitmap provided to this method is only guaranteed to be around for the lifespan of the
   * method.
   *
   * The framework will free the bitmap's memory after this method has completed.
   *
   * @param bitmap
   */
  protected abstract fun onNewResultImpl(bitmap: Bitmap?)
}
