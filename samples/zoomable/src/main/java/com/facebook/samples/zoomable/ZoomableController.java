/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.zoomable;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.MotionEvent;

/**
 * Interface for implementing a controller that works with {@link ZoomableDraweeView} to control the
 * zoom.
 */
public interface ZoomableController {

  /** Listener interface. */
  interface Listener {

    /**
     * Notifies the view that the transform began.
     *
     * @param transform the current transform matrix
     */
    void onTransformBegin(Matrix transform);

    /**
     * Notifies the view that the transform changed.
     *
     * @param transform the new matrix
     */
    void onTransformChanged(Matrix transform);

    /**
     * Notifies the view that the transform ended.
     *
     * @param transform the current transform matrix
     */
    void onTransformEnd(Matrix transform);
  }

  /**
   * Enables the controller. The controller is enabled when the image has been loaded.
   *
   * @param enabled whether to enable the controller
   */
  void setEnabled(boolean enabled);

  /**
   * Gets whether the controller is enabled. This should return the last value passed to {@link
   * #setEnabled}.
   *
   * @return whether the controller is enabled.
   */
  boolean isEnabled();

  /**
   * Sets the listener for the controller to call back when the matrix changes.
   *
   * @param listener the listener
   */
  void setListener(Listener listener);

  /**
   * Gets the current scale factor. A convenience method for calculating the scale from the
   * transform.
   *
   * @return the current scale factor
   */
  float getScaleFactor();

  /** Returns true if the zoomable transform is identity matrix, and the controller is idle. */
  boolean isIdentity();

  /**
   * Returns true if the transform was corrected during the last update.
   *
   * <p>This mainly happens when a gesture would cause the image to get out of limits and the
   * transform gets corrected in order to prevent that.
   */
  boolean wasTransformCorrected();

  /** See {@link androidx.core.view.ScrollingView}. */
  int computeHorizontalScrollRange();

  int computeHorizontalScrollOffset();

  int computeHorizontalScrollExtent();

  int computeVerticalScrollRange();

  int computeVerticalScrollOffset();

  int computeVerticalScrollExtent();

  /**
   * Gets the current transform.
   *
   * @return the transform
   */
  Matrix getTransform();

  /**
   * Sets the bounds of the image post transform prior to application of the zoomable
   * transformation.
   *
   * @param imageBounds the bounds of the image
   */
  void setImageBounds(RectF imageBounds);

  /**
   * Sets the bounds of the view.
   *
   * @param viewBounds the bounds of the view
   */
  void setViewBounds(RectF viewBounds);

  /**
   * Allows the controller to handle a touch event.
   *
   * @param event the touch event
   * @return whether the controller handled the event
   */
  boolean onTouchEvent(MotionEvent event);
}
