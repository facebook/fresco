/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

/**
 * Listens to ImagePerfState update notifications and turns them into ImagePerfData notifications
 */
open class ImagePerfDataNotifier(private val perfDataListener: ImagePerfDataListener) :
    ImagePerfNotifier {

  override fun notifyVisibilityUpdated(state: ImagePerfState, visibilityState: VisibilityState) {
    perfDataListener.onImageVisibilityUpdated(state.snapshot(), visibilityState)
  }

  override fun notifyStatusUpdated(state: ImagePerfState, imageLoadStatus: ImageLoadStatus) {
    perfDataListener.onImageLoadStatusUpdated(state.snapshot(), imageLoadStatus)
  }
}
