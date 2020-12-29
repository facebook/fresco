// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package com.facebook.drawee.backends.pipeline.info;

public interface ImagePerfNotifier {

  void notifyListenersOfVisibilityStateUpdate(
      ImagePerfState state, @VisibilityState int visibilityState);

  void notifyStatusUpdated(ImagePerfState state, @ImageLoadStatus int imageLoadStatus);
}
