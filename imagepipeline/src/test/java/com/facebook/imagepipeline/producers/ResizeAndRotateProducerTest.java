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
import com.facebook.imagepipeline.common.RotationOptions;
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
  }

  @Test
  public void testDoesNotTransformIfNotRequested() {
    whenResizingEnabled();
    whenRequestSpecificRotation(RotationOptions.NO_ROTATION);

    provideIntermediateResult(ImageFormat.JPEG);
    verifyIntermediateResultPassedThroughUnchanged();

    provideFinalResult(ImageFormat.JPEG);
    verifyFinalResultPassedThroughUnchanged();
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesNotTransformIfNotJpeg() throws Exception {
    whenResizingEnabled();
    whenRequestsRotationFromMetadataWithoutDeferring();

    provideIntermediateResult(ImageFormat.WEBP_SIMPLE);
    verifyIntermediateResultPassedThroughUnchanged();

    provideFinalResult(ImageFormat.WEBP_SIMPLE);
    verifyFinalResultPassedThroughUnchanged();
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesRotateIfJpegAndCannotDeferRotationAndResizingDisabled() throws Exception {
    whenResizingDisabled();
    testDoesRotateIfJpegAndCannotDeferRotation();
  }

  @Test
  public void testDoesRotateIfJpegAndCannotDeferRotationAndResizingEnabled() throws Exception {
    whenResizingEnabled();
    testDoesRotateIfJpegAndCannotDeferRotation();
  }

  private void testDoesRotateIfJpegAndCannotDeferRotation() throws Exception {
    int rotationAngle = 180;
    int sourceWidth = 10;
    int sourceHeight = 10;
    whenRequestWidthAndHeight(sourceWidth, sourceHeight);
    whenRequestsRotationFromMetadataWithoutDeferring();

    provideIntermediateResult(ImageFormat.JPEG, sourceWidth, sourceHeight, rotationAngle);
    verifyNoIntermediateResultPassedThrough();

    provideFinalResult(ImageFormat.JPEG, sourceWidth, sourceHeight, rotationAngle);
    verifyAFinalResultPassedThrough();

    assertEquals(2, mFinalResult.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertTrue(mPooledByteBuffer.isClosed());

    verifyJpegTranscoderInteractions(8, rotationAngle);
  }

  @Test
  public void testDoesNotRotateIfCanDeferRotationAndResizeNotNeeded() throws Exception {
    whenResizingEnabled();

    int rotationAngle = 180;
    int sourceWidth = 10;
    int sourceHeight = 10;
    whenRequestWidthAndHeight(sourceWidth, sourceHeight);
    whenRequestsRotationFromMetadataWithDeferringAllowed();

    provideIntermediateResult(ImageFormat.JPEG, sourceWidth, sourceHeight, rotationAngle);
    verifyIntermediateResultPassedThroughUnchanged();

    provideFinalResult(ImageFormat.JPEG, sourceWidth, sourceHeight, rotationAngle);
    verifyFinalResultPassedThroughUnchanged();

    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesResizeAndRotateIfCanDeferRotationButResizeIsNeeded() throws Exception {
    whenResizingEnabled();

    int rotationAngle = 90;
    int sourceWidth = 10;
    int sourceHeight = 10;
    whenRequestWidthAndHeight(sourceWidth, sourceHeight);
    whenRequestsRotationFromMetadataWithDeferringAllowed();

    provideIntermediateResult(ImageFormat.JPEG, sourceWidth * 2, sourceHeight * 2, rotationAngle);
    verifyNoIntermediateResultPassedThrough();

    provideFinalResult(ImageFormat.JPEG, sourceWidth * 2, sourceHeight * 2, rotationAngle);
    verifyAFinalResultPassedThrough();

    verifyJpegTranscoderInteractions(4, rotationAngle);
  }

  @Test
  public void testDoesResizeIfJpegAndResizingEnabled() throws Exception {
    whenResizingEnabled();
    final int preferredWidth = 300;
    final int preferredHeight = 600;
    whenRequestWidthAndHeight(preferredWidth, preferredHeight);
    whenRequestSpecificRotation(RotationOptions.NO_ROTATION);

    provideIntermediateResult(ImageFormat.JPEG, preferredWidth * 2, preferredHeight * 2, 0);
    verifyNoIntermediateResultPassedThrough();

    provideFinalResult(ImageFormat.JPEG, preferredWidth * 2, preferredHeight * 2, 0);
    verifyAFinalResultPassedThrough();

    assertEquals(2, mFinalResult.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertTrue(mPooledByteBuffer.isClosed());

    verifyJpegTranscoderInteractions(4, 0);
  }

  @Test
  public void testDoesNotResizeIfJpegButResizingDisabled() throws Exception {
    whenResizingDisabled();
    final int preferredWidth = 300;
    final int preferredHeight = 600;
    whenRequestWidthAndHeight(preferredWidth, preferredHeight);
    whenRequestSpecificRotation(RotationOptions.NO_ROTATION);

    provideIntermediateResult(ImageFormat.JPEG, preferredWidth * 2, preferredHeight * 2, 0);
    verifyIntermediateResultPassedThroughUnchanged();

    provideFinalResult(ImageFormat.JPEG, preferredWidth * 2, preferredHeight * 2, 0);
    verifyFinalResultPassedThroughUnchanged();

    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesNotUpscale() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(150, 150);
    whenRequestSpecificRotation(RotationOptions.NO_ROTATION);

    provideFinalResult(ImageFormat.JPEG, 100, 100, 0);
    verifyFinalResultPassedThroughUnchanged();
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesNotUpscaleWhenRotating() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(150, 150);
    whenRequestsRotationFromMetadataWithoutDeferring();

    provideFinalResult(ImageFormat.JPEG, 100, 100, 90);
    verifyAFinalResultPassedThrough();
    verifyJpegTranscoderInteractions(8, 90);
  }

  @Test
  public void testDoesComputeRightNumeratorWhenRotating_0() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(50, 100);
    whenRequestsRotationFromMetadataWithoutDeferring();

    provideFinalResult(ImageFormat.JPEG, 400, 200, 0);
    verifyAFinalResultPassedThrough();
    verifyJpegTranscoderInteractions(4, 0);
  }

  @Test
  public void testDoesComputeRightNumeratorWhenRotating_90() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(50, 100);
    whenRequestsRotationFromMetadataWithoutDeferring();

    provideFinalResult(ImageFormat.JPEG, 400, 200, 90);
    verifyAFinalResultPassedThrough();
    verifyJpegTranscoderInteractions(2, 90);
  }

  @Test
  public void testDoesComputeRightNumeratorWhenRotating_180() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(50, 100);
    whenRequestsRotationFromMetadataWithoutDeferring();

    provideFinalResult(ImageFormat.JPEG, 400, 200, 180);
    verifyAFinalResultPassedThrough();
    verifyJpegTranscoderInteractions(4, 180);
  }

  @Test
  public void testDoesComputeRightNumeratorWhenRotating_270() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(50, 100);
    whenRequestsRotationFromMetadataWithoutDeferring();

    provideFinalResult(ImageFormat.JPEG, 400, 200, 270);
    verifyAFinalResultPassedThrough();
    verifyJpegTranscoderInteractions(2, 270);
  }

  @Test
  public void testDoesRotateWhenNoResizeOptionsIfCannotBeDeferred() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(0, 0);
    whenRequestsRotationFromMetadataWithoutDeferring();

    provideFinalResult(ImageFormat.JPEG, 400, 200, 90);
    verifyAFinalResultPassedThrough();
    verifyJpegTranscoderInteractions(8, 90);
  }

  @Test
  public void testDoesNotRotateWhenNoResizeOptionsAndCanBeDeferred() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(0, 0);
    whenRequestsRotationFromMetadataWithDeferringAllowed();

    provideFinalResult(ImageFormat.JPEG, 400, 200, 90);
    verifyFinalResultPassedThroughUnchanged();
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesRotateWhenSpecificRotationRequested() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(200, 400);
    whenRequestSpecificRotation(RotationOptions.ROTATE_270);

    provideFinalResult(ImageFormat.JPEG, 400, 200, 0);
    verifyAFinalResultPassedThrough();
    verifyJpegTranscoderInteractions(8, 270);
  }

  @Test
  public void testDoesNothingWhenNotAskedToDoAnything() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(0, 0);
    whenRequestSpecificRotation(RotationOptions.NO_ROTATION);

    provideFinalResult(ImageFormat.JPEG, 400, 200, 90);
    verifyAFinalResultPassedThrough();
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testRoundNumerator() {
    assertEquals(1, ResizeAndRotateProducer.roundNumerator(
        1.0f/8, ResizeOptions.DEFAULT_ROUNDUP_FRACTION));
    assertEquals(1, ResizeAndRotateProducer.roundNumerator(
        5.0f/32, ResizeOptions.DEFAULT_ROUNDUP_FRACTION));
    assertEquals(1, ResizeAndRotateProducer.roundNumerator(
        1.0f/6 - 0.01f, ResizeOptions.DEFAULT_ROUNDUP_FRACTION));
    assertEquals(2, ResizeAndRotateProducer.roundNumerator(
        1.0f/6, ResizeOptions.DEFAULT_ROUNDUP_FRACTION));
    assertEquals(2, ResizeAndRotateProducer.roundNumerator(
        3.0f/16, ResizeOptions.DEFAULT_ROUNDUP_FRACTION));
    assertEquals(2, ResizeAndRotateProducer.roundNumerator(
        2.0f/8, ResizeOptions.DEFAULT_ROUNDUP_FRACTION));
  }

  @Test
  public void testResizeRatio() {
    ResizeOptions resizeOptions = new ResizeOptions(512, 512);
    assertEquals(
        0.5f,
        ResizeAndRotateProducer.determineResizeRatio(resizeOptions, 1024, 1024),
        0.01);
    assertEquals(
        0.25f,
        ResizeAndRotateProducer.determineResizeRatio(resizeOptions, 2048, 4096),
        0.01);
    assertEquals(
        0.5f,
        ResizeAndRotateProducer.determineResizeRatio(resizeOptions, 4096, 512),
        0.01);
  }

  private void verifyIntermediateResultPassedThroughUnchanged() {
    verify(mConsumer).onNewResult(mIntermediateEncodedImage, false);
  }

  private void verifyNoIntermediateResultPassedThrough() {
    verify(mConsumer, never()).onNewResult(any(EncodedImage.class), eq(false));
  }

  private void verifyFinalResultPassedThroughUnchanged() {
    verify(mConsumer).onNewResult(mFinalEncodedImage, true);
  }

  private void verifyAFinalResultPassedThrough() {
    verify(mConsumer).onNewResult(any(EncodedImage.class), eq(true));
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

  private void provideIntermediateResult(ImageFormat imageFormat) {
    provideIntermediateResult(imageFormat, 800, 800, 0);
  }

  private void provideIntermediateResult(
      ImageFormat imageFormat,
      int width,
      int height,
      int rotationAngle) {
    mIntermediateEncodedImage =
        buildEncodedImage(mIntermediateResult, imageFormat, width, height, rotationAngle);
    mResizeAndRotateProducerConsumer.onNewResult(mIntermediateEncodedImage, false);
  }

  private void provideFinalResult(ImageFormat imageFormat) {
    provideFinalResult(imageFormat, 800, 800, 0);
  }

  private void provideFinalResult(
      ImageFormat imageFormat,
      int width,
      int height,
      int rotationAngle) {
    mFinalEncodedImage =
        buildEncodedImage(mFinalResult, imageFormat, width, height, rotationAngle);
    mResizeAndRotateProducerConsumer.onNewResult(mFinalEncodedImage, true);
    mFakeClockForScheduled.incrementBy(MIN_TRANSFORM_INTERVAL_MS);
    mFakeClockForWorker.incrementBy(MIN_TRANSFORM_INTERVAL_MS);
  }

  private static EncodedImage buildEncodedImage(
      CloseableReference<PooledByteBuffer> pooledByteBufferRef,
      ImageFormat imageFormat,
      int width,
      int height,
      int rotationAngle) {
    EncodedImage encodedImage = new EncodedImage(pooledByteBufferRef);
    encodedImage.setImageFormat(imageFormat);
    encodedImage.setRotationAngle(rotationAngle);
    encodedImage.setWidth(width);
    encodedImage.setHeight(height);
    return encodedImage;
  }

  private void whenResizingEnabled() {
    whenResizingEnabledIs(true);
  }

  private void whenResizingDisabled() {
    whenResizingEnabledIs(false);
  }

  private void whenResizingEnabledIs(boolean resizingEnabled) {
    mResizeAndRotateProducer = new ResizeAndRotateProducer(
        mTestExecutorService,
        mPooledByteBufferFactory,
        resizingEnabled,
        mInputProducer);

    mResizeAndRotateProducer.produceResults(mConsumer, mProducerContext);
  }

  private void whenRequestWidthAndHeight(int preferredWidth, int preferredHeight) {
    when(mImageRequest.getPreferredWidth()).thenReturn(preferredWidth);
    when(mImageRequest.getPreferredHeight()).thenReturn(preferredHeight);
    ResizeOptions resizeOptions = null;
    if (preferredWidth > 0 || preferredHeight > 0) {
      resizeOptions = new ResizeOptions(preferredWidth, preferredHeight);
    }
    when(mImageRequest.getResizeOptions()).thenReturn(resizeOptions);
  }

  private void whenRequestSpecificRotation(
      @RotationOptions.RotationAngle int rotationAngle) {
    when(mImageRequest.getRotationOptions())
        .thenReturn(RotationOptions.forceRotation(rotationAngle));
  }

  private void whenRequestsRotationFromMetadataWithDeferringAllowed() {
    when(mImageRequest.getRotationOptions())
        .thenReturn(RotationOptions.autoRotateAtRenderTime());
  }

  private void whenRequestsRotationFromMetadataWithoutDeferring() {
    when(mImageRequest.getRotationOptions())
        .thenReturn(RotationOptions.autoRotate());
  }
}
