/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.media.ExifInterface;
import android.net.Uri;
import com.facebook.common.internal.Suppliers;
import com.facebook.common.memory.ByteArrayPool;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.CloseableReferenceFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.core.ImagePipelineExperiments;
import com.facebook.imagepipeline.debug.NoOpCloseableReferenceLeakTracker;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.decoder.ProgressiveJpegParser;
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.Mock;
import org.powermock.api.mockito.*;
import org.powermock.core.classloader.annotations.*;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
@Config(manifest = Config.NONE)
@PrepareForTest({JobScheduler.class, ProgressiveJpegParser.class, DecodeProducer.class})
public class DecodeProducerTest {

  private static final ImageDecodeOptions IMAGE_DECODE_OPTIONS =
      ImageDecodeOptions.newBuilder().setMinDecodeIntervalMs(100).build();
  private static final int PREVIEW_SCAN = 2;
  private static final int IGNORED_SCAN = 3;
  private static final int GOOD_ENOUGH_SCAN = 5;
  private static final int IMAGE_WIDTH = 200;
  private static final int IMAGE_HEIGHT = 100;
  private static final int IMAGE_SIZE = 1000;
  private static final int IMAGE_ROTATION_ANGLE = 0;
  private static final int IMAGE_EXIF_ORIENTATION = ExifInterface.ORIENTATION_NORMAL;
  private static final int MAX_BITMAP_SIZE = 2024;

  @Mock public ByteArrayPool mByteArrayPool;
  @Mock public Executor mExecutor;
  @Mock public ImageDecoder mImageDecoder;
  private ProgressiveJpegConfig mProgressiveJpegConfig;
  @Mock public Producer mInputProducer;

  private ImageRequest mImageRequest;
  private String mRequestId;
  private CloseableReference<PooledByteBuffer> mByteBufferRef;
  private EncodedImage mEncodedImage;
  @Mock public ProducerListener2 mProducerListener;
  private SettableProducerContext mProducerContext;
  @Mock public Consumer mConsumer;

  @Mock public ProgressiveJpegParser mProgressiveJpegParser;
  @Mock public JobScheduler mJobScheduler;

  @Mock public ImagePipelineConfig mConfig;
  @Mock public ImagePipelineExperiments mPipelineExperiments;

  private DecodeProducer mDecodeProducer;

  @Rule public PowerMockRule rule = new PowerMockRule();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    mProgressiveJpegConfig =
        new SimpleProgressiveJpegConfig(
            new SimpleProgressiveJpegConfig.DynamicValueConfig() {
              public List<Integer> getScansToDecode() {
                return Arrays.asList(PREVIEW_SCAN, GOOD_ENOUGH_SCAN);
              }

              public int getGoodEnoughScanNumber() {
                return GOOD_ENOUGH_SCAN;
              }
            });

    PowerMockito.mockStatic(ProgressiveJpegParser.class);
    PowerMockito.whenNew(ProgressiveJpegParser.class)
        .withAnyArguments()
        .thenReturn(mProgressiveJpegParser);
    PowerMockito.mockStatic(JobScheduler.class);
    PowerMockito.whenNew(JobScheduler.class).withAnyArguments().thenReturn(mJobScheduler);

    when(mConfig.getExperiments()).thenReturn(mPipelineExperiments);

    mDecodeProducer =
        new DecodeProducer(
            mByteArrayPool,
            mExecutor,
            mImageDecoder,
            mProgressiveJpegConfig,
            false, /* Set downsampleEnabled to false */
            false, /* Set resizeAndRotateForNetwork to false */
            false, /* We don't cancel when the request is cancelled */
            mInputProducer,
            MAX_BITMAP_SIZE,
            new CloseableReferenceFactory(new NoOpCloseableReferenceLeakTracker()),
            null,
            Suppliers.BOOLEAN_FALSE);

