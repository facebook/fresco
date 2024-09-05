/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.tools.liveeditor

import android.content.Context
import com.facebook.fresco.vito.core.FrescoController2
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.VitoImagePipeline
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.fresco.vito.source.ImageSourceProvider
import kotlin.Unit

class ImageLiveEditor(
    val drawable: FrescoDrawableInterface,
    private val imagePipeline: VitoImagePipeline,
    private val frescoController: FrescoController2
) {

  private var originalRequest = drawable.imageRequest

  fun editOptions(context: Context, function: (ImageOptions.Builder) -> Unit) {
    val builder = getOptions().extend()
    function.invoke(builder)
    fetch(context = context, imageOptions = builder.build())
  }

  fun editSource(context: Context, function: (ImageSource) -> ImageSource) {
    fetch(context = context, imageSource = function.invoke(getSource()))
  }

  fun fetch(
      context: Context,
      imageSource: ImageSource = getSource(),
      imageOptions: ImageOptions = getOptions()
  ) {
    if (originalRequest == null) {
      originalRequest = drawable.imageRequest
    }
    val newRequest = imagePipeline.createImageRequest(context.resources, imageSource, imageOptions)
    fetch(newRequest)
  }

  fun fetch(request: VitoImageRequest) {
    frescoController.fetch(
        drawable = drawable,
        imageRequest = request,
        callerContext = drawable.callerContext,
        contextChain = null,
        listener = drawable.imageListener,
        onFadeListener = null,
        viewportDimensions = null)
  }

  fun getOptions(): ImageOptions = drawable.imageRequest?.imageOptions ?: ImageOptions.defaults()

  fun getSource(): ImageSource =
      drawable.imageRequest?.imageSource ?: ImageSourceProvider.emptySource()

  fun getOriginalSource(): ImageSource =
      originalRequest?.imageSource ?: ImageSourceProvider.emptySource()

  fun reset(): Unit? = originalRequest?.let(::fetch)
}
