/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import android.graphics.Bitmap;
import android.media.ExifInterface;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imageutils.BitmapUtil;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

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
  private final int mExifOrientation;

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
    this(bitmap, resourceReleaser, qualityInfo, rotationAngle, ExifInterface.ORIENTATION_UNDEFINED);
  }

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
      int rotationAngle,
      int exifOrientation) {
    mBitmap = Preconditions.checkNotNull(bitmap);
    mBitmapReference = CloseableReference.of(
        mBitmap,
        Preconditions.checkNotNull(resourceReleaser));
    mQualityInfo = qualityInfo;
    mRotationAngle = rotationAngle;
    mExifOrientation = exifOrientation;
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
    this(bitmapReference, qualityInfo, rotationAngle, ExifInterface.ORIENTATION_UNDEFINED);
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
      int rotationAngle,
      int exifOrientation) {
    mBitmapReference = Preconditions.checkNotNull(bitmapReference.cloneOrNull());
    mBitmap = mBitmapReference.get();
    mQualityInfo = qualityInfo;
    mRotationAngle = rotationAngle;
    mExifOrientation = exifOrientation;
  }

  /**
   * Releases the bitmap to the pool.
   */
  @Override
  public void close() {
    CloseableReference<Bitmap> reference = detachBitmapReference();
    if (reference != null) {
      reference.close();
    }
  }

  private synchronized CloseableReference<Bitmap> detachBitmapReference() {
    CloseableReference<Bitmap> reference = mBitmapReference;
    mBitmapReference = null;
    mBitmap = null;
    return reference;
  }

  /**
   * Convert this object to a CloseableReference&lt;Bitmap&gt;.
   * <p>You cannot call this method on an object that has already been closed.
   * <p>The reference count of the bitmap is preserved. After calling this method, this object
   * can no longer be used and no longer points to the bitmap.
   * <p>See {@link #cloneUnderlyingBitmapReference()} for an alternative that returns a cloned
   * bitmap reference instead.
   *
   * @return the underlying bitmap reference after being detached from this instance
   * @throws IllegalArgumentException if this object has already been closed.
   */
  public synchronized CloseableReference<Bitmap> convertToBitmapReference() {
    Preconditions.checkNotNull(mBitmapReference, "Cannot convert a closed static bitmap");
    return detachBitmapReference();
  }

  /**
   * Get a cloned bitmap reference for the underlying original CloseableReference&lt;Bitmap&gt;.
   * <p>After calling this method, this object can still be used.
   * See {@link #convertToBitmapReference()} for an alternative that detaches the original reference
   * instead.
   *
   * @return the cloned bitmap reference without altering this instance or null if already closed
   */
  @Nullable
  public synchronized CloseableReference<Bitmap> cloneUnderlyingBitmapReference() {
    return CloseableReference.cloneOrNull(mBitmapReference);
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
    if (mRotationAngle % 180 != 0
        || mExifOrientation == ExifInterface.ORIENTATION_TRANSPOSE
        || mExifOrientation == ExifInterface.ORIENTATION_TRANSVERSE) {
      return getBitmapHeight(mBitmap);
    }
    return getBitmapWidth(mBitmap);
  }

  /**
   * @return height of the image
   */
  @Override
  public int getHeight() {
    if (mRotationAngle % 180 != 0
        || mExifOrientation == ExifInterface.ORIENTATION_TRANSPOSE
        || mExifOrientation == ExifInterface.ORIENTATION_TRANSVERSE) {
      return getBitmapWidth(mBitmap);
    }
    return getBitmapHeight(mBitmap);
  }

  private static int getBitmapWidth(@Nullable Bitmap bitmap) {
    return (bitmap == null) ? 0 : bitmap.getWidth();
  }

  private static int getBitmapHeight(@Nullable Bitmap bitmap) {
    return (bitmap == null) ? 0 : bitmap.getHeight();
  }

  /**
   * @return the rotation angle of the image
   */
  public int getRotationAngle() {
    return mRotationAngle;
  }

  /** @return the EXIF orientation of the image */
  public int getExifOrientation() {
    return mExifOrientation;
  }

  /** Returns quality information for the image. */
  @Override
  public QualityInfo getQualityInfo() {
    return mQualityInfo;
  }
}
