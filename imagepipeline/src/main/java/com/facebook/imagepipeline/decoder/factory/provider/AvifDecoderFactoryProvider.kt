package com.facebook.imagepipeline.decoder.factory.provider

import com.facebook.imagepipeline.decoder.factory.AvifDecoderFactory
import com.facebook.imagepipeline.memory.BitmapPool

object AvifDecoderFactoryProvider {

  private var checked = false

  private var avifDecoderFactory: AvifDecoderFactory? = null

  @JvmStatic
  fun loadIfExists(bitmapPool: BitmapPool): AvifDecoderFactory? {
    if (checked) return avifDecoderFactory

    try {
      avifDecoderFactory = Class
              .forName("com.facebook.avifsupport.AvifDecoderFactoryImpl")
              .getConstructor(BitmapPool::class.java)
              .newInstance(bitmapPool) as? AvifDecoderFactory
    } catch (_: Throwable) {
      // Not available
    }

    checked = true
    return avifDecoderFactory
  }
}
