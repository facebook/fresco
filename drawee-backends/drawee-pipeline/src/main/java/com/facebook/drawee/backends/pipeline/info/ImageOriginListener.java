/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

import javax.annotation.Nullable;

/**
 * Listener that can be attached to a {@link
 * com.facebook.drawee.backends.pipeline.PipelineDraweeController} to observe the image origin
 * (cache, disk, network, ...) for a given controller.
 */
public interface ImageOriginListener {

  /**
   * Called when an image has been loaded for the controller with the given controller ID that also
   * includes the {@link ImageOrigin} and whether the image has been loaded successfully.
   *
   * @param controllerId the controller ID for the loaded image
   * @param imageOrigin the origin of the loaded image
   * @param successful true if the image has been loaded successfully
   * @param ultimateProducerName the name of the producer that delivered the final result
   */
  void onImageLoaded(
      String controllerId,
      @ImageOrigin int imageOrigin,
      boolean successful,
      @Nullable String ultimateProducerName);
}
