/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.decoder

import android.graphics.Bitmap
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.references.CloseableReference
import com.facebook.common.references.ResourceReleaser
import com.facebook.fresco.middleware.HasExtraData
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.image.ImmutableQualityInfo
import com.facebook.imagepipeline.platform.PlatformDecoder
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer
import com.facebook.imagepipeline.transformation.BitmapTransformation
import com.facebook.imagepipeline.transformation.CircularTransformation
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultImageDecoderTest {

  private lateinit var platformDecoder: PlatformDecoder
  private lateinit var decoder: DefaultImageDecoder
  private lateinit var encodedImage: EncodedImage
  private lateinit var bitmap: Bitmap
  private lateinit var bitmapRef: CloseableReference<Bitmap>
  private val bitmapReleaser = ResourceReleaser<Bitmap> {}

  @Before
  fun setUp() {
    platformDecoder = mock()
    decoder = DefaultImageDecoder(null, platformDecoder)

    bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    bitmapRef = CloseableReference.of(bitmap, bitmapReleaser)

    val byteBuffer = TrivialPooledByteBuffer(ByteArray(256))
    encodedImage = EncodedImage(CloseableReference.of<PooledByteBuffer>(byteBuffer))

    whenever(
            platformDecoder.decodeJPEGFromEncodedImageWithColorSpace(
                any(),
                any(),
                anyOrNull(),
                any(),
                anyOrNull(),
            )
        )
        .thenReturn(bitmapRef.clone())
  }

  @After
  fun tearDown() {
    bitmapRef.close()
    encodedImage.close()
  }

  @Test
  fun decodeJpeg_intermediateQuality_withIntermediateTransformation_appliesBothTransformations() {
    val regularTransformation = mock<BitmapTransformation>()
    val intermediateTransformation = mock<BitmapTransformation>()
    val options =
        ImageDecodeOptions.newBuilder()
            .setBitmapTransformation(regularTransformation)
            .setIntermediateImageBitmapTransformation(intermediateTransformation)
            .build()
    val quality = ImmutableQualityInfo.of(1, false, false)

    decoder.decodeJpeg(encodedImage, 256, quality, options, null)

    verify(regularTransformation).transform(any())
    verify(intermediateTransformation).transform(any())
  }

  @Test
  fun decodeJpeg_fullQuality_withIntermediateTransformation_skipsIntermediateTransformation() {
    val regularTransformation = mock<BitmapTransformation>()
    val intermediateTransformation = mock<BitmapTransformation>()
    val options =
        ImageDecodeOptions.newBuilder()
            .setBitmapTransformation(regularTransformation)
            .setIntermediateImageBitmapTransformation(intermediateTransformation)
            .build()

    decoder.decodeJpeg(encodedImage, 256, ImmutableQualityInfo.FULL_QUALITY, options, null)

    verify(regularTransformation).transform(any())
    verify(intermediateTransformation, never()).transform(any())
  }

  @Test
  fun decodeJpeg_intermediateQuality_noIntermediateTransformation_appliesOnlyRegular() {
    val regularTransformation = mock<BitmapTransformation>()
    val options =
        ImageDecodeOptions.newBuilder().setBitmapTransformation(regularTransformation).build()
    val quality = ImmutableQualityInfo.of(1, false, false)

    decoder.decodeJpeg(encodedImage, 256, quality, options, null)

    verify(regularTransformation).transform(any())
  }

  @Test
  fun decodeJpeg_onlyIntermediateTransformation_intermediateQuality_appliesTransformation() {
    val intermediateTransformation = mock<BitmapTransformation>()
    val options =
        ImageDecodeOptions.newBuilder()
            .setIntermediateImageBitmapTransformation(intermediateTransformation)
            .build()
    val quality = ImmutableQualityInfo.of(1, false, false)

    decoder.decodeJpeg(encodedImage, 256, quality, options, null)

    verify(intermediateTransformation).transform(any())
  }

  @Test
  fun decodeJpeg_onlyIntermediateTransformation_fullQuality_doesNotApplyTransformation() {
    val intermediateTransformation = mock<BitmapTransformation>()
    val options =
        ImageDecodeOptions.newBuilder()
            .setIntermediateImageBitmapTransformation(intermediateTransformation)
            .build()

    decoder.decodeJpeg(encodedImage, 256, ImmutableQualityInfo.FULL_QUALITY, options, null)

    verify(intermediateTransformation, never()).transform(any())
  }

  @Test
  fun decodeJpeg_circularTransformation_withIntermediate_setsIsRoundedBasedOnRegularOnly() {
    val circularTransformation =
        mock<BitmapTransformation>(extraInterfaces = arrayOf(CircularTransformation::class))
    val intermediateTransformation = mock<BitmapTransformation>()
    val options =
        ImageDecodeOptions.newBuilder()
            .setBitmapTransformation(circularTransformation)
            .setIntermediateImageBitmapTransformation(intermediateTransformation)
            .build()
    val quality = ImmutableQualityInfo.of(1, false, false)

    val result = decoder.decodeJpeg(encodedImage, 256, quality, options, null)

    assertThat(result.getExtra<Boolean>(HasExtraData.KEY_IS_ROUNDED)).isTrue()
  }

  // The regular transformation (e.g. crop/resize) must run before the intermediate one (e.g.
  // blur). Progressive blur depends on this: blur a cropped image, not crop a blurred one.
  @Test
  fun decodeJpeg_intermediateQuality_appliesRegularBeforeIntermediateTransformation() {
    val regularTransformation = mock<BitmapTransformation>()
    val intermediateTransformation = mock<BitmapTransformation>()
    val options =
        ImageDecodeOptions.newBuilder()
            .setBitmapTransformation(regularTransformation)
            .setIntermediateImageBitmapTransformation(intermediateTransformation)
            .build()
    val quality = ImmutableQualityInfo.of(1, false, false)

    decoder.decodeJpeg(encodedImage, 256, quality, options, null)

    inOrder(regularTransformation, intermediateTransformation) {
      verify(regularTransformation).transform(any<Bitmap>())
      verify(intermediateTransformation).transform(any<Bitmap>())
    }
  }
}
