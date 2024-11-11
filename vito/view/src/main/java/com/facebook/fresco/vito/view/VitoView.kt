/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.view

import android.net.Uri
import android.view.View
import com.facebook.fresco.ui.common.OnFadeListener
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.core.VitoImageRequestListener
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.fresco.vito.view.impl.VitoViewImpl2

/** Load images into an ImageView or a plain View. */
object VitoView {

  /*
   * Display an image with default image options
   */
  @JvmStatic
  fun show(uri: Uri?, target: View) {
    show(ImageSourceProvider.forUri(uri), target)
  }

  /*
   * Display an image with default image options and a caller context.
   */
  @JvmStatic
  fun show(uri: Uri?, callerContext: Any, target: View) {
    show(ImageSourceProvider.forUri(uri), callerContext, target)
  }

  /*
   * Display an image with default image options
   */
  @JvmStatic
  fun show(imageSource: ImageSource, target: View) {
    VitoViewImpl2.show(imageSource, ImageOptions.defaults(), null, null, null, target)
  }

  /*
   * Display an image with default image options and a caller context.
   */
  @JvmStatic
  fun show(imageSource: ImageSource, callerContext: Any, target: View) {
    VitoViewImpl2.show(imageSource, ImageOptions.defaults(), callerContext, null, null, target)
  }

  /*
   * Display an image with the given image options
   */
  @JvmStatic
  fun show(uri: Uri?, imageOptions: ImageOptions, target: View) {
    show(ImageSourceProvider.forUri(uri), imageOptions, target)
  }

  /*
   * Display an image with the given image options
   */
  @JvmStatic
  fun show(imageSource: ImageSource, imageOptions: ImageOptions, target: View) {
    VitoViewImpl2.show(imageSource, imageOptions, null, null, null, target)
  }

  /*
   * Display an image
   */
  @JvmStatic
  fun show(uri: Uri?, imageOptions: ImageOptions, callerContext: Any?, target: View) {
    show(ImageSourceProvider.forUri(uri), imageOptions, callerContext, target)
  }

  /*
   * Display an image
   */
  @JvmStatic
  fun show(
      imageSource: ImageSource,
      imageOptions: ImageOptions,
      callerContext: Any?,
      target: View
  ) {
    VitoViewImpl2.show(imageSource, imageOptions, callerContext, null, null, target)
  }

  /*
   * Display an image
   */
  @JvmStatic
  fun show(
      uri: Uri?,
      imageOptions: ImageOptions,
      callerContext: Any?,
      imageListener: ImageListener?,
      target: View
  ) {
    show(ImageSourceProvider.forUri(uri), imageOptions, callerContext, imageListener, target)
  }

  /*
   * Display an image
   */
  @JvmStatic
  @JvmOverloads
  fun show(
      imageSource: ImageSource,
      imageOptions: ImageOptions,
      callerContext: Any?,
      imageListener: ImageListener?,
      target: View,
      onFadeListener: OnFadeListener? = null,
      uiFramework: String = "view",
  ) {
    VitoViewImpl2.show(
        imageSource,
        imageOptions,
        callerContext,
        imageListener,
        null,
        target,
        onFadeListener,
        uiFramework)
  }

  @JvmStatic
  fun show(
      imageSource: ImageSource,
      imageOptions: ImageOptions,
      callerContext: Any?,
      imageListener: ImageListener?,
      imageRequestListener: VitoImageRequestListener?,
      target: View
  ) {
    VitoViewImpl2.show(
        imageSource, imageOptions, callerContext, imageListener, imageRequestListener, target)
  }

  @JvmStatic
  fun show(
      vitoImageRequest: VitoImageRequest,
      callerContext: Any?,
      imageListener: ImageListener?,
      target: View
  ) {
    VitoViewImpl2.show(vitoImageRequest, callerContext, imageListener, null, target)
  }

  /**
   * Release the image for the given view target if loaded by Vito
   *
   * @param target the image target to release
   */
  @JvmStatic
  fun release(target: View) {
    VitoViewImpl2.release(target)
  }

  @JvmStatic
  fun getDrawable(target: View): FrescoDrawableInterface? {
    return VitoViewImpl2.getDrawable(target)
  }
}
