/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.facebook.common.references.CloseableReference;
import com.facebook.drawee.drawable.ForwardingDrawable;
import com.facebook.drawee.drawable.InstrumentedDrawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.infer.annotation.ThreadSafe;

/** Helper for building drawables */
public interface Hierarcher {

  /**
   * Build a placeholder drawable if specified in the given image options.
   *
   * @param resources resources to be used to load the drawable
   * @param imageOptions image options to be used to create the placeholder drawable
   * @return the placeholder drawable or NopDrawable.INSTANCE if unset.
   */
  Drawable buildPlaceholderDrawable(Resources resources, ImageOptions imageOptions);

  /**
   * Build a progressbar drawable if specified in the given image options
   *
   * @param resources resources to be used to load the drawable
   * @param imageOptions image options to be used to create the progressbar drawable
   * @return the progressbar drawable or NopDrawable.INSTANCE if unset.
   */
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
   * @return the actual image wrapper drawable
   */
  @ThreadSafe
  ForwardingDrawable buildActualImageWrapper(ImageOptions imageOptions);

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
   */
  void setupActualImageWrapper(ScaleTypeDrawable actualImageWrapper, ImageOptions imageOptions);

  /**
   * Sets up the actual image drawable for a given fresco drawable.
   *
   * @return actual image drawable for given {@code closeableImage} or null
   * @param wasImmediate true if result was delivered immediately e.g. from cache. Affects the
   *     decision of whether to animate transition
   */
  @Nullable
  Drawable setupActualImageDrawable(
      BaseFrescoDrawable frescoDrawable,
      Resources resources,
      ImageOptions imageOptions,
      CloseableReference<CloseableImage> closeableImage,
      @Nullable ForwardingDrawable actualImageWrapperDrawable,
      boolean wasImmediate,
      @Nullable InstrumentedDrawable.Listener instrumentedListener);

  /**
   * Sets up the overlay drawable for a given fresco drawable.
   *
   * @param frescoDrawable the Fresco drawable to set up
   * @param resources resources to be used to load drawables
   * @param imageOptions image options to be used to create the overlay
   * @param cachedOverlayDrawable a cached overlay drawable to be used instead of creating a new one
   */
  void setupOverlayDrawable(
      BaseFrescoDrawable frescoDrawable,
      Resources resources,
      ImageOptions imageOptions,
      @Nullable Drawable cachedOverlayDrawable);

  /**
   * Sets up the debug overlay drawable for a given fresco drawable.
   *
   * @param frescoDrawable the Fresco drawable to set up
   * @param overlayDrawable a cached overlay drawable to be used instead of creating a new one
   * @param debugOverlayDrawable a debug overlay drawable if enabled
   */
  void setupDebugOverlayDrawable(
      BaseFrescoDrawable frescoDrawable,
      @Nullable Drawable overlayDrawable,
      @Nullable Drawable debugOverlayDrawable);
}
