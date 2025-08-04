/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image

import android.graphics.Bitmap
import android.media.ExifInterface
import com.facebook.common.references.ResourceReleaser
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

/** Basic tests for closeable bitmap */
@RunWith(RobolectricTestRunner::class)
class CloseableBitmapTest {
  private lateinit var bitmap: Bitmap
  private lateinit var resourceReleaser: ResourceReleaser<Bitmap>
  private lateinit var closeableStaticBitmap: DefaultCloseableStaticBitmap

  @Before
  fun setup() {
    bitmap = mock<Bitmap>()
    resourceReleaser = mock<ResourceReleaser<Bitmap>>()
    closeableStaticBitmap =
        DefaultCloseableStaticBitmap(
            bitmap,
            resourceReleaser,
            ImmutableQualityInfo.FULL_QUALITY,
            0,
            ExifInterface.ORIENTATION_UNDEFINED)
  }

  @Test
  fun testBasic() {
    assertThat(closeableStaticBitmap.isClosed).isFalse()
    assertThat(closeableStaticBitmap.underlyingBitmap).isSameAs(bitmap)

    // close it now
    closeableStaticBitmap.close()
    assertThat(closeableStaticBitmap.isClosed).isTrue()
    assertThat(closeableStaticBitmap.underlyingBitmap).isNull()
    verify(resourceReleaser).release(bitmap)

    // close it again
    closeableStaticBitmap.close()
    assertThat(closeableStaticBitmap.isClosed).isTrue()
    assertThat(closeableStaticBitmap.underlyingBitmap).isNull()
  }

  @Test
  fun testFinalize() {
    closeableStaticBitmap.finalize()
    assertThat(closeableStaticBitmap.isClosed).isTrue()
    assertThat(closeableStaticBitmap.underlyingBitmap).isNull()
    verify(resourceReleaser).release(bitmap)
  }
}
