/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import android.net.Uri;

import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.request.ImageRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JUnit4.class)
public class DownsampleUtilTest {

  private ImageRequest mImageRequest;
  private EncodedImage mEncodedImage;

  @Before
  public void setup() {
    mImageRequest = mock(ImageRequest.class);
    Uri uri = mock(Uri.class);
    when(uri.toString()).thenReturn("test");
    when(mImageRequest.getSourceUri()).thenReturn(uri);
  }

  @Test
  public void testDetermineSampleSize_NullResizeOptions() {
    whenImageWidthAndHeight(0, 0);
    // Null resizeOptions
    assertEquals(1, DownsampleUtil.determineSampleSize(mImageRequest, mEncodedImage));
  }

  @Test
  public void testDetermineSampleSize_NoEncodedImageDimensions() {
    whenImageWidthAndHeight(0, 0);
    whenRequestResizeWidthAndHeightWithExifRotation(1, 1);
    assertEquals(1, DownsampleUtil.determineSampleSize(mImageRequest, mEncodedImage));

    // Width or height of the encoded image are 0
    mEncodedImage.setWidth(100);
    assertEquals(1, DownsampleUtil.determineSampleSize(mImageRequest, mEncodedImage));
    mEncodedImage.setWidth(0);
    mEncodedImage.setHeight(100);
    assertEquals(1, DownsampleUtil.determineSampleSize(mImageRequest, mEncodedImage));
  }

  @Test
  public void testDetermineSampleSize_JPEG() {
    whenImageWidthAndHeight(100, 100);
    whenRequestResizeWidthAndHeightWithExifRotation(50, 50);
    assertEquals(2, DownsampleUtil.determineSampleSize(mImageRequest, mEncodedImage));

    whenRequestResizeWidthAndHeightWithExifRotation(50, 25);
    assertEquals(2, DownsampleUtil.determineSampleSize(mImageRequest, mEncodedImage));
  }

  @Test
  public void testDetermineSampleSize_PNG() {
    whenImageWidthAndHeight(150, 150);
    mEncodedImage.setImageFormat(ImageFormat.PNG);
    whenRequestResizeWidthAndHeightWithExifRotation(50, 50);
    assertEquals(3, DownsampleUtil.determineSampleSize(mImageRequest, mEncodedImage));
  }

  @Test
  public void testDetermineSampleSize_WithRotation() {
    whenImageWidthHeightAndRotation(50, 100, 90);

    whenRequestResizeWidthAndHeightWithExifRotation(50, 25);
    assertEquals(2, DownsampleUtil.determineSampleSize(mImageRequest, mEncodedImage));

    whenRequestResizeWidthAndHeightWithExifRotation(25, 50);
    assertEquals(1, DownsampleUtil.determineSampleSize(mImageRequest, mEncodedImage));
  }

  @Test
  public void testDetermineSampleSize_WithRotationForcedByRequest() {
    whenImageWidthAndHeight(50, 100);

    // The rotation angles should be ignored as they're dealt with by the ResizeAndRotateProducer
    // 50,100 -> 50,25 = 1
    whenRequestResizeWidthHeightAndForcedRotation(50, 25, 90);
    assertEquals(1, DownsampleUtil.determineSampleSize(mImageRequest, mEncodedImage));

    // 50,100 -> 25,50 = 2
    whenRequestResizeWidthHeightAndForcedRotation(25, 50, 270);
    assertEquals(2, DownsampleUtil.determineSampleSize(mImageRequest, mEncodedImage));

    // 50,100 -> 10,20 = 5
    whenRequestResizeWidthHeightAndForcedRotation(10, 20, 180);
    assertEquals(5, DownsampleUtil.determineSampleSize(mImageRequest, mEncodedImage));
  }

  @Test
  public void testDetermineSampleSize_OverMaxPossibleSize() {
    whenImageWidthAndHeight(4000, 4000);

    whenRequestResizeWidthAndHeightWithExifRotation(4000, 4000);
    assertEquals(2, DownsampleUtil.determineSampleSize(mImageRequest, mEncodedImage));

    whenImageWidthAndHeight(8000, 8000);
    whenRequestResizeWidthAndHeightWithExifRotation(8000, 8000);
    assertEquals(4, DownsampleUtil.determineSampleSize(mImageRequest, mEncodedImage));
  }

