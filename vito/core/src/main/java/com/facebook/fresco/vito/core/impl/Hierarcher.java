/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.facebook.common.references.CloseableReference;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.infer.annotation.ThreadSafe;

/** Helper for building drawables */
@Nullsafe(Nullsafe.Mode.STRICT)
public interface Hierarcher {
  /**
   * Build an actual image drawable for the given closeable image. For scaling, color filters and
   * other transformation, the actual image wrapper is used. The drawable returned here does not
   * include scaling etc and should be combined with the actual image wrapper.
   *
   * <p>NOTE: the Drawable returned by this method will not hold on to the image reference. This
   * must be done separately.
   *
   * @param resources resources to be used to load the drawable
   * @param imageOptions image options to be used to create the drawable
   * @param closeableImage the decoded image to create the Drawable for
   * @return the actual image drawable or null if it cannot be rendered
   */
  @Nullable
  Drawable buildActualImageDrawable(
      Resources resources,
      ImageOptions imageOptions,
      CloseableReference<CloseableImage> closeableImage);

  /**
   * Build a placeholder drawable if specified in the given image options.
   *
   * @param resources resources to be used to load the drawable
   * @param imageOptions image options to be used to create the placeholder drawable
   * @return the placeholder drawable or NopDrawable.INSTANCE if unset.
   */
  @Nullable
  Drawable buildPlaceholderDrawable(Resources resources, ImageOptions imageOptions);

  /**
   * Build a progressbar drawable if specified in the given image options
   *
   * @param resources resources to be used to load the drawable
   * @param imageOptions image options to be used to create the progressbar drawable
   * @return the progressbar drawable or null if unset.
   */
  @Nullable
  Drawable buildProgressDrawable(Resources resources, ImageOptions imageOptions);

  /**
   * Build an error drawable if specified in the given image options.
   *
   * @param resources resources to be used to load the drawable
   * @param imageOptions image options to be used to create the error drawable
   * @return the error drawable or null if unset.
   */
  @Nullable
  Drawable buildErrorDrawable(Resources resources, ImageOptions imageOptions);

  /**
   * Build the actual image wrapper, a forwarding drawable that will forward to the actual image
   * drawable once set and can perform transformations, like scaling.
   *
   * @param imageOptions image options to be used to create the wrapper
   * @param callerContext the caller's context, may be null
   * @return the actual image wrapper drawable
   */
  @ThreadSafe
  ForwardingDrawable buildActualImageWrapper(
      ImageOptions imageOptions, @Nullable Object callerContext);

  /**
   * Builds the overlay drawable to be displayed for the given image options.
   *
   * @param resources resources to be used to load the drawable
   * @param imageOptions image options to be used to create the overlay
   * @return the overlay or null if not applicable
   */
  @Nullable
  Drawable buildOverlayDrawable(Resources resources, ImageOptions imageOptions);

  /**
   * Set up the actual image wrapper scale type Drawable.
   *
   * @param actualImageWrapper the wrapper to set up
   * @param imageOptions image options to be used
   * @param callerContext the caller's context, may be null
   */
  void setupActualImageWrapper(
      ScaleTypeDrawable actualImageWrapper,
      ImageOptions imageOptions,
      @Nullable Object callerContext);
}
