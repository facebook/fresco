/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

interface VitoImagePerfListener {
  fun onImageMount(drawable: FrescoDrawableInterface)

  fun onImageUnmount(drawable: FrescoDrawableInterface)

  fun onImageBind(drawable: FrescoDrawableInterface)

  fun onImageUnbind(drawable: FrescoDrawableInterface)

  fun onImageFetch(drawable: FrescoDrawableInterface)

  fun onImageSuccess(drawable: FrescoDrawableInterface, wasImmediate: Boolean)

  fun onImageError(drawable: FrescoDrawableInterface)

  fun onImageRelease(drawable: FrescoDrawableInterface)

  fun onScheduleReleaseDelayed(drawable: FrescoDrawableInterface)

  fun onScheduleReleaseNextFrame(drawable: FrescoDrawableInterface)

  fun onReleaseImmediately(drawable: FrescoDrawableInterface)

  fun onDrawableReconfigured(drawable: FrescoDrawableInterface)

  fun onIgnoreResult(drawable: FrescoDrawableInterface)

  fun onIgnoreFailure(drawable: FrescoDrawableInterface)
}
