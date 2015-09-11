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
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.request.ImageRequest;

import org.junit.*;
import org.junit.runner.*;
import org.junit.runners.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(JUnit4.class)
public class DownsampleUtilTest {

  private ImageRequest mImageRequest;
  private CloseableReference<PooledByteBuffer> bufferRef;

  @Before
  public void setUp() {
    bufferRef = CloseableReference.of(mock(PooledByteBuffer.class));
    mImageRequest = mock(ImageRequest.class);
    when(mImageRequest.getAutoRotateEnabled()).thenReturn(true);
    Uri uri = mock(Uri.class);
    when(uri.toString()).thenReturn("test");
    when(mImageRequest.getSourceUri()).thenReturn(uri);
  }

  @Test
  public void testDetermineSampleSize_NullResizeOptions() {
    ResizeOptions resizeOptions;
    EncodedImage encodedImage = new EncodedImage(bufferRef);
    encodedImage.setRotationAngle(0);
    encodedImage.setWidth(0);
    encodedImage.setHeight(0);
    // Null resizeOptions
    assertEquals(1, DownsampleUtil.determineSampleSize(mImageRequest, encodedImage));
  }

  @Test
  public void testDetermineSampleSize_NoEncodedImageDimensions() {
    EncodedImage encodedImage = new EncodedImage(bufferRef);
    encodedImage.setRotationAngle(0);
    encodedImage.setWidth(0);
    encodedImage.setHeight(0);
    ResizeOptions resizeOptions = new ResizeOptions(1, 1);
    when(mImageRequest.getResizeOptions()).thenReturn(resizeOptions);
    assertEquals(1, DownsampleUtil.determineSampleSize(mImageRequest, encodedImage));

    // Width or height of the encoded image are 0
    encodedImage.setWidth(100);
    assertEquals(1, DownsampleUtil.determineSampleSize(mImageRequest, encodedImage));
    encodedImage.setWidth(0);
    encodedImage.setHeight(100);
    assertEquals(1, DownsampleUtil.determineSampleSize(mImageRequest, encodedImage));
  }

  @Test
  public void testDetermineSampleSize_JPEG() {
    EncodedImage encodedImage = new EncodedImage(bufferRef);
    encodedImage.setRotationAngle(0);
    encodedImage.setWidth(100);
    encodedImage.setHeight(100);
    ResizeOptions resizeOptions = new ResizeOptions(50, 50);
    when(mImageRequest.getResizeOptions()).thenReturn(resizeOptions);
    assertEquals(2, DownsampleUtil.determineSampleSize(mImageRequest, encodedImage));
    resizeOptions = new ResizeOptions(50, 25);
    when(mImageRequest.getResizeOptions()).thenReturn(resizeOptions);
    assertEquals(2, DownsampleUtil.determineSampleSize(mImageRequest, encodedImage));
  }

  @Test
  public void testDetermineSampleSize_PNG() {
    EncodedImage encodedImage = new EncodedImage(bufferRef);
    encodedImage.setImageFormat(ImageFormat.PNG);
    ResizeOptions resizeOptions = new ResizeOptions(50, 50);
    encodedImage.setRotationAngle(0);
    encodedImage.setWidth(150);
    encodedImage.setHeight(150);
    when(mImageRequest.getResizeOptions()).thenReturn(resizeOptions);
    assertEquals(3, DownsampleUtil.determineSampleSize(mImageRequest, encodedImage));
  }

  @Test
  public void testDetermineSampleSize_WithRotation() {
    EncodedImage encodedImage = new EncodedImage(bufferRef);
    encodedImage.setRotationAngle(90);
    encodedImage.setWidth(50);
    encodedImage.setHeight(100);

    ResizeOptions resizeOptions = new ResizeOptions(50, 25);
    when(mImageRequest.getResizeOptions()).thenReturn(resizeOptions);
    assertEquals(2, DownsampleUtil.determineSampleSize(mImageRequest, encodedImage));
    resizeOptions = new ResizeOptions(25, 50);
    when(mImageRequest.getResizeOptions()).thenReturn(resizeOptions);
    assertEquals(1, DownsampleUtil.determineSampleSize(mImageRequest, encodedImage));
  }

  @Test
  public void testDetermineSampleSize_OverMaxPossibleSize() {
    EncodedImage encodedImage = new EncodedImage(bufferRef);
    encodedImage.setRotationAngle(0);
    encodedImage.setWidth(4000);
    encodedImage.setHeight(4000);

    ResizeOptions resizeOptions = new ResizeOptions(4000, 4000);
    when(mImageRequest.getResizeOptions()).thenReturn(resizeOptions);
    assertEquals(2, DownsampleUtil.determineSampleSize(mImageRequest, encodedImage));
    encodedImage.setWidth(8000);
    encodedImage.setHeight(8000);
    resizeOptions = new ResizeOptions(8000, 8000);
    when(mImageRequest.getResizeOptions()).thenReturn(resizeOptions);
    assertEquals(4, DownsampleUtil.determineSampleSize(mImageRequest, encodedImage));
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
}
