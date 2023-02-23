/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common;

public interface ImagePerfNotifier {

  void notifyListenersOfVisibilityStateUpdate(
      ImagePerfState state, VisibilityState visibilityState);

  void notifyStatusUpdated(ImagePerfState state, ImageLoadStatus imageLoadStatus);
}
