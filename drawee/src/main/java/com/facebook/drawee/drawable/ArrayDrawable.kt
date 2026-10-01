/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable

import android.annotation.TargetApi
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import kotlin.math.max

/**
 * A Drawable that contains an array of other Drawables (layers). These are drawn in array order, so
 * the element with the largest index will be drawn on top.
 *
 * Similar to android's LayerDrawable but it doesn't support adding/removing layers dynamically.
 */
open class ArrayDrawable(layers: Array<out Drawable?>) :
    Drawable(), Drawable.Callback, TransformCallback, TransformAwareDrawable {

  private var transformCallback: TransformCallback? = null

  private val drawableProperties = DrawableProperties()

  // layers
  private val layers: Array<Drawable?>

  // drawable parents for the layers (lazily created)
  private val drawableParents: Array<DrawableParent?>

  // temp rect to avoid allocations
  private val tmpRect = Rect()

  // Whether the drawable is stateful or not
  private var _isStateful = false
  private var isStatefulCalculated = false

  private var isMutated = false

  val numberOfLayers: Int
    /**
     * Gets the number of layers.
     *
     * @return number of layers
     */
    get() = layers.size

  /**
   * Constructs a new layer drawable.
   *
   * @param layers the layers that this drawable displays
   */
  init {
    checkNotNull(layers) { "Layers cannot be null" }
    @Suppress("UNCHECKED_CAST")
    this.layers = layers as Array<Drawable?>
    for (i in this.layers.indices) {
      DrawableUtils.setCallbacks(this.layers[i], this, this)
    }
    drawableParents = arrayOfNulls(this.layers.size)
  }

  /**
   * Gets the drawable at the specified index.
   *
   * @param index index of drawable to get
   * @return drawable at the specified index
   */
  fun getDrawable(index: Int): Drawable? {
    require(index >= 0)
    require(index < layers.size)
    return layers[index]
  }

  /** Sets a new drawable at the specified index, and return the previous drawable, if any. */
  fun setDrawable(index: Int, drawable: Drawable?): Drawable? {
    require(index >= 0)
    require(index < layers.size)
    val oldDrawable = layers[index]
    if (drawable !== oldDrawable) {
      if (drawable != null && isMutated) {
        drawable.mutate()
      }

      DrawableUtils.setCallbacks(layers[index], null, null)
      DrawableUtils.setCallbacks(drawable, null, null)
      DrawableUtils.setDrawableProperties(drawable, drawableProperties)
      DrawableUtils.copyProperties(drawable, this)
      DrawableUtils.setCallbacks(drawable, this, this)
      isStatefulCalculated = false
      layers[index] = drawable
      invalidateSelf()
    }
    return oldDrawable
  }

  override fun getIntrinsicWidth(): Int {
    var width = -1
    for (i in layers.indices) {
      val drawable = layers[i]
      if (drawable != null) {
        width = max(width, drawable.intrinsicWidth)
      }
    }
    return if (width > 0) width else -1
  }

  override fun getIntrinsicHeight(): Int {
    var height = -1
    for (i in layers.indices) {
      val drawable = layers[i]
      if (drawable != null) {
        height = max(height, drawable.intrinsicHeight)
      }
    }
    return if (height > 0) height else -1
  }

  public override fun onBoundsChange(bounds: Rect) {
    for (i in layers.indices) {
      val drawable = layers[i]
      if (drawable != null) {
        drawable.bounds = bounds
      }
    }
  }

  override fun isStateful(): Boolean {
    if (!isStatefulCalculated) {
      _isStateful = false
      for (i in layers.indices) {
        val drawable = layers[i]
        this._isStateful = this._isStateful or (drawable?.isStateful == true)
      }
      isStatefulCalculated = true
    }
    return _isStateful
  }

  override fun onStateChange(state: IntArray): Boolean {
    var stateChanged = false
    for (i in layers.indices) {
      val drawable = layers[i]
      if (drawable?.setState(state) == true) {
        stateChanged = true
      }
    }
    return stateChanged
  }

  override fun onLevelChange(level: Int): Boolean {
    var levelChanged = false
    for (i in layers.indices) {
      val drawable = layers[i]
      if (drawable?.setLevel(level) == true) {
        levelChanged = true
      }
    }
    return levelChanged
  }

  override fun draw(canvas: Canvas) {
    for (i in layers.indices) {
      val drawable = layers[i]
      drawable?.draw(canvas)
    }
  }

  override fun getPadding(padding: Rect): Boolean {
    padding.left = 0
    padding.top = 0
    padding.right = 0
    padding.bottom = 0
    val rect = tmpRect
    for (i in layers.indices) {
      val drawable = layers[i]
      if (drawable != null) {
        drawable.getPadding(rect)
        padding.left = max(padding.left, rect.left)
        padding.top = max(padding.top, rect.top)
        padding.right = max(padding.right, rect.right)
        padding.bottom = max(padding.bottom, rect.bottom)
      }
    }
    return true
  }

  override fun mutate(): Drawable {
    for (i in layers.indices) {
      val drawable = layers[i]
      drawable?.mutate()
    }
    isMutated = true
    return this
  }

  override fun getOpacity(): Int {
    if (layers.size == 0) {
      return PixelFormat.TRANSPARENT
    }
    var opacity = PixelFormat.OPAQUE
    for (i in 1..<layers.size) {
      val drawable = layers[i]
      if (drawable != null) {
        opacity = Drawable.resolveOpacity(opacity, drawable.opacity)
      }
    }
    return opacity
  }

  override fun setAlpha(alpha: Int) {
    drawableProperties.setAlpha(alpha)
    for (i in layers.indices) {
      val drawable = layers[i]
      if (drawable != null) {
        drawable.alpha = alpha
      }
    }
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    drawableProperties.setColorFilter(colorFilter)
    for (i in layers.indices) {
      val drawable = layers[i]
      if (drawable != null) {
        drawable.colorFilter = colorFilter
      }
    }
  }

  override fun setDither(dither: Boolean) {
    drawableProperties.setDither(dither)
    for (i in layers.indices) {
      val drawable = layers[i]
      drawable?.setDither(dither)
    }
  }

  override fun setFilterBitmap(filterBitmap: Boolean) {
    drawableProperties.setFilterBitmap(filterBitmap)
    for (i in layers.indices) {
      val drawable = layers[i]
      if (drawable != null) {
        drawable.isFilterBitmap = filterBitmap
      }
    }
  }

  override fun setVisible(visible: Boolean, restart: Boolean): Boolean {
    val changed = super.setVisible(visible, restart)
    for (i in layers.indices) {
      val drawable = layers[i]
      drawable?.setVisible(visible, restart)
    }
    return changed
  }

  /** Gets the `DrawableParent` for index. */
  fun getDrawableParentForIndex(index: Int): DrawableParent? {
    require(index >= 0)
    require(index < drawableParents.size)
    if (drawableParents[index] == null) {
      drawableParents[index] = createDrawableParentForIndex(index)
    }
    return drawableParents[index]
  }

  private fun createDrawableParentForIndex(index: Int): DrawableParent {
    return object : DrawableParent {
      override fun setDrawable(newDrawable: Drawable?): Drawable? =
          this@ArrayDrawable.setDrawable(index, newDrawable)

      override val drawable: Drawable?
        get() = this@ArrayDrawable.getDrawable(index)
    }
  }

  /** Drawable.Callback methods */
  override fun invalidateDrawable(who: Drawable) {
    invalidateSelf()
  }

  override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
    scheduleSelf(what, `when`)
  }

  override fun unscheduleDrawable(who: Drawable, what: Runnable) {
    unscheduleSelf(what)
  }

  /** TransformationCallbackSetter method */
  override fun setTransformCallback(transformCallback: TransformCallback?) {
    this.transformCallback = transformCallback
  }

  /** TransformationCallback methods */
  override fun getTransform(transform: Matrix) {
    if (transformCallback != null) {
      transformCallback!!.getTransform(transform)
    } else {
      transform.reset()
    }
  }

  override fun getRootBounds(bounds: RectF) {
    if (transformCallback != null) {
      transformCallback!!.getRootBounds(bounds)
    } else {
      bounds.set(getBounds())
    }
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  override fun setHotspot(x: Float, y: Float) {
    for (i in layers.indices) {
      val drawable = layers[i]
      drawable?.setHotspot(x, y)
    }
  }
}
