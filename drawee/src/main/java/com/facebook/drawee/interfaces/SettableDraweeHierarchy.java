/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.interfaces;

import android.graphics.drawable.Drawable;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Interface that represents a settable Drawee hierarchy. Hierarchy should display a placeholder
 * image until the actual image is set. In case of a failure, hierarchy can choose to display
 * a failure image.
 *
 * <p>IMPORTANT: methods of this interface are to be used by controllers ONLY!
 *
 * <p>
 * Example hierarchy:
 *
 *   o FadeDrawable (top level drawable)
 *   |
 *   +--o ScaleTypeDrawable
 *   |  |
 *   |  +--o ColorDrawable (placeholder image)
 *   |
 *   +--o ScaleTypeDrawable
 *   |  |
 *   |  +--o BitmapDrawable (failure image)
 *   |
 *   +--o ScaleTypeDrawable
 *      |
 *      +--o SettableDrawable
 *         |
 *         +--o BitmapDrawable (actual image)
 *
 *   SettableDraweeHierarchy in the given example has a FadeDrawable as its top level drawable.
 *   Top level drawable can be immediately put into view. Once the actual image is ready, it will
 *   be set to the hierarchy's SettableDrawable and fade animation between the placeholder and the
 *   actual image will be initiated. In case of failure, hierarchy will switch to failure image.
 *   All image branches are wrapped with ScaleType drawable which allows separate scale type to be
 *   applied on each.
 *
 */
@ThreadSafe
public interface SettableDraweeHierarchy extends DraweeHierarchy {

  /**
   * Called by controller when the hierarchy should be reset to its initial state. Any image
   * previously set by {@code setImage} should be detached and not used anymore.
   */
  void reset();

  /**
   * Called by controller when the future that provides the actual image completes successfully.
   * Hierarchy should display the actual image.
   * @param drawable drawable to be set as the temporary image
   * @param progress number in range [0, 1] that indicates progress
   * @param immediate if true, image will be shown immediately (without fade effect)
   */
  void setImage(Drawable drawable, float progress, boolean immediate);

  /**
   * Called by controller to update the progress.
   * Hierarchy can choose to hide the progressbar when progress is set to its final value of 1.
   * @param progress number in range [0, 1] that indicates progress
   * @param immediate if true, progressbar will be shown/hidden immediately (without fade effect)
   */
  void setProgress(float progress, boolean immediate);

  /**
   * Called by controller when the future that provides the actual image completes with failure.
   * Hierarchy can choose to display between different images based on cause of failure.
   * @param throwable cause of failure
   */
  void setFailure(Throwable throwable);

  /**
   * Called by controller when the future that provides the actual image completes with failure,
   * but the controller is prepared to kick off a retry when the user clicks on the image.
   * Hierarchy can choose to display a retry image.
   * @param throwable cause of failure
   */
  void setRetry(Throwable throwable);

  /**
   * Called by controller if it needs to display some controller overlay.
   * @param drawable drawable to be displayed as controller overlay
   */
  void setControllerOverlay(Drawable drawable);
}
