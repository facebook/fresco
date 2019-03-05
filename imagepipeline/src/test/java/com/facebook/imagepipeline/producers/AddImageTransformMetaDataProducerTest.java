/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;

import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imageutils.BitmapUtil;
import com.facebook.imageutils.ImageMetaData;
import com.facebook.imageutils.JfifUtil;
import java.io.InputStream;
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

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
@PrepareForTest({ImageFormatChecker.class, JfifUtil.class, BitmapUtil.class})
@Config(manifest = Config.NONE)
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
        .thenReturn(DefaultImageFormats.WEBP_SIMPLE);
    mAddMetaDataConsumer.onNewResult(mFinalResult, Consumer.IS_LAST);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(Consumer.IS_LAST));
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertTrue(EncodedImage.isValid(encodedImage));
    assertEquals(DefaultImageFormats.WEBP_SIMPLE, encodedImage.getImageFormat());
    assertEquals(0, encodedImage.getRotationAngle());
    assertEquals(-1, encodedImage.getWidth());
    assertEquals(-1, encodedImage.getHeight());
  }

  @Test
  public void testOnNewResultNotLastNotJpeg() {
    when(ImageFormatChecker.getImageFormat_WrapIOException(any(InputStream.class)))
        .thenReturn(DefaultImageFormats.WEBP_SIMPLE);
    when(BitmapUtil.decodeDimensions(any(InputStream.class))).thenReturn(null);
    mAddMetaDataConsumer.onNewResult(mIntermediateResult, Consumer.NO_FLAGS);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(Consumer.NO_FLAGS));
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertTrue(EncodedImage.isValid(encodedImage));
    assertEquals(DefaultImageFormats.WEBP_SIMPLE, encodedImage.getImageFormat());
    assertEquals(0, encodedImage.getRotationAngle());
    assertEquals(-1, encodedImage.getWidth());
    assertEquals(-1, encodedImage.getHeight());
  }

  @Test
  public void testOnNewResultNotLast_DimensionsNotFound() {
    int rotationAngle = 180;
    int orientation = 1;
    when(ImageFormatChecker.getImageFormat_WrapIOException(any(InputStream.class)))
        .thenReturn(DefaultImageFormats.JPEG);
    when(JfifUtil.getAutoRotateAngleFromOrientation(orientation)).thenReturn(rotationAngle);
    when(JfifUtil.getOrientation(any(InputStream.class))).thenReturn(orientation);
    when(BitmapUtil.decodeDimensionsAndColorSpace(any(InputStream.class)))
        .thenReturn(new ImageMetaData(-1, -1, null));
    mAddMetaDataConsumer.onNewResult(mIntermediateResult, Consumer.NO_FLAGS);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(Consumer.NO_FLAGS));
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertTrue(EncodedImage.isValid(encodedImage));
    assertEquals(-1, encodedImage.getRotationAngle());
    assertEquals(-1, encodedImage.getWidth());
    assertEquals(-1, encodedImage.getHeight());
  }

  @Test
  public void testOnNewResultNotLast_RotationNotFound() {
    when(ImageFormatChecker.getImageFormat_WrapIOException(any(InputStream.class)))
        .thenReturn(DefaultImageFormats.JPEG);
    when(JfifUtil.getOrientation(any(InputStream.class))).thenReturn(0);
    when(BitmapUtil.decodeDimensionsAndColorSpace(any(InputStream.class)))
        .thenReturn(new ImageMetaData(-1, -1, null));
    mAddMetaDataConsumer.onNewResult(mIntermediateResult, Consumer.NO_FLAGS);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(Consumer.NO_FLAGS));
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
        .thenReturn(DefaultImageFormats.JPEG);
    when(JfifUtil.getAutoRotateAngleFromOrientation(orientation)).thenReturn(rotationAngle);
    when(JfifUtil.getOrientation(any(InputStream.class))).thenReturn(orientation);
    when(BitmapUtil.decodeDimensionsAndColorSpace(any(InputStream.class)))
        .thenReturn(new ImageMetaData(width, height, null));
    mAddMetaDataConsumer.onNewResult(mFinalResult, Consumer.IS_LAST);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(Consumer.IS_LAST));
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertTrue(EncodedImage.isValid(encodedImage));
    assertEquals(DefaultImageFormats.JPEG, encodedImage.getImageFormat());
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
        .thenReturn(DefaultImageFormats.JPEG);
    when(JfifUtil.getAutoRotateAngleFromOrientation(orientation)).thenReturn(rotationAngle);
    when(JfifUtil.getOrientation(any(InputStream.class))).thenReturn(orientation);
    when(BitmapUtil.decodeDimensionsAndColorSpace(any(InputStream.class)))
        .thenReturn(new ImageMetaData(width, height, null));
    mAddMetaDataConsumer.onNewResult(mFinalResult, Consumer.IS_LAST);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(Consumer.IS_LAST));
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertTrue(EncodedImage.isValid(encodedImage));
    assertEquals(DefaultImageFormats.JPEG, encodedImage.getImageFormat());
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
    mAddMetaDataConsumer.onNewResult(null, Consumer.IS_LAST);
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(Consumer.IS_LAST));
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertEquals(encodedImage, null);
  }
}
