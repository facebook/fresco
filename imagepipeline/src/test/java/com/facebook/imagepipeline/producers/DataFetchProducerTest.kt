/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import com.facebook.imageformat.DefaultImageFormats
import com.facebook.imageformat.ImageFormatChecker
import java.io.ByteArrayInputStream
import java.io.IOException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Unit test for DataFetchProducer */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DataFetchProducerTest {

  @Test
  fun testBase64() {
    assertThat(DataFetchProducer.isBase64("data:")).isFalse()
    assertThat(DataFetchProducer.isBase64("data:text/plain;param=value;base64")).isTrue()
  }

  @Test
  fun testSimple() {
    assertThat(DataFetchProducer.getData("data:,A%20brief%20note"))
        .isEqualTo("A brief note".toByteArray())

    // 5-character Tamil sequence 'thamizh'
    assertThat(
            DataFetchProducer.getData(
                "data:text/plain;,%E0%AE%A4%E0%AE%AE%E0%AE%BF%E0%AE%B4%E0%AF%8D"
            )
        )
        .isEqualTo("\u0ba4\u0bae\u0bbf\u0bb4\u0bcd".toByteArray())
  }

  @Test
  @Throws(IOException::class)
  fun testGif() {
    val gif =
        DataFetchProducer.getData(
            "data:image/gif;base64," +
                "R0lGODdhMAAwAPAAAAAAAP///ywAAAAAMAAwAAAC8IyPqcvt3wCcDkiLc7C0qwyGHhSWpjQu5yqmCYsapyu" +
                "vUUlvONmOZtfzgFzByTB10QgxOR0TqBQejhRNzOfkVJ+5YiUqrXF5Y5lKh/DeuNcP5yLWGsEbtLiOSpa/TP" +
                "g7JpJHxyendzWTBfX0cxOnKPjgBzi4diinWGdkF8kjdfnycQZXZeYGejmJlZeGl9i2icVqaNVailT6F5iJ9" +
                "0m6mvuTS4OK05M0vDk0Q4XUtwvKOzrcd3iq9uisF81M1OIcR7lEewwcLp7tuNNkM3uNna3F2JQFo97Vriy/" +
                "Xl4/f1cf5VWzXyym7PHhhx4dbgYKAAA7"
        )
    assertThat(ImageFormatChecker.getImageFormat(ByteArrayInputStream(gif)))
        .isEqualTo(DefaultImageFormats.GIF)
  }
}
