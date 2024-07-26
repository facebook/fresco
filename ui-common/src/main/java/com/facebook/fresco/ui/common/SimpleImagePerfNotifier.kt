/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

class SimpleImagePerfNotifier(private val imagePerfDataListener: ImagePerfDataListener) :
    ImagePerfNotifier {

  override fun notifyVisibilityUpdated(state: ImagePerfState, visibilityState: VisibilityState) {
    imagePerfDataListener.onImageVisibilityUpdated(state.snapshot(), visibilityState)
  }

  override fun notifyStatusUpdated(state: ImagePerfState, imageLoadStatus: ImageLoadStatus) {
    imagePerfDataListener.onImageLoadStatusUpdated(state.snapshot(), imageLoadStatus)
  }
}
