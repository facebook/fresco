/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

import android.graphics.drawable.Drawable
import com.facebook.drawee.backends.pipeline.info.ImageOrigin
import com.facebook.fresco.ui.common.ControllerListener2.Extras
import com.facebook.imagepipeline.image.ImageInfo

/** Interface for an image status listener. */
interface VitoImageRequestListener {

  /**
   * Called before the image request is submitted.
   *
   * @param id image id
   * @param callerContext caller context
   */
  fun onSubmit(id: Long, imageRequest: VitoImageRequest, callerContext: Any?, extras: Extras?)

  /**
   * Called after a placeholder image has been set
   *
   * @param id image id
   * @param placeholder the placeholder drawable if set
   */
  fun onPlaceholderSet(id: Long, imageRequest: VitoImageRequest, placeholder: Drawable?)

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
      imageRequest: VitoImageRequest,
      @ImageOrigin imageOrigin: Int,
      imageInfo: ImageInfo?,
      extras: Extras?,
      drawable: Drawable?
  )

  /**
   * Called after any intermediate image has been set.
   *
   * @param id image id
   * @param imageInfo image info
   */
  fun onIntermediateImageSet(id: Long, imageRequest: VitoImageRequest, imageInfo: ImageInfo?)

  /**
   * Called after the fetch of the intermediate image failed.
   *
   * @param id image id
   * @param throwable failure cause
   */
  fun onIntermediateImageFailed(id: Long, imageRequest: VitoImageRequest, throwable: Throwable?)

  /**
   * Called after the fetch of the final image failed.
   *
   * @param id image id
   * @param error the displayed error drawable if set
   * @param throwable failure cause
   */
  fun onFailure(
      id: Long,
      imageRequest: VitoImageRequest,
      error: Drawable?,
      throwable: Throwable?,
      extras: Extras?
  )

  /**
   * Called after the controller released the fetched image.
   *
   * @param id image id
   */
  fun onRelease(id: Long, imageRequest: VitoImageRequest, extras: Extras?)

  /** Called on empty event. For instance when empty URI is requested. */
  fun onEmptyEvent(callerContext: Any?)
}
