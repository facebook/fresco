package com.facebook.avifsupport

import android.graphics.Bitmap
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.decoder.ImageDecoder
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.image.QualityInfo
import com.facebook.imageutils.ByteBufferUtil
import org.aomedia.avif.android.AvifDecoder
import org.aomedia.avif.android.AvifDecoder.Info

class AvifDecoder : ImageDecoder {
  override fun decode(
          encodedImage: EncodedImage,
          length: Int,
          qualityInfo: QualityInfo,
          options: ImageDecodeOptions
  ): CloseableImage? = encodedImage.inputStream?.let { inputStream ->
    val byteBuffer = ByteBufferUtil.fromStream(inputStream)

    val info = Info()
    if (!AvifDecoder.getInfo(byteBuffer, length, info)) {
      return null
    }

    val bitmap = Bitmap.createBitmap(info.width, info.height, options.bitmapConfig)

    if (!AvifDecoder.decode(byteBuffer, byteBuffer.remaining(), bitmap)) {
      return null
    }


    return CloseableStaticBitmap.of(
            bitmap,
            { bitmap.recycle() },
            qualityInfo,
            encodedImage.rotationAngle,
            encodedImage.exifOrientation
    )
  }
}
