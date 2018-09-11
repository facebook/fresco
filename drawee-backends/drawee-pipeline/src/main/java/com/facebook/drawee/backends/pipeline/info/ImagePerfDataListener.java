/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

public interface ImagePerfDataListener {

  void onImageLoadStatusUpdated(ImagePerfData imagePerfData, @ImageLoadStatus int imageLoadStatus);

  void onImageVisibilityUpdated(ImagePerfData imagePerfData, @VisibilityState int visibilityState);
}