    PooledByteBuffer pooledByteBuffer = mockPooledByteBuffer(IMAGE_SIZE);
    mByteBufferRef = CloseableReference.of(pooledByteBuffer);
    mEncodedImage = new EncodedImage(mByteBufferRef);
    mEncodedImage.setImageFormat(DefaultImageFormats.JPEG);
    mEncodedImage.setWidth(IMAGE_WIDTH);
    mEncodedImage.setHeight(IMAGE_HEIGHT);
    mEncodedImage.setRotationAngle(IMAGE_ROTATION_ANGLE);
    mEncodedImage.setExifOrientation(IMAGE_EXIF_ORIENTATION);
  }

  private static EncodedImage mockEncodedJpeg(CloseableReference<PooledByteBuffer> ref) {
    final EncodedImage encodedImage = new EncodedImage(ref);
    encodedImage.setImageFormat(DefaultImageFormats.JPEG);
    encodedImage.setWidth(IMAGE_WIDTH);
    encodedImage.setHeight(IMAGE_HEIGHT);
    return encodedImage;
  }

  @Test
  public void testNewResult_Final() {
    setupNetworkUri();
    Consumer<EncodedImage> consumer = produceResults();

    when(mJobScheduler.updateJob(mEncodedImage, Consumer.IS_LAST)).thenReturn(true);
    consumer.onNewResult(mEncodedImage, Consumer.IS_LAST);

    verify(mJobScheduler).updateJob(mEncodedImage, Consumer.IS_LAST);
    verify(mJobScheduler).scheduleJob();
    verifyZeroInteractions(mProgressiveJpegParser);
  }

  @Test
  public void testNewResult_Final_Local() {
    setupLocalUri();
    Consumer<EncodedImage> consumer = produceResults();

    when(mJobScheduler.updateJob(mEncodedImage, Consumer.IS_LAST)).thenReturn(true);
    consumer.onNewResult(mEncodedImage, Consumer.IS_LAST);

    verify(mJobScheduler).updateJob(mEncodedImage, Consumer.IS_LAST);
    verify(mJobScheduler).scheduleJob();
    verifyZeroInteractions(mProgressiveJpegParser);
  }

  @Test
  public void testNewResult_Intermediate_NonJPEG() {
    mEncodedImage.setImageFormat(DefaultImageFormats.WEBP_SIMPLE);
    setupNetworkUri();
    Consumer<EncodedImage> consumer = produceResults();

    when(mJobScheduler.updateJob(mEncodedImage, Consumer.NO_FLAGS)).thenReturn(true);
    consumer.onNewResult(mEncodedImage, Consumer.NO_FLAGS);

    InOrder inOrder = inOrder(mJobScheduler);
    inOrder.verify(mJobScheduler).updateJob(mEncodedImage, Consumer.NO_FLAGS);
    inOrder.verify(mJobScheduler).scheduleJob();
    verifyZeroInteractions(mProgressiveJpegParser);
  }

  @Test
  public void testNewResult_Intermediate_Local() {
    setupLocalUri();
    Consumer<EncodedImage> consumer = produceResults();

    when(mJobScheduler.updateJob(mEncodedImage, Consumer.NO_FLAGS)).thenReturn(true);
    consumer.onNewResult(mEncodedImage, Consumer.NO_FLAGS);

    verify(mJobScheduler, never()).updateJob(mEncodedImage, Consumer.NO_FLAGS);
    verify(mProgressiveJpegParser, never()).parseMoreData(mEncodedImage);
    verify(mJobScheduler, never()).scheduleJob();
  }

  @Test
  public void testNewResult_Placeholder() {
    setupNetworkUri();
    Consumer<EncodedImage> consumer = produceResults();

    when(mJobScheduler.updateJob(mEncodedImage, Consumer.IS_PLACEHOLDER)).thenReturn(true);
    consumer.onNewResult(mEncodedImage, Consumer.IS_PLACEHOLDER);

    verify(mJobScheduler, times(1)).updateJob(mEncodedImage, Consumer.IS_PLACEHOLDER);
    verify(mProgressiveJpegParser, never()).parseMoreData(mEncodedImage);
    verify(mJobScheduler, times(1)).scheduleJob();
  }

  @Test
  public void testNewResult_Intermediate_pJPEG() {
    setupNetworkUri();
    Consumer<EncodedImage> consumer = produceResults();

    InOrder inOrder = inOrder(mJobScheduler, mProgressiveJpegParser);

    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);

    // preview scan; schedule
    when(mJobScheduler.updateJob(mEncodedImage, Consumer.NO_FLAGS)).thenReturn(true);
    when(mProgressiveJpegParser.parseMoreData(any(EncodedImage.class))).thenReturn(true);
    when(mProgressiveJpegParser.getBestScanNumber()).thenReturn(PREVIEW_SCAN);
    consumer.onNewResult(mEncodedImage, Consumer.NO_FLAGS);
    inOrder.verify(mJobScheduler).updateJob(mEncodedImage, Consumer.NO_FLAGS);
    inOrder.verify(mProgressiveJpegParser).parseMoreData(argumentCaptor.capture());
    inOrder.verify(mJobScheduler).scheduleJob();
    assertSame(
        ((EncodedImage) argumentCaptor.getValue()).getUnderlyingReferenceTestOnly(),
        mByteBufferRef.getUnderlyingReferenceTestOnly());

    // no data parsed; ignore
    PooledByteBuffer pooledByteBuffer2 = mockPooledByteBuffer(210);
    CloseableReference<PooledByteBuffer> ref2 = CloseableReference.of(pooledByteBuffer2);
    EncodedImage encodedImage2 = mockEncodedJpeg(ref2);
    when(mJobScheduler.updateJob(encodedImage2, Consumer.NO_FLAGS)).thenReturn(true);
    when(mProgressiveJpegParser.parseMoreData(encodedImage2)).thenReturn(false);
    when(mProgressiveJpegParser.getBestScanNumber()).thenReturn(PREVIEW_SCAN);
    consumer.onNewResult(encodedImage2, Consumer.NO_FLAGS);
    inOrder.verify(mJobScheduler).updateJob(encodedImage2, Consumer.NO_FLAGS);
    inOrder.verify(mProgressiveJpegParser).parseMoreData(argumentCaptor.capture());
    inOrder.verify(mJobScheduler, never()).scheduleJob();
    assertSame(
        ((EncodedImage) argumentCaptor.getValue()).getUnderlyingReferenceTestOnly(),
        ref2.getUnderlyingReferenceTestOnly());

    // same scan; ignore
    PooledByteBuffer pooledByteBuffer3 = mockPooledByteBuffer(220);
    CloseableReference<PooledByteBuffer> ref3 = CloseableReference.of(pooledByteBuffer3);
    EncodedImage encodedImage3 = mockEncodedJpeg(ref3);
    when(mJobScheduler.updateJob(encodedImage3, Consumer.NO_FLAGS)).thenReturn(true);
    when(mProgressiveJpegParser.parseMoreData(encodedImage3)).thenReturn(true);
    when(mProgressiveJpegParser.getBestScanNumber()).thenReturn(PREVIEW_SCAN);
    consumer.onNewResult(encodedImage3, Consumer.NO_FLAGS);
    inOrder.verify(mJobScheduler).updateJob(encodedImage3, Consumer.NO_FLAGS);
    inOrder.verify(mProgressiveJpegParser).parseMoreData(argumentCaptor.capture());
    inOrder.verify(mJobScheduler, never()).scheduleJob();
    assertSame(
        ((EncodedImage) argumentCaptor.getValue()).getUnderlyingReferenceTestOnly(),
        ref3.getUnderlyingReferenceTestOnly());

    // scan not for decode; ignore
    PooledByteBuffer pooledByteBuffer4 = mockPooledByteBuffer(300);
    CloseableReference<PooledByteBuffer> ref4 = CloseableReference.of(pooledByteBuffer4);
    EncodedImage encodedImage4 = mockEncodedJpeg(ref4);
    when(mJobScheduler.updateJob(encodedImage4, Consumer.NO_FLAGS)).thenReturn(true);
    when(mProgressiveJpegParser.parseMoreData(encodedImage4)).thenReturn(true);
    when(mProgressiveJpegParser.getBestScanNumber()).thenReturn(IGNORED_SCAN);
    consumer.onNewResult(encodedImage4, Consumer.NO_FLAGS);
    inOrder.verify(mJobScheduler).updateJob(encodedImage4, Consumer.NO_FLAGS);
    inOrder.verify(mProgressiveJpegParser).parseMoreData(argumentCaptor.capture());
    inOrder.verify(mJobScheduler, never()).scheduleJob();
    assertSame(
        ((EncodedImage) argumentCaptor.getValue()).getUnderlyingReferenceTestOnly(),
        ref4.getUnderlyingReferenceTestOnly());

    // good-enough scan; schedule
    PooledByteBuffer pooledByteBuffer5 = mockPooledByteBuffer(500);
    CloseableReference<PooledByteBuffer> ref5 = CloseableReference.of(pooledByteBuffer5);
    EncodedImage encodedImage5 = mockEncodedJpeg(ref5);
    when(mJobScheduler.updateJob(encodedImage5, Consumer.NO_FLAGS)).thenReturn(true);
    when(mProgressiveJpegParser.parseMoreData(encodedImage5)).thenReturn(true);
    when(mProgressiveJpegParser.getBestScanNumber()).thenReturn(GOOD_ENOUGH_SCAN);
    consumer.onNewResult(encodedImage5, Consumer.NO_FLAGS);
    inOrder.verify(mJobScheduler).updateJob(encodedImage5, Consumer.NO_FLAGS);
    inOrder.verify(mProgressiveJpegParser).parseMoreData(argumentCaptor.capture());
    inOrder.verify(mJobScheduler).scheduleJob();
    assertSame(
        ((EncodedImage) argumentCaptor.getValue()).getUnderlyingReferenceTestOnly(),
        ref5.getUnderlyingReferenceTestOnly());
  }

  @Test
  public void testFailure() {
    setupNetworkUri();
    Consumer<EncodedImage> consumer = produceResults();

    Exception exception = mock(Exception.class);
    consumer.onFailure(exception);
    verify(mConsumer).onFailure(exception);
  }

  @Test
  public void testCancellation() {
    setupNetworkUri();
    Consumer<EncodedImage> consumer = produceResults();

    consumer.onCancellation();
    verify(mConsumer).onCancellation();
  }

  @Test
  public void testDecode_Final() throws Exception {
    setupNetworkUri();
    produceResults();
    JobScheduler.JobRunnable jobRunnable = getJobRunnable();

    jobRunnable.run(mEncodedImage, Consumer.IS_LAST);

    InOrder inOrder = inOrder(mProducerListener, mImageDecoder);
    inOrder
        .verify(mProducerListener)
        .onProducerStart(mProducerContext, DecodeProducer.PRODUCER_NAME);
    inOrder
        .verify(mImageDecoder)
        .decode(mEncodedImage, IMAGE_SIZE, ImmutableQualityInfo.FULL_QUALITY, IMAGE_DECODE_OPTIONS);
    inOrder
        .verify(mProducerListener)
        .onProducerFinishWithSuccess(
            eq(mProducerContext), eq(DecodeProducer.PRODUCER_NAME), nullable(Map.class));
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mProducerContext), anyString(), anyBoolean());
  }

  @Test
  public void testDecode_Intermediate_pJPEG() throws Exception {
    setupNetworkUri();
    produceResults();
    JobScheduler.JobRunnable jobRunnable = getJobRunnable();

    when(mProgressiveJpegParser.isJpeg()).thenReturn(true);
    when(mProgressiveJpegParser.getBestScanEndOffset()).thenReturn(200);
    when(mProgressiveJpegParser.getBestScanNumber()).thenReturn(PREVIEW_SCAN);
    jobRunnable.run(mEncodedImage, Consumer.NO_FLAGS);

    InOrder inOrder = inOrder(mProducerListener, mImageDecoder);
    inOrder
        .verify(mProducerListener)
        .onProducerStart(mProducerContext, DecodeProducer.PRODUCER_NAME);
    inOrder
        .verify(mImageDecoder)
        .decode(
            mEncodedImage,
            200,
            ImmutableQualityInfo.of(PREVIEW_SCAN, false, false),
            IMAGE_DECODE_OPTIONS);
    inOrder
        .verify(mProducerListener)
        .onProducerFinishWithSuccess(
            eq(mProducerContext), eq(DecodeProducer.PRODUCER_NAME), nullable(Map.class));
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void testDecode_Failure() throws Exception {
    setupNetworkUri();
    produceResults();
    JobScheduler.JobRunnable jobRunnable = getJobRunnable();

    Exception exception = new RuntimeException();
    when(mImageDecoder.decode(
            mEncodedImage, IMAGE_SIZE, ImmutableQualityInfo.FULL_QUALITY, IMAGE_DECODE_OPTIONS))
        .thenThrow(exception);
    jobRunnable.run(mEncodedImage, Consumer.IS_LAST);

    InOrder inOrder = inOrder(mProducerListener, mImageDecoder);
    inOrder
        .verify(mProducerListener)
        .onProducerStart(mProducerContext, DecodeProducer.PRODUCER_NAME);
    inOrder
        .verify(mImageDecoder)
        .decode(mEncodedImage, IMAGE_SIZE, ImmutableQualityInfo.FULL_QUALITY, IMAGE_DECODE_OPTIONS);
    inOrder
        .verify(mProducerListener)
        .onProducerFinishWithFailure(
            eq(mProducerContext),
            eq(DecodeProducer.PRODUCER_NAME),
            eq(exception),
            nullable(Map.class));
    verify(mProducerListener, never())
        .onUltimateProducerReached(eq(mProducerContext), anyString(), anyBoolean());
  }

  @Test
  public void testDecode_WhenSmartResizingEnabledAndLocalUri_ThenPerformDownsampling()
      throws Exception {
    int resizedWidth = 10;
    int resizedHeight = 10;
    setupLocalUri(ResizeOptions.forDimensions(resizedWidth, resizedHeight));

    produceResults();
    JobScheduler.JobRunnable jobRunnable = getJobRunnable();

    jobRunnable.run(mEncodedImage, Consumer.IS_LAST);

    // The sample size was modified, which means Downsampling has been performed
    assertNotEquals(mEncodedImage.getSampleSize(), EncodedImage.DEFAULT_SAMPLE_SIZE);
  }

  @Test
  public void testDecode_WhenSmartResizingEnabledAndNetworkUri_ThenPerformNoDownsampling()
      throws Exception {
    int resizedWidth = 10;
    int resizedHeight = 10;
    setupNetworkUri(ResizeOptions.forDimensions(resizedWidth, resizedHeight));

    produceResults();
    JobScheduler.JobRunnable jobRunnable = getJobRunnable();

    jobRunnable.run(mEncodedImage, Consumer.IS_LAST);

    // The sample size was not modified, which means Downsampling has not been performed
    assertEquals(mEncodedImage.getSampleSize(), EncodedImage.DEFAULT_SAMPLE_SIZE);
  }

  private void setupImageRequest(String requestId, ImageRequest imageRequest) {
    mImageRequest = imageRequest;
    mRequestId = requestId;
    mProducerContext =
        new SettableProducerContext(
            mImageRequest,
            mRequestId,
            mProducerListener,
            mock(Object.class),
            ImageRequest.RequestLevel.FULL_FETCH,
            /* isPrefetch */ false,
            /* isIntermediateResultExpected */ true,
            Priority.MEDIUM,
            mConfig);
  }

  private void setupNetworkUri() {
    setupNetworkUri(null);
  }

  private void setupNetworkUri(@Nullable ResizeOptions resizeOptions) {
    setupImageRequest(
        "networkRequest1",
        ImageRequestBuilder.newBuilderWithSource(Uri.parse("http://www.fb.com/image"))
            .setProgressiveRenderingEnabled(true)
            .setImageDecodeOptions(IMAGE_DECODE_OPTIONS)
            .setResizeOptions(resizeOptions)
            .build());
  }

  private void setupLocalUri() {
    setupLocalUri(null);
  }

  private void setupLocalUri(@Nullable ResizeOptions resizeOptions) {
    setupImageRequest(
        "localRequest1",
        ImageRequestBuilder.newBuilderWithSource(Uri.parse("file://path/image"))
            .setProgressiveRenderingEnabled(true) // this should be ignored
            .setImageDecodeOptions(IMAGE_DECODE_OPTIONS)
            .setResizeOptions(resizeOptions)
            .build());
  }

  private Consumer<EncodedImage> produceResults() {
    mDecodeProducer.produceResults(mConsumer, mProducerContext);
    ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
    verify(mInputProducer).produceResults(consumerCaptor.capture(), eq(mProducerContext));
    return consumerCaptor.getValue();
  }

  private JobScheduler.JobRunnable getJobRunnable() throws Exception {
    ArgumentCaptor<JobScheduler.JobRunnable> runnableCaptor =
        ArgumentCaptor.forClass(JobScheduler.JobRunnable.class);
    PowerMockito.verifyNew(JobScheduler.class)
        .withArguments(eq(mExecutor), runnableCaptor.capture(), anyInt());
    return runnableCaptor.getValue();
  }

  private static PooledByteBuffer mockPooledByteBuffer(int size) {
    PooledByteBuffer pooledByteBuffer = mock(PooledByteBuffer.class);
    when(pooledByteBuffer.size()).thenReturn(size);
    return pooledByteBuffer;
  }
}
