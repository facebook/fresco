/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline.info;

import com.facebook.imagepipeline.listener.BaseRequestListener;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/**
 * Image origin request listener that maps all image requests for a given Drawee controller to an
 * {@link ImageOrigin} and corresponding {@link ImageOriginListener}.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ImageOriginRequestListener extends BaseRequestListener {

  // NULLSAFE_FIXME[Field Not Initialized]
  private String mControllerId;
  private final @Nullable ImageOriginListener mImageOriginLister;

  public ImageOriginRequestListener(
      String controllerId, @Nullable ImageOriginListener imageOriginLister) {
    mImageOriginLister = imageOriginLister;
    init(controllerId);
  }

  /**
   * Re-initialize the listener in case the underlying controller ID changes.
   *
   * @param controllerId the new controller ID
   */
  public void init(String controllerId) {
    mControllerId = controllerId;
  }

  @Override
  public void onUltimateProducerReached(
      String requestId, String ultimateProducerName, boolean successful) {
    if (mImageOriginLister != null) {
      mImageOriginLister.onImageLoaded(
          mControllerId,
          ImageOriginUtils.mapProducerNameToImageOrigin(ultimateProducerName),
          successful,
          ultimateProducerName);
    }
  }
}
