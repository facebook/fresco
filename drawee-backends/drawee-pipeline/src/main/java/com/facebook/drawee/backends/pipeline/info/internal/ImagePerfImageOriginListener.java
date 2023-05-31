/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline.info.internal;

import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.backends.pipeline.info.ImageOriginListener;
import com.facebook.drawee.backends.pipeline.info.ImagePerfExtra;
import com.facebook.fresco.ui.common.ImagePerfState;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ImagePerfImageOriginListener implements ImageOriginListener {

  private final ImagePerfState mImagePerfState;

  public ImagePerfImageOriginListener(ImagePerfState imagePerfState) {
    mImagePerfState = imagePerfState;
  }

  @Override
  public void onImageLoaded(
      String controllerId,
      @ImageOrigin int imageOrigin,
      boolean successful,
      @Nullable String ultimateProducerName) {
    mImagePerfState.setPipelineExtra(ImagePerfExtra.IMAGE_ORIGIN, imageOrigin);
  }
}
