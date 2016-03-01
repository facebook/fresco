/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.datasource;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;

/**
 * Implementation of {@link DataSubscriber} for cases where the client wants to access
 * a list of bitmaps.
 *
 * <p>
 * Sample usage:
 * <code>
 *   dataSource.subscribe(
 *     new BaseListBitmapDataSubscriber() {
 *       @Override
 *       public void onNewResultImpl(@Nullable List<Bitmap> bitmapList) {
 *         // Pass bitmap list to system, which makes a copy of it.
 *         update(bitmapList);
 *         // No need to do any cleanup.
 *       }
 *
 *       @Override
 *       public void onFailureImpl(DataSource dataSource) {
 *         // No cleanup required here.
 *       }
 *     }
 * </code>
 */
public abstract class BaseListBitmapDataSubscriber extends
    BaseDataSubscriber<List<CloseableReference<CloseableImage>>> {

  @Override
  public void onNewResultImpl(DataSource<List<CloseableReference<CloseableImage>>> dataSource) {
    if (!dataSource.isFinished()) {
      return;
    }
    List<CloseableReference<CloseableImage>> imageRefList = dataSource.getResult();
    if (imageRefList == null) {
      onNewResultListImpl(null);
      return;
    }
    try {
      List<Bitmap> bitmapList = new ArrayList<>(imageRefList.size());
      for (CloseableReference<CloseableImage> closeableImageRef: imageRefList) {
        if (closeableImageRef != null && closeableImageRef.get() instanceof CloseableBitmap) {
          bitmapList.add(((CloseableBitmap) closeableImageRef.get()).getUnderlyingBitmap());
        } else {
          //This is so that client gets list with same length
          bitmapList.add(null);
        }
      }
      onNewResultListImpl(bitmapList);
    } finally {
      for (CloseableReference<CloseableImage> closeableImageRef: imageRefList) {
        CloseableReference.closeSafely(closeableImageRef);
      }
    }
  }

  /**
   * The bitmap list provided to this method is only guaranteed to be around for the lifespan of the
   * method. This list can be null or the elements in it can be null.
   *
   * <p>The framework will free the bitmaps in the list from memory after this method has completed.
   * @param bitmapList
   */
  protected abstract void onNewResultListImpl(List<Bitmap> bitmapList);
}
