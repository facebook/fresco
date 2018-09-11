/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.bitmaps;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.webp.BitmapCreator;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.FlexByteArrayPool;
import com.facebook.imagepipeline.memory.PoolFactory;

/**
 * This is the implementation of the BitmapCreator for the Honeycomb
 */
public class HoneycombBitmapCreator implements BitmapCreator {

  private final EmptyJpegGenerator mJpegGenerator;
  private final FlexByteArrayPool mFlexByteArrayPool;

  public HoneycombBitmapCreator(PoolFactory poolFactory) {
    mFlexByteArrayPool = poolFactory.getFlexByteArrayPool();
    mJpegGenerator = new EmptyJpegGenerator(poolFactory.getPooledByteBufferFactory());
  }

  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
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
      final PooledByteBuffer pooledByteBuffer = jpgRef.get();
      encodedBytesArrayRef =
          mFlexByteArrayPool.get(length + 2);
      byte[] encodedBytesArray = encodedBytesArrayRef.get();
      pooledByteBuffer.read(0, encodedBytesArray, 0, length);
      Bitmap bitmap = BitmapFactory.decodeByteArray(
          encodedBytesArray,
          0,
          length,
          options);
      bitmap.setHasAlpha(true);
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
}