  @Test
  public void testRatioToSampleSize() {
    assertEquals(1, DownsampleUtil.ratioToSampleSize(1.000f));
    assertEquals(1, DownsampleUtil.ratioToSampleSize(0.667f));
    assertEquals(2, DownsampleUtil.ratioToSampleSize(0.665f));
    assertEquals(2, DownsampleUtil.ratioToSampleSize(0.389f));
    assertEquals(3, DownsampleUtil.ratioToSampleSize(0.387f));
    assertEquals(3, DownsampleUtil.ratioToSampleSize(0.278f));
    assertEquals(4, DownsampleUtil.ratioToSampleSize(0.276f));
    assertEquals(4, DownsampleUtil.ratioToSampleSize(0.2167f));
    assertEquals(5, DownsampleUtil.ratioToSampleSize(0.2165f));
    assertEquals(5, DownsampleUtil.ratioToSampleSize(0.1778f));
    assertEquals(6, DownsampleUtil.ratioToSampleSize(0.1776f));
    assertEquals(6, DownsampleUtil.ratioToSampleSize(0.1508f));
    assertEquals(7, DownsampleUtil.ratioToSampleSize(0.1506f));
    assertEquals(7, DownsampleUtil.ratioToSampleSize(0.131f));
    assertEquals(8, DownsampleUtil.ratioToSampleSize(0.1308f));
  }

  @Test
  public void testRatioToSampleSizeJPEG() {
    assertEquals(1, DownsampleUtil.ratioToSampleSizeJPEG(1.000f));
    assertEquals(1, DownsampleUtil.ratioToSampleSizeJPEG(0.667f));
    assertEquals(2, DownsampleUtil.ratioToSampleSizeJPEG(0.665f));
    assertEquals(2, DownsampleUtil.ratioToSampleSizeJPEG(0.334f));
    assertEquals(4, DownsampleUtil.ratioToSampleSizeJPEG(0.332f));
    assertEquals(4, DownsampleUtil.ratioToSampleSizeJPEG(0.1667f));
    assertEquals(8, DownsampleUtil.ratioToSampleSizeJPEG(0.1665f));
    assertEquals(8, DownsampleUtil.ratioToSampleSizeJPEG(0.0834f));
    assertEquals(16, DownsampleUtil.ratioToSampleSizeJPEG(0.0832f));
  }

  @Test
  public void testRoundToPowerOfTwo() {
    assertEquals(1, DownsampleUtil.roundToPowerOfTwo(1));
    assertEquals(2, DownsampleUtil.roundToPowerOfTwo(2));
    assertEquals(4, DownsampleUtil.roundToPowerOfTwo(3));
    assertEquals(4, DownsampleUtil.roundToPowerOfTwo(4));
    assertEquals(8, DownsampleUtil.roundToPowerOfTwo(5));
    assertEquals(8, DownsampleUtil.roundToPowerOfTwo(6));
    assertEquals(8, DownsampleUtil.roundToPowerOfTwo(7));
    assertEquals(8, DownsampleUtil.roundToPowerOfTwo(8));
  }

  private void whenImageWidthAndHeight(int width, int height) {
    whenImageWidthHeightAndRotation(width, height, 0);
  }

  private void whenImageWidthHeightAndRotation(int width, int height, int rotationAngle) {
    mEncodedImage = new EncodedImage(CloseableReference.of(mock(PooledByteBuffer.class)));
    mEncodedImage.setWidth(width);
    mEncodedImage.setHeight(height);
    mEncodedImage.setRotationAngle(rotationAngle);
  }

  private void whenRequestResizeWidthAndHeightWithExifRotation(int width, int height) {
    whenRequestResizeWidthHeightAndForcedRotation(width, height, -1);
  }

  private void whenRequestResizeWidthHeightAndForcedRotation(
      int width,
      int height,
      int rotationAngle) {
    when(mImageRequest.getPreferredWidth()).thenReturn(width);
    when(mImageRequest.getPreferredHeight()).thenReturn(height);
    when(mImageRequest.getResizeOptions()).thenReturn(new ResizeOptions(width, height));
    when(mImageRequest.getRotationOptions())
        .thenReturn(RotationOptions.forceRotation(rotationAngle));
  }
}
