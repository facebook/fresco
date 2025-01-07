/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image

import android.graphics.drawable.Drawable

/** [CloseableImage] that wraps an Android binary XML. */
public interface CloseableXml : CloseableImage {

  /**
   * Builds the underlying drawable. This method will yield null if the image has been closed.
   *
   * @return the underlying drawable
   */
  fun buildDrawable(): Drawable?
}
