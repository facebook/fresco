/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.tools.liveeditor

import android.graphics.RectF
import android.graphics.drawable.Drawable
import com.facebook.drawee.drawable.VisibilityCallback
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.ImagePerfLoggingListener
import com.facebook.fresco.vito.core.VitoImagePerfListener
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.core.impl.BaseVitoImagePerfListener
import com.facebook.fresco.vito.listener.ImageListener
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Stress test for the unsynchronized `ArrayList` inside [ImageTracker]. `onImageMount` /
 * `onImageUnmount` are invoked from view attach/detach callbacks on whatever thread mounts the
 * image, so the backing list is mutated concurrently.
 */
@RunWith(RobolectricTestRunner::class)
class ImageTrackerConcurrencyTest {

  private class FakeFrescoDrawable(override val imageId: Long) : FrescoDrawableInterface {
    override var callerContext: Any? = null
    override val imagePerfListener: VitoImagePerfListener = BaseVitoImagePerfListener()
    override var uiFramework: String? = null
    override var forceReloadIfImageAlreadySet: Boolean = false
    override var retriggerListenersIfImageAlreadySet: Boolean = false
    override val actualImageDrawable: Drawable? = null
    override val isFetchSubmitted: Boolean = false
    override var imageRequest: VitoImageRequest? = null
    override var imageListener: ImageListener? = null
    override var extras: Any? = null
    override var refetchRunnable: Runnable? = null

    override fun setMutateDrawables(mutateDrawables: Boolean) = Unit

    override fun hasImage(): Boolean = false

    override fun setFetchSubmitted(fetchSubmitted: Boolean) = Unit

    override fun setVisibilityCallback(visibilityCallback: VisibilityCallback?) = Unit

    override fun setOverlayDrawable(drawable: Drawable?): Drawable? = null

    override fun getImagePerfLoggingListener(): ImagePerfLoggingListener? = null

    override fun setIntrinsicSize(width: Int, height: Int) = Unit

    override fun configureWhenUnderlyingChanged() = Unit

    override fun getActualImageBounds(outBounds: RectF) = Unit

    override fun hasBitmapWithGainmap(): Boolean = false

    override fun reportVisible(visible: Boolean) = Unit
  }

  @Test
  fun testMountUnmount_whenCalledConcurrently_thenNoException() {
    val threadCount = 16
    val opsPerThread = 4_000
    val failures = CopyOnWriteArrayList<Throwable>()

    var iteration = 0
    while (iteration < 5 && failures.isEmpty()) {
      iteration++
      val tracker = ImageTracker()
      val start = CountDownLatch(1)
      val done = CountDownLatch(threadCount)
      val threads =
          (0 until threadCount).map { threadIndex ->
            Thread {
              try {
                start.await()
                for (op in 0 until opsPerThread) {
                  val drawable = FakeFrescoDrawable((threadIndex * opsPerThread + op).toLong())
                  tracker.onImageMount(drawable)
                  tracker.onImageUnmount(drawable)
                }
              } catch (t: Throwable) {
                failures.add(t)
              } finally {
                done.countDown()
              }
            }
          }
      threads.forEach { it.start() }
      start.countDown()
      done.await(120, TimeUnit.SECONDS)
      threads.forEach { it.join(1_000) }
    }

    assertThat(failures)
        .describedAs(
            "ImageTracker.onImageMount/onImageUnmount threw under concurrent access: " +
                failures.joinToString("\n") { it.toString() },
        )
        .isEmpty()
  }
}
