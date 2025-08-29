/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

interface AnimatedImagePerfLoggingListener {

  /**
   * Called when creation of a CloseableAnimatedImage starts
   *
   * @param imageId unique identifier for the image
   * @param timestampNs timestamp in nanoseconds
   */
  fun onCloseableAnimatedImageCreationStart(imageId: String, timestampNs: Long)

  /**
   * Called when creation of a CloseableAnimatedImage completes
   *
   * @param imageId unique identifier for the image
   * @param timestampNs timestamp in nanoseconds
   * @param success whether creation was successful
   */
  fun onCloseableAnimatedImageCreationEnd(imageId: String, timestampNs: Long, success: Boolean)

  /**
   * Called when creation of a Fresco drawable starts
   *
   * @param imageId unique identifier for the image
   * @param timestampNs timestamp in nanoseconds
   */
  fun onDrawableCreationStart(imageId: String, timestampNs: Long)

  /**
   * Called when creation of a Fresco drawable completes
   *
   * @param imageId unique identifier for the image
   * @param timestampNs timestamp in nanoseconds
   * @param success whether creation was successful
   */
  fun onDrawableCreationEnd(imageId: String, timestampNs: Long, success: Boolean)

  /**
   * Called when a frame is dropped during animation
   *
   * @param imageId unique identifier for the image
   * @param frameNumber the frame number that was dropped
   * @param timestampNs timestamp in nanoseconds
   */
  fun onFrameDropped(imageId: String, frameNumber: Int, timestampNs: Long)
}
