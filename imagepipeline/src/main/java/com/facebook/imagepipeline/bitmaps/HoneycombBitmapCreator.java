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
import android.graphics.Color;
import android.os.Build;

import com.facebook.common.references.CloseableReference;
import com.facebook.common.webp.BitmapCreator;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.FlexByteArrayPool;
import com.facebook.imagepipeline.memory.PoolFactory;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imageutils.JfifUtil;

/**
 * This is the implementation of the BitmapCreator for the Honeycomb
 */
public class HoneycombBitmapCreator implements BitmapCreator {

  protected static final byte[] EOI = new byte[]{
      (byte) JfifUtil.MARKER_FIRST_BYTE, (byte) JfifUtil.MARKER_EOI};

  private final EmptyJpegGenerator mJpegGenerator;
  private final FlexByteArrayPool mFlexByteArrayPool;

  public HoneycombBitmapCreator(PoolFactory poolFactory) {
    mFlexByteArrayPool = poolFactory.getFlexByteArrayPool();
    mJpegGenerator = new EmptyJpegGenerator(poolFactory.getPooledByteBufferFactory());
  }

  @Override
  public Bitmap createNakedBitmap(
      int width, int height, Bitmap.Config bitmapConfig) {
    CloseableReference<PooledByteBuffer> jpgRef = mJpegGenerator.generate(
        (short) width,
        (short) height);
    EncodedImage encodedImage = null;
    CloseableReference<byte[]> encodedBytesArrayRef = null;
    try {
      encodedImage = new EncodedImage(jpgRef);
      encodedImage.setImageFormat(DefaultImageFormats.JPEG);
      BitmapFactory.Options options = getBitmapFactoryOptions(
          encodedImage.getSampleSize(),
          bitmapConfig);
      int length = jpgRef.get().size();
      byte[] suffix = endsWithEOI(jpgRef, length) ? null : EOI;
      final PooledByteBuffer pooledByteBuffer = jpgRef.get();
      encodedBytesArrayRef =
          mFlexByteArrayPool.get(length + 2);
      byte[] encodedBytesArray = encodedBytesArrayRef.get();
      pooledByteBuffer.read(0, encodedBytesArray, 0, length);
      if (suffix != null) {
        putEOI(encodedBytesArray, length);
        length += 2;
      }
      Bitmap bitmap = BitmapFactory.decodeByteArray(
          encodedBytesArray,
          0,
          length,
          options);
      bitmap.eraseColor(Color.TRANSPARENT);
      return bitmap;
    } finally {
      CloseableReference.closeSafely(encodedBytesArrayRef);
      EncodedImage.closeSafely(encodedImage);
      CloseableReference.closeSafely(jpgRef);
    }
  }

  private static BitmapFactory.Options getBitmapFactoryOptions(
      int sampleSize,
      Bitmap.Config bitmapConfig) {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inDither = true; // known to improve picture quality at low cost
    options.inPreferredConfig = bitmapConfig;
    // Decode the image into a 'purgeable' bitmap that lives on the ashmem heap
    options.inPurgeable = true;
    // Enable copy of of bitmap to enable purgeable decoding by filedescriptor
    options.inInputShareable = true;
    // Sample size should ONLY be different than 1 when downsampling is enabled in the pipeline
    options.inSampleSize = sampleSize;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      options.inMutable = true;  // no known perf difference; allows postprocessing to work
    }
    return options;
  }

  private static void putEOI(byte[] imageBytes, int offset) {
    // TODO 5884402: remove dependency on JfifUtil
    imageBytes[offset] = (byte) JfifUtil.MARKER_FIRST_BYTE;
    imageBytes[offset + 1] = (byte) JfifUtil.MARKER_EOI;
  }

  protected static boolean endsWithEOI(CloseableReference<PooledByteBuffer> bytesRef, int length) {
    PooledByteBuffer buffer = bytesRef.get();
    return length >= 2 &&
        buffer.read(length - 2) == (byte) JfifUtil.MARKER_FIRST_BYTE &&
        buffer.read(length - 1) == (byte) JfifUtil.MARKER_EOI;
  }
}
