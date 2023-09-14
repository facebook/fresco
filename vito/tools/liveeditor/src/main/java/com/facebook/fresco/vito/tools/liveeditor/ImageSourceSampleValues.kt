/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.tools.liveeditor

import com.facebook.fresco.vito.source.ImageSource
import com.facebook.fresco.vito.source.ImageSourceProvider

object ImageSourceSampleValues {
  data class Entry<T>(
      val name: String,
      val data: List<Pair<String, T>>,
      val updateFunction: (ImageSource, T) -> ImageSource
  )

  val entries: ImageSourceSampleValues.Entry<ImageSource> =
      Entry(
          "Image Source",
          listOf(
              Pair(
                  "JPEG",
                  ImageSourceProvider.forUri(
                      "https://frescolib.org/static/sample-images/animal_a_l.jpg")),
              Pair(
                  "PNG",
                  ImageSourceProvider.forUri(
                      "https://frescolib.org/static/sample-images/animal_b.png")),
              Pair(
                  "WebP",
                  ImageSourceProvider.forUri("https://www.gstatic.com/webp/gallery/2.webp")),
              Pair(
                  "Animated WebP",
                  ImageSourceProvider.forUri("https://www.gstatic.com/webp/animated/1.webp")),
              Pair(
                  "GIF",
                  ImageSourceProvider.forUri(
                      "https://media2.giphy.com/media/3oge84qhopFbFFkwec/giphy.gif")),
              Pair(
                  "non-existing",
                  ImageSourceProvider.forUri(
                      "https://frescolib.org/static/sample-images/does_not_exist.jpg")))) { _, s ->
            s
          }
}
