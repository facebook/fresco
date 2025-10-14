/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.animation.bitmap.cache

import android.graphics.Bitmap
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/** Tests [FrescoFrameCache]. */
@RunWith(RobolectricTestRunner::class)
class FrescoFrameCacheTest {
  private lateinit var imageReference: CloseableReference<CloseableImage>
  private lateinit var closeableStaticBitmap: CloseableStaticBitmap
  private lateinit var bitmapReference: CloseableReference<Bitmap>
  private lateinit var bitmapReferenceClone: CloseableReference<Bitmap>
  private lateinit var underlyingBitmap: Bitmap

  @Before
  fun setup() {
    imageReference = mock()
    closeableStaticBitmap = mock()
    bitmapReference = mock()
    bitmapReferenceClone = mock()
    underlyingBitmap = mock()

    whenever(bitmapReference.isValid).thenReturn(true)
    whenever(bitmapReference.get()).thenReturn(underlyingBitmap)

    whenever(bitmapReferenceClone.isValid).thenReturn(true)
    whenever(bitmapReferenceClone.get()).thenReturn(underlyingBitmap)

    whenever(closeableStaticBitmap.isClosed()).thenReturn(false)
    whenever(closeableStaticBitmap.getUnderlyingBitmap()).thenReturn(underlyingBitmap)
    whenever(closeableStaticBitmap.convertToBitmapReference()).thenReturn(bitmapReference)
    whenever(closeableStaticBitmap.cloneUnderlyingBitmapReference())
        .thenReturn(bitmapReferenceClone)

    whenever(imageReference.isValid).thenReturn(true)
    whenever(imageReference.get()).thenReturn(closeableStaticBitmap)
  }

  @Test
  @Throws(Exception::class)
  fun testExtractAndClose() {
    val extractedReference: CloseableReference<Bitmap>? =
        FrescoFrameCache.convertToBitmapReferenceAndClose(imageReference)

    Assertions.assertThat(extractedReference).isNotNull()
    extractedReference?.let { ref ->
      Assertions.assertThat(ref.get()).isEqualTo(underlyingBitmap)
      ref.close()
    }
    verify(imageReference).close()
  }

  @Test
  @Throws(Exception::class)
  fun testExtractAndClose_whenBitmapRecycled_thenReturnReference() {
    whenever(underlyingBitmap.isRecycled).thenReturn(true)

    val extractedReference: CloseableReference<Bitmap>? =
        FrescoFrameCache.convertToBitmapReferenceAndClose(imageReference)

    // We only detach the reference and do not care if the bitmap is valid
    Assertions.assertThat(extractedReference).isNotNull()
    extractedReference?.let { ref ->
      Assertions.assertThat(ref.get()).isEqualTo(underlyingBitmap)
      ref.close()
    }
    verify(imageReference).close()
  }

  @Test
  @Throws(Exception::class)
  fun testExtractAndClose_whenBitmapReferenceInvalid_thenReturnReference() {
    whenever(bitmapReference.isValid).thenReturn(false)

    val extractedReference: CloseableReference<Bitmap>? =
        FrescoFrameCache.convertToBitmapReferenceAndClose(imageReference)

    // We only detach the reference and do not care if the bitmap reference is valid
    Assertions.assertThat(extractedReference).isNotNull()
    extractedReference?.let { ref ->
      Assertions.assertThat(ref.get()).isEqualTo(underlyingBitmap)
      ref.close()
    }
    verify(imageReference).close()
  }

  @Test
  @Throws(Exception::class)
  fun testExtractAndClose_whenCloseableStaticBitmapClosed_thenReturnNull() {
    whenever(closeableStaticBitmap.isClosed()).thenReturn(true)
    whenever(closeableStaticBitmap.cloneUnderlyingBitmapReference()).thenReturn(null)

    val extractedReference: CloseableReference<Bitmap>? =
        FrescoFrameCache.convertToBitmapReferenceAndClose(imageReference)

    // We only detach the reference and do not care if the bitmap is valid
    Assertions.assertThat(extractedReference).isNull()
    verify(imageReference).close()
  }

  @Test
  @Throws(Exception::class)
  fun testExtractAndClose_whenImageReferenceInvalid_thenReturnNull() {
    whenever(imageReference.isValid).thenReturn(false)

    val extractedReference: CloseableReference<Bitmap>? =
        FrescoFrameCache.convertToBitmapReferenceAndClose(imageReference)

    // We only detach the reference and do not care if the bitmap is valid
    Assertions.assertThat(extractedReference).isNull()
    verify(imageReference).close()
  }

  @Test
  @Throws(Exception::class)
  fun testExtractAndClose_whenInputNull_thenReturnNull() {
    val extractedReference: CloseableReference<Bitmap>? =
        FrescoFrameCache.convertToBitmapReferenceAndClose(null)

    Assertions.assertThat(extractedReference).isNull()
    verifyNoMoreInteractions(imageReference)
  }

  @Test
  @Throws(Exception::class)
  fun testExtractAndClose_whenCloseableStaticBitmapNull_thenReturnNull() {
    whenever(imageReference.get()).thenReturn(null)

    val extractedReference: CloseableReference<Bitmap>? =
        FrescoFrameCache.convertToBitmapReferenceAndClose(imageReference)

    Assertions.assertThat(extractedReference).isNull()
    verify(imageReference).close()
  }
}
