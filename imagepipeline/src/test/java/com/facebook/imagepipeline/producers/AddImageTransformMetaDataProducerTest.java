/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.io.InputStream;

import android.util.Pair;

import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imageutils.BitmapUtil;
import com.facebook.imageutils.JfifUtil;

import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.Mock;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.powermock.api.mockito.*;
import org.powermock.core.classloader.annotations.*;
import org.powermock.modules.junit4.rule.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest({ImageFormatChecker.class, JfifUtil.class, BitmapUtil.class})
@Config(manifest= Config.NONE)
public class AddImageTransformMetaDataProducerTest {
  @Mock public Producer<EncodedImage> mInputProducer;
  @Mock public Consumer<EncodedImage> mConsumer;
  @Mock public ProducerContext mProducerContext;
  @Mock public Exception mException;

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  private AddImageTransformMetaDataProducer mAddMetaDataProducer;
  private Consumer<EncodedImage> mAddMetaDataConsumer;
  private CloseableReference<PooledByteBuffer> mIntermediateResultBufferRef;
  private CloseableReference<PooledByteBuffer> mFinalResultBufferRef;
  private EncodedImage mIntermediateResult;
  private EncodedImage mFinalResult;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    PowerMockito.mockStatic(ImageFormatChecker.class, JfifUtil.class, BitmapUtil.class);

    mAddMetaDataProducer = new AddImageTransformMetaDataProducer(mInputProducer);

    mIntermediateResultBufferRef = CloseableReference.of(mock(PooledByteBuffer.class));
    mFinalResultBufferRef = CloseableReference.of(mock(PooledByteBuffer.class));

    mIntermediateResult = new EncodedImage(mIntermediateResultBufferRef);
    mFinalResult = new EncodedImage(mFinalResultBufferRef);

    mAddMetaDataConsumer = null;
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            mAddMetaDataConsumer =
                (Consumer<EncodedImage>) invocation.getArguments()[0];
            return null;
          }
        }).when(mInputProducer).produceResults(any(Consumer.class), any(ProducerContext.class));
    mAddMetaDataProducer.produceResults(mConsumer, mProducerContext);
  }

  @Test
  public void testOnNewResultLastNotJpeg() {
    when(ImageFormatChecker.getImageFormat_WrapIOException(any(InputStream.class)))
        .thenReturn(ImageFormat.WEBP_SIMPLE);
    mAddMetaDataConsumer.onNewResult(mFinalResult, true);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(true));
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertTrue(EncodedImage.isValid(encodedImage));
    assertEquals(ImageFormat.WEBP_SIMPLE, encodedImage.getImageFormat());
    assertEquals(-1, encodedImage.getRotationAngle());
    assertEquals(-1, encodedImage.getWidth());
    assertEquals(-1, encodedImage.getHeight());
  }

  @Test
  public void testOnNewResultNotLastNotJpeg() {
    when(ImageFormatChecker.getImageFormat_WrapIOException(any(InputStream.class)))
        .thenReturn(ImageFormat.WEBP_SIMPLE);
    when(BitmapUtil.decodeDimensions(any(InputStream.class))).thenReturn(null);
    mAddMetaDataConsumer.onNewResult(mIntermediateResult, false);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(false));
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertTrue(EncodedImage.isValid(encodedImage));
    assertEquals(ImageFormat.WEBP_SIMPLE, encodedImage.getImageFormat());
    assertEquals(-1, encodedImage.getRotationAngle());
    assertEquals(-1, encodedImage.getWidth());
    assertEquals(-1, encodedImage.getHeight());
  }

  @Test
  public void testOnNewResultNotLast_DimensionsNotFound() {
    int rotationAngle = 180;
    int orientation = 1;
    when(ImageFormatChecker.getImageFormat_WrapIOException(any(InputStream.class)))
        .thenReturn(ImageFormat.JPEG);
    when(JfifUtil.getAutoRotateAngleFromOrientation(orientation)).thenReturn(rotationAngle);
    when(JfifUtil.getOrientation(any(InputStream.class))).thenReturn(orientation);
    when(BitmapUtil.decodeDimensions(any(InputStream.class))).thenReturn(null);
    mAddMetaDataConsumer.onNewResult(mIntermediateResult, false);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(false));
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertTrue(EncodedImage.isValid(encodedImage));
    assertEquals(-1, encodedImage.getRotationAngle());
    assertEquals(-1, encodedImage.getWidth());
    assertEquals(-1, encodedImage.getHeight());
  }

  @Test
  public void testOnNewResultNotLast_RotationNotFound() {
    when(ImageFormatChecker.getImageFormat_WrapIOException(any(InputStream.class)))
        .thenReturn(ImageFormat.JPEG);
    when(JfifUtil.getOrientation(any(InputStream.class))).thenReturn(0);
    mAddMetaDataConsumer.onNewResult(mIntermediateResult, false);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(false));
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertTrue(EncodedImage.isValid(encodedImage));
    assertEquals(-1, encodedImage.getRotationAngle());
    assertEquals(-1, encodedImage.getWidth());
    assertEquals(-1, encodedImage.getHeight());
  }

  @Test
  public void testOnNewResultNotLastAndJpeg() {
    int rotationAngle = 180;
    int orientation = 1;
    int width = 10;
    int height = 20;
    when(ImageFormatChecker.getImageFormat_WrapIOException(any(InputStream.class)))
        .thenReturn(ImageFormat.JPEG);
    when(JfifUtil.getAutoRotateAngleFromOrientation(orientation)).thenReturn(rotationAngle);
    when(JfifUtil.getOrientation(any(InputStream.class))).thenReturn(orientation);
    when(BitmapUtil.decodeDimensions(any(InputStream.class))).thenReturn(new Pair(width, height));
    mAddMetaDataConsumer.onNewResult(mFinalResult, true);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(true));
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertTrue(EncodedImage.isValid(encodedImage));
    assertEquals(ImageFormat.JPEG, encodedImage.getImageFormat());
    assertEquals(rotationAngle, encodedImage.getRotationAngle());
    assertEquals(width, encodedImage.getWidth());
    assertEquals(height, encodedImage.getHeight());
  }

  @Test
  public void testOnNewResultLastAndJpeg() {
    int rotationAngle = 180;
    int orientation = 1;
    int width = 10;
    int height = 20;
    when(ImageFormatChecker.getImageFormat_WrapIOException(any(InputStream.class)))
        .thenReturn(ImageFormat.JPEG);
    when(JfifUtil.getAutoRotateAngleFromOrientation(orientation)).thenReturn(rotationAngle);
    when(JfifUtil.getOrientation(any(InputStream.class))).thenReturn(orientation);
    when(BitmapUtil.decodeDimensions(any(InputStream.class))).thenReturn(new Pair(width, height));
    mAddMetaDataConsumer.onNewResult(mFinalResult, true);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(true));
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertTrue(EncodedImage.isValid(encodedImage));
    assertEquals(ImageFormat.JPEG, encodedImage.getImageFormat());
    assertEquals(rotationAngle, encodedImage.getRotationAngle());
    assertEquals(width, encodedImage.getWidth());
    assertEquals(height, encodedImage.getHeight());
  }

  @Test
  public void testOnFailure() {
    mAddMetaDataConsumer.onFailure(mException);
    verify(mConsumer).onFailure(mException);
  }

  @Test
  public void testOnCancellation() {
    mAddMetaDataConsumer.onCancellation();
    verify(mConsumer).onCancellation();
  }

  @Test
  public void testOnNullResult() {
    mAddMetaDataConsumer.onNewResult(null, true);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(true));
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertEquals(encodedImage, null);
  }
}
