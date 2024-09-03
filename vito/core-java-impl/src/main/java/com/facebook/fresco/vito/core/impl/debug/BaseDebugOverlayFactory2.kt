/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.debug

import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import com.facebook.common.internal.Supplier
import com.facebook.fresco.ui.common.ControllerListener2.Extras
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.impl.FrescoDrawable2

abstract class BaseDebugOverlayFactory2(private val debugOverlayEnabled: Supplier<Boolean>) :
    DebugOverlayFactory2 {

  override fun update(drawable: FrescoDrawable2, extras: Extras?) {
    if (!debugOverlayEnabled.get()) {
      return
    }
    val overlay = extractOrCreate(drawable)
    overlay.onBoundsChangedCallback = { _: Rect? -> update(drawable, extras) }
    overlay.reset()
    setData(overlay, drawable, extras)
    overlay.invalidateSelf()
  }

  protected abstract fun setData(
      overlay: DebugOverlayDrawable,
      drawable: FrescoDrawableInterface,
      extras: Extras?
  )

  private class DebugOverlayDrawableWrapper(
      existingOverlayDrawable: Drawable,
      val debugOverlayDrawable: DebugOverlayDrawable
  ) : LayerDrawable(arrayOf(existingOverlayDrawable, debugOverlayDrawable))

  companion object {
    private fun extractOrCreate(drawable: FrescoDrawable2): DebugOverlayDrawable {
      val existingOverlay = drawable.overlayDrawable
      if (existingOverlay is DebugOverlayDrawable) {
        return existingOverlay
      } else if (existingOverlay is DebugOverlayDrawableWrapper) {
        return existingOverlay.debugOverlayDrawable
      }
      val debugOverlay = DebugOverlayDrawable("v2")
      if (existingOverlay != null) {
        drawable.setOverlayDrawable(DebugOverlayDrawableWrapper(existingOverlay, debugOverlay))
      } else {
        drawable.setOverlayDrawable(debugOverlay)
      }
      return debugOverlay
    }
  }
}
