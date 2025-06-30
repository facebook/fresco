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
 * Implementation of [DataSubscriber] for cases where the client wants to access a list of bitmaps.
 *
 * Sample usage: ` dataSource.subscribe( new BaseListBitmapDataSubscriber() {
 *
 * @Override public void onNewResultImpl(@Nullable List<Bitmap> bitmapList) { // Pass bitmap list to
 *   system, which makes a copy of it. update(bitmapList); // No need to do any cleanup. }
 * @Override public void onFailureImpl(DataSource dataSource) { // No cleanup required here. } }
 *   </Bitmap>` *
 */
abstract class BaseListBitmapDataSubscriber :
    BaseDataSubscriber<List<CloseableReference<CloseableImage>>?>() {
  public override fun onNewResultImpl(
      dataSource: DataSource<List<CloseableReference<CloseableImage>>?>
  ) {
    if (!dataSource.isFinished) {
      return
    }
    val imageRefList = dataSource.result
    if (imageRefList == null) {
      onNewResultListImpl(null)
      return
    }
    try {
      val bitmapList: MutableList<Bitmap?> = ArrayList(imageRefList.size)
      for (closeableImageRef in imageRefList) {
        if (closeableImageRef != null && closeableImageRef.get() is CloseableBitmap) {
          bitmapList.add((closeableImageRef.get() as CloseableBitmap).underlyingBitmap)
        } else {
          // This is so that client gets list with same length
          bitmapList.add(null)
        }
      }
      onNewResultListImpl(bitmapList)
    } finally {
      for (closeableImageRef in imageRefList) {
        CloseableReference.closeSafely(closeableImageRef)
      }
    }
  }

  /**
   * The bitmap list provided to this method is only guaranteed to be around for the lifespan of the
   * method. This list can be null or the elements in it can be null.
   *
   * The framework will free the bitmaps in the list from memory after this method has completed.
   *
   * @param bitmapList
   */
  protected abstract fun onNewResultListImpl(bitmapList: List<Bitmap?>?)
}
