/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import org.junit.Test;

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
    testWithImageBigEnoughForResizeOptions(IMAGE_WIDTHS, IMAGE_HEIGHTS, 0);
  }

  @Test
  public void testWithRotatedImageAndResizeOptionsNotMoreThan133PercentOfActual() {
    testWithImageBigEnoughForResizeOptions(IMAGE_HEIGHTS, IMAGE_WIDTHS, 90);
  }

  private static void testWithImageBigEnoughForResizeOptions(
      int[] imageWidths,
      int[] imageHeights,
      int startRotation) {
    for (int rotation = startRotation; rotation < 360; rotation += 180) {
      for (int i = 0; i < TEST_COUNT; i++) {
        EncodedImage encodedImage = mockImage(imageWidths[i], imageHeights[i], rotation);
        ResizeOptions resizeOptions = new ResizeOptions(REQUEST_WIDTHS[i], REQUEST_HEIGHTS[i]);
        assertTrue(ThumbnailSizeChecker.isImageBigEnough(encodedImage, resizeOptions));
      }
    }
  }

  @Test
  public void testWithImageAndResizeOptionsWithWidthMoreThan133PercentOfActual() {
    testWithImageNotBigEnoughForResizeOptions(IMAGE_WIDTHS, IMAGE_HEIGHTS, 0, 1, 0);
  }

  @Test
  public void testWithImageAndResizeOptionsWithHeightMoreThan133PercentOfActual() {
    testWithImageNotBigEnoughForResizeOptions(IMAGE_WIDTHS, IMAGE_HEIGHTS, 0, 0, 1);
  }

  @Test
  public void testWithRotatedImageAndResizeOptionsWithWidthMoreThan133PercentOfActual() {
    testWithImageNotBigEnoughForResizeOptions(IMAGE_HEIGHTS, IMAGE_WIDTHS, 90, 1, 0);
  }

  @Test
  public void testWithRotatedImageAndResizeOptionsWithHeightMoreThan133PercentOfActual() {
    testWithImageNotBigEnoughForResizeOptions(IMAGE_HEIGHTS, IMAGE_WIDTHS, 90, 0, 1);
  }

  private static void testWithImageNotBigEnoughForResizeOptions(
      int[] imageWidths,
      int[] imageHeights,
      int startRotation,
      int additionalRequestWidth,
      int additionalRequestHeight) {
    for (int rotation = startRotation; rotation < 360; rotation += 180) {
      for (int i = 0; i < TEST_COUNT; i++) {
        ResizeOptions resizeOptions = new ResizeOptions(
            REQUEST_WIDTHS[i] + additionalRequestWidth,
            REQUEST_HEIGHTS[i] + additionalRequestHeight);
        EncodedImage encodedImage = mockImage(imageWidths[i], imageHeights[i], rotation);
        assertFalse(ThumbnailSizeChecker.isImageBigEnough(encodedImage, resizeOptions));
      }
    }
  }

  @Test
  public void testWithLargeEnoughImageWhenNoResizeOptions() {
    for (int rotation = 0; rotation < 360; rotation += 90) {
      assertTrue(ThumbnailSizeChecker.isImageBigEnough(
          mockImage(
          BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
          BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
          rotation),
          null));
    }
  }

  @Test
  public void testImageWithInsufficientWidthWhenNoResizeOptions() {
    for (int rotation = 0; rotation < 360; rotation += 90) {
      EncodedImage mockImage = mockImage(
          BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS - 1,
          BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
          rotation);
      assertFalse(ThumbnailSizeChecker.isImageBigEnough(mockImage, null));
    }
  }

  @Test
  public void testImageWithInsufficientHeightWhenNoResizeOptions() {
    for (int rotation = 0; rotation < 360; rotation += 90) {
      EncodedImage mockImage = mockImage(
          BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
          BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS - 1,
          rotation);
      assertFalse(ThumbnailSizeChecker.isImageBigEnough(mockImage, null));
    }
  }

  private static EncodedImage mockImage(int width, int height, int rotation) {
    EncodedImage image = mock(EncodedImage.class);
    when(image.getWidth()).thenReturn(width);
    when(image.getHeight()).thenReturn(height);
    when(image.getRotationAngle()).thenReturn(rotation);
    return image;
  }
}
