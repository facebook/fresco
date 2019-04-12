/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.backends.pipeline.debug;

import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.backends.pipeline.info.ImageOriginListener;
import com.facebook.drawee.backends.pipeline.info.ImageOriginUtils;

public class DebugOverlayImageOriginListener implements ImageOriginListener {

  private int mImageOrigin = ImageOrigin.UNKNOWN;

  @Override
  public void onImageLoaded(
      String controllerId, int imageOrigin, boolean successful, String ultimateProducerName) {
    mImageOrigin = imageOrigin;
  }

  public String getImageOrigin() {
    return ImageOriginUtils.toString(mImageOrigin);
  }
}
