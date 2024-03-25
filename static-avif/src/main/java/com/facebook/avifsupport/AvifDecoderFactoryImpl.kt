package com.facebook.avifsupport

import com.facebook.imagepipeline.decoder.ImageDecoder
import com.facebook.imagepipeline.decoder.factory.AvifDecoderFactory

class AvifDecoderFactoryImpl(override val avifDecoder: ImageDecoder? = null) : AvifDecoderFactory
