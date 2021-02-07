/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.graphics.drawable.Drawable;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.fresco.ui.common.DimensionsInfo;
import com.facebook.imagepipeline.image.ImageInfo;
import javax.annotation.Nullable;

@Deprecated
/** Please use {@link ImageListener} instead. Interface for an image status listener. */
public interface ImageStateListener {

  /**
   * Called before the image request is submitted.
   *
   * @param state image state
   * @param callerContext caller context
   */
  void onSubmit(FrescoState state, Object callerContext);

  /**
   * Called after a placeholder image has been set
   *
   * @param state image state
   * @param placeholder the placeholder drawable if set
   */
  void onPlaceholderSet(FrescoState state, @Nullable Drawable placeholder);

  /**
   * Called after the final image has been set.
   *
   * @param state image state
   * @param imageOrigin image origin that indicates where an image is being loaded from
   * @param imageInfo image info
   * @param drawable the Drawable to be displayed
   */
  void onFinalImageSet(
      FrescoState state,
      @ImageOrigin int imageOrigin,
      @Nullable ImageInfo imageInfo,
      @Nullable Drawable drawable);

  /**
   * Called after any intermediate image has been set.
   *
   * @param state image state
   * @param imageInfo image info
   */
  void onIntermediateImageSet(FrescoState state, @Nullable ImageInfo imageInfo);

  /**
   * Called after the fetch of the intermediate image failed.
   *
   * @param state image state
   * @param throwable failure cause
   */
  void onIntermediateImageFailed(FrescoState state, Throwable throwable);

  /**
   * Called after the fetch of the final image failed.
   *
   * @param state image state
   * @param error the displayed error drawable if set
   * @param throwable failure cause
   */
  void onFailure(FrescoState state, @Nullable Drawable error, @Nullable Throwable throwable);

  /**
   * Called after the controller released the fetched image.
   *
   * @param state image state
   */
  void onRelease(FrescoState state);

  void onImageDrawn(String id, ImageInfo info, DimensionsInfo dimensionsInfo);
}
