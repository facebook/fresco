package com.facebook.imagepipeline.decoder.factory

import com.facebook.imagepipeline.decoder.ImageDecoder

interface AvifDecoderFactory {
  val avifDecoder: ImageDecoder
}
