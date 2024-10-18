/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageformat

import java.io.InputStream
import java.util.ArrayList
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Tests [ImageFormatChecker] */
@RunWith(RobolectricTestRunner::class)
class ImageFormatCheckerTest constructor() {

  @Before
  fun setUp() {
    ImageFormatChecker.instance.setBinaryXmlEnabled(true)
  }

  @Test
  fun testSimpleWebps() {
    singleImageTypeTest(getNames(2, "webps/%d_webp_plain.webp"), DefaultImageFormats.WEBP_SIMPLE)
  }

  @Test
  fun testLosslessWebps() {
    singleImageTypeTest(getNames(5, "webps/%d_webp_ll.webp"), DefaultImageFormats.WEBP_LOSSLESS)
  }

  @Test
  fun testExtendedWebpsWithAlpha() {
    singleImageTypeTest(
        getNames(5, "webps/%d_webp_ea.webp"), DefaultImageFormats.WEBP_EXTENDED_WITH_ALPHA)
  }

  @Test
  fun testExtendedWebpsWithoutAlpha() {
    singleImageTypeTest(getName("webps/1_webp_e.webp"), DefaultImageFormats.WEBP_EXTENDED)
  }

  @Test
  fun testAnimatedWebps() {
    singleImageTypeTest(getName("webps/1_webp_anim.webp"), DefaultImageFormats.WEBP_ANIMATED)
  }

  @Test
  fun testJpegs() {
    singleImageTypeTest(getNames(5, "jpegs/%d.jpeg"), DefaultImageFormats.JPEG)
  }

  @Test
  fun testPngs() {
    singleImageTypeTest(getNames(5, "pngs/%d.png"), DefaultImageFormats.PNG)
  }

  @Test
  fun testGifs() {
    singleImageTypeTest(getNames(5, "gifs/%d.gif"), DefaultImageFormats.GIF)
  }

  @Test
  fun testBmps() {
    singleImageTypeTest(getNames(5, "bmps/%d.bmp"), DefaultImageFormats.BMP)
  }

  @Test
  fun testHeifs() {
    singleImageTypeTest(getName("heifs/1.heif"), DefaultImageFormats.HEIF)
  }

  @Test
  fun testXmlVectorDrawable() {
    singleImageTypeTest(
        getName("xmls/compiled/vector_drawable.xml"), DefaultImageFormats.BINARY_XML)
  }

  @Test
  fun testXmlLayerListDrawable() {
    singleImageTypeTest(getName("xmls/compiled/layer_list.xml"), DefaultImageFormats.BINARY_XML)
  }

  @Test
  fun testXmlLevelListDrawable() {
    singleImageTypeTest(getName("xmls/compiled/level_list.xml"), DefaultImageFormats.BINARY_XML)
  }

  @Test
  fun testXmlStateListDrawable() {
    singleImageTypeTest(getName("xmls/compiled/state_list.xml"), DefaultImageFormats.BINARY_XML)
  }

  private fun singleImageTypeTest(resourceNames: List<String>, expectedImageType: ImageFormat) {
    for (name: String in resourceNames) {
      val resourceStream = getResourceStream(name)
      try {
        assertSame(
            "failed with resource: ${name}",
            expectedImageType,
            ImageFormatChecker.getImageFormat(resourceStream))
      } finally {
        resourceStream.close()
      }
    }
  }

  private fun getResourceStream(name: String): InputStream {
    val `is` = ImageFormatCheckerTest::class.java.getResourceAsStream(name)
    assertNotNull("failed to read resource: ${name}", `is`)
    return `is`
  }

  companion object {
    private fun getName(path: String): List<String> = listOf(path)

    private fun getNames(amount: Int, pathFormat: String): List<String> {
      val result: MutableList<String> = ArrayList()
      for (i in 1..amount) {
        result.add(String.format(pathFormat, i))
      }
      return result
    }
  }
}
