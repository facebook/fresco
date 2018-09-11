/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class BranchOnSeparateImagesProducerTest {
  private static final int WIDTH = 10;
  private static final int HEIGHT = 20;
  @Mock public Consumer<EncodedImage> mConsumer;
  @Mock public ProducerContext mProducerContext;
  @Mock public ImageRequest mImageRequest;
  @Mock public Exception mException;

  private Producer<EncodedImage> mInputProducer1;
  private Producer<EncodedImage> mInputProducer2;
  private Consumer<EncodedImage> mFirstProducerConsumer;
  private Consumer<EncodedImage> mSecondProducerConsumer;
  private EncodedImage mIntermediateResult;
  private EncodedImage mFirstProducerFinalResult;
  private EncodedImage mSecondProducerFinalResult;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mInputProducer1 = mock(Producer.class);
    mInputProducer2 = mock(Producer.class);
    BranchOnSeparateImagesProducer branchOnSeparateImagesProducer =
        new BranchOnSeparateImagesProducer(mInputProducer1, mInputProducer2);

    when(mProducerContext.getImageRequest()).thenReturn(mImageRequest);
    when(mImageRequest.getPreferredWidth()).thenReturn(WIDTH);
    when(mImageRequest.getPreferredHeight()).thenReturn(HEIGHT);
    when(mImageRequest.getResizeOptions()).thenReturn(new ResizeOptions(WIDTH, HEIGHT));
    when(mImageRequest.getLocalThumbnailPreviewsEnabled()).thenReturn(true);
    mIntermediateResult = mock(EncodedImage.class);
    mFirstProducerFinalResult = mock(EncodedImage.class);
    mSecondProducerFinalResult = mock(EncodedImage.class);
    PooledByteBuffer mockByteBuffer = mock(PooledByteBuffer.class);
    when(mIntermediateResult.getByteBufferRef()).thenReturn(CloseableReference.of(mockByteBuffer));
    when(mFirstProducerFinalResult.getByteBufferRef())
        .thenReturn(CloseableReference.of(mockByteBuffer));
    when(mSecondProducerFinalResult.getByteBufferRef())
        .thenReturn(CloseableReference.of(mockByteBuffer));

    mFirstProducerConsumer = null;
    mSecondProducerConsumer = null;
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            mFirstProducerConsumer = (Consumer<EncodedImage>) invocation.getArguments()[0];
            return null;
          }
        }).when(mInputProducer1).produceResults(any(Consumer.class), any(ProducerContext.class));
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            mSecondProducerConsumer = (Consumer<EncodedImage>) invocation.getArguments()[0];
            return null;
          }
        }).when(mInputProducer2).produceResults(any(Consumer.class), any(ProducerContext.class));
    branchOnSeparateImagesProducer.produceResults(mConsumer, mProducerContext);
  }

  @Test
  public void testFirstProducerReturnsIntermediateResultThenGoodEnoughResult() {
    EncodedImage intermediateEncodedImage = new EncodedImage(
        mIntermediateResult.getByteBufferRef());
    intermediateEncodedImage.setImageFormat(DefaultImageFormats.JPEG);
    intermediateEncodedImage.setRotationAngle(-1);
    intermediateEncodedImage.setWidth(WIDTH);
    intermediateEncodedImage.setHeight(HEIGHT);
    mFirstProducerConsumer.onNewResult(intermediateEncodedImage, Consumer.NO_FLAGS);
    verify(mConsumer).onNewResult(intermediateEncodedImage, Consumer.NO_FLAGS);
    EncodedImage finalEncodedImage = new EncodedImage(
        mFirstProducerFinalResult.getByteBufferRef());
    finalEncodedImage.setImageFormat(DefaultImageFormats.JPEG);
    finalEncodedImage.setRotationAngle(-1);
    finalEncodedImage.setWidth(WIDTH);
    finalEncodedImage.setHeight(HEIGHT);
    mFirstProducerConsumer.onNewResult(finalEncodedImage, Consumer.IS_LAST);
    verify(mConsumer).onNewResult(finalEncodedImage, Consumer.IS_LAST);
    verify(mInputProducer2, never()).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  @Test
  public void testFirstProducerResultNotGoodEnough() {
    EncodedImage firstProducerEncodedImage = new EncodedImage(
        mFirstProducerFinalResult.getByteBufferRef());
    firstProducerEncodedImage.setRotationAngle(-1);
    firstProducerEncodedImage.setWidth(WIDTH / 2);
    firstProducerEncodedImage.setHeight(HEIGHT / 2);
    mFirstProducerConsumer.onNewResult(firstProducerEncodedImage, Consumer.IS_LAST);
    verify(mConsumer).onNewResult(firstProducerEncodedImage, Consumer.NO_FLAGS);

    EncodedImage intermediateEncodedImage = new EncodedImage(
        mIntermediateResult.getByteBufferRef());
    intermediateEncodedImage.setRotationAngle(-1);
    intermediateEncodedImage.setWidth(WIDTH / 2);
    intermediateEncodedImage.setHeight(HEIGHT / 2);
    mSecondProducerConsumer.onNewResult(intermediateEncodedImage, Consumer.NO_FLAGS);
    verify(mConsumer).onNewResult(intermediateEncodedImage, Consumer.NO_FLAGS);
    EncodedImage secondProducerEncodedImage = new EncodedImage(
        mSecondProducerFinalResult.getByteBufferRef());
    secondProducerEncodedImage.setRotationAngle(-1);
    secondProducerEncodedImage.setWidth(WIDTH / 2);
    secondProducerEncodedImage.setHeight(HEIGHT / 2);
    mSecondProducerConsumer.onNewResult(secondProducerEncodedImage, Consumer.IS_LAST);
    verify(mConsumer).onNewResult(secondProducerEncodedImage, Consumer.IS_LAST);
  }

  @Test
  public void testFirstProducerReturnsNull() {
    mFirstProducerConsumer.onNewResult(null, Consumer.IS_LAST);
    verify(mConsumer, never()).onNewResult(isNull(EncodedImage.class), anyInt());
    EncodedImage finalEncodedImage = new EncodedImage(mIntermediateResult.getByteBufferRef());
    mSecondProducerConsumer.onNewResult(finalEncodedImage, Consumer.IS_LAST);
    verify(mConsumer).onNewResult(finalEncodedImage, Consumer.IS_LAST);
  }

  @Test
  public void testFirstProducerFails() {
    mFirstProducerConsumer.onFailure(mException);
    verify(mConsumer, never()).onFailure(mException);
    EncodedImage finalEncodedImage = new EncodedImage(mIntermediateResult.getByteBufferRef());
    mSecondProducerConsumer.onNewResult(finalEncodedImage, Consumer.IS_LAST);
    verify(mConsumer).onNewResult(finalEncodedImage, Consumer.IS_LAST);
  }

  @Test
  public void testFirstProducerCancellation() {
    mFirstProducerConsumer.onCancellation();
    verify(mConsumer).onCancellation();
    verify(mInputProducer2, never()).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  @Test
  public void testFirstProducerReturnsTwoResultsThumbnailsNotAllowed() {
    when(mImageRequest.getLocalThumbnailPreviewsEnabled()).thenReturn(false);
    EncodedImage intermediateEncodedImage = new EncodedImage(
        mIntermediateResult.getByteBufferRef());
    intermediateEncodedImage.setImageFormat(DefaultImageFormats.JPEG);
    intermediateEncodedImage.setRotationAngle(-1);
    intermediateEncodedImage.setWidth(WIDTH / 2);
    intermediateEncodedImage.setHeight(HEIGHT / 2);
    mFirstProducerConsumer.onNewResult(intermediateEncodedImage, Consumer.NO_FLAGS);
    verify(mConsumer, never()).onNewResult(intermediateEncodedImage, Consumer.NO_FLAGS);
    EncodedImage finalEncodedImage = new EncodedImage(
        mFirstProducerFinalResult.getByteBufferRef());
    finalEncodedImage.setImageFormat(DefaultImageFormats.JPEG);
    finalEncodedImage.setRotationAngle(-1);
    finalEncodedImage.setWidth(WIDTH);
    finalEncodedImage.setHeight(HEIGHT);
    mFirstProducerConsumer.onNewResult(finalEncodedImage, Consumer.IS_LAST);
    verify(mConsumer).onNewResult(finalEncodedImage, Consumer.IS_LAST);
    verify(mInputProducer2, never()).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  @Test
  public void testFirstProducerResultNotGoodEnoughThumbnailsNotAllowed() {
    when(mImageRequest.getLocalThumbnailPreviewsEnabled()).thenReturn(false);
    EncodedImage firstProducerEncodedImage = new EncodedImage(
        mFirstProducerFinalResult.getByteBufferRef());
    firstProducerEncodedImage.setRotationAngle(-1);
    firstProducerEncodedImage.setWidth(WIDTH / 2);
    firstProducerEncodedImage.setHeight(HEIGHT / 2);
    mFirstProducerConsumer.onNewResult(firstProducerEncodedImage, Consumer.IS_LAST);
    verify(mConsumer, never()).onNewResult(firstProducerEncodedImage, Consumer.NO_FLAGS);

    EncodedImage intermediateEncodedImage = new EncodedImage(
        mIntermediateResult.getByteBufferRef());
    intermediateEncodedImage.setRotationAngle(-1);
    intermediateEncodedImage.setWidth(WIDTH / 2);
    intermediateEncodedImage.setHeight(HEIGHT / 2);
    mSecondProducerConsumer.onNewResult(intermediateEncodedImage, Consumer.NO_FLAGS);
    verify(mConsumer).onNewResult(intermediateEncodedImage, Consumer.NO_FLAGS);
    EncodedImage secondProducerEncodedImage = new EncodedImage(
        mFirstProducerFinalResult.getByteBufferRef());
    secondProducerEncodedImage.setRotationAngle(-1);
    secondProducerEncodedImage.setWidth(WIDTH / 2);
    secondProducerEncodedImage.setHeight(HEIGHT / 2);
    mSecondProducerConsumer.onNewResult(secondProducerEncodedImage, Consumer.IS_LAST);
    verify(mConsumer).onNewResult(secondProducerEncodedImage, Consumer.IS_LAST);
  }

}
