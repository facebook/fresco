/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl

import android.graphics.drawable.Drawable
import com.facebook.drawee.backends.pipeline.info.ImageOrigin
import com.facebook.fresco.ui.common.BaseControllerListener2
import com.facebook.fresco.ui.common.ControllerListener2
import com.facebook.fresco.ui.common.ControllerListener2.Extras
import com.facebook.fresco.ui.common.ImagePerfNotifier
import com.facebook.fresco.ui.common.ImagePerfNotifierHolder
import com.facebook.fresco.ui.common.VitoUtils
import com.facebook.fresco.vito.core.CombinedImageListener
import com.facebook.fresco.vito.core.ImagePerfLoggingListener
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.core.VitoImageRequestListener
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.imagepipeline.image.ImageInfo
import java.io.Closeable
import java.io.IOException

class CombinedImageListenerImpl : CombinedImageListener {

  private var localVitoImageRequestListener: VitoImageRequestListener? = null
  private var vitoImageRequestListener: VitoImageRequestListener? = null
  override var imageListener: ImageListener? = null
  private var controllerListener2: ControllerListener2<ImageInfo>? =
      BaseControllerListener2.getNoOpListener()
  private var imagePerfLoggingListener: ImagePerfLoggingListener? = null
  private var localImagePerfStateListener: ImagePerfNotifier? = null

  override fun setVitoImageRequestListener(vitoImageRequestListener: VitoImageRequestListener?) {
    this.vitoImageRequestListener = vitoImageRequestListener
  }

  override fun setLocalVitoImageRequestListener(
      vitoImageRequestListener: VitoImageRequestListener?
  ) {
    this.localVitoImageRequestListener = vitoImageRequestListener
  }

  override fun setControllerListener2(controllerListener2: ControllerListener2<ImageInfo>?) {
    this.controllerListener2 = controllerListener2
  }

  override fun setImagePerfLoggingListener(imagePerfLoggingListener: ImagePerfLoggingListener?) {
    this.imagePerfLoggingListener = imagePerfLoggingListener
    checkAndSetLocalImagePerfStateListener()
  }

  override fun getImagePerfLoggingListener(): ImagePerfLoggingListener? = imagePerfLoggingListener

  override fun setLocalImagePerfStateListener(imagePerfNotifier: ImagePerfNotifier?) {
    localImagePerfStateListener = imagePerfNotifier
    checkAndSetLocalImagePerfStateListener()
  }

  private fun checkAndSetLocalImagePerfStateListener() {
    val localPerfStatePublisher = imagePerfLoggingListener as? ImagePerfNotifierHolder
    if (localImagePerfStateListener != null && localPerfStatePublisher == null) {
      throw NullPointerException(
          "trying to set localImagePerfStateListener without a localPerfStatePublisher")
    }
    localPerfStatePublisher?.setImagePerfNotifier(localImagePerfStateListener)
  }

  override fun onSubmit(
      id: Long,
      imageRequest: VitoImageRequest,
      callerContext: Any?,
      extras: Extras?
  ) {
    vitoImageRequestListener?.onSubmit(id, imageRequest, callerContext, extras)
    localVitoImageRequestListener?.onSubmit(id, imageRequest, callerContext, extras)
    imageListener?.onSubmit(id, callerContext)
    val stringId = VitoUtils.getStringId(id)
    controllerListener2?.onSubmit(stringId, callerContext, extras)
    imagePerfLoggingListener?.onSubmit(stringId, callerContext, extras)
  }

  override fun onPlaceholderSet(id: Long, imageRequest: VitoImageRequest, placeholder: Drawable?) {
    vitoImageRequestListener?.onPlaceholderSet(id, imageRequest, placeholder)
    localVitoImageRequestListener?.onPlaceholderSet(id, imageRequest, placeholder)
    imageListener?.onPlaceholderSet(id, placeholder)
  }

