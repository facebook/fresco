/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.VitoImagePerfListener

class NopImagePerfListener : VitoImagePerfListener {
  override fun onImageMount(drawable: FrescoDrawableInterface) = Unit

  override fun onImageUnmount(drawable: FrescoDrawableInterface) = Unit

  override fun onImageBind(drawable: FrescoDrawableInterface) = Unit

  override fun onImageUnbind(drawable: FrescoDrawableInterface) = Unit

  override fun onImageFetch(drawable: FrescoDrawableInterface) = Unit

  override fun onImageSuccess(drawable: FrescoDrawableInterface, wasImmediate: Boolean) = Unit

  override fun onImageError(drawable: FrescoDrawableInterface) = Unit

  override fun onImageRelease(drawable: FrescoDrawableInterface) = Unit

  override fun onScheduleReleaseDelayed(drawable: FrescoDrawableInterface) = Unit

  override fun onScheduleReleaseNextFrame(drawable: FrescoDrawableInterface) = Unit

  override fun onReleaseImmediately(drawable: FrescoDrawableInterface) = Unit

  override fun onDrawableReconfigured(drawable: FrescoDrawableInterface) = Unit

  override fun onIgnoreResult(drawable: FrescoDrawableInterface) = Unit

  override fun onIgnoreFailure(drawable: FrescoDrawableInterface) = Unit
}
