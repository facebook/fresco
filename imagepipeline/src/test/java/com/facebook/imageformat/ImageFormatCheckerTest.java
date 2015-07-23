/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imageformat;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Tests {@link ImageFormatChecker}
 */
@RunWith(RobolectricTestRunner.class)
public class ImageFormatCheckerTest {

  @Test
  public void testSimpleWebps() throws Exception {
    singleImageTypeTest(
        getNames(2, "webps/%d_webp_plain.webp"),
        ImageFormat.WEBP_SIMPLE);
  }

  @Test
  public void testLosslessWebps() throws Exception {
    singleImageTypeTest(
        getNames(5, "webps/%d_webp_ll.webp"),
        ImageFormat.WEBP_LOSSLESS);
  }

  @Test
  public void testExtendedWebpsWithAlpha() throws Exception {
    singleImageTypeTest(
        getNames(5, "webps/%d_webp_ea.webp"),
        ImageFormat.WEBP_EXTENDED_WITH_ALPHA);
  }

  @Test
  public void testExtendedWebpsWithoutAlpha() throws Exception {
    singleImageTypeTest(
        getName("webps/1_webp_e.webp"),
        ImageFormat.WEBP_EXTENDED);
  }

  @Test
  public void testAnimatedWebps() throws Exception {
    singleImageTypeTest(
        getName("webps/1_webp_anim.webp"),
        ImageFormat.WEBP_ANIMATED);
  }

  @Test
  public void testJpegs() throws Exception {
    singleImageTypeTest(
        getNames(5, "jpegs/%d.jpeg"),
        ImageFormat.JPEG);
  }

  @Test
  public void testPngs() throws Exception {
    singleImageTypeTest(
        getNames(5, "pngs/%d.png"),
        ImageFormat.PNG);
  }

  @Test
  public void testGifs() throws Exception {
    singleImageTypeTest(
        getNames(5, "gifs/%d.gif"),
        ImageFormat.GIF);
  }

  @Test
  public void testBmps() throws Exception {
    singleImageTypeTest(
        getNames(5, "bmps/%d.bmp"),
        ImageFormat.BMP);
  }

  private void singleImageTypeTest(
      final List<String> resourceNames,
      final ImageFormat expectedImageType)
      throws Exception {
    for (String name : resourceNames) {
      final InputStream resourceStream = getResourceStream(name);
      try {
        assertSame(
            "failed with resource: " + name,
            expectedImageType,
            ImageFormatChecker.getImageFormat(resourceStream));
      } finally {
        resourceStream.close();
      }
    }
  }

  private static List<String> getName(String path) {
    return Arrays.asList(path);
  }

  private static List<String> getNames(int amount, String pathFormat) {
    List<String> result = new ArrayList<>();
    for (int i = 1; i <= amount; ++i) {
      result.add(String.format(pathFormat, i));
    }
    return result;
  }

  private InputStream getResourceStream(String name) throws IOException {
    InputStream is = ImageFormatCheckerTest.class.getResourceAsStream(name);
    assertNotNull("failed to read resource: " + name, is);
    return is;
  }
}