  override fun onFinalImageSet(
      id: Long,
      imageRequest: VitoImageRequest,
      @ImageOrigin imageOrigin: Int,
      imageInfo: ImageInfo?,
      extras: Extras?,
      drawable: Drawable?
  ) {
    vitoImageRequestListener?.onFinalImageSet(
        id, imageRequest, imageOrigin, imageInfo, extras, drawable)
    localVitoImageRequestListener?.onFinalImageSet(
        id, imageRequest, imageOrigin, imageInfo, extras, drawable)
    imageListener?.onFinalImageSet(id, imageOrigin, imageInfo, drawable)
    val stringId = VitoUtils.getStringId(id)
    controllerListener2?.onFinalImageSet(stringId, imageInfo, extras)
    imagePerfLoggingListener?.onFinalImageSet(stringId, imageInfo, extras)
  }

  override fun onIntermediateImageSet(
      id: Long,
      imageRequest: VitoImageRequest,
      imageInfo: ImageInfo?
  ) {
    vitoImageRequestListener?.onIntermediateImageSet(id, imageRequest, imageInfo)
    localVitoImageRequestListener?.onIntermediateImageSet(id, imageRequest, imageInfo)
    imageListener?.onIntermediateImageSet(id, imageInfo)
    val stringId = VitoUtils.getStringId(id)
    controllerListener2?.onIntermediateImageSet(stringId, imageInfo)
    imagePerfLoggingListener?.onIntermediateImageSet(stringId, imageInfo)
  }

  override fun onIntermediateImageFailed(
      id: Long,
      imageRequest: VitoImageRequest,
      throwable: Throwable?
  ) {
    vitoImageRequestListener?.onIntermediateImageFailed(id, imageRequest, throwable)
    localVitoImageRequestListener?.onIntermediateImageFailed(id, imageRequest, throwable)
    imageListener?.onIntermediateImageFailed(id, throwable)
    val stringId = VitoUtils.getStringId(id)
    controllerListener2?.onIntermediateImageFailed(stringId)
    imagePerfLoggingListener?.onIntermediateImageFailed(stringId)
  }

  override fun onFailure(
      id: Long,
      imageRequest: VitoImageRequest,
      error: Drawable?,
      throwable: Throwable?,
      extras: Extras?
  ) {
    vitoImageRequestListener?.onFailure(id, imageRequest, error, throwable, extras)
    localVitoImageRequestListener?.onFailure(id, imageRequest, error, throwable, extras)
    imageListener?.onFailure(id, error, throwable)
    val stringId = VitoUtils.getStringId(id)
    controllerListener2?.onFailure(stringId, throwable, extras)
    imagePerfLoggingListener?.onFailure(stringId, throwable, extras)
  }

  override fun onRelease(id: Long, imageRequest: VitoImageRequest, extras: Extras?) {
    vitoImageRequestListener?.onRelease(id, imageRequest, extras)
    localVitoImageRequestListener?.onRelease(id, imageRequest, extras)
    imageListener?.onRelease(id)
    val stringId = VitoUtils.getStringId(id)
    controllerListener2?.onRelease(stringId, extras)
    imagePerfLoggingListener?.onRelease(stringId, extras)
  }

  override fun onEmptyEvent(callerContext: Any?) {
    vitoImageRequestListener?.onEmptyEvent(callerContext)
    localVitoImageRequestListener?.onEmptyEvent(callerContext)
    controllerListener2?.onEmptyEvent(callerContext)
    imagePerfLoggingListener?.onEmptyEvent(callerContext)
  }

  override fun onReset(
      resetVitoImageRequestListener: Boolean,
      resetLocalVitoImageRequestListener: Boolean,
      resetLocalImagePerfStateListener: Boolean,
      resetControllerListener2: Boolean,
  ) {
    try {
      imageListener = null
      (imagePerfLoggingListener as? Closeable)?.close()
      if (resetVitoImageRequestListener) {
        vitoImageRequestListener = null
      }
      if (resetLocalVitoImageRequestListener) {
        localVitoImageRequestListener = null
      }
      if (resetLocalImagePerfStateListener) {
        localImagePerfStateListener = null
      }
      if (resetControllerListener2) {
        controllerListener2 = null
      }
    } catch (e: IOException) {}
  }
}
