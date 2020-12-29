/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import com.facebook.drawee.backends.pipeline.info.ImageLoadStatus;
import com.facebook.drawee.backends.pipeline.info.ImagePerfDataListener;
import com.facebook.drawee.backends.pipeline.info.ImagePerfNotifier;
import com.facebook.drawee.backends.pipeline.info.ImagePerfState;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.STRICT)
public class VitoImagePerfNotifierImpl implements ImagePerfNotifier {

  private final ImagePerfDataListener mImagePerfDataListener;

  public VitoImagePerfNotifierImpl(final ImagePerfDataListener imagePerfDataListener) {
    mImagePerfDataListener = imagePerfDataListener;
  }

  @Override
  public void notifyListenersOfVisibilityStateUpdate(
      ImagePerfState state, @ImageLoadStatus int visibilityState) {
    mImagePerfDataListener.onImageVisibilityUpdated(state.snapshot(), visibilityState);
  }

  @Override
  public void notifyStatusUpdated(ImagePerfState state, @ImageLoadStatus int imageLoadStatus) {
    state.setImageLoadStatus(imageLoadStatus);
    mImagePerfDataListener.onImageLoadStatusUpdated(state.snapshot(), imageLoadStatus);
  }
}
