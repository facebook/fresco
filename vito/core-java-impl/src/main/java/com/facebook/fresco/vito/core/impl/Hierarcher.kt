/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.content.res.Resources
import android.graphics.drawable.Drawable
import com.facebook.common.references.CloseableReference
import com.facebook.drawee.drawable.ForwardingDrawable
import com.facebook.drawee.drawable.ScaleTypeDrawable
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.infer.annotation.ThreadSafe

/** Helper for building drawables */
interface Hierarcher {

  /**
   * Build an actual image drawable for the given closeable image. For scaling, color filters and
   * other transformation, the actual image wrapper is used. The drawable returned here does not
   * include scaling etc and should be combined with the actual image wrapper.
   *
   * NOTE: the Drawable returned by this method will not hold on to the image reference. This must
   * be done separately.
   *
   * @param resources resources to be used to load the drawable
   * @param imageOptions image options to be used to create the drawable
   * @param closeableImage the decoded image to create the Drawable for
   * @return the actual image drawable or null if it cannot be rendered
   */
  fun buildActualImageDrawable(
      resources: Resources,
      imageOptions: ImageOptions,
      closeableImage: CloseableReference<CloseableImage>
  ): Drawable?

  /**
   * Build a placeholder drawable if specified in the given image options.
   *
   * @param resources resources to be used to load the drawable
   * @param imageOptions image options to be used to create the placeholder drawable
   * @return the placeholder drawable or NopDrawable.INSTANCE if unset.
   */
  fun buildPlaceholderDrawable(resources: Resources, imageOptions: ImageOptions): Drawable?

  /**
   * Build a progressbar drawable if specified in the given image options
   *
   * @param resources resources to be used to load the drawable
   * @param imageOptions image options to be used to create the progressbar drawable
   * @return the progressbar drawable or null if unset.
   */
  fun buildProgressDrawable(resources: Resources, imageOptions: ImageOptions): Drawable?

  /**
   * Build an error drawable if specified in the given image options.
   *
   * @param resources resources to be used to load the drawable
   * @param imageOptions image options to be used to create the error drawable
   * @return the error drawable or null if unset.
   */
  fun buildErrorDrawable(resources: Resources, imageOptions: ImageOptions): Drawable?

  /**
   * Build the actual image wrapper, a forwarding drawable that will forward to the actual image
   * drawable once set and can perform transformations, like scaling.
   *
   * @param imageOptions image options to be used to create the wrapper
   * @param callerContext the caller's context, may be null
   * @return the actual image wrapper drawable
   */
  @ThreadSafe
  fun buildActualImageWrapper(imageOptions: ImageOptions, callerContext: Any?): ForwardingDrawable

  /**
   * Builds the overlay drawable to be displayed for the given image options.
   *
   * @param resources resources to be used to load the drawable
   * @param imageOptions image options to be used to create the overlay
   * @return the overlay or null if not applicable
   */
  fun buildOverlayDrawable(resources: Resources, imageOptions: ImageOptions): Drawable?

  /**
   * Set up the actual image wrapper scale type Drawable.
   *
   * @param actualImageWrapper the wrapper to set up
   * @param imageOptions image options to be used
   * @param callerContext the caller's context, may be null
   */
  fun setupActualImageWrapper(
      actualImageWrapper: ScaleTypeDrawable,
      imageOptions: ImageOptions,
      callerContext: Any?
  )

  /**
   * Round the given Drawable if set via the given on imageOptions
   *
   * @param resources
   * @param drawable
   * @param imageOptions
   * @return the rounded drawable or original if no rounding specified
   */
  fun applyRoundingOptions(
      resources: Resources,
      drawable: Drawable,
      imageOptions: ImageOptions
  ): Drawable
}
