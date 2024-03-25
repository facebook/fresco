package com.facebook.imagepipeline.decoder.factory.provider

import com.facebook.imagepipeline.decoder.factory.AvifDecoderFactory

object AvifDecoderFactoryProvider {

  private var checked = false

  private var avifDecoderFactory: AvifDecoderFactory? = null

  @JvmStatic
  fun loadIfExists(): AvifDecoderFactory? {
    if (checked) return avifDecoderFactory

    try {
      avifDecoderFactory = Class
              .forName("com.facebook.avifsupport.AvifDecoderFactoryImpl")
              .newInstance() as? AvifDecoderFactory
    } catch (_: Throwable) {
      // Not available
    }

    checked = true
    return avifDecoderFactory
  }
}
