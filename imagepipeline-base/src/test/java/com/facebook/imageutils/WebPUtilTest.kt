/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageutils

import java.io.IOException
import java.io.InputStream
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests [WebpUtil] */
@RunWith(RobolectricTestRunner::class)
class WebPUtilTest {
  @Test
  @Throws(Exception::class)
  fun testSimpleWebps() {
    checkImage("webps/1_webp_plain.webp", 320, 214)
    checkImage("webps/2_webp_plain.webp", 320, 235)
  }

  @Test
  @Throws(Exception::class)
  fun testLosslessWebps() {
    checkImage("webps/1_webp_ll.webp", 400, 301)
    checkImage("webps/2_webp_ll.webp", 386, 395)
    checkImage("webps/3_webp_ll.webp", 800, 600)
    checkImage("webps/4_webp_ll.webp", 421, 163)
    checkImage("webps/5_webp_ll.webp", 300, 300)
  }

  @Test
  @Throws(Exception::class)
  fun testExtendedWebpsExtendedWithAlpha() {
    checkImage("webps/1_webp_ea.webp", 400, 301)
    checkImage("webps/2_webp_ea.webp", 386, 395)
    checkImage("webps/3_webp_ea.webp", 800, 600)
    checkImage("webps/4_webp_ea.webp", 421, 163)
    checkImage("webps/5_webp_ea.webp", 300, 300)
  }

  @Test
  @Throws(Exception::class)
  fun testExtendedWebpsExtendedNoAlpha() {
    checkImage("webps/1_webp_e.webp", 480, 320)
  }

  @Test
  @Throws(Exception::class)
  fun testExtendedWebpsAnimated() {
    checkImage("webps/1_webp_anim.webp", 322, 477)
  }

  @Throws(IOException::class)
  private fun getResourceStream(name: String): InputStream {
    val `is` = WebPUtilTest::class.java.getResourceAsStream(name)
    assertThat(`is`).describedAs("failed to read resource: " + name).isNotNull()
    return `is`!!
  }

  /**
   * Contains the logic in order to test the size of an image
   *
   * @param imagePath The Path of the given image
   * @param expectedWidth The expected width
   * @param expectedHeight The expected height
   * @throws IOException In case of errors
   */
  @Throws(IOException::class)
  private fun checkImage(imagePath: String, expectedWidth: Int, expectedHeight: Int) {
    val size: Pair<Int?, Int?>? = WebpUtil.getSize(getResourceStream(imagePath))
    assertThat(size)
        .describedAs("Something went wrong during parsing WebP! " + imagePath)
        .isNotNull()
    assertThat(size!!.component1()).isEqualTo(expectedWidth)
    assertThat(size.component2()).isEqualTo(expectedHeight)
  }
}
