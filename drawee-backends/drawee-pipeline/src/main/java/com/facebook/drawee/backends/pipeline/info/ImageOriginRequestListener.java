/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

import com.facebook.imagepipeline.listener.BaseRequestListener;
import javax.annotation.Nullable;

/**
 * Image origin request listener that maps all image requests for a given Drawee controller to an
 * {@link ImageOrigin} and corresponding {@link ImageOriginListener}.
 */
public class ImageOriginRequestListener extends BaseRequestListener {

  private final String mControllerId;
  private final @Nullable ImageOriginListener mImageOriginLister;

  public ImageOriginRequestListener(
      String controllerId, @Nullable ImageOriginListener imageOriginLister) {
    mControllerId = controllerId;
    mImageOriginLister = imageOriginLister;
  }

  @Override
  public void onUltimateProducerReached(String requestId, String producerName, boolean successful) {
    if (mImageOriginLister != null) {
      mImageOriginLister.onImageLoaded(
          mControllerId, ImageOriginUtils.mapProducerNameToImageOrigin(producerName), successful);
    }
  }
}
