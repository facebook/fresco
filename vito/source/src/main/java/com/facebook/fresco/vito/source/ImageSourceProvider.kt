/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import com.facebook.fresco.middleware.HasExtraData

/**
 * Create image sources that can be passed to Fresco's image components. For example, to create a
 * single image source for a given URI, call [forUri(Uri)] or [forUri(String)].
 */
object ImageSourceProvider {

  /**
   * Uri parsing function from String to URI. The default implementation can be replaced with any
   * custom URI parsing logic if required.
   */
  var uriParser: (String) -> Uri? = { uriString ->
    val uri: Uri = Uri.parse(uriString)
    uri
  }

  var shortcutResUris: Boolean = false

  private val emptyImageSource = EmptyImageSource("emptySource()")
  private val nullUriImageSource = EmptyImageSource("forUri(null)")

  /** @return an empty image source if no image URI is available to pass to the UI component */
  @JvmStatic fun emptySource(): ImageSource = emptyImageSource

  /**
   * Create a single image source for a given image URI.
   *
   * @param uri the image URI to use
   * @return the ImageSource to be passed to the UI component
   */
  @JvmOverloads
  @JvmStatic
  fun forUri(uri: Uri?, extras: Map<String, Any>? = null): ImageSource =
      if (uri == null) {
        nullUriImageSource
      } else if (shortcutResUris && uri.scheme == "res") {
        val resId = uri.lastPathSegment?.toInt() ?: 0
        DrawableResImageSource(resId)
      } else {
        SingleImageSourceImpl(
            uri,
            buildMap {
              if (extras != null) {
                putAll(extras)
              }
              put(HasExtraData.KEY_URI_SOURCE, uri)
            },
        )
      }

  /**
   * Create a single image source for a given image URI.
   *
   * @param uriString the image URI String to use
   * @return the ImageSource to be passed to the UI component
   */
  @JvmOverloads
  @JvmStatic
  fun forUri(uriString: String?, extras: Map<String, Any>? = null): ImageSource {
    val uri: Uri? = uriString?.let { uriParser(it) }
    return forUri(uri, extras)
  }

  /**
   * Create a multi image source for a given set of sources. Image sources are obtained in order.
   * Only if the current source fails, or if it finishes without a result, the next one will be
   * tried.
   *
   * @param imageSources the list of image sources to be used
   * @return the ImageSource to be passed to the UI component
   */
  @JvmStatic
  fun firstAvailable(vararg imageSources: ImageSource): ImageSource =
      FirstAvailableImageSource(imageSources)

  /**
   * Create a multi image source for a low- and high resolution image. Both requests will be sent
   * off, the low resolution will be used as an intermediate image until the high resolution one is
   * available.
   *
   * @param lowResImageSource the low resolution image source to be used
   * @param highResImageSource the high resolution image source to be used
   * @return the ImageSource to be passed to the UI component
   */
  @JvmStatic
  fun increasingQuality(
      lowResImageSource: ImageSource,
      highResImageSource: ImageSource,
  ): ImageSource = IncreasingQualityImageSource(lowResImageSource, highResImageSource, null)

  /**
   * Create a multi image source for a low- and high resolution image. Both requests will be sent
   * off, the low resolution will be used as an intermediate image until the high resolution one is
   * available.
   *
   * @param lowResImageSource the low resolution image source to be used
   * @param highResImageSource the high resolution image source to be used
   * @param extras optional extra metadata to be used for logging
   * @return the ImageSource to be passed to the UI component
   */
  @JvmStatic
  fun increasingQuality(
      lowResImageSource: ImageSource,
      highResImageSource: ImageSource,
      extras: Map<String, Any>? = null,
  ): ImageSource = IncreasingQualityImageSource(lowResImageSource, highResImageSource, extras)

  /**
   * Create a multi image source for a low- and high resolution image. Both requests will be sent
   * off, the low resolution will be used as an intermediate image until the high resolution one is
   * available.
   *
   * @param lowResImageUri the low resolution image URI to be used
   * @param highResImageUri the high resolution image URI to be used
   * @return the ImageSource to be passed to the UI component
   */
  @JvmStatic
  fun increasingQuality(
      lowResImageUri: Uri?,
      highResImageUri: Uri?,
  ): ImageSource =
      if (lowResImageUri == null) {
        forUri(highResImageUri)
      } else IncreasingQualityImageSource(forUri(lowResImageUri), forUri(highResImageUri))

  /**
   * Create a bitmap image source for a given bitmap image buffer.
   *
   * @param bitmap the bitmap image buffer to be used
   * @return the ImageSource be be passed to the UI component
   */
  @JvmStatic fun bitmap(bitmap: Bitmap): ImageSource = BitmapImageSource(bitmap)

  /**
   * Create a drawable image source for a given drawable.
   *
   * @param drawable the drawable to be used
   * @return the ImageSource to be passed to the UI component
   */
  @JvmStatic fun drawable(drawable: Drawable): ImageSource = DrawableImageSource(drawable)
}
