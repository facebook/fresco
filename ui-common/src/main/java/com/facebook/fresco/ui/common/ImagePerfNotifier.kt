/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common

interface ImagePerfNotifier {

  fun notifyVisibilityUpdated(state: ImagePerfState, visibilityState: VisibilityState)

  fun notifyStatusUpdated(state: ImagePerfState, imageLoadStatus: ImageLoadStatus)
}
