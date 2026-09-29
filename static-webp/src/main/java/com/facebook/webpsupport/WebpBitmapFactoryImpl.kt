/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.webpsupport

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.util.DisplayMetrics
import android.util.TypedValue
import com.facebook.common.internal.DoNotStrip
import com.facebook.common.webp.BitmapCreator
import com.facebook.common.webp.WebpBitmapFactory
import com.facebook.common.webp.WebpBitmapFactory.WebpErrorLogger
import com.facebook.imagepipeline.nativecode.StaticWebpNativeLoader
import java.io.BufferedInputStream
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import kotlin.concurrent.Volatile

@DoNotStrip
class WebpBitmapFactoryImpl : WebpBitmapFactory {

  override fun setBitmapCreator(bitmapCreator: BitmapCreator) {
    WebpBitmapFactoryImpl.bitmapCreator = bitmapCreator
  }

  override fun setWebpErrorLogger(logger: WebpErrorLogger) {
    webpErrorLogger = logger
  }

  override fun decodeFileDescriptor(
      fd: FileDescriptor,
      outPadding: Rect?,
      opts: BitmapFactory.Options?,
  ): Bitmap? = hookDecodeFileDescriptor(fd, outPadding, opts)

  override fun decodeStream(
      inputStream: InputStream,
      outPadding: Rect?,
      opts: BitmapFactory.Options?,
  ): Bitmap? = hookDecodeStream(inputStream, outPadding, opts)

  override fun decodeFile(pathName: String, opts: BitmapFactory.Options?): Bitmap? =
      hookDecodeFile(pathName, opts)

  override fun decodeByteArray(
      array: ByteArray,
      offset: Int,
      length: Int,
      opts: BitmapFactory.Options?,
  ): Bitmap? = hookDecodeByteArray(array, offset, length, opts)

