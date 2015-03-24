/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import android.graphics.Matrix;
import android.graphics.Rect;

/**
 * Performs scale type calculations.
 */
public class ScalingUtils {

  /**
   * Options for scaling the child bounds to the parent bounds.
   * <p>
   * Similar to {@link android.widget.ImageView.ScaleType}, but ScaleType.MATRIX is not supported.
   * To use matrix scaling, use a {@link MatrixDrawable}. An additional scale type (FOCUS_CROP) is
   * provided.
   */
  public enum ScaleType {

    /**
     * Scales width and height independently, so that the child matches the parent exactly.
     * This may change the aspect ratio of the child.
     */
    FIT_XY,

    /**
     * Scales the child so that it fits entirely inside the parent. At least one dimension (width or
     * height) will fit exactly. Aspect ratio is preserved.
     * Child is aligned to the top-left corner of the parent.
     */
    FIT_START,

    /**
     * Scales the child so that it fits entirely inside the parent. At least one dimension (width or
     * height) will fit exactly. Aspect ratio is preserved.
     * Child is centered within the parent's bounds.
     */
    FIT_CENTER,

    /**
     * Scales the child so that it fits entirely inside the parent. At least one dimension (width or
     * height) will fit exactly. Aspect ratio is preserved.
     * Child is aligned to the bottom-right corner of the parent.
     */
    FIT_END,

    /**
     * Performs no scaling.
     * Child is centered within parent's bounds.
     */
    CENTER,

    /**
     * Scales the child so that it fits entirely inside the parent. Unlike FIT_CENTER, if the child
     * is smaller, no up-scaling will be performed. Aspect ratio is preserved.
     * Child is centered within parent's bounds.
     */
    CENTER_INSIDE,

    /**
     * Scales the child so that both dimensions will be greater than or equal to the corresponding
     * dimension of the parent. At least one dimension (width or height) will fit exactly.
     * Child is centered within parent's bounds.
     */
    CENTER_CROP,

    /**
     * Scales the child so that both dimensions will be greater than or equal to the corresponding
     * dimension of the parent. At least one dimension (width or height) will fit exactly.
     * The child's focus point will be centered within the parent's bounds as much as possible
     * without leaving empty space.
     * It is guaranteed that the focus point will be visible and centered as much as possible.
     * If the focus point is set to (0.5f, 0.5f), result will be equivalent to CENTER_CROP.
     */
    FOCUS_CROP;

    /**
     * Gets the scale type out of string.
     *
     * <p> Used by GenericDraweeView styleable in
     * android_res/com/facebook/custom/res/values/attrs.xml
     *
     * @param value string value to parse
     * @return scale type if recognized
     * @throws IllegalArgumentException if scale type is not recognized
     */
    public static ScaleType fromString(String value) {
      if (value.equals("none")) {
        return null;
      } else if (value.equals("fitXY")) {
        return ScaleType.FIT_XY;
      } else if (value.equals("fitStart")) {
        return ScaleType.FIT_START;
      } else if (value.equals("fitCenter")) {
        return FIT_CENTER;
      } else if (value.equals("fitEnd")) {
        return FIT_END;
      } else if (value.equals("center")) {
        return CENTER;
      } else if (value.equals("centerInside")) {
        return CENTER_INSIDE;
      } else if (value.equals("centerCrop")) {
        return CENTER_CROP;
      } else if (value.equals("focusCrop")) {
        return FOCUS_CROP;
      } else {
        throw new IllegalArgumentException(
            "Unrecognized scale type: " + value +
                "; use a value defined in the ScalingUtils.fromString method");
      }
    }
  }

  /**
   * Gets transformation based on the scale type.
   * @param transform out matrix to store result
   * @param parentBounds parent bounds
   * @param childWidth child width
   * @param childHeight child height
   * @param focusX focus point x coordinate, relative [0...1]
   * @param focusY focus point y coordinate, relative [0...1]
   * @param scaleType scale type to be used
   * @return reference to the out matrix
   */
  public static Matrix getTransform(
      final Matrix transform,
      final Rect parentBounds,
      final int childWidth,
      final int childHeight,
      final float focusX,
      final float focusY,
      final ScaleType scaleType) {

    final int parentWidth = parentBounds.width();
    final int parentHeight = parentBounds.height();

    final float scaleX = (float) parentWidth / (float) childWidth;
    final float scaleY = (float) parentHeight / (float) childHeight;

    float scale = 1.0f;
    float dx = 0;
    float dy = 0;


    switch (scaleType) {
      case FIT_XY:
        dx = parentBounds.left;
        dy = parentBounds.top;
        transform.setScale(scaleX, scaleY);
        transform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        break;

      case FIT_START:
        scale = Math.min(scaleX, scaleY);
        dx = parentBounds.left;
        dy = parentBounds.top;
        transform.setScale(scale, scale);
        transform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        break;

      case FIT_CENTER:
        scale = Math.min(scaleX, scaleY);
        dx = parentBounds.left + (parentWidth - childWidth * scale) * 0.5f;
        dy = parentBounds.top + (parentHeight - childHeight * scale) * 0.5f;
        transform.setScale(scale, scale);
        transform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        break;

      case FIT_END:
        scale = Math.min(scaleX, scaleY);
        dx = parentBounds.left + (parentWidth - childWidth * scale);
        dy = parentBounds.top + (parentHeight - childHeight * scale);
        transform.setScale(scale, scale);
        transform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        break;

      case CENTER:
        dx = parentBounds.left + (parentWidth - childWidth) * 0.5f;
        dy = parentBounds.top + (parentHeight - childHeight) * 0.5f;
        transform.setTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        break;

      case CENTER_INSIDE:
        scale = Math.min(Math.min(scaleX, scaleY), 1.0f);
        dx = parentBounds.left + (parentWidth - childWidth * scale) * 0.5f;
        dy = parentBounds.top + (parentHeight - childHeight * scale) * 0.5f;
        transform.setScale(scale, scale);
        transform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        break;

      case CENTER_CROP:
        if (scaleY > scaleX) {
          scale = scaleY;
          dx = parentBounds.left + (parentWidth - childWidth * scale) * 0.5f;
          dy = parentBounds.top;
        } else {
          scale = scaleX;
          dx = parentBounds.left;
          dy = parentBounds.top + (parentHeight - childHeight * scale) * 0.5f;
        }
        transform.setScale(scale, scale);
        transform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        break;

      case FOCUS_CROP:
        if (scaleY > scaleX) {
          scale = scaleY;
          dx = parentWidth * 0.5f - childWidth * scale * focusX;
          dx = parentBounds.left + Math.max(Math.min(dx, 0), parentWidth - childWidth * scale);
          dy = parentBounds.top;
        } else {
          scale = scaleX;
          dx = parentBounds.left;
          dy = parentHeight * 0.5f - childHeight * scale * focusY;
          dy = parentBounds.top + Math.max(Math.min(dy, 0), parentHeight - childHeight * scale);
        }
        transform.setScale(scale, scale);
        transform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        break;

      default:
        throw new UnsupportedOperationException("Unsupported scale type: " + scaleType);
    }

    return transform;
  }
}
