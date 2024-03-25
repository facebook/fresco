package com.facebook.avifsupport

import com.facebook.common.internal.DoNotStrip
import com.facebook.imagepipeline.decoder.ImageDecoder
import com.facebook.imagepipeline.decoder.factory.AvifDecoderFactory
import com.facebook.imagepipeline.memory.BitmapPool
import com.facebook.infer.annotation.Nullsafe

@Nullsafe(Nullsafe.Mode.STRICT)
@DoNotStrip
class AvifDecoderFactoryImpl(bitmapPool: BitmapPool) : AvifDecoderFactory {
  override val avifDecoder: ImageDecoder = AvifDecoder(bitmapPool)
}