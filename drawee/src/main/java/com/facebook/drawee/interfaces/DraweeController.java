/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.interfaces;

import android.graphics.drawable.Animatable;
import android.view.MotionEvent;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Interface that represents a Drawee controller used by a DraweeView.
 * <p> The view forwards events to the controller. The controller controls
 * its hierarchy based on those events.
 */
@ThreadSafe
public interface DraweeController {

  /** Gets the hierarchy. */
  @Nullable
  DraweeHierarchy getHierarchy();

  /** Sets a new hierarchy. */
  void setHierarchy(@Nullable DraweeHierarchy hierarchy);

  /**
   * Called when the view containing the hierarchy is attached to a window
   * (either temporarily or permanently).
   */
  void onAttach();

  /**
   * Called when the view containing the hierarchy is detached from a window
   * (either temporarily or permanently).
   */
  void onDetach();

  /**
   * An optional hint whether the view containing the hierarchy is currently within the visible
   * viewport or not.
   */
  void onViewportVisibilityHint(boolean isVisibleInViewportHint);

  /**
   * Called when the view containing the hierarchy receives a touch event.
   * @return true if the event was handled by the controller, false otherwise
   */
  boolean onTouchEvent(MotionEvent event);

  /**
   * For an animated image, returns an Animatable that lets clients control the animation.
   * @return animatable, or null if the image is not animated or not loaded yet
   */
  Animatable getAnimatable();

  /** Sets the accessibility content description. */
  void setContentDescription(String contentDescription);

  /**
   * Gets the accessibility content description.
   * @return content description, or null if the image has no content description
   */
  String getContentDescription();

  /** Returns whether {@code other} would fetch the same image as {@code this}. */
  boolean isSameImageRequest(DraweeController other);
}
