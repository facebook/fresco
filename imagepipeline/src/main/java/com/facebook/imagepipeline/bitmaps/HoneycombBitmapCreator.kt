/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.bitmaps

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapFactory.Options
import android.graphics.Color
import com.facebook.common.references.CloseableReference
import com.facebook.common.webp.BitmapCreator
import com.facebook.imageformat.DefaultImageFormats
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.memory.FlexByteArrayPool
import com.facebook.imagepipeline.memory.PoolFactory

/** This is the implementation of the BitmapCreator for the Honeycomb */
class HoneycombBitmapCreator(poolFactory: PoolFactory) : BitmapCreator {

  private val jpegGenerator: EmptyJpegGenerator =
      EmptyJpegGenerator(poolFactory.pooledByteBufferFactory)
  private val flexByteArrayPool: FlexByteArrayPool = poolFactory.flexByteArrayPool

  override fun createNakedBitmap(width: Int, height: Int, bitmapConfig: Bitmap.Config): Bitmap {
    val jpgRef = jpegGenerator.generate(width.toShort(), height.toShort())
    var encodedImage: EncodedImage? = null
    var encodedBytesArrayRef: CloseableReference<ByteArray?>? = null
    return try {
      encodedImage = EncodedImage(jpgRef)
      encodedImage.imageFormat = DefaultImageFormats.JPEG
      val options = getBitmapFactoryOptions(encodedImage.sampleSize, bitmapConfig)
      val length = jpgRef.get().size()
      val pooledByteBuffer = jpgRef.get()
      encodedBytesArrayRef = flexByteArrayPool[length + 2]
      val encodedBytesArray = encodedBytesArrayRef.get()
      pooledByteBuffer.read(0, encodedBytesArray, 0, length)
      val bitmap =
          checkNotNull(BitmapFactory.decodeByteArray(encodedBytesArray, 0, length, options))
      bitmap.setHasAlpha(true)
      bitmap.eraseColor(Color.TRANSPARENT)
      bitmap
    } finally {
      CloseableReference.closeSafely(encodedBytesArrayRef)
      EncodedImage.closeSafely(encodedImage)
      CloseableReference.closeSafely(jpgRef)
    }
  }

  companion object {
    private fun getBitmapFactoryOptions(sampleSize: Int, bitmapConfig: Bitmap.Config): Options {
      val options = Options()
      options.inDither = true // known to improve picture quality at low cost
      options.inPreferredConfig = bitmapConfig
      // Decode the image into a 'purgeable' bitmap that lives on the ashmem heap
      options.inPurgeable = true
      // Enable copy of of bitmap to enable purgeable decoding by filedescriptor
      options.inInputShareable = true
      // Sample size should ONLY be different than 1 when downsampling is enabled in the pipeline
      options.inSampleSize = sampleSize
      options.inMutable = true // no known perf difference; allows postprocessing to work
      return options
    }
  }
}
