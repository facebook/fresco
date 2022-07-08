/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.listener

import android.graphics.drawable.Drawable
import com.facebook.drawee.backends.pipeline.info.ImageOrigin
import com.facebook.fresco.ui.common.DimensionsInfo
import com.facebook.imagepipeline.image.ImageInfo

class ForwardingImageListener(private vararg val listeners: ImageListener?) : ImageListener {

  override fun onSubmit(id: Long, callerContext: Any?) {
    listeners.forEach { it?.onSubmit(id, callerContext) }
  }

  override fun onPlaceholderSet(id: Long, placeholder: Drawable?) {
    listeners.forEach { it?.onPlaceholderSet(id, placeholder) }
  }

  override fun onFinalImageSet(
      id: Long,
      @ImageOrigin imageOrigin: Int,
      imageInfo: ImageInfo?,
      drawable: Drawable?
  ) {
    listeners.forEach { it?.onFinalImageSet(id, imageOrigin, imageInfo, drawable) }
  }

  override fun onIntermediateImageSet(id: Long, imageInfo: ImageInfo?) {
    listeners.forEach { it?.onIntermediateImageSet(id, imageInfo) }
  }

  override fun onIntermediateImageFailed(id: Long, throwable: Throwable?) {
    listeners.forEach { it?.onIntermediateImageFailed(id, throwable) }
  }

  override fun onFailure(id: Long, error: Drawable?, throwable: Throwable?) {
    listeners.forEach { it?.onFailure(id, error, throwable) }
  }

  override fun onRelease(id: Long) {
    listeners.forEach { it?.onRelease(id) }
  }

  override fun onImageDrawn(id: String, imageInfo: ImageInfo, dimensionsInfo: DimensionsInfo) {
    listeners.forEach { it?.onImageDrawn(id, imageInfo, dimensionsInfo) }
  }

  override fun equals(o: Any?): Boolean {
    // If the object is compared with itself then return true
    if (o === this) {
      return true
    }

    // Check if o is an instance of ForwardingImageListener or not
    if (o !is ForwardingImageListener) {
      return false
    }
    val listeners = o.listeners
    if (this.listeners.size != listeners.size) {
      return false
    }
    for (i in this.listeners.indices) {
      if (this.listeners[i] != listeners[i]) {
        return false
      }
    }
    return true
  }

  override fun hashCode(): Int {
    var result = if (this.listeners[0] != null) this.listeners[0].hashCode() else 0
    for (i in 1 until this.listeners.size) {
      result = 31 * result + if (this.listeners[i] != null) this.listeners[i].hashCode() else 0
    }
    return result
  }

  companion object {
    @JvmStatic
    fun create(a: ImageListener?, b: ImageListener?): ImageListener? =
        if (a == null) {
          b
        } else {
          b?.let { ForwardingImageListener(a, it) } ?: a
        }

    @JvmStatic
    fun create(a: ImageListener?, b: ImageListener?, c: ImageListener?): ImageListener? {
      if (a == null) {
        return create(b, c)
      }
      return if (b == null) {
        create(a, c)
      } else {
        c?.let { ForwardingImageListener(a, b, it) } ?: create(a, b)
      }
    }
  }
}
