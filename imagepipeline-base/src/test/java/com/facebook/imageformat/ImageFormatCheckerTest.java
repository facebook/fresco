/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageformat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import com.facebook.soloader.SoLoader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests {@link ImageFormatChecker}
 */
@RunWith(RobolectricTestRunner.class)
public class ImageFormatCheckerTest {

  static {
    SoLoader.setInTestMode();
  }

  @Test
  public void testSimpleWebps() throws Exception {
    singleImageTypeTest(
        getNames(2, "webps/%d_webp_plain.webp"),
        DefaultImageFormats.WEBP_SIMPLE);
  }

  @Test
  public void testLosslessWebps() throws Exception {
    singleImageTypeTest(
        getNames(5, "webps/%d_webp_ll.webp"),
        DefaultImageFormats.WEBP_LOSSLESS);
  }

  @Test
  public void testExtendedWebpsWithAlpha() throws Exception {
    singleImageTypeTest(
        getNames(5, "webps/%d_webp_ea.webp"),
        DefaultImageFormats.WEBP_EXTENDED_WITH_ALPHA);
  }

  @Test
  public void testExtendedWebpsWithoutAlpha() throws Exception {
    singleImageTypeTest(
        getName("webps/1_webp_e.webp"),
        DefaultImageFormats.WEBP_EXTENDED);
  }

  @Test
  public void testAnimatedWebps() throws Exception {
    singleImageTypeTest(
        getName("webps/1_webp_anim.webp"),
        DefaultImageFormats.WEBP_ANIMATED);
  }

  @Test
  public void testJpegs() throws Exception {
    singleImageTypeTest(
        getNames(5, "jpegs/%d.jpeg"),
        DefaultImageFormats.JPEG);
  }

  @Test
  public void testPngs() throws Exception {
    singleImageTypeTest(
        getNames(5, "pngs/%d.png"),
        DefaultImageFormats.PNG);
  }

  @Test
  public void testGifs() throws Exception {
    singleImageTypeTest(
        getNames(5, "gifs/%d.gif"),
        DefaultImageFormats.GIF);
  }

  @Test
  public void testBmps() throws Exception {
    singleImageTypeTest(
        getNames(5, "bmps/%d.bmp"),
        DefaultImageFormats.BMP);
  }

  @Test
  public void testHeifs() throws Exception {
    singleImageTypeTest(getName("heifs/1.heif"), DefaultImageFormats.HEIF);
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
