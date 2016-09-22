/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import android.net.Uri;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.decoder.ProgressiveJpegParser;
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.memory.ByteArrayPool;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.Mock;
import org.powermock.api.mockito.*;
import org.powermock.core.classloader.annotations.*;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@Config(manifest= Config.NONE)
@PrepareForTest({JobScheduler.class, ProgressiveJpegParser.class, DecodeProducer.class})
public class DecodeProducerTest {

  private static final ImageDecodeOptions IMAGE_DECODE_OPTIONS = ImageDecodeOptions.newBuilder()
      .setMinDecodeIntervalMs(100)
      .build();
  private static final int PREVIEW_SCAN = 2;
  private static final int IGNORED_SCAN = 3;
  private static final int GOOD_ENOUGH_SCAN = 5;
  private static final int IMAGE_SIZE = 1000;

  @Mock public ByteArrayPool mByteArrayPool;
  @Mock public Executor mExecutor;
  @Mock public ImageDecoder mImageDecoder;
  private ProgressiveJpegConfig mProgressiveJpegConfig;
  @Mock public Producer mInputProducer;

  private ImageRequest mImageRequest;
  private String mRequestId;
  private CloseableReference<PooledByteBuffer> mByteBufferRef;
  private EncodedImage mEncodedImage;
  @Mock public ProducerListener mProducerListener;
  private SettableProducerContext mProducerContext;
  @Mock public Consumer mConsumer;

  @Mock public ProgressiveJpegParser mProgressiveJpegParser;
  @Mock public JobScheduler mJobScheduler;

  private DecodeProducer mDecodeProducer;

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    mProgressiveJpegConfig = new SimpleProgressiveJpegConfig(
        new SimpleProgressiveJpegConfig.DynamicValueConfig() {
          public List<Integer> getScansToDecode() {
            return Arrays.asList(PREVIEW_SCAN, GOOD_ENOUGH_SCAN);
          }

          public int getGoodEnoughScanNumber() {
            return GOOD_ENOUGH_SCAN;
          }
        });

    PowerMockito.mockStatic(ProgressiveJpegParser.class);
    PowerMockito.whenNew(ProgressiveJpegParser.class).withAnyArguments()
        .thenReturn(mProgressiveJpegParser);
    PowerMockito.mockStatic(JobScheduler.class);
    PowerMockito.whenNew(JobScheduler.class).withAnyArguments()
        .thenReturn(mJobScheduler);

    mDecodeProducer = new DecodeProducer(
        mByteArrayPool,
        mExecutor,
        mImageDecoder,
        mProgressiveJpegConfig,
        false, /* Set downsampleEnabled to false */
        false, /* Set resizeAndRotateForNetwork to false */
        mInputProducer);