  companion object {
    private const val HEADER_SIZE = 20

    private const val IN_TEMP_BUFFER_SIZE = 8 * 1_024

    // Pulled by native code via JNI to check if sandboxed WebP decode is enabled.
    // Set from Java (LibwebpSandboxInit) — no native setter needed.
    @DoNotStrip @Volatile private var sWebPSandboxEnabled = false

    @JvmStatic
    @DoNotStrip
    fun setWebPSandboxEnabled(enabled: Boolean) {
      sWebPSandboxEnabled = enabled
    }

    private var webpErrorLogger: WebpErrorLogger? = null

    private var bitmapCreator: BitmapCreator? = null

    @JvmStatic
    private fun wrapToMarkSupportedStream(inputStream: InputStream): InputStream {
      var inputStream = inputStream
      if (!inputStream.markSupported()) {
        inputStream = BufferedInputStream(inputStream, HEADER_SIZE)
      }
      return inputStream
    }

    @JvmStatic
    private fun getWebpHeader(
        inputStream: InputStream,
        opts: BitmapFactory.Options?,
    ): ByteArray? {
      inputStream.mark(HEADER_SIZE)
      val header =
          if (opts?.inTempStorage != null && opts.inTempStorage.size >= HEADER_SIZE) {
            opts.inTempStorage
          } else {
            ByteArray(HEADER_SIZE)
          }
      try {
        inputStream.read(header, 0, HEADER_SIZE)
        inputStream.reset()
      } catch (exp: IOException) {
        return null
      }
      return header
    }

    @JvmStatic
    private fun setDensityFromOptions(
        outputBitmap: Bitmap?,
        opts: BitmapFactory.Options?,
    ) {
      if (outputBitmap == null || opts == null) {
        return
      }
      val density = opts.inDensity
      if (density != 0) {
        outputBitmap.density = density
        val targetDensity = opts.inTargetDensity
        if (targetDensity == 0 || density == targetDensity || density == opts.inScreenDensity) {
          return
        }
        if (opts.inScaled) {
          outputBitmap.density = targetDensity
        }
      } else if (opts.inBitmap != null) {
        // bitmap was reused, ensure density is reset
        outputBitmap.density = DisplayMetrics.DENSITY_DEFAULT
      }
    }

    @JvmStatic
    @DoNotStrip
    fun hookDecodeByteArray(
        array: ByteArray,
        offset: Int,
        length: Int,
        opts: BitmapFactory.Options?,
    ): Bitmap? {
      StaticWebpNativeLoader.ensure()
      val bitmap = originalDecodeByteArray(array, offset, length, opts)
      if (bitmap == null) {
        sendWebpErrorLog("webp_direct_decode_array_failed_on_no_webp")
      }
      return bitmap
    }

    @JvmStatic
    @DoNotStrip
    private fun originalDecodeByteArray(
        array: ByteArray,
        offset: Int,
        length: Int,
        opts: BitmapFactory.Options?,
    ): Bitmap? = BitmapFactory.decodeByteArray(array, offset, length, opts)

    @JvmStatic
    @DoNotStrip
    fun hookDecodeByteArray(array: ByteArray, offset: Int, length: Int): Bitmap? =
        hookDecodeByteArray(array, offset, length, null)

    @JvmStatic
    @DoNotStrip
    private fun originalDecodeByteArray(array: ByteArray, offset: Int, length: Int): Bitmap? =
        BitmapFactory.decodeByteArray(array, offset, length)

    @JvmStatic
    @DoNotStrip
    fun hookDecodeStream(
        inputStream: InputStream,
        outPadding: Rect?,
        opts: BitmapFactory.Options?,
    ): Bitmap? {
      var inputStream = inputStream
      StaticWebpNativeLoader.ensure()
      inputStream = wrapToMarkSupportedStream(inputStream)
      val bitmap = originalDecodeStream(inputStream, outPadding, opts)
      if (bitmap == null) {
        sendWebpErrorLog("webp_direct_decode_stream_failed_on_no_webp")
      }
      return bitmap
    }

    @JvmStatic
    @DoNotStrip
    private fun originalDecodeStream(
        inputStream: InputStream,
        outPadding: Rect?,
        opts: BitmapFactory.Options?,
    ): Bitmap? = BitmapFactory.decodeStream(inputStream, outPadding, opts)

    @JvmStatic
    @DoNotStrip
    fun hookDecodeStream(inputStream: InputStream): Bitmap? =
        hookDecodeStream(inputStream, null, null)

    @JvmStatic
    @DoNotStrip
    private fun originalDecodeStream(inputStream: InputStream): Bitmap? =
        BitmapFactory.decodeStream(inputStream)

    @JvmStatic
    @DoNotStrip
    fun hookDecodeFile(pathName: String, opts: BitmapFactory.Options?): Bitmap? {
      var bitmap: Bitmap? = null
      try {
        FileInputStream(pathName).use { stream ->
          bitmap = hookDecodeStream(stream, null, opts)
        }
      } catch (e: Exception) {
        // Ignore, will just return null
      }
      return bitmap
    }

    @JvmStatic
    @DoNotStrip
    fun hookDecodeFile(pathName: String): Bitmap? = hookDecodeFile(pathName, null)

    @JvmStatic
    @DoNotStrip
    fun hookDecodeResourceStream(
        res: Resources?,
        value: TypedValue?,
        `is`: InputStream,
        pad: Rect?,
        opts: BitmapFactory.Options?,
    ): Bitmap? {
      var opts = opts
      if (opts == null) {
        opts = BitmapFactory.Options()
      }
      if (opts.inDensity == 0 && value != null) {
        val density = value.density
        if (density == TypedValue.DENSITY_DEFAULT) {
          opts.inDensity = DisplayMetrics.DENSITY_DEFAULT
        } else if (density != TypedValue.DENSITY_NONE) {
          opts.inDensity = density
        }
      }
      if (opts.inTargetDensity == 0 && res != null) {
        opts.inTargetDensity = res.displayMetrics.densityDpi
      }
      return hookDecodeStream(`is`, pad, opts)
    }

    @JvmStatic
    @DoNotStrip
    private fun originalDecodeResourceStream(
        res: Resources,
        value: TypedValue,
        `is`: InputStream,
        pad: Rect,
        opts: BitmapFactory.Options,
    ): Bitmap? = BitmapFactory.decodeResourceStream(res, value, `is`, pad, opts)

    @JvmStatic
    @DoNotStrip
    fun hookDecodeResource(
        res: Resources,
        id: Int,
        opts: BitmapFactory.Options?,
    ): Bitmap? {
      var bm: Bitmap? = null
      val value = TypedValue()
      try {
        res.openRawResource(id, value).use { `is` ->
          bm = hookDecodeResourceStream(res, value, `is`, null, opts)
        }
      } catch (e: Exception) {
        // Keep resulting bitmap as null
      }
      require(!(bm == null && opts?.inBitmap != null)) {
        "Problem decoding into existing bitmap"
      }
      return bm
    }

    @JvmStatic
    @DoNotStrip
    private fun originalDecodeResource(
        res: Resources,
        id: Int,
        opts: BitmapFactory.Options,
    ): Bitmap? = BitmapFactory.decodeResource(res, id, opts)

    @JvmStatic
    @DoNotStrip
    fun hookDecodeResource(res: Resources, id: Int): Bitmap? = hookDecodeResource(res, id, null)

    @JvmStatic
    @DoNotStrip
    private fun originalDecodeResource(res: Resources, id: Int): Bitmap? =
        BitmapFactory.decodeResource(res, id)

    @JvmStatic
    @DoNotStrip
    private fun setOutDimensions(
        options: BitmapFactory.Options?,
        imageWidth: Int,
        imageHeight: Int,
    ): Boolean {
      if (options?.inJustDecodeBounds == true) {
        options.outWidth = imageWidth
        options.outHeight = imageHeight
        return true
      }
      return false
    }

    @JvmStatic
    @DoNotStrip
    private fun setPaddingDefaultValues(padding: Rect?) {
      if (padding != null) {
        padding.top = -1
        padding.left = -1
        padding.bottom = -1
        padding.right = -1
      }
    }

    @JvmStatic
    @DoNotStrip
    private fun setBitmapSize(
        options: BitmapFactory.Options?,
        width: Int,
        height: Int,
    ) {
      if (options != null) {
        options.outWidth = width
        options.outHeight = height
      }
    }

    @JvmStatic
    @DoNotStrip
    private fun originalDecodeFile(pathName: String, opts: BitmapFactory.Options?): Bitmap? =
        BitmapFactory.decodeFile(pathName, opts)

    @JvmStatic
    @DoNotStrip
    private fun originalDecodeFile(pathName: String): Bitmap? = BitmapFactory.decodeFile(pathName)

    @JvmStatic
    @DoNotStrip
    fun hookDecodeFileDescriptor(
        fd: FileDescriptor,
        outPadding: Rect?,
        opts: BitmapFactory.Options?,
    ): Bitmap? {
      StaticWebpNativeLoader.ensure()
      val bitmap: Bitmap?
      val originalSeekPosition = nativeSeek(fd, 0, false)
      if (originalSeekPosition != -1L) {
        val inputStream = wrapToMarkSupportedStream(FileInputStream(fd))
        try {
          getWebpHeader(inputStream, opts)
          nativeSeek(fd, originalSeekPosition, true)
          bitmap = originalDecodeFileDescriptor(fd, outPadding, opts)
          if (bitmap == null) {
            sendWebpErrorLog("webp_direct_decode_fd_failed_on_no_webp")
          }
        } finally {
          try {
            inputStream.close()
          } catch (t: Throwable) {
            /* ignore */
          }
        }
      } else {
        bitmap = hookDecodeStream(FileInputStream(fd), outPadding, opts)
        setPaddingDefaultValues(outPadding)
      }
      return bitmap
    }

    @JvmStatic
    @DoNotStrip
    private fun originalDecodeFileDescriptor(
        fd: FileDescriptor,
        outPadding: Rect?,
        opts: BitmapFactory.Options?,
    ): Bitmap? = BitmapFactory.decodeFileDescriptor(fd, outPadding, opts)

    @JvmStatic
    @DoNotStrip
    fun hookDecodeFileDescriptor(fd: FileDescriptor): Bitmap? =
        hookDecodeFileDescriptor(fd, null, null)

    @JvmStatic
    @DoNotStrip
    private fun originalDecodeFileDescriptor(fd: FileDescriptor): Bitmap? =
        BitmapFactory.decodeFileDescriptor(fd)

    @JvmStatic
    private fun setWebpBitmapOptions(
        bitmap: Bitmap?,
        opts: BitmapFactory.Options?,
    ) {
      setDensityFromOptions(bitmap, opts)
      if (opts != null) {
        opts.outMimeType = "image/webp"
      }
    }

    @JvmStatic
    @DoNotStrip
    private fun shouldPremultiply(options: BitmapFactory.Options?): Boolean {
      if (options != null) {
        return options.inPremultiplied
      }
      return true
    }

    @JvmStatic
    @DoNotStrip
    private fun createBitmap(
        width: Int,
        height: Int,
        options: BitmapFactory.Options?,
    ): Bitmap? {
      if (options?.inBitmap?.isMutable == true) {
        return options.inBitmap
      }
      return bitmapCreator!!.createNakedBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    @JvmStatic
    @DoNotStrip
    private external fun nativeDecodeStream(
        `is`: InputStream,
        options: BitmapFactory.Options?,
        scale: Float,
        inTempStorage: ByteArray,
    ): Bitmap?

    @JvmStatic
    @DoNotStrip
    private external fun nativeDecodeByteArray(
        data: ByteArray,
        offset: Int,
        length: Int,
        opts: BitmapFactory.Options?,
        scale: Float,
        inTempStorage: ByteArray,
    ): Bitmap?

    @JvmStatic
    @DoNotStrip
    private external fun nativeSeek(fd: FileDescriptor, offset: Long, absolute: Boolean): Long

    @JvmStatic
    @DoNotStrip
    private fun getInTempStorageFromOptions(options: BitmapFactory.Options?): ByteArray =
        if (options?.inTempStorage != null) {
          options.inTempStorage
        } else {
          ByteArray(IN_TEMP_BUFFER_SIZE)
        }

    @JvmStatic
    @DoNotStrip
    private fun getScaleFromOptions(options: BitmapFactory.Options?): Float {
      var scale = 1.0f
      if (options != null) {
        val sampleSize = options.inSampleSize
        if (sampleSize > 1) {
          scale = 1.0f / sampleSize.toFloat()
        }
        if (options.inScaled) {
          val density = options.inDensity
          val targetDensity = options.inTargetDensity
          val screenDensity = options.inScreenDensity
          if (density != 0 && targetDensity != 0 && density != screenDensity) {
            scale = targetDensity / density.toFloat()
          }
        }
      }
      return scale
    }

    @JvmStatic
    private fun sendWebpErrorLog(message: String) {
      // We want to track only when bitmap is null after native decoding
      webpErrorLogger?.onWebpErrorLog(message, "decoding_failure")
    }
  }
}
