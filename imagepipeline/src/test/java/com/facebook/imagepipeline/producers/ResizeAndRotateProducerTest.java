/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static com.facebook.imagepipeline.transcoder.JpegTranscoderUtils.DEFAULT_JPEG_QUALITY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.media.ExifInterface;
import android.net.Uri;
import android.os.SystemClock;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteBufferOutputStream;
import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.nativecode.NativeJpegTranscoder;
import com.facebook.imagepipeline.nativecode.NativeJpegTranscoderFactory;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import com.facebook.imagepipeline.testing.TestScheduledExecutorService;
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer;
import com.facebook.imagepipeline.transcoder.JpegTranscoderUtils;
import com.facebook.soloader.SoLoader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
@Config(manifest = Config.NONE)
@PrepareOnlyThisForTest({
  NativeJpegTranscoder.class,
  SystemClock.class,
  UiThreadImmediateExecutorService.class,
  JpegTranscoderUtils.class
})
public class ResizeAndRotateProducerTest {
  static {
    SoLoader.setInTestMode();
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
  private static final int MAX_BITMAP_SIZE = 2024;
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

    when(mImageRequest.getSourceUri()).thenReturn(Uri.parse("http://testuri"));
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

    PowerMockito.mockStatic(NativeJpegTranscoder.class);
    PowerMockito.mockStatic(UiThreadImmediateExecutorService.class);
    when(UiThreadImmediateExecutorService.getInstance()).thenReturn(
        mUiThreadImmediateExecutorService);

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
  public void testDoesNotTransformIfImageRotationAngleUnkown() {
    whenResizingEnabled();
    whenRequestSpecificRotation(RotationOptions.NO_ROTATION);

    provideIntermediateResult(
        DefaultImageFormats.JPEG,
        800,
        800,
        EncodedImage.UNKNOWN_ROTATION_ANGLE,
        ExifInterface.ORIENTATION_UNDEFINED);
    verifyIntermediateResultPassedThroughUnchanged();

    provideFinalResult(
        DefaultImageFormats.JPEG,
        800,
        800,
        EncodedImage.UNKNOWN_ROTATION_ANGLE,
        ExifInterface.ORIENTATION_UNDEFINED);
    verifyFinalResultPassedThroughUnchanged();
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesNotTransformIfRotationDisabled() {
    whenResizingEnabled();
    whenDisableRotation();

    provideIntermediateResult(DefaultImageFormats.JPEG);
    verifyIntermediateResultPassedThroughUnchanged();

    provideFinalResult(DefaultImageFormats.JPEG);
    verifyFinalResultPassedThroughUnchanged();
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesNotTransformIfMetadataAngleAndRequestedRotationHaveOppositeValues() {
    whenResizingEnabled();
    whenRequestSpecificRotation(RotationOptions.ROTATE_270);

    provideFinalResult(DefaultImageFormats.JPEG, 400, 200, 90, ExifInterface.ORIENTATION_ROTATE_90);
    verifyAFinalResultPassedThroughNotResized();
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesNotTransformIfNotRequested() {
    whenResizingDisabled();
    whenRequestsRotationFromMetadataWithoutDeferring();

    provideIntermediateResult(DefaultImageFormats.JPEG);
    verifyIntermediateResultPassedThroughUnchanged();

    provideFinalResult(DefaultImageFormats.JPEG);
    verifyFinalResultPassedThroughUnchanged();
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesNotTransformIfNotJpeg() throws Exception {
    whenResizingEnabled();
    whenRequestsRotationFromMetadataWithoutDeferring();

    provideIntermediateResult(DefaultImageFormats.WEBP_SIMPLE);
    verifyIntermediateResultPassedThroughUnchanged();

    provideFinalResult(DefaultImageFormats.WEBP_SIMPLE);
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
    int exifOrientation = ExifInterface.ORIENTATION_ROTATE_180;
    int sourceWidth = 10;
    int sourceHeight = 10;
    whenRequestWidthAndHeight(sourceWidth, sourceHeight);
    whenRequestsRotationFromMetadataWithoutDeferring();

    provideIntermediateResult(
        DefaultImageFormats.JPEG, sourceWidth, sourceHeight, rotationAngle, exifOrientation);
    verifyNoIntermediateResultPassedThrough();

    provideFinalResult(
        DefaultImageFormats.JPEG, sourceWidth, sourceHeight, rotationAngle, exifOrientation);
    verifyAFinalResultPassedThroughNotResized();

    assertEquals(2, mFinalResult.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertTrue(mPooledByteBuffer.isClosed());

    verifyJpegTranscoderInteractions(8, rotationAngle);
  }

  @Test
  public void testRotateUsingRotationAngleOnlyForJPEG() throws Exception {
    whenResizingEnabled();
    int rotationAngle = 90;
    whenRequestSpecificRotation(rotationAngle);
    provideFinalResult(DefaultImageFormats.JPEG, 10, 10, 0, ExifInterface.ORIENTATION_UNDEFINED);

    verifyJpegTranscoderInteractions(8, rotationAngle);
    verifyZeroJpegTranscoderExifOrientationInteractions();
  }

  @Test
  public void testRotationAngleIsSetForPNG() throws Exception {
    whenResizingEnabled();
    testNewResultContainsRotationAngleForImageFormat(DefaultImageFormats.PNG);
  }

  @Test
  public void testRotationAngleIsSetForWebp() throws Exception {
    whenResizingEnabled();
    testNewResultContainsRotationAngleForImageFormat(DefaultImageFormats.WEBP_SIMPLE);
  }

  private void testNewResultContainsRotationAngleForImageFormat(ImageFormat imageFormat) {
    int rotationAngle = 90;
    whenRequestSpecificRotation(rotationAngle);
    provideFinalResult(imageFormat, 10, 10, 0, ExifInterface.ORIENTATION_UNDEFINED);

    assertAngleOnNewResult(rotationAngle);
    verifyZeroJpegTranscoderInteractions();
    verifyZeroJpegTranscoderExifOrientationInteractions();
  }

  private void assertAngleOnNewResult(int expectedRotationAngle) {
    ArgumentCaptor<EncodedImage> captor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(captor.capture(), eq(Consumer.IS_LAST));
    assertEquals(expectedRotationAngle, captor.getValue().getRotationAngle());
  }

  @Test
  public void testRotateUsingExifOrientationOnly() throws Exception {
    whenResizingEnabled();
    int exifOrientation = ExifInterface.ORIENTATION_FLIP_HORIZONTAL;
    whenRequestsRotationFromMetadataWithoutDeferring();
    provideFinalResult(DefaultImageFormats.JPEG, 10, 10, 0, exifOrientation);

    verifyZeroJpegTranscoderInteractions();
    verifyJpegTranscoderExifOrientationInteractions(8, exifOrientation);
  }

  @Test
  public void testDoesNotRotateIfCanDeferRotationAndResizeNotNeeded() throws Exception {
    whenResizingEnabled();

    int rotationAngle = 180;
    int exifOrientation = ExifInterface.ORIENTATION_ROTATE_180;
    int sourceWidth = 10;
    int sourceHeight = 10;
    whenRequestWidthAndHeight(sourceWidth, sourceHeight);
    whenRequestsRotationFromMetadataWithDeferringAllowed();

    provideIntermediateResult(
        DefaultImageFormats.JPEG, sourceWidth, sourceHeight, rotationAngle, exifOrientation);
    verifyIntermediateResultPassedThroughUnchanged();

    provideFinalResult(
        DefaultImageFormats.JPEG, sourceWidth, sourceHeight, rotationAngle, exifOrientation);
    verifyFinalResultPassedThroughUnchanged();

    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesResizeAndRotateIfCanDeferRotationButResizeIsNeeded() throws Exception {
    whenResizingEnabled();

    int rotationAngle = 90;
    int exifOrientation = ExifInterface.ORIENTATION_ROTATE_90;
    int sourceWidth = 10;
    int sourceHeight = 10;
    whenRequestWidthAndHeight(sourceWidth, sourceHeight);
    whenRequestsRotationFromMetadataWithDeferringAllowed();

    provideIntermediateResult(
        DefaultImageFormats.JPEG,
        sourceWidth * 2,
        sourceHeight * 2,
        rotationAngle,
        exifOrientation);
    verifyNoIntermediateResultPassedThrough();

    provideFinalResult(
        DefaultImageFormats.JPEG,
        sourceWidth * 2,
        sourceHeight * 2,
        rotationAngle,
        exifOrientation);
    verifyAFinalResultPassedThroughResized();

    verifyJpegTranscoderInteractions(4, rotationAngle);
  }

  @Test
  public void testDoesResizeIfJpegAndResizingEnabled() throws Exception {
    whenResizingEnabled();
    final int preferredWidth = 300;
    final int preferredHeight = 600;
    whenRequestWidthAndHeight(preferredWidth, preferredHeight);
    whenRequestSpecificRotation(RotationOptions.NO_ROTATION);

    provideIntermediateResult(
        DefaultImageFormats.JPEG,
        preferredWidth * 2,
        preferredHeight * 2,
        0,
        ExifInterface.ORIENTATION_NORMAL);
    verifyNoIntermediateResultPassedThrough();

    provideFinalResult(
        DefaultImageFormats.JPEG,
        preferredWidth * 2,
        preferredHeight * 2,
        0,
        ExifInterface.ORIENTATION_NORMAL);
    verifyAFinalResultPassedThroughResized();

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

    provideIntermediateResult(
        DefaultImageFormats.JPEG,
        preferredWidth * 2,
        preferredHeight * 2,
        0,
        ExifInterface.ORIENTATION_NORMAL);
    verifyIntermediateResultPassedThroughUnchanged();

    provideFinalResult(
        DefaultImageFormats.JPEG,
        preferredWidth * 2,
        preferredHeight * 2,
        0,
        ExifInterface.ORIENTATION_NORMAL);
    verifyFinalResultPassedThroughUnchanged();

    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesNotUpscale() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(150, 150);
    whenRequestSpecificRotation(RotationOptions.NO_ROTATION);

    provideFinalResult(DefaultImageFormats.JPEG, 100, 100, 0, ExifInterface.ORIENTATION_NORMAL);
    verifyFinalResultPassedThroughUnchanged();
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesNotUpscaleWhenRotating() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(150, 150);
    whenRequestsRotationFromMetadataWithoutDeferring();

    provideFinalResult(DefaultImageFormats.JPEG, 100, 100, 90, ExifInterface.ORIENTATION_ROTATE_90);
    verifyAFinalResultPassedThroughNotResized();
    verifyJpegTranscoderInteractions(8, 90);
  }

  @Test
  public void testDoesComputeRightNumeratorWhenRotating_0() {
    testDoesComputeRightNumerator(0, ExifInterface.ORIENTATION_NORMAL, 4);
  }

  @Test
  public void testDoesComputeRightNumeratorWhenRotating_90() {
    testDoesComputeRightNumerator(90, ExifInterface.ORIENTATION_ROTATE_90, 2);
  }

  @Test
  public void testDoesComputeRightNumeratorWhenRotating_180() {
    testDoesComputeRightNumerator(180, ExifInterface.ORIENTATION_ROTATE_180, 4);
  }

  @Test
  public void testDoesComputeRightNumeratorWhenRotating_270() {
    testDoesComputeRightNumerator(270, ExifInterface.ORIENTATION_ROTATE_270, 2);
  }

  private void testDoesComputeRightNumerator(
      int rotationAngle, int exifOrientation, int numerator) {
    whenResizingEnabled();
    whenRequestWidthAndHeight(50, 100);
    whenRequestsRotationFromMetadataWithoutDeferring();

    provideFinalResult(DefaultImageFormats.JPEG, 400, 200, rotationAngle, exifOrientation);
    verifyAFinalResultPassedThroughResized();
    verifyJpegTranscoderInteractions(numerator, rotationAngle);
  }

  @Test
  public void testDoesComputeRightNumeratorInvertedOrientation_flipHorizontal() {
    testDoesComputeRightNumeratorInvertedOrientation(ExifInterface.ORIENTATION_FLIP_HORIZONTAL, 4);
  }

  @Test
  public void testDoesComputeRightNumeratorInvertedOrientation_flipVertical() {
    testDoesComputeRightNumeratorInvertedOrientation(ExifInterface.ORIENTATION_FLIP_VERTICAL, 4);
  }

  @Test
  public void testDoesComputeRightNumeratorInvertedOrientation_transpose() {
    testDoesComputeRightNumeratorInvertedOrientation(ExifInterface.ORIENTATION_TRANSPOSE, 2);
  }

  @Test
  public void testDoesComputeRightNumeratorInvertedOrientation_transverse() {
    testDoesComputeRightNumeratorInvertedOrientation(ExifInterface.ORIENTATION_TRANSVERSE, 2);
  }

  private void testDoesComputeRightNumeratorInvertedOrientation(
      int exifOrientation, int numerator) {
    whenResizingEnabled();
    whenRequestWidthAndHeight(50, 100);
    whenRequestsRotationFromMetadataWithoutDeferring();

    provideFinalResult(DefaultImageFormats.JPEG, 400, 200, 0, exifOrientation);
    verifyAFinalResultPassedThroughResized();
    verifyJpegTranscoderExifOrientationInteractions(numerator, exifOrientation);
  }

  @Test
  public void testDoesRotateWhenNoResizeOptionsIfCannotBeDeferred() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(0, 0);
    whenRequestsRotationFromMetadataWithoutDeferring();

    provideFinalResult(DefaultImageFormats.JPEG, 400, 200, 90, ExifInterface.ORIENTATION_ROTATE_90);
    verifyAFinalResultPassedThroughNotResized();
    verifyJpegTranscoderInteractions(8, 90);
  }

  @Test
  public void testDoesNotRotateWhenNoResizeOptionsAndCanBeDeferred() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(0, 0);
    whenRequestsRotationFromMetadataWithDeferringAllowed();

    provideFinalResult(DefaultImageFormats.JPEG, 400, 200, 90, ExifInterface.ORIENTATION_ROTATE_90);
    verifyFinalResultPassedThroughUnchanged();
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testDoesRotateWhenSpecificRotationRequested() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(200, 400);
    whenRequestSpecificRotation(RotationOptions.ROTATE_270);

    provideFinalResult(DefaultImageFormats.JPEG, 400, 200, 0, ExifInterface.ORIENTATION_UNDEFINED);
    verifyAFinalResultPassedThroughNotResized();
    verifyJpegTranscoderInteractions(8, 270);
  }

  @Test
  public void testDoesNothingWhenNotAskedToDoAnything() {
    whenResizingEnabled();
    whenRequestWidthAndHeight(0, 0);
    whenDisableRotation();

    provideFinalResult(DefaultImageFormats.JPEG, 400, 200, 90, ExifInterface.ORIENTATION_ROTATE_90);
    verifyAFinalResultPassedThroughNotResized();
    verifyZeroJpegTranscoderInteractions();
  }

  @Test
  public void testRoundNumerator() {
    assertEquals(
        1, JpegTranscoderUtils.roundNumerator(1.0f / 8, ResizeOptions.DEFAULT_ROUNDUP_FRACTION));
    assertEquals(
        1, JpegTranscoderUtils.roundNumerator(5.0f / 32, ResizeOptions.DEFAULT_ROUNDUP_FRACTION));
    assertEquals(
        1,
        JpegTranscoderUtils.roundNumerator(
            1.0f / 6 - 0.01f, ResizeOptions.DEFAULT_ROUNDUP_FRACTION));
    assertEquals(
        2, JpegTranscoderUtils.roundNumerator(1.0f / 6, ResizeOptions.DEFAULT_ROUNDUP_FRACTION));
    assertEquals(
        2, JpegTranscoderUtils.roundNumerator(3.0f / 16, ResizeOptions.DEFAULT_ROUNDUP_FRACTION));
    assertEquals(
        2, JpegTranscoderUtils.roundNumerator(2.0f / 8, ResizeOptions.DEFAULT_ROUNDUP_FRACTION));
  }

  @Test
  public void testDownsamplingRatioUsage() {
    assertEquals(8, JpegTranscoderUtils.calculateDownsampleNumerator(1));
    assertEquals(4, JpegTranscoderUtils.calculateDownsampleNumerator(2));
    assertEquals(2, JpegTranscoderUtils.calculateDownsampleNumerator(4));
    assertEquals(1, JpegTranscoderUtils.calculateDownsampleNumerator(8));
    assertEquals(1, JpegTranscoderUtils.calculateDownsampleNumerator(16));
    assertEquals(1, JpegTranscoderUtils.calculateDownsampleNumerator(32));
  }

  @Test
  public void testResizeRatio() {
    ResizeOptions resizeOptions = new ResizeOptions(512, 512);
    assertEquals(0.5f, JpegTranscoderUtils.determineResizeRatio(resizeOptions, 1024, 1024), 0.01);
    assertEquals(0.25f, JpegTranscoderUtils.determineResizeRatio(resizeOptions, 2048, 4096), 0.01);
    assertEquals(0.5f, JpegTranscoderUtils.determineResizeRatio(resizeOptions, 4096, 512), 0.01);
  }

  private void verifyIntermediateResultPassedThroughUnchanged() {
    verify(mConsumer).onNewResult(mIntermediateEncodedImage, Consumer.NO_FLAGS);
  }

  private void verifyNoIntermediateResultPassedThrough() {
    verify(mConsumer, never()).onNewResult(any(EncodedImage.class), eq(Consumer.NO_FLAGS));
  }

  private void verifyFinalResultPassedThroughUnchanged() {
    verify(mConsumer).onNewResult(mFinalEncodedImage, Consumer.IS_LAST);
  }

  private void verifyAFinalResultPassedThroughResized() {
    verify(mConsumer)
        .onNewResult(any(EncodedImage.class), eq(Consumer.IS_LAST | Consumer.IS_RESIZING_DONE));
  }

  private void verifyAFinalResultPassedThroughNotResized() {
    verify(mConsumer).onNewResult(any(EncodedImage.class), eq(Consumer.IS_LAST));
  }

  private static void verifyJpegTranscoderInteractions(int numerator, int rotationAngle) {
    PowerMockito.verifyStatic();
    try {
      NativeJpegTranscoder.transcodeJpeg(
          any(InputStream.class),
          any(OutputStream.class),
          eq(rotationAngle),
          eq(numerator),
          eq(DEFAULT_JPEG_QUALITY));
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private static void verifyJpegTranscoderExifOrientationInteractions(
      int numerator, int exifOrientation) {
    PowerMockito.verifyStatic();
    try {
      NativeJpegTranscoder.transcodeJpegWithExifOrientation(
          any(InputStream.class),
          any(OutputStream.class),
          eq(exifOrientation),
          eq(numerator),
          eq(DEFAULT_JPEG_QUALITY));
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private static void verifyZeroJpegTranscoderInteractions() {
    PowerMockito.verifyStatic(never());
    try {
      NativeJpegTranscoder.transcodeJpeg(
          any(InputStream.class), any(OutputStream.class), anyInt(), anyInt(), anyInt());
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private static void verifyZeroJpegTranscoderExifOrientationInteractions() {
    PowerMockito.verifyStatic(never());
    try {
      NativeJpegTranscoder.transcodeJpegWithExifOrientation(
          any(InputStream.class), any(OutputStream.class), anyInt(), anyInt(), anyInt());
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private void provideIntermediateResult(ImageFormat imageFormat) {
    provideIntermediateResult(imageFormat, 800, 800, 0, ExifInterface.ORIENTATION_UNDEFINED);
  }

  private void provideIntermediateResult(
      ImageFormat imageFormat, int width, int height, int rotationAngle, int exifOrientation) {
    mIntermediateEncodedImage =
        buildEncodedImage(
            mIntermediateResult, imageFormat, width, height, rotationAngle, exifOrientation);
    mResizeAndRotateProducerConsumer.onNewResult(mIntermediateEncodedImage, Consumer.NO_FLAGS);
  }

  private void provideFinalResult(ImageFormat imageFormat) {
    provideFinalResult(imageFormat, 800, 800, 0, ExifInterface.ORIENTATION_UNDEFINED);
  }

  private void provideFinalResult(
      ImageFormat imageFormat, int width, int height, int rotationAngle, int exifOrientation) {
    mFinalEncodedImage =
        buildEncodedImage(mFinalResult, imageFormat, width, height, rotationAngle, exifOrientation);
    mResizeAndRotateProducerConsumer.onNewResult(mFinalEncodedImage, Consumer.IS_LAST);
    mFakeClockForScheduled.incrementBy(MIN_TRANSFORM_INTERVAL_MS);
    mFakeClockForWorker.incrementBy(MIN_TRANSFORM_INTERVAL_MS);
  }

  private static EncodedImage buildEncodedImage(
      CloseableReference<PooledByteBuffer> pooledByteBufferRef,
      ImageFormat imageFormat,
      int width,
      int height,
      int rotationAngle,
      int exifOrientation) {
    EncodedImage encodedImage = new EncodedImage(pooledByteBufferRef);
    encodedImage.setImageFormat(imageFormat);
    encodedImage.setRotationAngle(rotationAngle);
    encodedImage.setExifOrientation(exifOrientation);
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
    NativeJpegTranscoder nativeJpegTranscoder =
        new NativeJpegTranscoder(resizingEnabled, MAX_BITMAP_SIZE, false);
    NativeJpegTranscoderFactory jpegTranscoderFactory = mock(NativeJpegTranscoderFactory.class);
    when(jpegTranscoderFactory.createImageTranscoder(any(ImageFormat.class), anyBoolean()))
        .thenReturn(nativeJpegTranscoder);

    mResizeAndRotateProducer =
        new ResizeAndRotateProducer(
            mTestExecutorService,
            mPooledByteBufferFactory,
            mInputProducer,
            resizingEnabled,
            jpegTranscoderFactory);

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

  private void whenDisableRotation() {
    when(mImageRequest.getRotationOptions())
        .thenReturn(RotationOptions.disableRotation());
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
