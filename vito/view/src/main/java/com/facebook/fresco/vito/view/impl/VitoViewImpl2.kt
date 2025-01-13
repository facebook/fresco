/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.view.impl

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.widget.ImageView
import com.facebook.common.internal.Supplier
import com.facebook.common.internal.Suppliers
import com.facebook.drawee.drawable.VisibilityCallback
import com.facebook.fresco.ui.common.OnFadeListener
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.core.VitoImageRequestListener
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.provider.FrescoVitoProvider
import com.facebook.fresco.vito.source.ImageSource

/** Vito View implementation */
object VitoViewImpl2 {
  @JvmField var useVisibilityCallbacks: Supplier<Boolean> = Suppliers.BOOLEAN_TRUE
  @JvmField var useVisibilityAggregatedCallbacks: Supplier<Boolean> = Suppliers.BOOLEAN_FALSE
  @JvmField var useSimpleFetchLogic: Supplier<Boolean> = Suppliers.BOOLEAN_FALSE
  @JvmField var useReleaseInViewDetached: Supplier<Boolean> = Suppliers.BOOLEAN_TRUE
  @JvmField var useReleaseDelayedInViewDetached: Supplier<Boolean> = Suppliers.BOOLEAN_FALSE

  private val onAttachStateChangeListenerCallback: OnAttachStateChangeListener =
      object : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
          getDrawable(view)?.apply {
            imagePerfListener.onImageMount(this)
            maybeFetchImage(this)
          }
        }

        override fun onViewDetachedFromWindow(view: View) {
          getDrawable(view)?.apply {
            imagePerfListener.onImageUnmount(this)
            if (useReleaseInViewDetached.get()) {
              FrescoVitoProvider.getController().release(this)
            }
            if (useReleaseDelayedInViewDetached.get()) {
              FrescoVitoProvider.getController().releaseDelayed(this)
            }
          }
        }
      }

  @JvmStatic
  @JvmOverloads
  fun show(
      imageSource: ImageSource,
      imageOptions: ImageOptions,
      callerContext: Any?,
      imageListener: ImageListener?,
      imageRequestListener: VitoImageRequestListener?,
      target: View,
      onFadeListener: OnFadeListener? = null,
      uiFramework: String = "view",
  ) {
    show(
        FrescoVitoProvider.getImagePipeline()
            .createImageRequest(
                target.resources,
                imageSource,
                imageOptions,
                viewport = Rect(0, 0, target.width, target.height),
                callerContext = callerContext,
                forceKeepOriginalSize = false),
        callerContext,
        imageListener,
        imageRequestListener,
        target,
        onFadeListener,
        uiFramework)
  }

  @JvmStatic
  @JvmOverloads
  fun show(
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      imageListener: ImageListener?,
      imageRequestListener: VitoImageRequestListener?,
      target: View,
      onFadeListener: OnFadeListener? = null,
      uiFramework: String = "view",
  ) {
    val frescoDrawable = ensureDrawableSet(target, uiFramework)
    // The Drawable might be re-purposed before being cleaned up, so we release if necessary.
    val oldImageRequest = frescoDrawable.imageRequest
    if (oldImageRequest != null && oldImageRequest != imageRequest) {
      FrescoVitoProvider.getController().releaseImmediately(frescoDrawable)
    }
    frescoDrawable.refetchRunnable = Runnable {
      FrescoVitoProvider.getController()
          .fetch(
              drawable = frescoDrawable,
              imageRequest = imageRequest,
              callerContext = callerContext,
              contextChain = null,
              listener = imageListener,
              perfDataListener = null,
              onFadeListener = onFadeListener,
              viewportDimensions = Rect(0, 0, target.width, target.height),
              vitoImageRequestListener = imageRequestListener)
    }
    if (useSimpleFetchLogic.get()) {
      frescoDrawable.imagePerfListener.onImageMount(frescoDrawable)
      maybeFetchImage(frescoDrawable)
    } else {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        // If the view is already attached to the window, immediately fetch the image.
        // Otherwise, the fetch will be submitted later when then View is attached.
        if (target.isAttachedToWindow) {
          frescoDrawable.imagePerfListener.onImageMount(frescoDrawable)
          maybeFetchImage(frescoDrawable)
        }
      } else {
        // Before Kitkat we don't have a good way to know.
        // Normally we expect the view to be already attached, thus we always fetch the image.
        frescoDrawable.imagePerfListener.onImageMount(frescoDrawable)
        maybeFetchImage(frescoDrawable)
      }
    }

    // `addOnAttachStateChangeListener` is not idempotent
    target.removeOnAttachStateChangeListener(onAttachStateChangeListenerCallback)
    target.addOnAttachStateChangeListener(onAttachStateChangeListenerCallback)
  }

  @JvmStatic
  fun release(target: View) {
    getDrawable(target)?.apply {
      imagePerfListener.onImageUnmount(this)
      FrescoVitoProvider.getController().releaseImmediately(this)
      refetchRunnable = null
    }
  }

  private fun maybeFetchImage(drawable: FrescoDrawableInterface) {
    drawable.refetchRunnable?.run()
  }

  /**
   * Ensure that a [FrescoDrawableInterface] is set for the given View target
   *
   * @param target the target to use
   * @return The drawable to use for the given target
   */
  private fun ensureDrawableSet(target: View, uiFramework: String): FrescoDrawableInterface {
    return when (target) {
      is ImageView ->
          when (val current = target.drawable) {
            is FrescoDrawableInterface -> current
            else ->
                createDrawable(uiFramework).also { image ->
                  // Force the Drawable to adjust its bounds to match the hosting ImageView's
                  // bounds, since Fresco has custom scale types that are separate from ImageView's
                  // scale type.
                  // Without this, the Drawable would not respect the given Fresco ScaleType,
                  // effectively resulting in CENTER_INSIDE.
                  target.scaleType = ImageView.ScaleType.FIT_XY
                  target.setImageDrawable(image as Drawable)
                }
          }
      else ->
          when (val current = target.background) {
            is FrescoDrawableInterface -> current
            else -> createDrawable(uiFramework).also { target.setBackground(it as Drawable) }
          }
    }
  }

  fun getDrawable(view: View): FrescoDrawableInterface? {
    return (if (view is ImageView) view.drawable else view.background) as? FrescoDrawableInterface
  }

  private fun createDrawable(uiFramework: String): FrescoDrawableInterface {
    val frescoDrawable: FrescoDrawableInterface =
        FrescoVitoProvider.getController().createDrawable(uiFramework)
    if (useVisibilityCallbacks.get()) {
      frescoDrawable.setVisibilityCallback(
          object : VisibilityCallback {
            override fun onVisibilityChange(visible: Boolean) {
              if (!visible) {
                FrescoVitoProvider.getController().release(frescoDrawable)
              } else {
                maybeFetchImage(frescoDrawable)
              }
            }

            override fun onDraw() {
              // NOP
            }
          })
    }
    return frescoDrawable
  }
}
