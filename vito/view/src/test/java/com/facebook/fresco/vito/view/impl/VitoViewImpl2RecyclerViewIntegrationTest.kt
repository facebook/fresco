/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.view.impl

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.facebook.fresco.vito.core.FrescoController2
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.FrescoVitoConfig
import com.facebook.fresco.vito.core.FrescoVitoPrefetcher
import com.facebook.fresco.vito.core.VitoImagePipeline
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.provider.components.FrescoVitoComponents
import com.facebook.fresco.vito.provider.setup.FrescoVitoSetup
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

/**
 * Integration test verifying that view-recycling (detach/reattach) preserves Fresco Vito's
 * refetchRunnable and triggers image refetch on reattached views.
 *
 * Simulates the RecyclerView ViewHolder lifecycle: multiple ImageViews bound via
 * VitoViewImpl2.show(), detached and reattached through the onAttachStateChangeListener callback.
 * Uses reflection because Robolectric's ViewGroup does not reliably dispatch
 * onViewDetachedFromWindow/onViewAttachedToWindow during programmatic add/remove.
 */
@RunWith(RobolectricTestRunner::class)
class VitoViewImpl2RecyclerViewIntegrationTest {

  private val mockController = mock<FrescoController2>()
  private val mockImagePipeline = mock<VitoImagePipeline>()
  private val mockPrefetcher = mock<FrescoVitoPrefetcher>()
  private val mockConfig = mock<FrescoVitoConfig>()
  private val mockImageRequest = mock<VitoImageRequest>()

  private val drawablePool = mutableListOf<VitoViewImpl2Test.TestFrescoDrawableImpl>()

  private lateinit var activityController: ActivityController<Activity>
  private lateinit var container: ViewGroup
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

    // Each call to createDrawable returns a fresh TestFrescoDrawableImpl
    @Suppress("UNCHECKED_CAST")
    whenever(mockController.createDrawable<VitoViewImpl2Test.TestFrescoDrawableImpl>(anyOrNull()))
        .thenAnswer {
          val drawable = VitoViewImpl2Test.TestFrescoDrawableImpl()
          drawablePool.add(drawable)
          drawable
        }

    activityController = Robolectric.buildActivity(Activity::class.java).create().start().resume()
    val activity = activityController.get()

    container = LinearLayout(activity)
    activity.setContentView(container)

