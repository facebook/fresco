/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.media.ExifInterface
import androidx.annotation.VisibleForTesting
import kotlin.jvm.JvmField

/**
 * Drawable that automatically rotates the underlying drawable with a pivot in the center of the
 * drawable bounds based on a rotation angle.
 */
class OrientedDrawable
@JvmOverloads
constructor(
    drawable: Drawable?,
    rotationAngle: Int,
    exifOrientation: Int = ExifInterface.ORIENTATION_UNDEFINED
) : ForwardingDrawable(drawable) {

  @JvmField @VisibleForTesting val mRotationMatrix: Matrix = Matrix()
  private val rotationAngle = rotationAngle - rotationAngle % 90
  private val exifOrientation =
      if (exifOrientation >= 0 && exifOrientation <= 8) exifOrientation
      else ExifInterface.ORIENTATION_UNDEFINED

  // Temporary objects preallocated in advance to save future allocations.
  private val tempMatrix = Matrix()
  private val tempRectF = RectF()

  /**
   * Creates a new OrientedDrawable.
   *
   * @param rotationAngle multiples of 90. Invalid value is clamped to a closest multiple of 90.
   * @param exifOrientation EXIF values (1-8), or 0 if unknown. Invalid value is replaced with 0.
   */
  override fun draw(canvas: Canvas) {
    if (rotationAngle <= 0 &&
        (exifOrientation == ExifInterface.ORIENTATION_UNDEFINED ||
            exifOrientation == ExifInterface.ORIENTATION_NORMAL)) {
      super.draw(canvas)
      return
    }
    val saveCount = canvas.save()
    canvas.concat(mRotationMatrix)
    super.draw(canvas)
    canvas.restoreToCount(saveCount)
  }

  override fun getIntrinsicWidth(): Int =
      if (exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE ||
          exifOrientation == ExifInterface.ORIENTATION_TRANSVERSE ||
          rotationAngle % 180 != 0) {
        super.getIntrinsicHeight()
      } else {
        super.getIntrinsicWidth()
      }

  override fun getIntrinsicHeight(): Int =
      if (exifOrientation == ExifInterface.ORIENTATION_TRANSPOSE ||
          exifOrientation == ExifInterface.ORIENTATION_TRANSVERSE ||
          rotationAngle % 180 != 0) {
        super.getIntrinsicWidth()
      } else {
        super.getIntrinsicHeight()
      }

  override fun onBoundsChange(bounds: Rect) {
    val underlyingDrawable = current ?: return

    if (rotationAngle > 0 ||
        (exifOrientation != ExifInterface.ORIENTATION_UNDEFINED &&
            exifOrientation != ExifInterface.ORIENTATION_NORMAL)) {
      when (exifOrientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> mRotationMatrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> mRotationMatrix.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
          mRotationMatrix.setRotate(270f, bounds.centerX().toFloat(), bounds.centerY().toFloat())
          mRotationMatrix.postScale(1f, -1f)
        }

        ExifInterface.ORIENTATION_TRANSVERSE -> {
          mRotationMatrix.setRotate(270f, bounds.centerX().toFloat(), bounds.centerY().toFloat())
          mRotationMatrix.postScale(-1f, 1f)
        }

        else ->
            mRotationMatrix.setRotate(
                rotationAngle.toFloat(), bounds.centerX().toFloat(), bounds.centerY().toFloat())
      }

      // Set the rotated bounds on the underlying drawable
      tempMatrix.reset()
      mRotationMatrix.invert(tempMatrix)
      tempRectF.set(bounds)
      tempMatrix.mapRect(tempRectF)
      underlyingDrawable.setBounds(
          tempRectF.left.toInt(),
          tempRectF.top.toInt(),
          tempRectF.right.toInt(),
          tempRectF.bottom.toInt())
    } else {
      underlyingDrawable.bounds = bounds
    }
  }

  override fun getTransform(transform: Matrix) {
    getParentTransform(transform)
    if (!mRotationMatrix.isIdentity) {
      transform.preConcat(mRotationMatrix)
    }
  }
}
