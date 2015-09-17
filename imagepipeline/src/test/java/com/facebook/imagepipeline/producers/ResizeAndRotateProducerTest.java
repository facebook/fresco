/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import android.os.SystemClock;

import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.soloader.SoLoaderShim;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.memory.PooledByteBufferOutputStream;
import com.facebook.imagepipeline.nativecode.JpegTranscoder;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import com.facebook.imagepipeline.testing.TestScheduledExecutorService;

import org.junit.*;
import org.junit.runner.*;
import org.mockito.Mock;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.powermock.api.mockito.*;
import org.powermock.core.classloader.annotations.*;
import org.powermock.modules.junit4.rule.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@Config(manifest= Config.NONE)
@PrepareOnlyThisForTest({JpegTranscoder.class, SystemClock.class,
                          UiThreadImmediateExecutorService.class})

public class ResizeAndRotateProducerTest {
  static {
    SoLoaderShim.setInTestMode();
  }

  @Mock public Producer mInputProducer;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener mProducerListener;
  @Mock public Consumer<EncodedImage> mConsumer;
  @Mock public ProducerContext mProducerContext;
  @Mock public PooledByteBufferFactory mPooledByteBufferFactory;
  @Mock public PooledByteBufferOutputStream mPooledByteBufferOutputStream;

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  private static final int MIN_TRANSFORM_INTERVAL_MS =
      ResizeAndRotateProducer.MIN_TRANSFORM_INTERVAL_MS;
  private TestExecutorService mTestExecutorService;
  private ResizeAndRotateProducer mResizeAndRotateProducer;
  private Consumer<EncodedImage> mResizeAndRotateProducerConsumer;
  private CloseableReference<PooledByteBuffer> mIntermediateResult;
  private CloseableReference<PooledByteBuffer> mFinalResult;
  private PooledByteBuffer mPooledByteBuffer;

  private FakeClock mFakeClockForWorker;
  private FakeClock mFakeClockForScheduled;
  private TestScheduledExecutorService mTestScheduledExecutorService;
  private UiThreadImmediateExecutorService mUiThreadImmediateExecutorService;
  private EncodedImage mIntermediateEncodedImage;
  private EncodedImage mFinalEncodedImage;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mFakeClockForWorker = new FakeClock();
    mFakeClockForScheduled = new FakeClock();
    mFakeClockForWorker.incrementBy(1000);
    mFakeClockForScheduled.incrementBy(1000);
    PowerMockito.mockStatic(SystemClock.class);
    when(SystemClock.uptimeMillis()).thenAnswer(
        new Answer<Long>() {
          @Override
          public Long answer(InvocationOnMock invocation) throws Throwable {
            return mFakeClockForWorker.now();
          }
        });