    // Access the private onAttachStateChangeListenerCallback via reflection
    val field = VitoViewImpl2::class.java.getDeclaredField("onAttachStateChangeListenerCallback")
    field.isAccessible = true
    attachListener = field.get(VitoViewImpl2) as View.OnAttachStateChangeListener
  }

  @After
  fun teardown() {
    FrescoVitoComponents.resetImplementation()
    drawablePool.clear()
  }

  @Test
  fun testRecyclerViewScroll_detachAndReattach_refetchRunnableSurvives() {
    // Simulate a ViewHolder binding: create an ImageView, call show(), then detach + reattach
    val imageView = ImageView(activityController.get())
    container.addView(imageView)

    VitoViewImpl2.show(
        mockImageRequest,
        null,
        null,
        null,
        imageView,
    )

    val drawable = VitoViewImpl2.getDrawable(imageView)
    assertThat(drawable).isNotNull()
    val originalRefetchRunnable = drawable!!.refetchRunnable
    assertThat(originalRefetchRunnable).isNotNull()

    // Simulate RecyclerView detach (scroll off-screen)
    attachListener.onViewDetachedFromWindow(imageView)

    // refetchRunnable must survive — this catches D86529437's regression
    assertThat(drawable.refetchRunnable).isNotNull()
    assertThat(drawable.refetchRunnable).isSameAs(originalRefetchRunnable)

    // Simulate RecyclerView reattach (scroll back on-screen)
    attachListener.onViewAttachedToWindow(imageView)

    // refetchRunnable must still be intact after full cycle
    assertThat(drawable.refetchRunnable).isSameAs(originalRefetchRunnable)
  }

  @Test
  fun testRecyclerViewScroll_detachAndReattach_controllerFetchCalledOnReattach() {
    val imageView = ImageView(activityController.get())
    container.addView(imageView)

    VitoViewImpl2.show(
        mockImageRequest,
        null,
        null,
        null,
        imageView,
    )

    val drawable = VitoViewImpl2.getDrawable(imageView)
    assertThat(drawable).isNotNull()

    // Simulate detach + reattach cycle
    attachListener.onViewDetachedFromWindow(imageView)
    attachListener.onViewAttachedToWindow(imageView)

    // controller.fetch() should be called on reattach via maybeFetchImage -> refetchRunnable.run()
    verify(mockController, atLeast(1))
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
  fun testRecyclerViewScroll_multipleViewHolders_allRefetchRunnablesSurviveRecycling() {
    // Create multiple ImageViews simulating multiple ViewHolders in a recycling pool
    val viewCount = 3
    val imageViews = (1..viewCount).map { ImageView(activityController.get()) }
    val drawables = mutableListOf<FrescoDrawableInterface>()

    for (imageView in imageViews) {
      container.addView(imageView)
      VitoViewImpl2.show(
          mockImageRequest,
          null,
          null,
          null,
          imageView,
      )
      val drawable = VitoViewImpl2.getDrawable(imageView)
      assertThat(drawable).isNotNull()
      assertThat(drawable!!.refetchRunnable).isNotNull()
      drawables.add(drawable)
    }

    // Capture original runnables
    val originalRunnables = drawables.map { it.refetchRunnable }

    // Detach all views (simulating scroll that moves all off-screen)
    for (imageView in imageViews) {
      attachListener.onViewDetachedFromWindow(imageView)
    }

    // All refetchRunnables must survive
    for (i in drawables.indices) {
      assertThat(drawables[i].refetchRunnable)
          .describedAs("refetchRunnable for view %d must survive detach", i)
          .isNotNull()
          .isSameAs(originalRunnables[i])
    }

    // Reattach all views
    for (imageView in imageViews) {
      attachListener.onViewAttachedToWindow(imageView)
    }

    // All refetchRunnables must still be intact
    for (i in drawables.indices) {
      assertThat(drawables[i].refetchRunnable)
          .describedAs("refetchRunnable for view %d must survive reattach", i)
          .isNotNull()
          .isSameAs(originalRunnables[i])
    }
  }

  @Test
  fun testRecyclerViewScroll_releaseCalledOnRemovedItem_refetchRunnableCleared() {
    val imageView = ImageView(activityController.get())
    container.addView(imageView)

    VitoViewImpl2.show(
        mockImageRequest,
        null,
        null,
        null,
        imageView,
    )

    val drawable = VitoViewImpl2.getDrawable(imageView)
    assertThat(drawable).isNotNull()
    assertThat(drawable!!.refetchRunnable).isNotNull()

    // Permanent removal via release() — this SHOULD clear refetchRunnable
    VitoViewImpl2.release(imageView)

    assertThat(drawable.refetchRunnable).isNull()
    verify(mockController).releaseImmediately(any())
  }

  @Test
  fun testRecyclerViewScroll_visibilityCallbackOnRecycle_doesNotClearRefetchRunnable() {
    // Disable visibility callbacks for this test — we want to verify the base
    // detach behavior without the VisibilityCallback interfering
    val originalUseVisibilityCallbacks = VitoViewImpl2.useVisibilityCallbacks
    VitoViewImpl2.useVisibilityCallbacks = com.facebook.common.internal.Suppliers.BOOLEAN_FALSE

    try {
      val imageView = ImageView(activityController.get())
      container.addView(imageView)

      VitoViewImpl2.show(
          mockImageRequest,
          null,
          null,
          null,
          imageView,
      )

      val drawable = VitoViewImpl2.getDrawable(imageView)
      assertThat(drawable).isNotNull()
      val originalRefetchRunnable = drawable!!.refetchRunnable
      assertThat(originalRefetchRunnable).isNotNull()

      // Simulate detach — refetchRunnable must survive
      attachListener.onViewDetachedFromWindow(imageView)
      assertThat(drawable.refetchRunnable).isSameAs(originalRefetchRunnable)
    } finally {
      VitoViewImpl2.useVisibilityCallbacks = originalUseVisibilityCallbacks
    }
  }
}
