/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.draweesupport

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import com.facebook.drawee.backends.pipeline.info.ImageOrigin
import com.facebook.drawee.controller.ControllerListener
import com.facebook.fresco.ui.common.DimensionsInfo
import com.facebook.fresco.ui.common.OnDrawControllerListener
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.infer.annotation.Assertions
import com.facebook.infer.annotation.PropagatesNullable

@Deprecated("ControllerListenerWrapper is only supposed to be used for DraweeMigration.")
class ControllerListenerWrapper(private val controllerListener: ControllerListener<ImageInfo>) :
    ImageListener {

  override fun onSubmit(id: Long, callerContext: Any?) {
    controllerListener.onSubmit(
        toStringId(id),
        Assertions.nullsafeFIXME(callerContext, "Legacy ControllerListener is not nullsafe"))
  }

  override fun onPlaceholderSet(id: Long, placeholder: Drawable?) {
    // Not present in old API
  }

  override fun onFinalImageSet(
      id: Long,
      @ImageOrigin imageOrigin: Int,
      imageInfo: ImageInfo?,
      drawable: Drawable?
  ) {
    controllerListener.onFinalImageSet(
        toStringId(id), imageInfo, if (drawable is Animatable) drawable else null)
  }

  override fun onIntermediateImageSet(id: Long, imageInfo: ImageInfo?) {
    controllerListener.onIntermediateImageSet(toStringId(id), imageInfo)
  }

  override fun onIntermediateImageFailed(id: Long, throwable: Throwable?) {
    controllerListener.onIntermediateImageFailed(
        toStringId(id),
        Assertions.nullsafeFIXME(throwable, "Legacy ControllerListener is not nullsafe"))
  }

  override fun onFailure(id: Long, error: Drawable?, throwable: Throwable?) {
    controllerListener.onFailure(
        toStringId(id),
        Assertions.nullsafeFIXME(throwable, "Legacy ControllerListener is not nullsafe"))
  }

  override fun onRelease(id: Long) {
    controllerListener.onRelease(toStringId(id))
  }

  override fun onImageDrawn(id: String, imageInfo: ImageInfo, dimensionsInfo: DimensionsInfo) {
    (controllerListener as? OnDrawControllerListener<ImageInfo>)?.onImageDrawn(
        id, imageInfo, dimensionsInfo)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ControllerListenerWrapper

    if (controllerListener != other.controllerListener) return false

    return true
  }

  override fun hashCode(): Int {
    return controllerListener.hashCode()
  }

  companion object {
    /**
     * Create a new controller listener wrapper or return null if the listener is null.
     *
     * @param controllerListener the controller listener to wrap
     * @return the wrapped controller listener or null if no wrapping required
     */
    @JvmStatic
    fun create(
        @PropagatesNullable controllerListener: ControllerListener<ImageInfo>?
    ): ControllerListenerWrapper? = controllerListener?.let { ControllerListenerWrapper(it) }

    private fun toStringId(id: Long): String = "v$id"
  }
}
