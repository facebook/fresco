/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.transcoder

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.os.Build
import com.facebook.common.logging.FLog
import com.facebook.imageformat.DefaultImageFormats
import com.facebook.imageformat.ImageFormat
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.image.EncodedImage
import java.io.OutputStream

/**
 * Image transcoder using only the Android API. Clients can use this if they don't want to use the
 * native implementation. This image transcoder requires more memory.
 */
class SimpleImageTranscoder(private val resizingEnabled: Boolean, private val maxBitmapSize: Int) :
    ImageTranscoder {

  override fun transcode(
      encodedImage: EncodedImage,
      outputStream: OutputStream,
      rotationOptions: RotationOptions?,
      resizeOptions: ResizeOptions?,
      outputFormat: ImageFormat?,
      quality: Int?,
      colorSpace: ColorSpace?,
  ): ImageTranscodeResult {
    var rotationOptions = rotationOptions
    var quality = quality
    if (quality == null) {
      quality = JpegTranscoderUtils.DEFAULT_JPEG_QUALITY
    }
    if (rotationOptions == null) {
      rotationOptions = RotationOptions.autoRotate()
    }
    val sampleSize = getSampleSize(encodedImage, rotationOptions, resizeOptions)
    val options = BitmapFactory.Options()
    options.inSampleSize = sampleSize
    if (colorSpace != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      options.inPreferredColorSpace = colorSpace
    }
    val resizedBitmap: Bitmap? =
        try {
          BitmapFactory.decodeStream(encodedImage.inputStream, null, options)
        } catch (oom: OutOfMemoryError) {
          FLog.e(TAG, "Out-Of-Memory during transcode", oom)
          return ImageTranscodeResult(TranscodeStatus.TRANSCODING_ERROR)
        }
    if (resizedBitmap == null) {
      FLog.e(TAG, "Couldn't decode the EncodedImage InputStream ! ")
      return ImageTranscodeResult(TranscodeStatus.TRANSCODING_ERROR)
    }
    val transformationMatrix =
        JpegTranscoderUtils.getTransformationMatrix(encodedImage, rotationOptions)
    var srcBitmap: Bitmap = resizedBitmap
    return try {
      if (transformationMatrix != null) {
        srcBitmap =
            Bitmap.createBitmap(
                resizedBitmap,
                0,
                0,
                resizedBitmap.width,
                resizedBitmap.height,
                transformationMatrix,
                false)
      }
      srcBitmap.compress(getOutputFormat(outputFormat), quality, outputStream)
      ImageTranscodeResult(
          if (sampleSize > DownsampleUtil.DEFAULT_SAMPLE_SIZE) TranscodeStatus.TRANSCODING_SUCCESS
          else TranscodeStatus.TRANSCODING_NO_RESIZING)
    } catch (oom: OutOfMemoryError) {
      FLog.e(TAG, "Out-Of-Memory during transcode", oom)
      ImageTranscodeResult(TranscodeStatus.TRANSCODING_ERROR)
    } finally {
      srcBitmap.recycle()
      resizedBitmap.recycle()
    }
  }

  override fun canResize(
      encodedImage: EncodedImage,
      rotationOptions: RotationOptions?,
      resizeOptions: ResizeOptions?
  ): Boolean {
    var rotationOptions = rotationOptions
    if (rotationOptions == null) {
      rotationOptions = RotationOptions.autoRotate()
    }
    return resizingEnabled &&
        DownsampleUtil.determineSampleSize(
            rotationOptions, resizeOptions, encodedImage, maxBitmapSize) >
            DownsampleUtil.DEFAULT_SAMPLE_SIZE
  }

  override fun canTranscode(imageFormat: ImageFormat): Boolean =
      imageFormat === DefaultImageFormats.HEIF || imageFormat === DefaultImageFormats.JPEG

  override val identifier: String = "SimpleImageTranscoder"

  private fun getSampleSize(
      encodedImage: EncodedImage,
      rotationOptions: RotationOptions,
      resizeOptions: ResizeOptions?
  ): Int {
    val sampleSize: Int =
        if (!resizingEnabled) {
          DownsampleUtil.DEFAULT_SAMPLE_SIZE
        } else {
          DownsampleUtil.determineSampleSize(
              rotationOptions, resizeOptions, encodedImage, maxBitmapSize)
        }
    return sampleSize
  }

  companion object {
    private const val TAG = "SimpleImageTranscoder"

    /**
     * Determine the [Bitmap.CompressFormat] given the [ImageFormat]. If no match is found, it
     * returns [Bitmap.CompressFormat#JPEG]
     *
     * @param format The [ImageFormat] used as input
     * @return The corresponding [Bitmap.CompressFormat]
     */
    private fun getOutputFormat(format: ImageFormat?): CompressFormat {
      if (format == null) {
        return CompressFormat.JPEG
      }
      return if (format === DefaultImageFormats.JPEG) {
        CompressFormat.JPEG
      } else if (format === DefaultImageFormats.PNG) {
        CompressFormat.PNG
      } else {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
            DefaultImageFormats.isStaticWebpFormat(format)) {
          CompressFormat.WEBP
        } else {
          CompressFormat.JPEG
        }
      }
    }
  }
}