    mTestExecutorService = new TestExecutorService(mFakeClockForWorker);
    mTestScheduledExecutorService = new TestScheduledExecutorService(mFakeClockForScheduled);
    mUiThreadImmediateExecutorService = mock(UiThreadImmediateExecutorService.class);
    when(mUiThreadImmediateExecutorService.schedule(
        any(Runnable.class),
        anyLong(),
        any(TimeUnit.class)))
        .thenAnswer(
            new Answer<Object>() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                return mTestScheduledExecutorService.schedule(
                    (Runnable) invocation.getArguments()[0],
                    (long) invocation.getArguments()[1],
                    (TimeUnit) invocation.getArguments()[2]);
              }
            });
    PowerMockito.mockStatic(UiThreadImmediateExecutorService.class);
    when(UiThreadImmediateExecutorService.getInstance()).thenReturn(
        mUiThreadImmediateExecutorService);

    PowerMockito.mockStatic(JpegTranscoder.class);
    PowerMockito.when(JpegTranscoder.isRotationAngleAllowed(anyInt())).thenCallRealMethod();
    mTestExecutorService = new TestExecutorService(mFakeClockForWorker);

    mResizeAndRotateProducer = new ResizeAndRotateProducer(
        mTestExecutorService,
        mPooledByteBufferFactory,
        mInputProducer);

    when(mProducerContext.getImageRequest()).thenReturn(mImageRequest);
    when(mProducerContext.getListener()).thenReturn(mProducerListener);
    when(mProducerListener.requiresExtraMap(anyString())).thenReturn(true);
    mIntermediateResult = CloseableReference.of(mock(PooledByteBuffer.class));
    mFinalResult = CloseableReference.of(mock(PooledByteBuffer.class));

    mResizeAndRotateProducerConsumer = null;
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            mResizeAndRotateProducerConsumer =
                (Consumer<EncodedImage>) invocation.getArguments()[0];
            return null;
          }
        }).when(mInputProducer).produceResults(any(Consumer.class), any(ProducerContext.class));
    doReturn(mPooledByteBufferOutputStream).when(mPooledByteBufferFactory).newOutputStream();
    mPooledByteBuffer = new TrivialPooledByteBuffer(new byte[]{1}, 0);
    doReturn(mPooledByteBuffer).when(mPooledByteBufferOutputStream).toByteBuffer();
    mResizeAndRotateProducer.produceResults(mConsumer, mProducerContext);
  }

  @Test
  public void testUnknownDoesNotPassOnIntermediateResult() throws Exception {
    when(mImageRequest.getAutoRotateEnabled()).thenReturn(true);
    EncodedImage intermediateEncodedImage = new EncodedImage(mIntermediateResult);
    mResizeAndRotateProducerConsumer.onNewResult(intermediateEncodedImage, false);
    verify(mConsumer, times(0)).onNewResult(intermediateEncodedImage, false);
  }

  @Test
  public void testUnknownPassesOnResultIfIsLast() throws Exception {
    when(mImageRequest.getAutoRotateEnabled()).thenReturn(true);
    EncodedImage finalEncodedImage = new EncodedImage(mIntermediateResult);
    mResizeAndRotateProducerConsumer.onNewResult(finalEncodedImage, true);
    verify(mConsumer).onNewResult(finalEncodedImage, true);
  }

  @Test
  public void testDoesNotTransformIfNotRequested() {
    when(mImageRequest.getAutoRotateEnabled()).thenReturn(false);
    when(mImageRequest.getPreferredWidth()).thenReturn(0);
    when(mImageRequest.getPreferredHeight()).thenReturn(0);

    EncodedImage intermediateEncodedImage = new EncodedImage(mIntermediateResult);
    intermediateEncodedImage.setImageFormat(ImageFormat.JPEG);
    intermediateEncodedImage.setRotationAngle(0);
    intermediateEncodedImage.setWidth(-1);
    intermediateEncodedImage.setHeight(-1);
    mResizeAndRotateProducerConsumer.onNewResult(intermediateEncodedImage, false);
    verify(mConsumer).onNewResult(intermediateEncodedImage, false);
    EncodedImage finalEncodedImage = new EncodedImage(mFinalResult);
    mResizeAndRotateProducerConsumer.onNewResult(finalEncodedImage, true);
    verify(mConsumer).onNewResult(finalEncodedImage, true);
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesNotTransformIfNotJpeg() throws Exception {
    when(mImageRequest.getAutoRotateEnabled()).thenReturn(true);
    EncodedImage intermediateEncodedImage = new EncodedImage(mIntermediateResult);
    intermediateEncodedImage.setImageFormat(ImageFormat.WEBP_SIMPLE);
    mResizeAndRotateProducerConsumer.onNewResult(intermediateEncodedImage, false);
    verify(mConsumer).onNewResult(intermediateEncodedImage, false);
    EncodedImage finalEncodedImage = new EncodedImage(mFinalResult);
    mResizeAndRotateProducerConsumer.onNewResult(finalEncodedImage, true);
    verify(mConsumer).onNewResult(finalEncodedImage, true);
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesRotateIfJpeg() throws Exception {
    int rotationAngle = 180;
    int sourceWidth = 10;
    int sourceHeight = 10;
    setUpRequestedImageProperties(sourceWidth, sourceHeight, true /* auto rotate */);

    provideIntermediateResultAndVerifyNoConsumerInteractions(
        sourceWidth, sourceHeight, rotationAngle);
    provideFinalResultAndVerifyConsumerInteractions(sourceWidth, sourceHeight, rotationAngle);
    assertEquals(2, mFinalResult.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertTrue(mPooledByteBuffer.isClosed());

    verifyJpegTranscoderInteractions(8, rotationAngle);
  }

  @Test
  public void testDoesResizeIfJpeg() throws Exception {
    final int preferredWidth = 300;
    final int preferredHeight = 600;
    setUpRequestedImageProperties(preferredWidth, preferredHeight, false /* do not auto rotate */);

    provideIntermediateResultAndVerifyNoConsumerInteractions(
        preferredWidth * 2,
        preferredHeight * 2,
        0);
    provideFinalResultAndVerifyConsumerInteractions(
        preferredWidth * 2,
        preferredHeight * 2,
        0);
    setUpRequestedImageProperties(preferredWidth, preferredHeight, false /* do not auto rotate */);

    assertEquals(2, mFinalResult.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertTrue(mPooledByteBuffer.isClosed());

    verifyJpegTranscoderInteractions(4, 0);
  }

  @Test
  public void testDoesNotUpscale() {
    setUpRequestedImageProperties(150, 150, false);
    provideFinalResultAndVerifyConsumerInteractions(100, 100, 0);
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesNotUpscaleWhenRotating() {
    setUpRequestedImageProperties(150, 150, true);
    provideFinalResultAndVerifyConsumerInteractions(100, 100, 90);
    verifyJpegTranscoderInteractions(8, 90);
  }

  @Test
  public void testDoesComputeRightNumeratorWhenRotating_0() {
    setUpRequestedImageProperties(50, 100, true);
    provideFinalResultAndVerifyConsumerInteractions(400, 200, 0);
    verifyJpegTranscoderInteractions(4, 0);
  }

  @Test
  public void testDoesComputeRightNumeratorWhenRotating_90() {
    setUpRequestedImageProperties(50, 100, true);
    provideFinalResultAndVerifyConsumerInteractions(400, 200, 90);
    verifyJpegTranscoderInteractions(2, 90);
  }

  @Test
  public void testDoesComputeRightNumeratorWhenRotating_180() {
    setUpRequestedImageProperties(50, 100, true);
    provideFinalResultAndVerifyConsumerInteractions(400, 200, 180);
    verifyJpegTranscoderInteractions(4, 180);
  }

  @Test
  public void testDoesComputeRightNumeratorWhenRotating_270() {
    setUpRequestedImageProperties(50, 100, true);
    provideFinalResultAndVerifyConsumerInteractions(400, 200, 270);
    verifyJpegTranscoderInteractions(2, 270);
  }

  @Test
  public void testDoesRotateWhenNoResizeOptions() {
    setUpRequestedImageProperties(0, 0, true);
    provideFinalResultAndVerifyConsumerInteractions(400, 200, 90);
    verifyJpegTranscoderInteractions(8, 90);
  }

  @Test
  public void testDoesNothingWhenNotAskedToDoAnything() {
    setUpRequestedImageProperties(0, 0, false);
    provideFinalResultAndVerifyConsumerInteractions(400, 200, 90);
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testRoundNumerator() {
    assertEquals(1, ResizeAndRotateProducer.roundNumerator(1.0f/8));
    assertEquals(1, ResizeAndRotateProducer.roundNumerator(5.0f/32));
    assertEquals(1, ResizeAndRotateProducer.roundNumerator(1.0f/6 - 0.01f));
    assertEquals(2, ResizeAndRotateProducer.roundNumerator(1.0f/6));
    assertEquals(2, ResizeAndRotateProducer.roundNumerator(3.0f/16));
    assertEquals(2, ResizeAndRotateProducer.roundNumerator(2.0f/8));
  }

  @Test
  public void testResizeRatio() {
    ResizeOptions resizeOptions = new ResizeOptions(512, 512);
    assertEquals(
        0.5f,
        ResizeAndRotateProducer.determineResizeRatio(
            resizeOptions, 1024, 1024),
        0.01);
    assertEquals(
        0.25f,
        ResizeAndRotateProducer.determineResizeRatio(
            resizeOptions, 2048, 4096),
        0.01);
    assertEquals(
        0.5f,
        ResizeAndRotateProducer.determineResizeRatio(
            resizeOptions, 4096, 512),
        0.01);

  }

  private static void verifyJpegTranscoderInteractions(int numerator, int rotationAngle) {
    PowerMockito.verifyStatic();
    try {
      JpegTranscoder.transcodeJpeg(
          any(InputStream.class),
          any(OutputStream.class),
          eq(rotationAngle),
          eq(numerator),
          eq(ResizeAndRotateProducer.DEFAULT_JPEG_QUALITY));
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private static void verifyZeroJpegTranscoderInteractions() {
    PowerMockito.verifyStatic(never());
    try {
      JpegTranscoder.transcodeJpeg(
          any(InputStream.class),
          any(OutputStream.class),
          anyInt(),
          anyInt(),
          anyInt());
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private void provideIntermediateResultAndVerifyNoConsumerInteractions(
      int width,
      int height,
      int rotationAngle) {
    mIntermediateEncodedImage = new EncodedImage(mIntermediateResult);
    mIntermediateEncodedImage.setImageFormat(ImageFormat.JPEG);
    mIntermediateEncodedImage.setRotationAngle(rotationAngle);
    mIntermediateEncodedImage.setWidth(width);
    mIntermediateEncodedImage.setHeight(height);
    mResizeAndRotateProducerConsumer.onNewResult(mIntermediateEncodedImage, false);
    verify(mConsumer, times(0)).onNewResult(mIntermediateEncodedImage, false);
  }

  private void provideFinalResultAndVerifyConsumerInteractions(
      int width,
      int height,
      int rotationAngle) {
    mFinalEncodedImage = new EncodedImage(mFinalResult);
    mFinalEncodedImage.setImageFormat(ImageFormat.JPEG);
    mFinalEncodedImage.setRotationAngle(rotationAngle);
    mFinalEncodedImage.setWidth(width);
    mFinalEncodedImage.setHeight(height);
    mResizeAndRotateProducerConsumer.onNewResult(mFinalEncodedImage, true);
    mFakeClockForScheduled.incrementBy(MIN_TRANSFORM_INTERVAL_MS);
    mFakeClockForWorker.incrementBy(MIN_TRANSFORM_INTERVAL_MS);
    verify(mConsumer).onNewResult(any(EncodedImage.class), eq(true));
  }

  private void setUpRequestedImageProperties(
      int preferredWidth,
      int preferredHeight,
      boolean autoRotate) {
    when(mImageRequest.getAutoRotateEnabled()).thenReturn(autoRotate);
    when(mImageRequest.getPreferredWidth()).thenReturn(preferredWidth);
    when(mImageRequest.getPreferredHeight()).thenReturn(preferredHeight);
    ResizeOptions resizeOptions = null;
    if (preferredWidth > 0 || preferredHeight > 0) {
      resizeOptions = new ResizeOptions(preferredWidth, preferredHeight);
    }
    when(mImageRequest.getResizeOptions()).thenReturn(resizeOptions);
  }
}
