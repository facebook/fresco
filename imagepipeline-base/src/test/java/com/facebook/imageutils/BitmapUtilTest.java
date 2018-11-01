/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imageutils;

import static org.junit.Assert.*;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Pair;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests {@link BitmapUtil}
 */
@RunWith(RobolectricTestRunner.class)
public class BitmapUtilTest {

  @Test
  public void testGetSizeInBytes() {
    // 0 for null
    assertEquals(0, BitmapUtil.getSizeInBytes(null));
    // 240 * 181 * 4 = 173760
    final Bitmap bitmap1 =
        BitmapFactory.decodeStream(BitmapUtilTest.class.getResourceAsStream("pngs/1.png"));
    assertEquals(173760, BitmapUtil.getSizeInBytes(bitmap1));
    // 240 * 246 * 4 = 236160
    final Bitmap bitmap2 =
        BitmapFactory.decodeStream(BitmapUtilTest.class.getResourceAsStream("pngs/2.png"));
    assertEquals(236160, BitmapUtil.getSizeInBytes(bitmap2));
    // 240 * 180 * 4 = 172800
    final Bitmap bitmap3 =
        BitmapFactory.decodeStream(BitmapUtilTest.class.getResourceAsStream("pngs/3.png"));
    assertEquals(172800, BitmapUtil.getSizeInBytes(bitmap3));
  }

  @Test
  public void testDecodeDimensionsUri_test() {
    assertEquals(new Pair(100, 100), BitmapUtil.decodeDimensions(Uri.parse("pngs/1.png")));
    assertEquals(new Pair(100, 100), BitmapUtil.decodeDimensions(Uri.parse("jpegs/1.jpeg")));
  }

  @Test
  public void testDecodeDimensions_testPngs() {
    assertEquals(
        new Pair(240, 181),
        BitmapUtil.decodeDimensions(BitmapUtilTest.class.getResourceAsStream("pngs/1.png")));
    assertEquals(
        new Pair(240, 246),
        BitmapUtil.decodeDimensions(BitmapUtilTest.class.getResourceAsStream("pngs/2.png")));
    assertEquals(
        new Pair(240, 180),
        BitmapUtil.decodeDimensions(BitmapUtilTest.class.getResourceAsStream("pngs/3.png")));
  }

  @Test
  public void testDecodeDimensions_testJpegs() {
    assertEquals(
        new Pair(240, 181),
        BitmapUtil.decodeDimensions(BitmapUtilTest.class.getResourceAsStream("jpegs/1.jpeg")));
    assertEquals(
        new Pair(240, 93),
        BitmapUtil.decodeDimensions(BitmapUtilTest.class.getResourceAsStream("jpegs/2.jpeg")));
    assertEquals(
        new Pair(240, 240),
        BitmapUtil.decodeDimensions(BitmapUtilTest.class.getResourceAsStream("jpegs/3.jpeg")));
  }

  @Test
  public void testDecodeDimensions_testIncompleteJpegs() {
    assertEquals(
        new Pair(240, 181),
        BitmapUtil.decodeDimensions(BitmapUtilTest.class.getResourceAsStream("jpegs/1cut.jpeg")));
  }

  @Test
  public void testDecodeDimensions_testProgressiveJpegs() {
    assertEquals(
        new Pair(981, 657),
        BitmapUtil.decodeDimensions(BitmapUtilTest.class.getResourceAsStream("jpegs/1prog.jpeg")));
    assertEquals(
        new Pair(800, 531),
        BitmapUtil.decodeDimensions(BitmapUtilTest.class.getResourceAsStream("jpegs/2prog.jpeg")));
  }

  @Test
  public void testDecodeDimensions_testStaticGifs() throws Exception {
    assertEquals(
        new Pair(240, 181),
        BitmapUtil.decodeDimensions(BitmapUtilTest.class.getResourceAsStream("gifs/1.gif")));
    assertEquals(
        new Pair(240, 246),
        BitmapUtil.decodeDimensions(BitmapUtilTest.class.getResourceAsStream("gifs/2.gif")));
    assertEquals(
        new Pair(240, 180),
        BitmapUtil.decodeDimensions(BitmapUtilTest.class.getResourceAsStream("gifs/3.gif")));
  }

  @Test
  public void testDecodeDimensions_testAnimatedGifs() {
    assertEquals(
        new Pair(500, 500),
        BitmapUtil.decodeDimensions(
            BitmapUtilTest.class.getResourceAsStream("animatedgifs/1.gif")));
    assertEquals(
        new Pair(550, 400),
        BitmapUtil.decodeDimensions(
            BitmapUtilTest.class.getResourceAsStream("animatedgifs/2.gif")));
  }

  @Test
  public void testDecodeDimensions_testBmps() throws Exception {
    assertEquals(
        new Pair(240, 181),
        BitmapUtil.decodeDimensions(BitmapUtilTest.class.getResourceAsStream("bmps/1.bmp")));
    assertEquals(
        new Pair(240, 246),
        BitmapUtil.decodeDimensions(BitmapUtilTest.class.getResourceAsStream("bmps/2.bmp")));
    assertEquals(
        new Pair(240, 180),
        BitmapUtil.decodeDimensions(BitmapUtilTest.class.getResourceAsStream("bmps/3.bmp")));
  }

}
