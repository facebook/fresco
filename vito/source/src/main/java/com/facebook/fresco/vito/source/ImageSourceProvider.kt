/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.source

import android.net.Uri
import java.lang.RuntimeException

/**
 * Create image sources that can be passed to Fresco's image components. For example, to create a
 * single image source for a given URI, call [forUri(Uri)] or [forUri(String)].
 *
 * It is also possible to set your own provider by calling [setImplementation(Implementation)]
 */
object ImageSourceProvider {

  /** @return an empty image source if no image URI is available to pass to the UI component */
  @JvmStatic fun emptySource(): ImageSource = get().emptySource()

  /**
   * Create a single image source for a given image URI.
   *
   * @param uri the image URI to use
   * @return the ImageSource to be passed to the UI component
   */
  @JvmStatic
  fun forUri(uri: Uri?): ImageSource =
      if (uri == null) {
        emptySource()
      } else {
        get().singleUri(uri)
      }

  /**
   * Create a single image source for a given image URI.
   *
   * @param uriString the image URI String to use
   * @return the ImageSource to be passed to the UI component
   */
  @JvmStatic
  fun forUri(uriString: String?): ImageSource =
      if (uriString == null) {
        emptySource()
      } else {
        get().singleUri(Uri.parse(uriString))
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
      get().firstAvailable(*imageSources)

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
      highResImageSource: ImageSource
  ): ImageSource = get().increasingQuality(lowResImageSource, highResImageSource)

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
  fun increasingQuality(lowResImageUri: Uri?, highResImageUri: Uri?): ImageSource =
      get().increasingQuality(forUri(lowResImageUri), forUri(highResImageUri))

  private var impl: Implementation? = null

  @JvmStatic
  fun setImplementation(implementation: Implementation?) {
    impl = implementation
  }

  @JvmStatic
  fun shutdown() {
    impl = null
  }

  private fun get(): Implementation {
    return impl ?: throw RuntimeException("ImageSourceProvider must be initialized first!")
  }

  interface Implementation {
    fun emptySource(): ImageSource

    fun singleUri(uri: Uri): ImageSource

    fun firstAvailable(vararg imageSources: ImageSource): ImageSource

    fun increasingQuality(
        lowResImageSource: ImageSource,
        highResImageSource: ImageSource
    ): ImageSource
  }
}