    PooledByteBuffer pooledByteBuffer = mockPooledByteBuffer(IMAGE_SIZE);
    mByteBufferRef = CloseableReference.of(pooledByteBuffer);
    mEncodedImage = new EncodedImage(mByteBufferRef);
  }

  private static EncodedImage mockEncodedImage(CloseableReference<PooledByteBuffer> ref) {
    return new EncodedImage(ref);
  }

  @Test
  public void testNewResult_Final() {
    setupNetworkUri();
    Consumer<EncodedImage> consumer = produceResults();

    when(mJobScheduler.updateJob(mEncodedImage, true)).thenReturn(true);
    consumer.onNewResult(mEncodedImage, true);

    verify(mJobScheduler).updateJob(mEncodedImage, true);
    verify(mJobScheduler).scheduleJob();
    verifyZeroInteractions(mProgressiveJpegParser);
  }

  @Test
  public void testNewResult_Final_Local() {
    setupLocalUri();
    Consumer<EncodedImage> consumer = produceResults();

    when(mJobScheduler.updateJob(mEncodedImage, true)).thenReturn(true);
    consumer.onNewResult(mEncodedImage, true);

    verify(mJobScheduler).updateJob(mEncodedImage, true);
    verify(mJobScheduler).scheduleJob();
    verifyZeroInteractions(mProgressiveJpegParser);
  }

  @Test
  public void testNewResult_Intermediate_NonJPEG() {
    setupNetworkUri();
    Consumer<EncodedImage> consumer = produceResults();

    when(mJobScheduler.updateJob(mEncodedImage, false)).thenReturn(true);
    when(mProgressiveJpegParser.parseMoreData(mEncodedImage)).thenReturn(false);
    consumer.onNewResult(mEncodedImage, false);

    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);

    verify(mJobScheduler).updateJob(mEncodedImage, false);
    verify(mProgressiveJpegParser).parseMoreData(argumentCaptor.capture());
    verify(mJobScheduler, never()).scheduleJob();
    assertSame(
        ((EncodedImage) argumentCaptor.getValue())
            .getUnderlyingReferenceTestOnly(),
        mByteBufferRef.getUnderlyingReferenceTestOnly());
  }

  @Test
  public void testNewResult_Intermediate_Local() {
    setupLocalUri();
    Consumer<EncodedImage> consumer = produceResults();

    when(mJobScheduler.updateJob(mEncodedImage, false)).thenReturn(true);
    consumer.onNewResult(mEncodedImage, false);

    verify(mJobScheduler, never()).updateJob(mEncodedImage, false);
    verify(mProgressiveJpegParser, never()).parseMoreData(mEncodedImage);
    verify(mJobScheduler, never()).scheduleJob();
  }

  @Test
  public void testNewResult_Intermediate_pJPEG() {
    setupNetworkUri();
    Consumer<EncodedImage> consumer = produceResults();

    InOrder inOrder = inOrder(mJobScheduler, mProgressiveJpegParser);

    ArgumentCaptor<EncodedImage> argumentCaptor =
        ArgumentCaptor.forClass(EncodedImage.class);

    // preview scan; schedule
    when(mJobScheduler.updateJob(mEncodedImage, false)).thenReturn(true);
    when(mProgressiveJpegParser.parseMoreData(any(EncodedImage.class))).thenReturn(true);
    when(mProgressiveJpegParser.getBestScanNumber()).thenReturn(PREVIEW_SCAN);
    consumer.onNewResult(mEncodedImage, false);
    inOrder.verify(mJobScheduler).updateJob(mEncodedImage, false);
    inOrder.verify(mProgressiveJpegParser).parseMoreData(argumentCaptor.capture());
    inOrder.verify(mJobScheduler).scheduleJob();
    assertSame(
        ((EncodedImage) argumentCaptor.getValue())
            .getUnderlyingReferenceTestOnly(),
        mByteBufferRef.getUnderlyingReferenceTestOnly());

    // no data parsed; ignore
    PooledByteBuffer pooledByteBuffer2 = mockPooledByteBuffer(210);
    CloseableReference<PooledByteBuffer> ref2 = CloseableReference.of(pooledByteBuffer2);
    EncodedImage encodedImage2 = mockEncodedImage(ref2);
    when(mJobScheduler.updateJob(encodedImage2, false)).thenReturn(true);
    when(mProgressiveJpegParser.parseMoreData(encodedImage2)).thenReturn(false);
    when(mProgressiveJpegParser.getBestScanNumber()).thenReturn(PREVIEW_SCAN);
    consumer.onNewResult(encodedImage2, false);
    inOrder.verify(mJobScheduler).updateJob(encodedImage2, false);
    inOrder.verify(mProgressiveJpegParser).parseMoreData(argumentCaptor.capture());
    inOrder.verify(mJobScheduler, never()).scheduleJob();
    assertSame(
        ((EncodedImage) argumentCaptor.getValue())
            .getUnderlyingReferenceTestOnly(),
        ref2.getUnderlyingReferenceTestOnly());

    // same scan; ignore
    PooledByteBuffer pooledByteBuffer3 = mockPooledByteBuffer(220);
    CloseableReference<PooledByteBuffer> ref3 = CloseableReference.of(pooledByteBuffer3);
    EncodedImage encodedImage3 = mockEncodedImage(ref3);
    when(mJobScheduler.updateJob(encodedImage3, false)).thenReturn(true);
    when(mProgressiveJpegParser.parseMoreData(encodedImage3)).thenReturn(true);
    when(mProgressiveJpegParser.getBestScanNumber()).thenReturn(PREVIEW_SCAN);
    consumer.onNewResult(encodedImage3, false);
    inOrder.verify(mJobScheduler).updateJob(encodedImage3, false);
    inOrder.verify(mProgressiveJpegParser).parseMoreData(argumentCaptor.capture());
    inOrder.verify(mJobScheduler, never()).scheduleJob();
    assertSame(
        ((EncodedImage) argumentCaptor.getValue())
            .getUnderlyingReferenceTestOnly(),
        ref3.getUnderlyingReferenceTestOnly());

    // scan not for decode; ignore
    PooledByteBuffer pooledByteBuffer4 = mockPooledByteBuffer(300);
    CloseableReference<PooledByteBuffer> ref4 = CloseableReference.of(pooledByteBuffer4);
    EncodedImage encodedImage4 = mockEncodedImage(ref4);
    when(mJobScheduler.updateJob(encodedImage4, false)).thenReturn(true);
    when(mProgressiveJpegParser.parseMoreData(encodedImage4)).thenReturn(true);
    when(mProgressiveJpegParser.getBestScanNumber()).thenReturn(IGNORED_SCAN);
    consumer.onNewResult(encodedImage4, false);
    inOrder.verify(mJobScheduler).updateJob(encodedImage4, false);
    inOrder.verify(mProgressiveJpegParser).parseMoreData(argumentCaptor.capture());
    inOrder.verify(mJobScheduler, never()).scheduleJob();
    assertSame(
        ((EncodedImage) argumentCaptor.getValue())
            .getUnderlyingReferenceTestOnly(),
        ref4.getUnderlyingReferenceTestOnly());

    // good-enough scan; schedule
    PooledByteBuffer pooledByteBuffer5 = mockPooledByteBuffer(500);
    CloseableReference<PooledByteBuffer> ref5 = CloseableReference.of(pooledByteBuffer5);
    EncodedImage encodedImage5 = mockEncodedImage(ref5);
    when(mJobScheduler.updateJob(encodedImage5, false)).thenReturn(true);
    when(mProgressiveJpegParser.parseMoreData(encodedImage5)).thenReturn(true);
    when(mProgressiveJpegParser.getBestScanNumber()).thenReturn(GOOD_ENOUGH_SCAN);
    consumer.onNewResult(encodedImage5, false);
    inOrder.verify(mJobScheduler).updateJob(encodedImage5, false);
    inOrder.verify(mProgressiveJpegParser).parseMoreData(argumentCaptor.capture());
    inOrder.verify(mJobScheduler).scheduleJob();
    assertSame(
        ((EncodedImage) argumentCaptor.getValue())
            .getUnderlyingReferenceTestOnly(),
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

    jobRunnable.run(mEncodedImage, true);

    InOrder inOrder = inOrder(mProducerListener, mImageDecoder);
    inOrder.verify(mProducerListener).onProducerStart(mRequestId, DecodeProducer.PRODUCER_NAME);
    inOrder.verify(mImageDecoder).decodeImage(
        mEncodedImage,
        IMAGE_SIZE,
        ImmutableQualityInfo.FULL_QUALITY,
        IMAGE_DECODE_OPTIONS);
    inOrder.verify(mProducerListener).onProducerFinishWithSuccess(
        eq(mRequestId),
        eq(DecodeProducer.PRODUCER_NAME),
        any(Map.class));
  }

  @Test
  public void testDecode_Intermediate_pJPEG() throws Exception {
    setupNetworkUri();
    produceResults();
    JobScheduler.JobRunnable jobRunnable = getJobRunnable();

    when(mProgressiveJpegParser.isJpeg()).thenReturn(true);
    when(mProgressiveJpegParser.getBestScanEndOffset()).thenReturn(200);
    when(mProgressiveJpegParser.getBestScanNumber()).thenReturn(PREVIEW_SCAN);
    jobRunnable.run(mEncodedImage, false);

    InOrder inOrder = inOrder(mProducerListener, mImageDecoder);
    inOrder.verify(mProducerListener).onProducerStart(mRequestId, DecodeProducer.PRODUCER_NAME);
    inOrder.verify(mImageDecoder).decodeImage(
        mEncodedImage,
        200,
        ImmutableQualityInfo.of(PREVIEW_SCAN, false, false),
        IMAGE_DECODE_OPTIONS);
    inOrder.verify(mProducerListener).onProducerFinishWithSuccess(
        eq(mRequestId),
        eq(DecodeProducer.PRODUCER_NAME),
        any(Map.class));
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void testDecode_Failure() throws Exception {
    setupNetworkUri();
    produceResults();
    JobScheduler.JobRunnable jobRunnable = getJobRunnable();

    Exception exception = new RuntimeException();
    when(mImageDecoder.decodeImage(
        mEncodedImage,
        IMAGE_SIZE,
        ImmutableQualityInfo.FULL_QUALITY,
        IMAGE_DECODE_OPTIONS))
        .thenThrow(exception);
    jobRunnable.run(mEncodedImage, true);

    InOrder inOrder = inOrder(mProducerListener, mImageDecoder);
    inOrder.verify(mProducerListener).onProducerStart(mRequestId, DecodeProducer.PRODUCER_NAME);
    inOrder.verify(mImageDecoder).decodeImage(
        mEncodedImage,
        IMAGE_SIZE,
        ImmutableQualityInfo.FULL_QUALITY,
        IMAGE_DECODE_OPTIONS);
    inOrder.verify(mProducerListener).onProducerFinishWithFailure(
        eq(mRequestId),
        eq(DecodeProducer.PRODUCER_NAME),
        eq(exception),
        any(Map.class));
  }

  private void setupNetworkUri() {
    //Uri.parse("file://path/image")
    mImageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse("http://www.fb.com/image"))
        .setProgressiveRenderingEnabled(true)
        .setImageDecodeOptions(IMAGE_DECODE_OPTIONS)
        .build();
    mRequestId = "networkRequest1";
    mProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mProducerListener,
        mock(Object.class),
        ImageRequest.RequestLevel.FULL_FETCH,
        /* isPrefetch */ false,
        /* isIntermediateResultExpected */ true,
        Priority.MEDIUM);
  }

  private void setupLocalUri() {
    mImageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse("file://path/image"))
        .setProgressiveRenderingEnabled(true) // this should be ignored
        .setImageDecodeOptions(IMAGE_DECODE_OPTIONS)
        .build();
    mRequestId = "localRequest1";
    mProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mProducerListener,
        mock(Object.class),
        ImageRequest.RequestLevel.FULL_FETCH,
        /* isPrefetch */ false,
        /* isIntermediateResultExpected */ true,
        Priority.MEDIUM);
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
