/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core

class ListVitoImagePerfListener(private vararg val listeners: VitoImagePerfListener) :
    VitoImagePerfListener {
  override fun onImageMount(drawable: FrescoDrawableInterface): Unit =
      listeners.forEach { it.onImageMount(drawable) }

  override fun onImageUnmount(drawable: FrescoDrawableInterface): Unit =
      listeners.forEach { it.onImageUnmount(drawable) }

  override fun onImageBind(drawable: FrescoDrawableInterface): Unit =
      listeners.forEach { it.onImageBind(drawable) }

  override fun onImageUnbind(drawable: FrescoDrawableInterface): Unit =
      listeners.forEach { it.onImageUnbind(drawable) }

  override fun onImageFetch(drawable: FrescoDrawableInterface): Unit =
      listeners.forEach { it.onImageFetch(drawable) }

  override fun onImageSuccess(drawable: FrescoDrawableInterface, wasImmediate: Boolean): Unit =
      listeners.forEach { it.onImageSuccess(drawable, wasImmediate) }

  override fun onImageError(drawable: FrescoDrawableInterface): Unit =
      listeners.forEach { it.onImageError(drawable) }

  override fun onImageRelease(drawable: FrescoDrawableInterface): Unit =
      listeners.forEach { it.onImageRelease(drawable) }

  override fun onScheduleReleaseDelayed(drawable: FrescoDrawableInterface): Unit =
      listeners.forEach { it.onScheduleReleaseDelayed(drawable) }

  override fun onScheduleReleaseNextFrame(drawable: FrescoDrawableInterface): Unit =
      listeners.forEach { it.onScheduleReleaseNextFrame(drawable) }

  override fun onReleaseImmediately(drawable: FrescoDrawableInterface): Unit =
      listeners.forEach { it.onReleaseImmediately(drawable) }

  override fun onDrawableReconfigured(drawable: FrescoDrawableInterface): Unit =
      listeners.forEach { it.onDrawableReconfigured(drawable) }

  override fun onIgnoreResult(drawable: FrescoDrawableInterface): Unit =
      listeners.forEach { it.onIgnoreResult(drawable) }

  override fun onIgnoreFailure(drawable: FrescoDrawableInterface): Unit =
      listeners.forEach { it.onIgnoreFailure(drawable) }
}
