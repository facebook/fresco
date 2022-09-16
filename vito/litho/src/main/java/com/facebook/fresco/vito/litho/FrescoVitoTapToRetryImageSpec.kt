/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.facebook.drawee.drawable.ScaleTypeDrawable
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.fresco.ui.common.OnFadeListener
import com.facebook.fresco.vito.listener.BaseImageListener
import com.facebook.fresco.vito.listener.ForwardingImageListener
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.litho.FrescoVitoImage2Spec.Prefetch
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.litho.AccessibilityRole
import com.facebook.litho.ClickEvent
import com.facebook.litho.Component
import com.facebook.litho.ComponentContext
import com.facebook.litho.EventHandler
import com.facebook.litho.LongClickEvent
import com.facebook.litho.StateValue
import com.facebook.litho.TouchEvent
import com.facebook.litho.annotations.LayoutSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateLayout
import com.facebook.litho.annotations.OnEvent
import com.facebook.litho.annotations.OnPopulateAccessibilityNode
import com.facebook.litho.annotations.OnUpdateState
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.ResType
import com.facebook.litho.annotations.State
import com.facebook.litho.widget.Image

@LayoutSpec
object FrescoVitoTapToRetryImageSpec {

  @PropDefault const val maxTapCount = Int.MAX_VALUE

  @PropDefault const val imageAspectRatio = FrescoVitoImage2Spec.imageAspectRatio

  @PropDefault val prefetch = FrescoVitoImage2Spec.prefetch

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(
      c: ComponentContext,
      isTapToRetry: StateValue<Boolean>,
      tapCount: StateValue<Int>,
      useFallbackImageSource: StateValue<Boolean>,
      @Prop(optional = true) isInitialTapToLoad: Boolean
  ) {
    isTapToRetry.set(isInitialTapToLoad)
    tapCount.set(if (isInitialTapToLoad) 1 else 0)
    useFallbackImageSource.set(false)
  }

  @JvmStatic
  @OnCreateLayout
  fun onCreateLayout(
      c: ComponentContext,
      @Prop imageSource: ImageSource?,
      @Prop(optional = true) fallbackImageSource: ImageSource?,
      @Prop(optional = true) callerContext: Any?,
      @Prop(optional = true, resType = ResType.FLOAT) imageAspectRatio: Float,
      @Prop(optional = true) imageClickHandler: EventHandler<ClickEvent>?,
      @Prop(optional = true) imageLongClickHandler: EventHandler<LongClickEvent>?,
      @Prop(optional = true) imageTouchHandler: EventHandler<TouchEvent>?,
      @Prop(optional = true) imageListener: ImageListener?,
      @Prop(optional = true) imageOptions: ImageOptions?,
      @Prop(optional = true) maxTapCount: Int,
      @Prop(optional = true) onFadeListener: OnFadeListener?,
      @Prop(optional = true) prefetch: Prefetch?,
      @Prop(optional = true) prefetchRequestListener: RequestListener?,
      @Prop(resType = ResType.DRAWABLE, optional = true) retryImage: Drawable?,
      @Prop(optional = true) retryImageScaleType: ScalingUtils.ScaleType?,
      @State isTapToRetry: Boolean,
      @State useFallbackImageSource: Boolean,
      @State tapCount: Int
  ): Component {
    if (isTapToRetry) {
      val scaledRetryDrawable =
          if (retryImageScaleType == null || retryImage == null) retryImage
          else ScaleTypeDrawable(retryImage, retryImageScaleType)
      return Image.create(c)
          .drawable(scaledRetryDrawable)
          .scaleType(ImageView.ScaleType.FIT_XY)
          .clickHandler(FrescoVitoTapToRetryImage.onRetryClickEvent(c))
          .aspectRatio(imageAspectRatio)
          .build()
    }
    val internalListener: ImageListener =
        object : BaseImageListener() {
          override fun onFailure(id: Long, error: Drawable?, throwable: Throwable?) {
            if (tapCount < maxTapCount) {
              FrescoVitoTapToRetryImage.onImageFailure(c)
            }
          }
        }
    val useFallback = fallbackImageSource != null && useFallbackImageSource
    return FrescoVitoImage2.create(c)
        .callerContext(callerContext)
        .imageAspectRatio(imageAspectRatio)
        .imageListener(ForwardingImageListener.create(internalListener, imageListener))
        .imageOptions(imageOptions)
        .imageSource(if (useFallback) fallbackImageSource else imageSource)
        .onFadeListener(onFadeListener)
        .prefetch(prefetch)
        .clickHandler(imageClickHandler)
        .longClickHandler(imageLongClickHandler)
        .touchHandler(imageTouchHandler)
        .prefetchRequestListener(prefetchRequestListener)
        .build()
  }

  @JvmStatic
  @OnPopulateAccessibilityNode
  fun onPopulateAccessibilityNode(
      c: ComponentContext,
      host: View,
      node: AccessibilityNodeInfoCompat
  ) {
    node.className = AccessibilityRole.IMAGE
  }

  @JvmStatic
  @OnUpdateState
  fun onImageFailure(
      isTapToRetry: StateValue<Boolean>,
      tapCount: StateValue<Int>,
      useFallbackImageSource: StateValue<Boolean>
  ) {
    val oldTapCount = tapCount.get()
    val newTapCount = if (oldTapCount == null) 1 else oldTapCount + 1
    tapCount.set(newTapCount)
    isTapToRetry.set(true)
    useFallbackImageSource.set(true)
  }

  @JvmStatic
  @OnUpdateState
  fun onTapToRetry(isTapToRetry: StateValue<Boolean>) {
    isTapToRetry.set(false)
  }

  @JvmStatic
  @OnEvent(ClickEvent::class)
  fun onRetryClickEvent(c: ComponentContext) {
    FrescoVitoTapToRetryImage.onTapToRetry(c)
  }
}
