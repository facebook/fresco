/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl.source

import com.facebook.common.internal.Supplier
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.datasource.RetainingDataSourceSupplier
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.imagepipeline.image.CloseableImage

/**
 * Retaining image source. This image source allows to be updated to load a new image but retain the
 * old image until the new image is available. The image source can be updated by calling
 * [updateImageSource].
 */
class RetainingImageSource(
    private var _currentSource: ImageSource = ImageSourceProvider.emptySource(),
    val dataSourceSupplier: RetainingDataSourceSupplier<CloseableReference<CloseableImage>> =
        RetainingDataSourceSupplier(),
) : ImageSource {
  val currentSource
    get() = _currentSource

  private var imageSourceUpdateFunction:
      ((ImageSource) -> Supplier<DataSource<CloseableReference<CloseableImage>>>)? =
      null

  /**
   * Update the image to be displayed. Will keep the current image visible until the new one is
   * available.
   *
   * @param imageSource the new image source to be used
   * @param forceReload to force reloading the image, even if the same image source is already set
   */
  @JvmOverloads
  fun updateImageSource(imageSource: ImageSource, forceReload: Boolean = false) {
    if (forceReload || _currentSource != imageSource) {
      _currentSource = imageSource
      imageSourceUpdateFunction?.let { dataSourceSupplier.replaceSupplier(it(imageSource)) }
    }
  }

  fun setImageSourceUpdateFunction(
      function: (ImageSource) -> Supplier<DataSource<CloseableReference<CloseableImage>>>
  ) {
    imageSourceUpdateFunction = function
    updateImageSource(_currentSource, true)
  }
}
