/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.view.impl

import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.widget.ImageView
import androidx.core.view.ViewCompat
import com.facebook.common.internal.Supplier
import com.facebook.common.internal.Suppliers
import com.facebook.drawee.drawable.VisibilityCallback
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.provider.FrescoVitoProvider
import com.facebook.fresco.vito.source.ImageSource

/** Vito View implementation */
object VitoViewImpl2 {
  @JvmStatic var useVisibilityCallbacks: Supplier<Boolean> = Suppliers.BOOLEAN_TRUE
  @JvmStatic var useSimpleFetchLogic: Supplier<Boolean> = Suppliers.BOOLEAN_FALSE

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
            FrescoVitoProvider.getController().release(this)
          }
        }
      }

  @JvmStatic
  fun show(
      imageSource: ImageSource,
      imageOptions: ImageOptions,
      callerContext: Any?,
      imageListener: ImageListener?,
      target: View
  ) {
    show(
        FrescoVitoProvider.getImagePipeline()
            .createImageRequest(target.resources, imageSource, imageOptions),
        callerContext,
        imageListener,
        target)
  }

  @JvmStatic
  fun show(
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      imageListener: ImageListener?,
      target: View
  ) {
    val frescoDrawable = ensureDrawableSet(target)
    // The Drawable might be re-purposed before being cleaned up, so we release if necessary.
    val oldImageRequest = frescoDrawable.imageRequest
    if (oldImageRequest != null && oldImageRequest != imageRequest) {
      FrescoVitoProvider.getController().releaseImmediately(frescoDrawable)
    }
    frescoDrawable.refetchRunnable = Runnable {
      FrescoVitoProvider.getController()
          .fetch(frescoDrawable, imageRequest, callerContext, null, imageListener, null, null)
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
  private fun ensureDrawableSet(target: View): FrescoDrawableInterface {
    return when (target) {
      is ImageView ->
          when (val current = target.drawable) {
            is FrescoDrawableInterface -> current
            else ->
                createDrawable().also {
                  // Force the Drawable to adjust its bounds to match the hosting ImageView's
                  // bounds, since Fresco has custom scale types that are separate from ImageView's
                  // scale type.
                  // Without this, the Drawable would not respect the given Fresco ScaleType,
                  // effectively resulting in CENTER_INSIDE.
                  target.scaleType = ImageView.ScaleType.FIT_XY
                  target.setImageDrawable(it as Drawable)
                }
          }
      else ->
          when (val current = target.background) {
            is FrescoDrawableInterface -> current
            else -> createDrawable().also { ViewCompat.setBackground(target, it as Drawable) }
          }
    }
  }

  private fun getDrawable(view: View): FrescoDrawableInterface? {
    return (if (view is ImageView) view.drawable else view.background) as? FrescoDrawableInterface
  }

  private fun createDrawable(): FrescoDrawableInterface {
    val frescoDrawable: FrescoDrawableInterface =
        FrescoVitoProvider.getController().createDrawable()
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
