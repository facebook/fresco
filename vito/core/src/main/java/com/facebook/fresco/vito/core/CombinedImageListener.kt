/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

import com.facebook.fresco.ui.common.ControllerListener2
import com.facebook.fresco.ui.common.ImagePerfNotifier
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.imagepipeline.image.ImageInfo

interface CombinedImageListener : VitoImageRequestListener {
  fun setVitoImageRequestListener(vitoImageRequestListener: VitoImageRequestListener?)

  fun setLocalVitoImageRequestListener(vitoImageRequestListener: VitoImageRequestListener?)

  fun setControllerListener2(controllerListener2: ControllerListener2<ImageInfo>?)

  var imageListener: ImageListener?

  fun setImagePerfLoggingListener(imagePerfLoggingListener: ImagePerfLoggingListener?)

  fun getImagePerfLoggingListener(): ImagePerfLoggingListener?

  fun setLocalImagePerfStateListener(imagePerfNotifier: ImagePerfNotifier?)

  fun onReset(
      resetVitoImageRequestListener: Boolean,
      resetLocalVitoImageRequestListener: Boolean,
      resetLocalImagePerfStateListener: Boolean,
      resetControllerListener2: Boolean
  )
}
