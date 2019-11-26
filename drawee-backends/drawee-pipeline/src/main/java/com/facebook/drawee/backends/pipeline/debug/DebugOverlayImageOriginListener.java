/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline.debug;

import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.backends.pipeline.info.ImageOriginListener;

public class DebugOverlayImageOriginListener implements ImageOriginListener {

  private int mImageOrigin = ImageOrigin.UNKNOWN;

  @Override
  public void onImageLoaded(
      String controllerId, int imageOrigin, boolean successful, String ultimateProducerName) {
    mImageOrigin = imageOrigin;
  }

  public int getImageOrigin() {
    return mImageOrigin;
  }
}
