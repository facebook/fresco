/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.bitmaps;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.memory.FlexByteArrayPool;
import com.facebook.imagepipeline.memory.PooledByteBuffer;

/**
 * Bitmap factory for Gingerbread.
 */
class GingerbreadBitmapFactory extends DalvikBitmapFactory {

  GingerbreadBitmapFactory(FlexByteArrayPool flexByteArrayPool) {
    super(flexByteArrayPool);
  }

  /**
   * Creates a bitmap of the specified width and height.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config}
   * used to create the decoded Bitmap
   * @return a reference to the bitmap
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  @Override
  public CloseableReference<Bitmap> createBitmap(
      int width,
      int height,
      Bitmap.Config bitmapConfig) {
    Bitmap bitmap = Bitmap.createBitmap(width, height, bitmapConfig);
    return CloseableReference.of(bitmap, SimpleBitmapReleaser.getInstance());
  }

  @Override
  protected boolean isPinBitmapEnabled() {
    return false;
  }

  /**
   * Decodes a byteArray into a purgeable bitmap
   *
   * @param bytesRef the byte buffer that contains the encoded bytes
   * @return
   */
  @Override
  protected Bitmap decodeByteArrayAsPurgeable(
      CloseableReference<PooledByteBuffer> bytesRef,
      BitmapFactory.Options options) {
    return decodeFileDescriptorAsPurgeable(bytesRef, bytesRef.get().size(), null, options);
  }

  /**
   * Decodes a byteArray containing jpeg encoded bytes into a purgeable bitmap
   *
   * <p> Adds a JFIF End-Of-Image marker if needed before decoding.
   *
   * @param bytesRef the byte buffer that contains the encoded bytes
   * @return
   */
  @Override
  protected Bitmap decodeJPEGByteArrayAsPurgeable(
      CloseableReference<PooledByteBuffer> bytesRef,
      int length,
      BitmapFactory.Options options) {
    byte[] suffix = endsWithEOI(bytesRef, length) ? null : EOI;
    return decodeFileDescriptorAsPurgeable(bytesRef, length, suffix, options);
  }

}
