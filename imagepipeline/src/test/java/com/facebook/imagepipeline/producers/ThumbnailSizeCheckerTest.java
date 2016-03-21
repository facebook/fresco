/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.EncodedImage;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ThumbnailSizeCheckerTest {

  private static final int BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS = 1536;

  private static final int[] IMAGE_WIDTHS = { 75, 300, 450 };
  private static final int[] IMAGE_HEIGHTS = { 150, 300, 100 };
  private static final int[] REQUEST_WIDTHS = { 100, 400, 600 };
  private static final int[] REQUEST_HEIGHTS = { 200, 400, 133 };
  private static final int TEST_COUNT = IMAGE_WIDTHS.length;

  @Test
  public void testWithWidthAndHeightAndResizeOptionsNotMoreThan133PercentOfActual() {
    for (int i = 0; i < TEST_COUNT; i++) {
      ResizeOptions resizeOptions = new ResizeOptions(REQUEST_WIDTHS[i], REQUEST_HEIGHTS[i]);
      assertTrue(ThumbnailSizeChecker
          .isImageBigEnough(IMAGE_WIDTHS[i], IMAGE_HEIGHTS[i], resizeOptions));
    }
  }

  @Test
  public void testWithWidthAndHeightAndResizeOptionsWithWidthMoreThan133PercentOfActual() {
    testWithWidthAndHeightNotBigEnoughForResizeOptions(1, 0);
  }

  @Test
  public void testWithWidthAndHeightAndResizeOptionsWithHeightMoreThan133PercentOfActual() {
    testWithWidthAndHeightNotBigEnoughForResizeOptions(0, 1);
  }

  private static void testWithWidthAndHeightNotBigEnoughForResizeOptions(
      int additionalRequestWidth,
      int additionalRequestHeight) {
    for (int i = 0; i < TEST_COUNT; i++) {
      ResizeOptions resizeOptions = new ResizeOptions(
          REQUEST_WIDTHS[i] + additionalRequestWidth,
          REQUEST_HEIGHTS[i] + additionalRequestHeight);
      assertFalse(ThumbnailSizeChecker
          .isImageBigEnough(IMAGE_WIDTHS[i], IMAGE_HEIGHTS[i], resizeOptions));
    }
  }

  @Test
  public void testWithLargeEnoughWidthAndHeightWhenNoResizeOptions() {
    assertTrue(ThumbnailSizeChecker.isImageBigEnough(
        BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
        BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
        null));
  }

  @Test
  public void testWithInsufficientWidthWhenNoResizeOptions() {
    assertFalse(ThumbnailSizeChecker.isImageBigEnough(
        BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS - 1,
        BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
        null));
  }

  @Test
  public void testWithInsufficientHeightWhenNoResizeOptions() {
    assertFalse(ThumbnailSizeChecker.isImageBigEnough(
        BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
        BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS - 1,
        null));
  }

  @Test
  public void testWithImageAndResizeOptionsNotMoreThan133PercentOfActual() {
    for (int i = 0; i < TEST_COUNT; i++) {
      EncodedImage encodedImage = mockImage(IMAGE_WIDTHS[i], IMAGE_HEIGHTS[i]);
      ResizeOptions resizeOptions = new ResizeOptions(REQUEST_WIDTHS[i], REQUEST_HEIGHTS[i]);
      assertTrue(ThumbnailSizeChecker.isImageBigEnough(encodedImage, resizeOptions));
    }
  }

  @Test
  public void testWithImageAndResizeOptionsWithWidthMoreThan133PercentOfActual() {
    testWithImageNotBigEnoughForResizeOptions(1, 0);
  }

  @Test
  public void testWithImageAndResizeOptionsWithHeightMoreThan133PercentOfActual() {
    testWithImageNotBigEnoughForResizeOptions(0, 1);
  }

  private static void testWithImageNotBigEnoughForResizeOptions(
      int additionalRequestWidth,
      int additionalRequestHeight) {
    for (int i = 0; i < TEST_COUNT; i++) {
      ResizeOptions resizeOptions = new ResizeOptions(
          REQUEST_WIDTHS[i] + additionalRequestWidth,
          REQUEST_HEIGHTS[i] + additionalRequestHeight);
      EncodedImage encodedImage = mockImage(IMAGE_WIDTHS[i], IMAGE_HEIGHTS[i]);
      assertFalse(ThumbnailSizeChecker.isImageBigEnough(encodedImage, resizeOptions));
    }
  }

  @Test
  public void testWithLargeEnoughImageWhenNoResizeOptions() {
    assertTrue(ThumbnailSizeChecker.isImageBigEnough(
        mockImage(BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS, BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS),
        null));
  }

  @Test
  public void testImageWithInsufficientWidthWhenNoResizeOptions() {
    assertFalse(ThumbnailSizeChecker.isImageBigEnough(
        mockImage(BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS - 1, BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS),
        null));
  }

  @Test
  public void testImageWithInsufficientHeightWhenNoResizeOptions() {
    assertFalse(ThumbnailSizeChecker.isImageBigEnough(
        mockImage(BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS, BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS - 1),
        null));
  }

  private static EncodedImage mockImage(int width, int height) {
    EncodedImage image = mock(EncodedImage.class);
    when(image.getWidth()).thenReturn(width);
    when(image.getHeight()).thenReturn(height);
    return image;
  }
}
