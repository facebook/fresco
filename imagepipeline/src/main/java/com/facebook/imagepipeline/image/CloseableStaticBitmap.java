/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.image;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import android.graphics.Bitmap;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imageutils.BitmapUtil;

/**
 * CloseableImage that contains one Bitmap.
 */
@ThreadSafe
public class CloseableStaticBitmap extends CloseableBitmap {

  @GuardedBy("this")
  private CloseableReference<Bitmap> mBitmapReference;

  private volatile Bitmap mBitmap;

  // quality info
  private final QualityInfo mQualityInfo;

  private final int mRotationAngle;

  /**
   * Creates a new instance of a CloseableStaticBitmap.
   *
   * @param bitmap the bitmap to wrap
   * @param resourceReleaser ResourceReleaser to release the bitmap to
   */
  public CloseableStaticBitmap(
      Bitmap bitmap,
      ResourceReleaser<Bitmap> resourceReleaser,
      QualityInfo qualityInfo,
      int rotationAngle) {
    mBitmap = Preconditions.checkNotNull(bitmap);
    mBitmapReference = CloseableReference.of(
        mBitmap,
        Preconditions.checkNotNull(resourceReleaser));
    mQualityInfo = qualityInfo;
    mRotationAngle = rotationAngle;
  }

  /**
   * Creates a new instance of a CloseableStaticBitmap from an existing CloseableReference. The
   * CloseableStaticBitmap will hold a reference to the Bitmap until it's closed.
   *
   * @param bitmapReference the bitmap reference.
   */
  public CloseableStaticBitmap(
      CloseableReference<Bitmap> bitmapReference,
      QualityInfo qualityInfo,
      int rotationAngle) {
    mBitmapReference = Preconditions.checkNotNull(bitmapReference.cloneOrNull());
    mBitmap = mBitmapReference.get();
    mQualityInfo = qualityInfo;
    mRotationAngle = rotationAngle;
  }

  /**
   * Releases the bitmap to the pool.
   */
  @Override
  public void close() {
    CloseableReference<Bitmap> reference;
    synchronized (this) {
      if (mBitmapReference == null) {
        return;
      }
      reference = mBitmapReference;
      mBitmapReference = null;
      mBitmap = null;
    }
    reference.close();
  }

  /**
   * Returns whether this instance is closed.
   */
  @Override
  public synchronized boolean isClosed() {
    return mBitmapReference == null;
  }

  /**
   * Gets the underlying bitmap.
   *
   * @return the underlying bitmap
   */
  @Override
  public Bitmap getUnderlyingBitmap() {
    return mBitmap;
  }

  /**
   * @return size in bytes of the underlying bitmap
   */
  @Override
  public int getSizeInBytes() {
    return BitmapUtil.getSizeInBytes(mBitmap);
  }

  /**
   * @return width of the image
   */
  @Override
  public int getWidth() {
    Bitmap bitmap = mBitmap;
    return (bitmap == null) ? 0 : bitmap.getWidth();
  }

  /**
   * @return height of the image
   */
  @Override
  public int getHeight() {
    Bitmap bitmap = mBitmap;
    return (bitmap == null) ? 0 : bitmap.getHeight();
  }

  /**
   * @return the rotation angle of the image
   */
  public int getRotationAngle() {
    return mRotationAngle;
  }

  /**
   * Returns quality information for the image.
   */
  @Override
  public QualityInfo getQualityInfo() {
    return mQualityInfo;
  }
}
