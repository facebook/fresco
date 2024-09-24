/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.drawableholder

import android.content.res.Resources
import android.graphics.drawable.Drawable
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.provider.FrescoVitoProvider
import com.facebook.fresco.vito.source.ImageSource

class MultiVitoDrawableHolder {

  class VitoDrawableHolder(
      var drawable: FrescoDrawableInterface,
      var imageOptionsBuilder: ImageOptions.Builder,
      var imageSource: ImageSource,
      var resources: Resources,
      var callerContext: Any?,
      var imageListener: ImageListener? = null,
  )

  private val holders: ArrayList<VitoDrawableHolder> = ArrayList()

  private var isAttached: Boolean = false

  fun iterator(): Iterator<VitoDrawableHolder> = holders.iterator()

  fun onAttach() {
    if (isAttached) {
      return
    }
    isAttached = true
    for (i in holders.indices) {
      attachHolder(holders[i])
    }
  }

  fun onDetach() {
    if (!isAttached) {
      return
    }
    isAttached = false
    for (i in holders.indices) {
      detachHolder(holders[i])
    }
  }

  fun add(holder: VitoDrawableHolder) {
    holders.add(holder)
    if (isAttached) {
      attachHolder(holder)
    }
  }

  fun addAtIndex(index: Int, holder: VitoDrawableHolder) {
    holders.add(index, holder)
    if (isAttached) {
      attachHolder(holder)
    }
  }

  fun remove(index: Int) {
    val holder = holders[index]
    if (isAttached) {
      detachHolder(holder)
    }
    holders.removeAt(index)
  }

  fun get(index: Int): VitoDrawableHolder {
    return holders[index]
  }

  fun size(): Int {
    return holders.size
  }

  fun draw(canvas: android.graphics.Canvas) {
    for (i in holders.indices) {
      val drawable = holders[i].drawable as Drawable
      drawable.draw(canvas)
    }
  }

  fun verifyDrawable(who: Drawable): Boolean {
    for (i in holders.indices) {
      val drawable = holders[i].drawable as Drawable
      if (drawable === who) {
        return true
      }
    }
    return false
  }

  fun clear() {
    if (isAttached) {
      for (i in holders.indices) {
        detachHolder(holders[i])
      }
    }
    holders.clear()
  }

  companion object {
    @JvmStatic
    fun attachHolder(holder: VitoDrawableHolder) {
      val vitoImageRequest =
          FrescoVitoProvider.getImagePipeline()
              .createImageRequest(
                  holder.resources, holder.imageSource, holder.imageOptionsBuilder.build())
      FrescoVitoProvider.getController()
          .fetch(
              drawable = holder.drawable,
              imageRequest = vitoImageRequest,
              callerContext = holder.callerContext,
              contextChain = null,
              listener = holder.imageListener,
              onFadeListener = null,
              viewportDimensions = null,
          )
    }

    @JvmStatic
    fun detachHolder(holder: VitoDrawableHolder) {
      FrescoVitoProvider.getController().release(holder.drawable)
    }
  }
}
