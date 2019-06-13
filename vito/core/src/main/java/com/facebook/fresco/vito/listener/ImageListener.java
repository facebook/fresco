/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.listener;

import android.graphics.drawable.Drawable;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.imagepipeline.image.ImageInfo;
import javax.annotation.Nullable;

/** Interface for an image status listener. */
public interface ImageListener {

  /**
   * Called before the image request is submitted.
   *
   * @param id image id
   * @param callerContext caller context
   */
  void onSubmit(long id, Object callerContext);

  /**
   * Called after a placeholder image has been set
   *
   * @param id image id
   * @param placeholder the placeholder drawable if set
   */
  void onPlaceholderSet(long id, @Nullable Drawable placeholder);

  /**
   * Called after the final image has been set.
   *
   * @param id image id
   * @param imageOrigin image origin that indicates where an image is being loaded from
   * @param imageInfo image info
   * @param drawable the Drawable to be displayed
   */
  void onFinalImageSet(
      long id,
      @ImageOrigin int imageOrigin,
      @Nullable ImageInfo imageInfo,
      @Nullable Drawable drawable);

  /**
   * Called after any intermediate image has been set.
   *
   * @param id image id
   * @param imageInfo image info
   */
  void onIntermediateImageSet(long id, @Nullable ImageInfo imageInfo);

  /**
   * Called after the fetch of the intermediate image failed.
   *
   * @param id image id
   * @param throwable failure cause
   */
  void onIntermediateImageFailed(long id, Throwable throwable);

  /**
   * Called after the fetch of the final image failed.
   *
   * @param id image id
   * @param error the displayed error drawable if set
   * @param throwable failure cause
   */
  void onFailure(long id, @Nullable Drawable error, @Nullable Throwable throwable);

  /**
   * Called after the controller released the fetched image.
   *
   * @param id image id
   */
  void onRelease(long id);
}
