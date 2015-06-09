/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.image;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferInputStream;

import java.io.Closeable;
import java.io.InputStream;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * Class that contains all the information for an encoded image, both the ByteBuffer representing it
 * and the extracted meta data that is useful for image transforms.
 *
 * <p>Currently the data is useful for rotation and resize.
 */
@Immutable
public class EncodedImage implements Closeable {
  private static final int UNKNOWN_ROTATION_ANGLE = -1;
  private static final int UNKNOWN_WIDTH = -1;
  private static final int UNKNOWN_HEIGHT = -1;

  private final CloseableReference<PooledByteBuffer> mPooledByteBufferRef;
  private final ImageFormat mImageFormat;
  private final int mRotationAngle;
  private final int mWidth;
  private final int mHeight;

  public EncodedImage(
      CloseableReference<PooledByteBuffer> pooledByteBufferRef) {
    this(pooledByteBufferRef, ImageFormat.UNKNOWN);
  }

  public EncodedImage(
      CloseableReference<PooledByteBuffer> pooledByteBufferRef,
      ImageFormat imageFormat) {
    this(pooledByteBufferRef, imageFormat, UNKNOWN_ROTATION_ANGLE, UNKNOWN_WIDTH, UNKNOWN_HEIGHT);
  }

  public EncodedImage(
      CloseableReference<PooledByteBuffer> pooledByteBufferRef,
      ImageFormat imageFormat,
      int rotationAngle,
      int width,
      int height) {
    Preconditions.checkArgument(CloseableReference.isValid(pooledByteBufferRef));
    this.mPooledByteBufferRef = pooledByteBufferRef.clone();
    this.mImageFormat = imageFormat;
    this.mRotationAngle = rotationAngle;
    this.mWidth = width;
    this.mHeight = height;
  }

  /**
   * Returns the cloned encoded image if the parameter received is not null, null otherwise.
   *
   * @param encodedImage the EncodedImage to clone
   */
  public static EncodedImage cloneOrNull(EncodedImage encodedImage) {
    return encodedImage != null ? encodedImage.cloneOrNull() : null;
  }

  public EncodedImage cloneOrNull() {
    CloseableReference<PooledByteBuffer> pooledByteBufferRef = mPooledByteBufferRef.cloneOrNull();
    try {
      return (pooledByteBufferRef == null) ? null : new EncodedImage(
          pooledByteBufferRef,
          mImageFormat,
          mRotationAngle,
          mWidth,
          mHeight);
    } finally {
      // Close the recently created reference since it will be cloned again in the constructor.
      CloseableReference.closeSafely(pooledByteBufferRef);
    }
  }

  /**
   * Closes the buffer enclosed by this class.
   */
  @Override
  public void close() {
    CloseableReference.closeSafely(mPooledByteBufferRef);
  }

  /**
   * Returns true if the internal buffer reference is valid, false otherwise.
   */
  public synchronized boolean isValid() {
    return CloseableReference.isValid(mPooledByteBufferRef);
  }

  /**
   * Returns a cloned reference to the stored encoded bytes.
   *
   * <p>The caller has to close the reference once it has finished using it.
   */
  public CloseableReference<PooledByteBuffer> getByteBufferRef() {
    return CloseableReference.cloneOrNull(mPooledByteBufferRef);
  }

  /**
   * Returns an InputStream for the internal buffer reference if valid, null otherwise.
   */
  public InputStream getInputStream() {
    CloseableReference<PooledByteBuffer> pooledByteBufferRef = mPooledByteBufferRef.cloneOrNull();
    if (pooledByteBufferRef != null) {
      try {
        return new PooledByteBufferInputStream(pooledByteBufferRef.get());
      } finally {
        CloseableReference.closeSafely(pooledByteBufferRef);
      }
    }
    return null;
  }

  /**
   * Returns the image format if known, otherwise ImageFormat.UNKNOWN.
   */
  public ImageFormat getImageFormat() {
    return mImageFormat;
  }

  /**
   * Only valid if the image format is JPEG.
   * @return the rotation angle if the rotation angle is known, else -1. The rotation angle may not
   * be known if the image is incomplete (e.g. for progressive JPEGs).
   */
  public int getRotationAngle() {
    return mRotationAngle;
  }

  /**
   * Only valid if the image format is JPEG.
   * @return width if the width is known, else -1.
   */
  public int getWidth() {
    return mWidth;
  }

  /**
   * Only valid if the image format is JPEG.
   * @return height if the height is known, else -1.
   */
  public int getHeight() {
    return mHeight;
  }

  /**
   * Only valid the image format is JPEG.
   * @return true if all the image information has loaded, false otherwise.
   */
  public static boolean isJpegMetaDataAvailable(EncodedImage encodedImage) {
    Preconditions.checkArgument(encodedImage.getImageFormat() == ImageFormat.JPEG);
    return encodedImage.mRotationAngle >= 0
        && encodedImage.mWidth >= 0
        && encodedImage.mHeight >= 0;
  }

  /**
   * Closes the encoded image handling null.
   *
   * @param encodedImage the encoded image to close
   */
  public static void closeSafely(@Nullable EncodedImage encodedImage) {
    if (encodedImage != null) {
      encodedImage.close();
    }
  }
}
