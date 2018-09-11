/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imageutils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.*;

import android.util.Pair;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests {@link WebpUtil}
 */
@RunWith(RobolectricTestRunner.class)
public class WebPUtilTest {

  @Test
  public void testSimpleWebps() throws Exception {
    checkImage("webps/1_webp_plain.webp", 320, 214);
    checkImage("webps/2_webp_plain.webp", 320, 235);
  }

  @Test
  public void testLosslessWebps() throws Exception {
    checkImage("webps/1_webp_ll.webp", 400, 301);
    checkImage("webps/2_webp_ll.webp", 386, 395);
    checkImage("webps/3_webp_ll.webp", 800, 600);
    checkImage("webps/4_webp_ll.webp", 421, 163);
    checkImage("webps/5_webp_ll.webp", 300, 300);
  }

  @Test
  public void testExtendedWebpsExtendedWithAlpha() throws Exception {
    checkImage("webps/1_webp_ea.webp", 400, 301);
    checkImage("webps/2_webp_ea.webp", 386, 395);
    checkImage("webps/3_webp_ea.webp", 800, 600);
    checkImage("webps/4_webp_ea.webp", 421, 163);
    checkImage("webps/5_webp_ea.webp", 300, 300);
  }

  @Test
  public void testExtendedWebpsExtendedNoAlpha() throws Exception {
    checkImage("webps/1_webp_e.webp", 480, 320);
  }

  @Test
  public void testExtendedWebpsAnimated() throws Exception {
    checkImage("webps/1_webp_anim.webp", 322, 477);
  }

  private InputStream getResourceStream(String name) throws IOException {
    InputStream is = WebPUtilTest.class.getResourceAsStream(name);
    assertNotNull("failed to read resource: " + name, is);
    return is;
  }

  /**
   * Contains the logic in order to test the size of an image
   *
   * @param imagePath The Path of the given image
   * @param expectedWidth The expected width
   * @param expectedHeight The expected height
   * @throws IOException In case of errors
   */
  private void checkImage(
      final String imagePath,
      int expectedWidth,
      int expectedHeight)
      throws IOException {
    Pair<Integer, Integer> size = WebpUtil.getSize(getResourceStream(imagePath));
    assertNotNull("Something went wrong during parsing WebP! " + imagePath, size);
    assertEquals(expectedWidth, size.first.intValue());
    assertEquals(expectedHeight, size.second.intValue());
  }
}
