/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.listener

import android.graphics.drawable.Drawable
import com.facebook.drawee.backends.pipeline.info.ImageOrigin
import com.facebook.fresco.ui.common.OnDrawControllerListener
import com.facebook.imagepipeline.image.ImageInfo

/** Interface for an image status listener. */
interface ImageListener : OnDrawControllerListener<ImageInfo> {

  /**
   * Called before the image request is submitted.
   *
   * @param id image id
   * @param callerContext caller context
   */
  fun onSubmit(id: Long, callerContext: Any?): Unit = Unit

  /**
   * Called after a placeholder image has been set
   *
   * @param id image id
   * @param placeholder the placeholder drawable if set
   */
  fun onPlaceholderSet(id: Long, placeholder: Drawable?): Unit = Unit

  /**
   * Called after the final image has been set.
   *
   * @param id image id
   * @param imageOrigin image origin that indicates where an image is being loaded from
   * @param imageInfo image info
   * @param drawable the Drawable to be displayed
   */
  fun onFinalImageSet(
      id: Long,
      @ImageOrigin imageOrigin: Int,
      imageInfo: ImageInfo?,
      drawable: Drawable?
  ): Unit = Unit

  /**
   * Called after any intermediate image has been set.
   *
   * @param id image id
   * @param imageInfo image info
   */
  fun onIntermediateImageSet(id: Long, imageInfo: ImageInfo?): Unit = Unit

  /**
   * Called after the fetch of the intermediate image failed.
   *
   * @param id image id
   * @param throwable failure cause
   */
  fun onIntermediateImageFailed(id: Long, throwable: Throwable?): Unit = Unit

  /**
   * Called after the fetch of the final image failed.
   *
   * @param id image id
   * @param error the displayed error drawable if set
   * @param throwable failure cause
   */
  fun onFailure(id: Long, error: Drawable?, throwable: Throwable?): Unit = Unit

  /**
   * Called after the controller released the fetched image.
   *
   * @param id image id
   */
  fun onRelease(id: Long): Unit = Unit
}
