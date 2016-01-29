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
   * <p>
   */
  public interface ScaleType {

    /**
     * Scales width and height independently, so that the child matches the parent exactly.
     * This may change the aspect ratio of the child.
     */
    static final ScaleType FIT_XY = ScaleTypeFitXY.INSTANCE;

    /**
     * Scales the child so that it fits entirely inside the parent. At least one dimension (width or
     * height) will fit exactly. Aspect ratio is preserved.
     * Child is aligned to the top-left corner of the parent.
     */
    static final ScaleType FIT_START = ScaleTypeFitStart.INSTANCE;

    /**
     * Scales the child so that it fits entirely inside the parent. At least one dimension (width or
     * height) will fit exactly. Aspect ratio is preserved.
     * Child is centered within the parent's bounds.
     */
    static final ScaleType FIT_CENTER = ScaleTypeFitCenter.INSTANCE;

    /**
     * Scales the child so that it fits entirely inside the parent. At least one dimension (width or
     * height) will fit exactly. Aspect ratio is preserved.
     * Child is aligned to the bottom-right corner of the parent.
     */
    static final ScaleType FIT_END = ScaleTypeFitEnd.INSTANCE;

    /**
     * Performs no scaling.
     * Child is centered within parent's bounds.
     */
    static final ScaleType CENTER = ScaleTypeCenter.INSTANCE;

    /**
     * Scales the child so that it fits entirely inside the parent. Unlike FIT_CENTER, if the child
     * is smaller, no up-scaling will be performed. Aspect ratio is preserved.
     * Child is centered within parent's bounds.
     */
    static final ScaleType CENTER_INSIDE = ScaleTypeCenterInside.INSTANCE;

    /**
     * Scales the child so that both dimensions will be greater than or equal to the corresponding
     * dimension of the parent. At least one dimension (width or height) will fit exactly.
     * Child is centered within parent's bounds.
     */
    static final ScaleType CENTER_CROP = ScaleTypeCenterCrop.INSTANCE;

    /**
     * Scales the child so that both dimensions will be greater than or equal to the corresponding
     * dimension of the parent. At least one dimension (width or height) will fit exactly.
     * The child's focus point will be centered within the parent's bounds as much as possible
     * without leaving empty space.
     * It is guaranteed that the focus point will be visible and centered as much as possible.
     * If the focus point is set to (0.5f, 0.5f), result will be equivalent to CENTER_CROP.
     */
    static final ScaleType FOCUS_CROP = ScaleTypeFocusCrop.INSTANCE;

    /**
     * Gets transformation matrix based on the scale type.
     * @param outTransform out matrix to store result
     * @param parentBounds parent bounds
     * @param childWidth child width
     * @param childHeight child height
     * @param focusX focus point x coordinate, relative [0...1]
     * @param focusY focus point y coordinate, relative [0...1]
     * @return same reference to the out matrix for convenience
     */
    Matrix getTransform(
        Matrix outTransform,
        Rect parentBounds,
        int childWidth,
        int childHeight,
        float focusX,
        float focusY);
  }

  /**
   * A convenience base class that has some common logic.
   */
  public static abstract class AbstractScaleType implements ScaleType {

    @Override
    public Matrix getTransform(
        Matrix outTransform,
        Rect parentRect,
        int childWidth,
        int childHeight,
        float focusX,
        float focusY) {
      final float sX = (float) parentRect.width() / (float) childWidth;
      final float sY = (float) parentRect.height() / (float) childHeight;
      getTransformImpl(outTransform, parentRect, childWidth, childHeight, focusX, focusY, sX, sY);
      return outTransform;
    }

    public abstract void getTransformImpl(
        Matrix outTransform,
        Rect parentRect,
        int childWidth,
        int childHeight,
        float focusX,
        float focusY,
        float scaleX,
        float scaleY);
  }

  private static class ScaleTypeFitXY extends AbstractScaleType {
    public static final ScaleType INSTANCE = new ScaleTypeFitXY();
    @Override
    public void getTransformImpl(
        Matrix outTransform,
        Rect parentRect,
        int childWidth,
        int childHeight,
        float focusX,
        float focusY,
        float scaleX,
        float scaleY) {
      float dx = parentRect.left;
      float dy = parentRect.top;
      outTransform.setScale(scaleX, scaleY);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }
  }

  private static class ScaleTypeFitStart extends AbstractScaleType {
    public static final ScaleType INSTANCE = new ScaleTypeFitStart();
    @Override
    public void getTransformImpl(
        Matrix outTransform,
        Rect parentRect,
        int childWidth,
        int childHeight,
        float focusX,
        float focusY,
        float scaleX,
        float scaleY) {
      float scale = Math.min(scaleX, scaleY);
      float dx = parentRect.left;
      float dy = parentRect.top;
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }
  }

  private static class ScaleTypeFitCenter extends AbstractScaleType {

    public static final ScaleType INSTANCE = new ScaleTypeFitCenter();

    @Override
    public void getTransformImpl(
        Matrix outTransform,
        Rect parentRect,
        int childWidth,
        int childHeight,
        float focusX,
        float focusY,
        float scaleX,
        float scaleY) {
      float scale = Math.min(scaleX, scaleY);
      float dx = parentRect.left + (parentRect.width() - childWidth * scale) * 0.5f;
      float dy = parentRect.top + (parentRect.height() - childHeight * scale) * 0.5f;
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }
  }

  private static class ScaleTypeFitEnd extends AbstractScaleType {

    public static final ScaleType INSTANCE = new ScaleTypeFitEnd();

    @Override
    public void getTransformImpl(
        Matrix outTransform,
        Rect parentRect,
        int childWidth,
        int childHeight,
        float focusX,
        float focusY,
        float scaleX,
        float scaleY) {
      float scale = Math.min(scaleX, scaleY);
      float dx = parentRect.left + (parentRect.width() - childWidth * scale);
      float dy = parentRect.top + (parentRect.height() - childHeight * scale);
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }
  }

  private static class ScaleTypeCenter extends AbstractScaleType {

    public static final ScaleType INSTANCE = new ScaleTypeCenter();

    @Override
    public void getTransformImpl(
        Matrix outTransform,
        Rect parentRect,
        int childWidth,
        int childHeight,
        float focusX,
        float focusY,
        float scaleX,
        float scaleY) {
      float dx = parentRect.left + (parentRect.width() - childWidth) * 0.5f;
      float dy = parentRect.top + (parentRect.height() - childHeight) * 0.5f;
      outTransform.setTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }
  }

  private static class ScaleTypeCenterInside extends AbstractScaleType {

    public static final ScaleType INSTANCE = new ScaleTypeCenterInside();

    @Override
    public void getTransformImpl(
        Matrix outTransform,
        Rect parentRect,
        int childWidth,
        int childHeight,
        float focusX,
        float focusY,
        float scaleX,
        float scaleY) {
      float scale = Math.min(Math.min(scaleX, scaleY), 1.0f);
      float dx = parentRect.left + (parentRect.width() - childWidth * scale) * 0.5f;
      float dy = parentRect.top + (parentRect.height() - childHeight * scale) * 0.5f;
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }
  }

  private static class ScaleTypeCenterCrop extends AbstractScaleType {

    public static final ScaleType INSTANCE = new ScaleTypeCenterCrop();

    @Override
    public void getTransformImpl(
        Matrix outTransform,
        Rect parentRect,
        int childWidth,
        int childHeight,
        float focusX,
        float focusY,
        float scaleX,
        float scaleY) {
      float scale, dx, dy;
      if (scaleY > scaleX) {
        scale = scaleY;
        dx = parentRect.left + (parentRect.width() - childWidth * scale) * 0.5f;
        dy = parentRect.top;
      } else {
        scale = scaleX;
        dx = parentRect.left;
        dy = parentRect.top + (parentRect.height() - childHeight * scale) * 0.5f;
      }
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }
  }

  private static class ScaleTypeFocusCrop extends AbstractScaleType {

    public static final ScaleType INSTANCE = new ScaleTypeFocusCrop();

    @Override
    public void getTransformImpl(
        Matrix outTransform,
        Rect parentRect,
        int childWidth,
        int childHeight,
        float focusX,
        float focusY,
        float scaleX,
        float scaleY) {
      float scale, dx, dy;
      if (scaleY > scaleX) {
        scale = scaleY;
        dx = parentRect.width() * 0.5f - childWidth * scale * focusX;
        dx = parentRect.left + Math.max(Math.min(dx, 0), parentRect.width() - childWidth * scale);
        dy = parentRect.top;
      } else {
        scale = scaleX;
        dx = parentRect.left;
        dy = parentRect.height() * 0.5f - childHeight * scale * focusY;
        dy = parentRect.top + Math.max(Math.min(dy, 0), parentRect.height() - childHeight * scale);
      }
      outTransform.setScale(scale, scale);
      outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
    }
  }
}
