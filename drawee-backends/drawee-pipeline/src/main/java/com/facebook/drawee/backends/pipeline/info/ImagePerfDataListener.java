/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

public interface ImagePerfDataListener {

  void onImagePerfDataUpdated(ImagePerfData imagePerfData, @ImageLoadStatus int imageLoadStatus);
}
