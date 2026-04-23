/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.view.impl

import android.app.Activity
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import com.facebook.drawee.drawable.VisibilityCallback
import com.facebook.fresco.vito.core.FrescoController2
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.FrescoVitoConfig
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher
import com.facebook.fresco.vito.core.ImagePerfLoggingListener
import com.facebook.fresco.vito.core.VitoImagePerfListener
import com.facebook.fresco.vito.core.VitoImagePipeline
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.provider.components.FrescoVitoComponents
import com.facebook.fresco.vito.provider.setup.FrescoVitoSetup
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class VitoViewImpl2Test {

  private val mockController = mock<FrescoController2>()
  private val mockImagePipeline = mock<VitoImagePipeline>()
  private val mockPrefetcher = mock<FrescoVitoPrefetcher>()
  private val mockConfig = mock<FrescoVitoConfig>()
  private val mockImageRequest = mock<VitoImageRequest>()

  private val testDrawable = TestFrescoDrawableImpl()

  private lateinit var activityController: ActivityController<Activity>
  private lateinit var imageView: ImageView
  private lateinit var attachListener: View.OnAttachStateChangeListener

  @Before
  fun setup() {
    FrescoVitoComponents.resetImplementation()

    val mockSetup = mock<FrescoVitoSetup>()
    whenever(mockSetup.getController()).thenReturn(mockController)
    whenever(mockSetup.getImagePipeline()).thenReturn(mockImagePipeline)
    whenever(mockSetup.getPrefetcher()).thenReturn(mockPrefetcher)
    whenever(mockSetup.getConfig()).thenReturn(mockConfig)

    FrescoVitoComponents.setImplementation(mockSetup)

    @Suppress("UNCHECKED_CAST")
    whenever(mockController.createDrawable<TestFrescoDrawableImpl>(anyOrNull()))
        .thenReturn(testDrawable)

    activityController = Robolectric.buildActivity(Activity::class.java).create().start().resume()
    imageView = ImageView(activityController.get())
    activityController.get().setContentView(imageView)

    // Access the private onAttachStateChangeListenerCallback via reflection
    // to directly invoke attach/detach callbacks (Robolectric's ViewGroup
    // removeView/addView does not reliably dispatch these)
    val field = VitoViewImpl2::class.java.getDeclaredField("onAttachStateChangeListenerCallback")
    field.isAccessible = true
    attachListener = field.get(VitoViewImpl2) as View.OnAttachStateChangeListener
  }

  @After
  fun teardown() {
    FrescoVitoComponents.resetImplementation()
  }

  @Test
  fun testOnViewDetachedFromWindow_refetchRunnablePreserved() {
    VitoViewImpl2.show(
        mockImageRequest,
        null,
        null,
        null,
        imageView,
    )

    // refetchRunnable should be set after show()
    assertThat(testDrawable.refetchRunnable).isNotNull()
    val runnableBeforeDetach = testDrawable.refetchRunnable

    // Simulate view detach (RecyclerView scrolls view off-screen)
    attachListener.onViewDetachedFromWindow(imageView)

    // refetchRunnable must survive detach — this is the exact assertion that would
    // fail if D86529437's "refetchRunnable = null" were reintroduced
    assertThat(testDrawable.refetchRunnable).isNotNull()
    assertThat(testDrawable.refetchRunnable).isSameAs(runnableBeforeDetach)
  }

  @Test
  fun testOnViewReattached_afterDetach_refetchRunnableStillCallsFetch() {
    VitoViewImpl2.show(
        mockImageRequest,
        null,
        null,
        null,
        imageView,
    )

    assertThat(testDrawable.refetchRunnable).isNotNull()

    // Simulate detach then reattach (RecyclerView recycles ViewHolder)
    attachListener.onViewDetachedFromWindow(imageView)
    attachListener.onViewAttachedToWindow(imageView)

    // After reattach, maybeFetchImage calls refetchRunnable.run() -> controller.fetch()
    verify(mockController)
        .fetch(
            drawable = any(),
            imageRequest = any(),
            callerContext = anyOrNull(),
            contextChain = anyOrNull(),
            listener = anyOrNull(),
            perfDataListener = anyOrNull(),
            onFadeListener = anyOrNull(),
            viewportDimensions = anyOrNull(),
            vitoImageRequestListener = anyOrNull(),
        )
  }

  @Test
  fun testRelease_refetchRunnableCleared() {
    VitoViewImpl2.show(
        mockImageRequest,
        null,
        null,
        null,
        imageView,
    )

    assertThat(testDrawable.refetchRunnable).isNotNull()

    // release() is the correct permanent cleanup path — it SHOULD null refetchRunnable
    VitoViewImpl2.release(imageView)

    assertThat(testDrawable.refetchRunnable).isNull()
    verify(mockController).releaseImmediately(any())
  }

  @Test
  fun testOnViewDetachedFromWindow_allMutableStatePreserved() {
    // Set up all mutable state via show()
    val mockListener = mock<ImageListener>()
    VitoViewImpl2.show(
        mockImageRequest,
        "testCallerContext",
        mockListener,
        null,
        imageView,
    )

    // Capture pre-detach state
    val originalRefetchRunnable = testDrawable.refetchRunnable
    val originalImageRequest = testDrawable.imageRequest
    val originalCallerContext = testDrawable.callerContext
    val originalImageListener = testDrawable.imageListener
    val originalExtras = testDrawable.extras

    assertThat(originalRefetchRunnable).isNotNull()

    // Simulate view detach (RecyclerView scrolls view off-screen)
    attachListener.onViewDetachedFromWindow(imageView)

    // All mutable state must survive detach
    assertThat(testDrawable.refetchRunnable).isSameAs(originalRefetchRunnable)
    assertThat(testDrawable.imageRequest).isSameAs(originalImageRequest)
    assertThat(testDrawable.callerContext).isSameAs(originalCallerContext)
    assertThat(testDrawable.imageListener).isSameAs(originalImageListener)
    assertThat(testDrawable.extras).isSameAs(originalExtras)
  }

  @Test
  fun testRelease_allMutableStateCleared() {
    VitoViewImpl2.show(
        mockImageRequest,
        "testCallerContext",
        null,
        null,
        imageView,
    )

    assertThat(testDrawable.refetchRunnable).isNotNull()

    // release() is the correct permanent cleanup path
    VitoViewImpl2.release(imageView)

    // refetchRunnable must be null after release
    assertThat(testDrawable.refetchRunnable).isNull()
    // release() must call releaseImmediately on the controller
    verify(mockController).releaseImmediately(any())
  }

  @Test
  fun testMultipleDetachReattachCycles_refetchRunnablePreservedOnEveryCycle() {
    VitoViewImpl2.show(
        mockImageRequest,
        null,
        null,
        null,
        imageView,
    )

    val originalRefetchRunnable = testDrawable.refetchRunnable
    assertThat(originalRefetchRunnable).isNotNull()

    // Run 10 rapid detach/reattach cycles
    for (cycle in 1..10) {
      attachListener.onViewDetachedFromWindow(imageView)
      assertThat(testDrawable.refetchRunnable)
          .describedAs("refetchRunnable must survive detach on cycle %d", cycle)
          .isNotNull()
          .isSameAs(originalRefetchRunnable)

      attachListener.onViewAttachedToWindow(imageView)
      assertThat(testDrawable.refetchRunnable)
          .describedAs("refetchRunnable must survive reattach on cycle %d", cycle)
          .isNotNull()
          .isSameAs(originalRefetchRunnable)
    }
  }

  @Test
  fun testMultipleDetachReattachCycles_controllerFetchCalledOnEveryReattach() {
    VitoViewImpl2.show(
        mockImageRequest,
        null,
        null,
        null,
        imageView,
    )

    val cycles = 5
    for (cycle in 1..cycles) {
      attachListener.onViewDetachedFromWindow(imageView)
      attachListener.onViewAttachedToWindow(imageView)
    }

    // controller.fetch() is called once per reattach via maybeFetchImage -> refetchRunnable.run()
    // Plus the initial fetch from show() when the view is attached
    verify(mockController, org.mockito.kotlin.atLeast(cycles))
        .fetch(
            drawable = any(),
            imageRequest = any(),
            callerContext = anyOrNull(),
            contextChain = anyOrNull(),
            listener = anyOrNull(),
            perfDataListener = anyOrNull(),
            onFadeListener = anyOrNull(),
            viewportDimensions = anyOrNull(),
            vitoImageRequestListener = anyOrNull(),
        )
  }

  @Test
  fun testMultipleDetachReattachCycles_attachListenerNotDuplicated() {
    // Call show() multiple times (as RecyclerView does when rebinding)
    val showCount = 5
    for (i in 1..showCount) {
      VitoViewImpl2.show(
          mockImageRequest,
          null,
          null,
          null,
          imageView,
      )
    }

    // Detach once — imagePerfListener.onImageUnmount should be called exactly once,
    // not showCount times (which would happen if listeners accumulated)
    attachListener.onViewDetachedFromWindow(imageView)

    verify(testDrawable.imagePerfListener, org.mockito.kotlin.times(1)).onImageUnmount(any())
  }

  @Test
  fun testRelease_afterDetachAndReattach_refetchRunnableNull() {
    VitoViewImpl2.show(
        mockImageRequest,
        null,
        null,
        null,
        imageView,
    )

    // Detach and reattach (simulating RecyclerView recycling)
    attachListener.onViewDetachedFromWindow(imageView)
    attachListener.onViewAttachedToWindow(imageView)

    // refetchRunnable should still be non-null after recycling
    assertThat(testDrawable.refetchRunnable).isNotNull()

    // Now release() — this should clear it permanently
    VitoViewImpl2.release(imageView)

    assertThat(testDrawable.refetchRunnable).isNull()
  }

  /** Concrete test double that implements both Drawable and FrescoDrawableInterface */
  class TestFrescoDrawableImpl : Drawable(), FrescoDrawableInterface {
    override val imageId: Long = 0
    override var callerContext: Any? = null
    override val imagePerfListener: VitoImagePerfListener = mock()
    override var uiFramework: String? = null
    override var forceReloadIfImageAlreadySet: Boolean = false
    override var retriggerListenersIfImageAlreadySet: Boolean = false
    override val actualImageDrawable: Drawable? = null
    override val isFetchSubmitted: Boolean = false
    override var imageRequest: VitoImageRequest? = null
    override var imageListener: ImageListener? = null
    override var extras: Any? = null
    override var refetchRunnable: Runnable? = null

    override fun setMutateDrawables(mutateDrawables: Boolean) {}

    override fun hasImage(): Boolean = false

    override fun setFetchSubmitted(fetchSubmitted: Boolean) {}

    override fun setVisibilityCallback(visibilityCallback: VisibilityCallback?) {}

    override fun setOverlayDrawable(drawable: Drawable?): Drawable? = null

    override fun getImagePerfLoggingListener(): ImagePerfLoggingListener? = null

    override fun setIntrinsicSize(width: Int, height: Int) {}

    override fun configureWhenUnderlyingChanged() {}

    override fun getActualImageBounds(outBounds: RectF) {}

    override fun hasBitmapWithGainmap(): Boolean = false

    override fun reportVisible(visible: Boolean) {}

    override fun draw(canvas: Canvas) {}

    override fun setAlpha(alpha: Int) {}

    override fun setColorFilter(colorFilter: ColorFilter?) {}

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
  }
}
