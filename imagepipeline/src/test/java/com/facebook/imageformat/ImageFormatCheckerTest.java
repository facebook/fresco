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
import java.util.List;

import com.facebook.common.internal.Lists;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Tests {@link ImageFormatChecker}
 */
@RunWith(RobolectricTestRunner.class)
public class ImageFormatCheckerTest {

  @Test
  public void testSimpleWebps() throws Exception {
    singleImageTypeTest(getSimpleWebpNames(), ImageFormat.WEBP_SIMPLE);
  }

  @Test
  public void testLosslessWebps() throws Exception {
    singleImageTypeTest(getLosslessWebpNames(), ImageFormat.WEBP_LOSSLESS);
  }

  @Test
  public void testExtendedWebpsWithAlpha() throws Exception {
    singleImageTypeTest(getExtendedWebpWithAlphaNames(), ImageFormat.WEBP_EXTENDED_WITH_ALPHA);
  }

  @Test
  public void testExtendedWebpsWithoutAlpha() throws Exception {
    singleImageTypeTest(getExtendedWebpWithoutAlphaNames(), ImageFormat.WEBP_EXTENDED);
  }

  @Test
  public void testAnimatedWebps() throws Exception {
    singleImageTypeTest(getAnimatedWebpNames(), ImageFormat.WEBP_ANIMATED);
  }

  @Test
  public void testJpegs() throws Exception {
    singleImageTypeTest(getJpegNames(), ImageFormat.JPEG);
  }

  @Test
  public void testPngs() throws Exception {
    singleImageTypeTest(getPngNames(), ImageFormat.PNG);
  }

  @Test
  public void testGifs() throws Exception {
    singleImageTypeTest(getGifsNames(), ImageFormat.GIF);
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

  private List<String> getSimpleWebpNames() {
    List<String> result = Lists.newArrayList();
    for (int i = 1; i <= 2; ++i) {
      result.add(String.format("webps/%d_webp_plain.webp", i));
    }
    return result;
  }

  private List<String> getLosslessWebpNames() {
    List<String> result = Lists.newArrayList();
    for (int i = 1; i <= 5; ++i) {
      result.add(String.format("webps/%d_webp_ll.webp", i));
    }
    return result;
  }

  private List<String> getExtendedWebpWithoutAlphaNames() {
    return Lists.newArrayList("webps/1_webp_e.webp");
  }

  private List<String> getExtendedWebpWithAlphaNames() {
    List<String> result = Lists.newArrayList();
    for (int i = 1; i <= 5; ++i) {
      result.add(String.format("webps/%d_webp_ea.webp", i));
    }
    return result;
  }

  private List<String> getAnimatedWebpNames() {
    return Lists.newArrayList("webps/1_webp_anim.webp");
  }

  private List<String> getJpegNames() {
    List<String> result = Lists.newArrayList();
    for (int i = 1; i <= 5; ++i) {
      result.add(String.format("jpegs/%d.jpeg", i));
    }
    return result;
  }

  private List<String> getPngNames() {
    List<String> result = Lists.newArrayList();
    for (int i = 1; i <= 5; ++i) {
      result.add(String.format("pngs/%d.png", i));
    }
    return result;
  }

  private List<String> getGifsNames() {
    List<String> result = Lists.newArrayList();
    for (int i = 1; i <= 5; ++i) {
      result.add(String.format("gifs/%d.gif", i));
    }
    return result;
  }

  private InputStream getResourceStream(String name) throws IOException {
    InputStream is = ImageFormatCheckerTest.class.getResourceAsStream(name);
    assertNotNull("failed to read resource: " + name, is);
    return is;
  }
}
