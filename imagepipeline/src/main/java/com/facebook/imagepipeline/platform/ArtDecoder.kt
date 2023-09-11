/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.graphics.BitmapFactory.Options
import android.os.Build
import androidx.core.util.Pools
import com.facebook.imagepipeline.memory.BitmapPool
import com.facebook.imageutils.BitmapUtil
import java.nio.ByteBuffer
import javax.annotation.concurrent.ThreadSafe

/** Bitmap decoder for ART VM (Lollipop and up). */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@ThreadSafe
class ArtDecoder(
    bitmapPool: BitmapPool,
    decodeBuffers: Pools.Pool<ByteBuffer>,
    platformDecoderOptions: PlatformDecoderOptions
) : DefaultDecoder(bitmapPool, decodeBuffers, platformDecoderOptions) {
  override fun getBitmapSize(width: Int, height: Int, options: Options): Int {
    @SuppressLint("RestrictedApi") val c = checkNotNull(options.inPreferredConfig)
    return BitmapUtil.getSizeInByteForBitmap(width, height, c)
  }
}
