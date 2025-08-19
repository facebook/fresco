/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers

import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.producers.ThumbnailSizeChecker.isImageBigEnough
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito

class ThumbnailSizeCheckerTest {
  @Test
  fun testWithWidthAndHeightAndResizeOptionsNotMoreThan133PercentOfActual() {
    for (i in 0..<TEST_COUNT) {
      val resizeOptions = ResizeOptions(REQUEST_WIDTHS[i], REQUEST_HEIGHTS[i])
      assertThat(isImageBigEnough(IMAGE_WIDTHS[i], IMAGE_HEIGHTS[i], resizeOptions)).isTrue()
    }
  }

  @Test
  fun testWithWidthAndHeightAndResizeOptionsWithWidthMoreThan133PercentOfActual() {
    testWithWidthAndHeightNotBigEnoughForResizeOptions(1, 0)
  }

  @Test
  fun testWithWidthAndHeightAndResizeOptionsWithHeightMoreThan133PercentOfActual() {
    testWithWidthAndHeightNotBigEnoughForResizeOptions(0, 1)
  }

  @Test
  fun testWithLargeEnoughWidthAndHeightWhenNoResizeOptions() {
    assertThat(
            isImageBigEnough(
                BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
                BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
                null,
            ))
        .isTrue()
  }

  @Test
  fun testWithInsufficientWidthWhenNoResizeOptions() {
    assertThat(
            isImageBigEnough(
                BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS - 1,
                BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
                null,
            ))
        .isFalse()
  }

  @Test
  fun testWithInsufficientHeightWhenNoResizeOptions() {
    assertThat(
            isImageBigEnough(
                BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
                BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS - 1,
                null,
            ))
        .isFalse()
  }

  @Test
  fun testWithImageAndResizeOptionsNotMoreThan133PercentOfActual() {
    testWithImageBigEnoughForResizeOptions(IMAGE_WIDTHS, IMAGE_HEIGHTS, 0)
  }

  @Test
  fun testWithRotatedImageAndResizeOptionsNotMoreThan133PercentOfActual() {
    testWithImageBigEnoughForResizeOptions(IMAGE_HEIGHTS, IMAGE_WIDTHS, 90)
  }

  @Test
  fun testWithImageAndResizeOptionsWithWidthMoreThan133PercentOfActual() {
    testWithImageNotBigEnoughForResizeOptions(IMAGE_WIDTHS, IMAGE_HEIGHTS, 0, 1, 0)
  }

  @Test
  fun testWithImageAndResizeOptionsWithHeightMoreThan133PercentOfActual() {
    testWithImageNotBigEnoughForResizeOptions(IMAGE_WIDTHS, IMAGE_HEIGHTS, 0, 0, 1)
  }

  @Test
  fun testWithRotatedImageAndResizeOptionsWithWidthMoreThan133PercentOfActual() {
    testWithImageNotBigEnoughForResizeOptions(IMAGE_HEIGHTS, IMAGE_WIDTHS, 90, 1, 0)
  }

  @Test
  fun testWithRotatedImageAndResizeOptionsWithHeightMoreThan133PercentOfActual() {
    testWithImageNotBigEnoughForResizeOptions(IMAGE_HEIGHTS, IMAGE_WIDTHS, 90, 0, 1)
  }

  @Test
  fun testWithLargeEnoughImageWhenNoResizeOptions() {
    for (rotation in 0 until 360 step 90) {
      assertThat(
              isImageBigEnough(
                  mockImage(
                      BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
                      BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
                      rotation,
                  ),
                  null,
              ))
          .isTrue()
    }
  }

  @Test
  fun testImageWithInsufficientWidthWhenNoResizeOptions() {
    var rotation = 0
    for (rotation in 0 until 360 step 90) {
      val mockImage: EncodedImage =
          mockImage(
              BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS - 1,
              BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
              rotation,
          )
      assertThat(isImageBigEnough(mockImage, null)).isFalse()
    }
  }

  @Test
  fun testImageWithInsufficientHeightWhenNoResizeOptions() {
    for (rotation in 0 until 360 step 90) {
      val mockImage: EncodedImage =
          mockImage(
              BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS,
              BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS - 1,
              rotation,
          )
      assertThat(isImageBigEnough(mockImage, null)).isFalse()
    }
  }

  companion object {
    private const val BIG_ENOUGH_SIZE_FOR_NO_RESIZE_OPTIONS = 1536

    private val IMAGE_WIDTHS = intArrayOf(75, 300, 450)
    private val IMAGE_HEIGHTS = intArrayOf(150, 300, 100)
    private val REQUEST_WIDTHS = intArrayOf(100, 400, 600)
    private val REQUEST_HEIGHTS = intArrayOf(200, 400, 133)
    private val TEST_COUNT: Int = IMAGE_WIDTHS.size

    private fun testWithWidthAndHeightNotBigEnoughForResizeOptions(
        additionalRequestWidth: Int,
        additionalRequestHeight: Int,
    ) {
      for (i in 0..<TEST_COUNT) {
        val resizeOptions =
            ResizeOptions(
                REQUEST_WIDTHS[i] + additionalRequestWidth,
                REQUEST_HEIGHTS[i] + additionalRequestHeight,
            )
        assertThat(isImageBigEnough(IMAGE_WIDTHS[i], IMAGE_HEIGHTS[i], resizeOptions)).isFalse()
      }
    }

    private fun testWithImageBigEnoughForResizeOptions(
        imageWidths: IntArray,
        imageHeights: IntArray,
        startRotation: Int,
    ) {
      for (rotation in startRotation until 360 step 180) {
        for (i in 0..<TEST_COUNT) {
          val encodedImage: EncodedImage = mockImage(imageWidths[i], imageHeights[i], rotation)
          val resizeOptions = ResizeOptions(REQUEST_WIDTHS[i], REQUEST_HEIGHTS[i])
          assertThat(isImageBigEnough(encodedImage, resizeOptions)).isTrue()
        }
      }
    }

    private fun testWithImageNotBigEnoughForResizeOptions(
        imageWidths: IntArray,
        imageHeights: IntArray,
        startRotation: Int,
        additionalRequestWidth: Int,
        additionalRequestHeight: Int,
    ) {
      for (rotation in startRotation until 360 step 180) {
        for (i in 0..<TEST_COUNT) {
          val resizeOptions =
              ResizeOptions(
                  REQUEST_WIDTHS[i] + additionalRequestWidth,
                  REQUEST_HEIGHTS[i] + additionalRequestHeight,
              )
          val encodedImage: EncodedImage = mockImage(imageWidths[i], imageHeights[i], rotation)
          assertThat(isImageBigEnough(encodedImage, resizeOptions)).isFalse()
        }
      }
    }

    private fun mockImage(width: Int, height: Int, rotation: Int): EncodedImage {
      val image = Mockito.mock<EncodedImage>(EncodedImage::class.java)
      Mockito.`when`<Int?>(image.getWidth()).thenReturn(width)
      Mockito.`when`<Int?>(image.getHeight()).thenReturn(height)
      Mockito.`when`<Int?>(image.getRotationAngle()).thenReturn(rotation)
      return image
    }
  }
}
