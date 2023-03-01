/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

class LazyImagePerfDataNotifier(private val perfDataListenerLambda: () -> ImagePerfDataListener) :
    ImagePerfNotifier {

  private val imagePerfDataNotifier: ImagePerfDataNotifier by lazy {
    ImagePerfDataNotifier(perfDataListenerLambda.invoke())
  }

  override fun notifyListenersOfVisibilityStateUpdate(
      state: ImagePerfState,
      visibilityState: VisibilityState
  ) = imagePerfDataNotifier.notifyListenersOfVisibilityStateUpdate(state, visibilityState)

  override fun notifyStatusUpdated(state: ImagePerfState, imageLoadStatus: ImageLoadStatus) =
      imagePerfDataNotifier.notifyStatusUpdated(state, imageLoadStatus)
}
