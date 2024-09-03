/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.drawable.Drawable
import com.facebook.common.references.CloseableReference
import com.facebook.fresco.vito.core.NopDrawable
import com.facebook.imagepipeline.image.BaseCloseableImage
import com.facebook.imagepipeline.image.CloseableImage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FrescoDrawable2ImplTest {

  private lateinit var frescoDrawable: FrescoDrawable2Impl
  private lateinit var latch: CountDownLatch
  private lateinit var closeableImage: CloseableImage
  private lateinit var closeableReference: CloseableReference<CloseableImage>

  @Before
  fun setup() {
    frescoDrawable = FrescoDrawable2Impl(false, null, BaseVitoImagePerfListener())
    latch = CountDownLatch(1)
    closeableImage = DummyCloseableImage()
    closeableReference =
        CloseableReference.of(closeableImage) { value ->
          value.close()
          latch.countDown()
        }
  }

  @Test
  fun testFrescoDrawable_whenDrawableClosed_thenReleaseActualImageReference() {
    frescoDrawable.setImage(NopDrawable, closeableReference)
    closeableReference.close()
    Assert.assertFalse(closeableImage.isClosed)
    frescoDrawable.close()
    Assert.assertNull(frescoDrawable.imageReference)
    Assert.assertTrue(closeableImage.isClosed)
    Assert.assertTrue(latch.await(3, TimeUnit.SECONDS))
  }

  @Test
  fun testFrescoDrawable_whenNewImageDrawableSet_thenReleaseOldImageReference() {
    frescoDrawable.setImage(NopDrawable, closeableReference)
    closeableReference.close()
    val dummyCloseableImage = DummyCloseableImage()
    frescoDrawable.setImage(NopDrawable, CloseableReference.of(dummyCloseableImage))
    Assert.assertTrue(closeableImage.isClosed)
    Assert.assertTrue(latch.await(3, TimeUnit.SECONDS))
  }

  @Test
  fun testFrescoDrawable_whenNewNullImageDrawableSet_thenReleaseOldImageReference() {
    frescoDrawable.setImage(NopDrawable, closeableReference)
    closeableReference.close()
    frescoDrawable.setImage(null, null)
    Assert.assertTrue(closeableImage.isClosed)
    Assert.assertTrue(latch.await(3, TimeUnit.SECONDS))
  }

  @Test
  fun testFrescoDrawable_whenImageDrawableSet_thenReleaseImageReference() {
    frescoDrawable.setImage(NopDrawable, closeableReference)
    closeableReference.close()
    val drawable = mock<Drawable>()
    frescoDrawable.setImageDrawable(drawable)
    Assert.assertNull(frescoDrawable.imageReference)
    Assert.assertTrue(closeableImage.isClosed)
    Assert.assertTrue(latch.await(3, TimeUnit.SECONDS))
  }

  @Test
  fun testFrescoDrawable_whenImageDrawableReset_thenReleaseImageReference() {
    frescoDrawable.setImage(NopDrawable, closeableReference)
    closeableReference.close()
    frescoDrawable.setImageDrawable(null)
    Assert.assertNull(frescoDrawable.imageReference)
    Assert.assertTrue(closeableImage.isClosed)
    Assert.assertTrue(latch.await(3, TimeUnit.SECONDS))
  }

  internal class DummyCloseableImage : BaseCloseableImage() {
    private var closed = false

    override fun getSizeInBytes(): Int = 0

    override fun close() {
      closed = true
    }

    override fun isClosed(): Boolean = closed

    override fun getWidth(): Int = 0

    override fun getHeight(): Int = 0
  }
}
