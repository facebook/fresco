/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import com.facebook.imagepipeline.systrace.FrescoSystrace
import java.util.Arrays
import kotlin.math.min

abstract class RoundedDrawable
/** Constructs a new forwarding drawable. */
internal constructor(private val delegate: Drawable) : Drawable(), Rounded, TransformAwareDrawable {

  @JvmField internal var mIsCircle: Boolean = false

  @JvmField protected var mRadiiNonZero: Boolean = false

  /** Gets the border width. */
  override var borderWidth: Float = 0f
    protected set

  @JvmField internal val mPath: Path = Path()

  @JvmField internal var mIsShaderTransformDirty: Boolean = true

  /** Gets the border color. */
  override var borderColor: Int = Color.TRANSPARENT
    protected set

  @JvmField internal val mBorderPath: Path = Path()

  private val cornerRadii = FloatArray(8)

  @JvmField @VisibleForTesting val mBorderRadii: FloatArray = FloatArray(8)

  @JvmField @VisibleForTesting var mInsideBorderRadii: FloatArray? = null

  @JvmField @VisibleForTesting val mRootBounds: RectF = RectF()

  @JvmField @VisibleForTesting val mPrevRootBounds: RectF = RectF()

  @JvmField @VisibleForTesting val mBitmapBounds: RectF = RectF()

  @JvmField @VisibleForTesting val mDrawableBounds: RectF = RectF()

  @JvmField @VisibleForTesting var mInsideBorderBounds: RectF? = null

  @JvmField @VisibleForTesting val mBoundsTransform: Matrix = Matrix()

  @JvmField @VisibleForTesting val mPrevBoundsTransform: Matrix = Matrix()

  @JvmField @VisibleForTesting val mParentTransform: Matrix = Matrix()

  @JvmField @VisibleForTesting val mPrevParentTransform: Matrix = Matrix()

  @JvmField @VisibleForTesting val mInverseParentTransform: Matrix = Matrix()

  @JvmField @VisibleForTesting var mInsideBorderTransform: Matrix? = null

  @JvmField @VisibleForTesting var mPrevInsideBorderTransform: Matrix? = null

  @JvmField @VisibleForTesting val mTransform: Matrix = Matrix()
  private var _padding = 0f
  private var _scaleDownInsideBorders = false
  private var _paintFilterBitmap = false

  private var isPathDirty = true

  private var transformCallback: TransformCallback? = null

  override var isCircle: Boolean
    /** Returns whether or not this drawable rounds as circle. */
    get() = mIsCircle
    /**
     * Sets whether to round as circle.
     *
     * @param isCircle whether or not to round as circle
     */
    set(isCircle) {
      this.mIsCircle = isCircle
      isPathDirty = true
      invalidateSelf()
    }

  /**
   * Specify radius for the corners of the rectangle. If this is > 0, then the drawable is drawn in
   * a round-rectangle, rather than a rectangle.
   *
   * @param radius the radius for the corners of the rectangle
   */
  override fun setRadius(radius: Float) {
    check(radius >= 0)
    Arrays.fill(cornerRadii, radius)
    mRadiiNonZero = (radius != 0f)
    isPathDirty = true
    invalidateSelf()
  }

  override var radii: FloatArray?
    /** Gets the radii. */
    get() = cornerRadii
    /**
     * Specify radii for each of the 4 corners. For each corner, the array contains 2 values,
     * [X_radius, Y_radius]. The corners are ordered top-left, top-right, bottom-right, bottom-left
     *
     * @param radii the x and y radii of the corners
     */
    set(radii) {
      if (radii == null) {
        Arrays.fill(cornerRadii, 0f)
        mRadiiNonZero = false
      } else {
        require(radii.size == 8) { "radii should have exactly 8 values" }
        System.arraycopy(radii, 0, cornerRadii, 0, 8)
        mRadiiNonZero = false
        for (i in 0..7) {
          mRadiiNonZero = mRadiiNonZero or (radii[i] > 0)
        }
      }
      isPathDirty = true
      invalidateSelf()
    }

  /**
   * Sets the border
   *
   * @param color of the border
   * @param width of the border
   */
  override fun setBorder(color: Int, width: Float) {
    if (borderColor != color || borderWidth != width) {
      borderColor = color
      borderWidth = width
      isPathDirty = true
      invalidateSelf()
    }
  }

  override var padding: Float
    /** Gets the padding. */
    get() = _padding
    /**
     * Sets the padding for the bitmap.
     *
     * @param padding
     */
    set(padding) {
      if (this._padding != padding) {
        this._padding = padding
        isPathDirty = true
        invalidateSelf()
      }
    }

  override var scaleDownInsideBorders: Boolean
    /** Gets whether image should be scaled down inside borders. */
    get() = _scaleDownInsideBorders
    /**
     * Sets whether image should be scaled down inside borders.
     *
     * @param scaleDownInsideBorders
     */
    set(scaleDownInsideBorders) {
      if (this._scaleDownInsideBorders != scaleDownInsideBorders) {
        this._scaleDownInsideBorders = scaleDownInsideBorders
        isPathDirty = true
        invalidateSelf()
      }
    }

  override var paintFilterBitmap: Boolean
    /** Gets whether to set FILTER_BITMAP_FLAG flag to Paint. */
    get() = _paintFilterBitmap
    /**
     * Sets FILTER_BITMAP_FLAG flag to Paint. [android.graphics.Paint#FILTER_BITMAP_FLAG]
     *
     * This should generally be on when drawing bitmaps, unless performance-bound (rendering to
     * software canvas) or preferring pixelation artifacts to blurriness when scaling significantly.
     *
     * @param paintFilterBitmap whether to set FILTER_BITMAP_FLAG flag to Paint.
     */
    set(paintFilterBitmap) {
      if (this._paintFilterBitmap != paintFilterBitmap) {
        this._paintFilterBitmap = paintFilterBitmap
        invalidateSelf()
      }
    }

  /** TransformAwareDrawable method */
  override fun setTransformCallback(transformCallback: TransformCallback?) {
    this.transformCallback = transformCallback
  }

  internal open fun updateTransform() {
    if (transformCallback != null) {
      transformCallback!!.getTransform(mParentTransform)
      transformCallback!!.getRootBounds(mRootBounds)
    } else {
      mParentTransform.reset()
      mRootBounds.set(bounds)
    }

    mBitmapBounds[0f, 0f, intrinsicWidth.toFloat()] = intrinsicHeight.toFloat()
    mDrawableBounds.set(delegate.bounds)
    mBoundsTransform.setRectToRect(mBitmapBounds, mDrawableBounds, Matrix.ScaleToFit.FILL)
    if (_scaleDownInsideBorders) {
      if (mInsideBorderBounds == null) {
        mInsideBorderBounds = RectF(mRootBounds)
      } else {
        mInsideBorderBounds!!.set(mRootBounds)
      }
      mInsideBorderBounds!!.inset(borderWidth, borderWidth)
      if (mInsideBorderTransform == null) {
        mInsideBorderTransform = Matrix()
      }
      mInsideBorderTransform!!.setRectToRect(
          mRootBounds,
          mInsideBorderBounds,
          Matrix.ScaleToFit.FILL,
      )
    } else mInsideBorderTransform?.reset()

    if (
        (mParentTransform != mPrevParentTransform) ||
            (mBoundsTransform != mPrevBoundsTransform) ||
            (mInsideBorderTransform != null &&
                !matrixEquals(mInsideBorderTransform, mPrevInsideBorderTransform))
    ) {
      mIsShaderTransformDirty = true

      mParentTransform.invert(mInverseParentTransform)
      mTransform.set(mParentTransform)
      if (_scaleDownInsideBorders && mInsideBorderTransform != null) {
        mTransform.postConcat(mInsideBorderTransform)
      }
      mTransform.preConcat(mBoundsTransform)

      mPrevParentTransform.set(mParentTransform)
      mPrevBoundsTransform.set(mBoundsTransform)
      if (_scaleDownInsideBorders) {
        if (mPrevInsideBorderTransform == null) {
          mPrevInsideBorderTransform = deepCopyMatrix(mInsideBorderTransform)
        } else {
          mPrevInsideBorderTransform!!.set(mInsideBorderTransform)
        }
      } else mPrevInsideBorderTransform?.reset()
    }

    if (mRootBounds != mPrevRootBounds) {
      isPathDirty = true
      mPrevRootBounds.set(mRootBounds)
    }
  }

  internal fun updatePath() {
    if (isPathDirty) {
      mBorderPath.reset()
      mRootBounds.inset(borderWidth / 2, borderWidth / 2)
      if (mIsCircle) {
        val radius = min(mRootBounds.width(), mRootBounds.height()) / 2
        mBorderPath.addCircle(
            mRootBounds.centerX(),
            mRootBounds.centerY(),
            radius,
            Path.Direction.CW,
        )
      } else {
        for (i in mBorderRadii.indices) {
          mBorderRadii[i] = cornerRadii[i] + _padding - borderWidth / 2
        }
        mBorderPath.addRoundRect(mRootBounds, mBorderRadii, Path.Direction.CW)
      }
      mRootBounds.inset(-borderWidth / 2, -borderWidth / 2)

      mPath.reset()
      val totalPadding = _padding + (if (_scaleDownInsideBorders) borderWidth else 0f)
      mRootBounds.inset(totalPadding, totalPadding)
      if (mIsCircle) {
        mPath.addCircle(
            mRootBounds.centerX(),
            mRootBounds.centerY(),
            min(mRootBounds.width(), mRootBounds.height()) / 2,
            Path.Direction.CW,
        )
      } else if (_scaleDownInsideBorders) {
        if (mInsideBorderRadii == null) {
          mInsideBorderRadii = FloatArray(8)
        }
        for (i in mBorderRadii.indices) {
          mInsideBorderRadii!![i] = cornerRadii[i] - borderWidth
        }
        mPath.addRoundRect(mRootBounds, mInsideBorderRadii!!, Path.Direction.CW)
      } else {
        mPath.addRoundRect(mRootBounds, cornerRadii, Path.Direction.CW)
      }
      mRootBounds.inset(-(totalPadding), -(totalPadding))
      mPath.fillType = Path.FillType.WINDING
      isPathDirty = false
    }
  }

  /** If both the radii and border width are zero, there is nothing to round. */
  @VisibleForTesting open fun shouldRound(): Boolean = mIsCircle || mRadiiNonZero || borderWidth > 0

  override fun onBoundsChange(bounds: Rect) {
    delegate.bounds = bounds
  }

  override fun getIntrinsicWidth(): Int = delegate.intrinsicWidth

  override fun getIntrinsicHeight(): Int = delegate.intrinsicHeight

  override fun getOpacity(): Int = delegate.opacity

  override fun setColorFilter(color: Int, mode: PorterDuff.Mode) {
    delegate.setColorFilter(color, mode)
  }

  override fun setColorFilter(colorFilter: ColorFilter?) {
    delegate.colorFilter = colorFilter
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  override fun getColorFilter(): ColorFilter? = delegate.colorFilter

  override fun clearColorFilter() {
    delegate.clearColorFilter()
  }

  @RequiresApi(api = Build.VERSION_CODES.KITKAT) override fun getAlpha(): Int = delegate.alpha

  override fun setAlpha(alpha: Int) {
    delegate.alpha = alpha
  }

  override fun draw(canvas: Canvas) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("RoundedDrawable#draw")
    }
    delegate.draw(canvas)
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection()
    }
  }

  override fun setRepeatEdgePixels(repeatEdgePixels: Boolean) {
    // no-op
  }

  companion object {
    /** Nullsafe Matrix equality check. This is needed because Matrix.equals() is not nullsafe. */
    private fun matrixEquals(a: Matrix?, b: Matrix?): Boolean {
      if (a == null && b == null) {
        return true
      }
      if (a == null || b == null) {
        return false
      }

      return a == b
    }

    /** Nullsafe Matrix deep copy. */
    private fun deepCopyMatrix(matrix: Matrix?): Matrix? =
        if (matrix == null) {
          null
        } else {
          Matrix(matrix)
        }
  }
}
