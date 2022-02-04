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

  private var useVisibilityCallbacks = Suppliers.BOOLEAN_TRUE
  private var useSimpleFetchLogic = Suppliers.BOOLEAN_FALSE

  @JvmStatic
  fun setUseVisibilityCallbacks(useVisibilityCallbacks: Supplier<Boolean>) {
    this.useVisibilityCallbacks = useVisibilityCallbacks
  }

  @JvmStatic
  fun setUseSimpleFetchLogic(useSimpleFetchLogic: Supplier<Boolean>) {
    this.useSimpleFetchLogic = useSimpleFetchLogic
  }

  private val onAttachStateChangeListenerCallback: OnAttachStateChangeListener =
      object : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
          val current = getDrawable(view)
          if (current != null) {
            current.imagePerfListener.onImageMount(current)
            maybeFetchImage(current)
          }
        }

        override fun onViewDetachedFromWindow(view: View) {
          val current = getDrawable(view)
          if (current != null) {
            current.imagePerfListener.onImageUnmount(current)
            FrescoVitoProvider.getController().release(current)
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
    val refetchRunnable = Runnable {
      FrescoVitoProvider.getController()
          .fetch(frescoDrawable, imageRequest, callerContext, null, imageListener, null, null)
    }
    frescoDrawable.refetchRunnable = refetchRunnable
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
    val drawable = getDrawable(target)
    if (drawable != null) {
      drawable.imagePerfListener.onImageUnmount(drawable)
      FrescoVitoProvider.getController().releaseImmediately(drawable)
      // When we manually release an image, we do not want the possibility to refetch later since
      // we expect a new fetch call.
      drawable.refetchRunnable = null
    }
  }

  private fun maybeFetchImage(drawable: FrescoDrawableInterface) {
    val refetchRunnable = drawable.refetchRunnable ?: return
    refetchRunnable.run()
  }

  /**
   * Ensure that a [FrescoDrawableInterface] is set for the given View target
   *
   * @param target the target to use
   * @return The drawable to use for the given target
   */
  private fun ensureDrawableSet(target: View): FrescoDrawableInterface {
    if (target is ImageView) {
      val iv = target
      val current = iv.drawable
      return if (current is FrescoDrawableInterface) {
        current
      } else {
        val drawable = createDrawable()
        iv.setImageDrawable(drawable as Drawable)
        drawable
      }
    }
    val background = target.background
    if (background is FrescoDrawableInterface) {
      return background
    }
    val drawable = createDrawable()
    androidx.core.view.ViewCompat.setBackground(target, drawable as Drawable)
    return drawable
  }

  private fun getDrawable(view: View): FrescoDrawableInterface? {
    val d = if (view is ImageView) view.drawable else view.background
    return if (d is FrescoDrawableInterface) d else null
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
