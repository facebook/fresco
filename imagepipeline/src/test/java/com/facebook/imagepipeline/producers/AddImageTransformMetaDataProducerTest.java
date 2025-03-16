/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import android.util.Pair;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imageutils.BitmapUtil;
import com.facebook.imageutils.ImageMetaData;
import com.facebook.imageutils.JfifUtil;
import java.io.InputStream;
import javax.annotation.Nullable;
import org.junit.*;
import org.junit.After;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class AddImageTransformMetaDataProducerTest {
  @Mock public Producer<EncodedImage> mInputProducer;
  @Mock public Consumer<EncodedImage> mConsumer;
  @Mock public ProducerContext mProducerContext;
  @Mock public Exception mException;

  private AddImageTransformMetaDataProducer mAddMetaDataProducer;
  @Nullable private Consumer<EncodedImage> mAddMetaDataConsumer;
  private CloseableReference<PooledByteBuffer> mIntermediateResultBufferRef;
  private CloseableReference<PooledByteBuffer> mFinalResultBufferRef;
  private EncodedImage mIntermediateResult;
  private EncodedImage mFinalResult;
  private MockedStatic<ImageFormatChecker> mockedImageFormatChecker;
  private MockedStatic<BitmapUtil> mockedBitmapUtil;
  private MockedStatic<JfifUtil> mockedJfifUtil;

  @Before
  public void setUp() {
    mockedJfifUtil = mockStatic(JfifUtil.class);
    mockedBitmapUtil = mockStatic(BitmapUtil.class);
    mockedImageFormatChecker = mockStatic(ImageFormatChecker.class);
    MockitoAnnotations.initMocks(this);

    mAddMetaDataProducer = new AddImageTransformMetaDataProducer(mInputProducer);

    mIntermediateResultBufferRef = CloseableReference.of(mock(PooledByteBuffer.class));
    mFinalResultBufferRef = CloseableReference.of(mock(PooledByteBuffer.class));

    mIntermediateResult = new EncodedImage(mIntermediateResultBufferRef);
    mFinalResult = new EncodedImage(mFinalResultBufferRef);

    mAddMetaDataConsumer = null;
    doAnswer(
            new Answer() {
              @Nullable
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                mAddMetaDataConsumer = (Consumer<EncodedImage>) invocation.getArguments()[0];
                return null;
              }
            })
        .when(mInputProducer)
        .produceResults(any(Consumer.class), any(ProducerContext.class));
    mAddMetaDataProducer.produceResults(mConsumer, mProducerContext);
  }

  @After
  public void tearDownStaticMocks() {
    mockedImageFormatChecker.close();
    mockedBitmapUtil.close();
    mockedJfifUtil.close();
  }

  @Test
  public void testOnNewResultLastNotJpeg() {
    mockedImageFormatChecker
        .when(() -> ImageFormatChecker.getImageFormat_WrapIOException(any(InputStream.class)))
        .thenAnswer((Answer<ImageFormat>) i -> DefaultImageFormats.WEBP_SIMPLE);
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
    mockedImageFormatChecker
        .when(() -> ImageFormatChecker.getImageFormat_WrapIOException(any(InputStream.class)))
        .thenAnswer((Answer<ImageFormat>) i -> DefaultImageFormats.WEBP_SIMPLE);
    mockedBitmapUtil
        .when(() -> BitmapUtil.decodeDimensions(any(InputStream.class)))
        .thenAnswer((Answer<Pair<Integer, Integer>>) i -> null);
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
    mockedImageFormatChecker
        .when(() -> ImageFormatChecker.getImageFormat_WrapIOException(any(InputStream.class)))
        .thenAnswer((Answer<ImageFormat>) i -> DefaultImageFormats.JPEG);
    mockedJfifUtil
        .when(() -> JfifUtil.getAutoRotateAngleFromOrientation(orientation))
        .thenReturn(rotationAngle);
    mockedJfifUtil
        .when(() -> JfifUtil.getOrientation(any(InputStream.class)))
        .thenReturn(orientation);
    mockedBitmapUtil
        .when(() -> BitmapUtil.decodeDimensionsAndColorSpace(any(InputStream.class)))
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
    mockedImageFormatChecker
        .when(() -> ImageFormatChecker.getImageFormat_WrapIOException(any(InputStream.class)))
        .thenAnswer((Answer<ImageFormat>) i -> DefaultImageFormats.JPEG);
    mockedJfifUtil
        .when(() -> JfifUtil.getOrientation(any(InputStream.class)))
        .thenAnswer((Answer<Integer>) i -> 0);
    mockedBitmapUtil
        .when(() -> BitmapUtil.decodeDimensionsAndColorSpace(any(InputStream.class)))
        .thenAnswer((Answer<ImageMetaData>) i -> new ImageMetaData(-1, -1, null));
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
    mockedImageFormatChecker
        .when(() -> ImageFormatChecker.getImageFormat_WrapIOException(any(InputStream.class)))
        .thenAnswer((Answer<ImageFormat>) i -> DefaultImageFormats.JPEG);
    mockedJfifUtil
        .when(() -> JfifUtil.getAutoRotateAngleFromOrientation(orientation))
        .thenAnswer((Answer<Integer>) i -> rotationAngle);
    mockedJfifUtil
        .when(() -> JfifUtil.getOrientation(any(InputStream.class)))
        .thenAnswer((Answer<Integer>) i -> orientation);
    mockedBitmapUtil
        .when(() -> BitmapUtil.decodeDimensionsAndColorSpace(any(InputStream.class)))
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
    mockedImageFormatChecker
        .when(() -> ImageFormatChecker.getImageFormat_WrapIOException(any(InputStream.class)))
        .thenAnswer((Answer<ImageFormat>) i -> DefaultImageFormats.JPEG);
    mockedJfifUtil
        .when(() -> JfifUtil.getAutoRotateAngleFromOrientation(orientation))
        .thenReturn(rotationAngle);
    mockedJfifUtil
        .when(() -> JfifUtil.getOrientation(any(InputStream.class)))
        .thenReturn(orientation);
    mockedBitmapUtil
        .when(() -> BitmapUtil.decodeDimensionsAndColorSpace(any(InputStream.class)))
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
    assertNull(encodedImage);
  }
}
