/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import java.io.IOException;
import java.lang.reflect.Array;
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
@Config(manifest = Config.NONE)
public class ThumbnailBranchProducerTest {

  private static final int[] THUMBNAIL_WIDTHS = {100, 400, 800};
  private static final int[] THUMBNAIL_HEIGHTS = {100, 600, 400};

  private static final EncodedImage THROW_FAILURE = mock(EncodedImage.class);

  @Mock private ProducerContext mProducerContext;
  @Mock private ImageRequest mImageRequest;
  @Mock private Consumer<EncodedImage> mImageConsumer;
  private ThumbnailProducer<EncodedImage>[] mThumbnailProducers;

  private ThumbnailBranchProducer mProducer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(mProducerContext.getImageRequest()).thenReturn(mImageRequest);

    mThumbnailProducers =
        (ThumbnailProducer<EncodedImage>[]) Array.newInstance(ThumbnailProducer.class, 3);
    for (int i = 0; i < mThumbnailProducers.length; i++) {
      mThumbnailProducers[i] = mock(ThumbnailProducer.class);
      mockProducerToSupportSize(mThumbnailProducers[i], THUMBNAIL_WIDTHS[i], THUMBNAIL_HEIGHTS[i]);
    }

    mProducer = new ThumbnailBranchProducer(mThumbnailProducers);
  }

  @Test
  public void testNullReturnedIfNoResizeOptions() {
    mProducer.produceResults(mImageConsumer, mProducerContext);

    verify(mImageConsumer).onNewResult(null, Consumer.IS_LAST);
    verifyZeroInteractions((Object[]) mThumbnailProducers);
  }

  @Test
  public void testFirstProducerUsedIfSufficientForResizeOptions() {
    mockRequestWithResizeOptions(THUMBNAIL_WIDTHS[0], THUMBNAIL_HEIGHTS[0]);

    EncodedImage firstImage = mockEncodedImage(THUMBNAIL_WIDTHS[0], THUMBNAIL_HEIGHTS[0], 0);
    mockProducersToProduce(firstImage);

    mProducer.produceResults(mImageConsumer, mProducerContext);

    verify(mImageConsumer).onNewResult(firstImage, Consumer.IS_LAST);
    verifyZeroInteractions(mThumbnailProducers[1], mThumbnailProducers[2]);
  }

  @Test
  public void testSecondProducerUsedIfSufficientForResizeOptions() {
    mockRequestWithResizeOptions(THUMBNAIL_WIDTHS[0] + 50, THUMBNAIL_HEIGHTS[0] + 50);

    EncodedImage secondImage = mockEncodedImage(THUMBNAIL_WIDTHS[1], THUMBNAIL_HEIGHTS[1], 0);
    mockProducersToProduce(
        mockEncodedImage(THUMBNAIL_WIDTHS[0] + 50, THUMBNAIL_HEIGHTS[0] + 50, 0),
        secondImage);

    mProducer.produceResults(mImageConsumer, mProducerContext);

    verify(mImageConsumer).onNewResult(secondImage, Consumer.IS_LAST);
    verifyZeroInteractions(mThumbnailProducers[2]);
  }

  @Test
  public void testFinalProducerUsedIfFirstTwoReturnNullOrFailure() {
    mockRequestWithResizeOptions(THUMBNAIL_WIDTHS[0] - 50, THUMBNAIL_HEIGHTS[0] - 50);

    EncodedImage thirdImage = mockEncodedImage(THUMBNAIL_WIDTHS[2], THUMBNAIL_HEIGHTS[2], 0);
    mockProducersToProduce(THROW_FAILURE, null, thirdImage);

    mProducer.produceResults(mImageConsumer, mProducerContext);

    verify(mImageConsumer).onNewResult(thirdImage, Consumer.IS_LAST);
    verifyAllProducersRequestedForResults();
  }

  @Test
  public void testNullReturnedIfNoProducerSufficientForResizeOptions() {
    int width = THUMBNAIL_WIDTHS[2] + 50;
    int height = THUMBNAIL_HEIGHTS[2] + 50;
    mockRequestWithResizeOptions(width, height);

    mProducer.produceResults(mImageConsumer, mProducerContext);

    verify(mImageConsumer).onNewResult(null, Consumer.IS_LAST);
    ResizeOptions resizeOptions = new ResizeOptions(width, height);
    verify(mThumbnailProducers[0]).canProvideImageForSize(resizeOptions);
    verify(mThumbnailProducers[1]).canProvideImageForSize(resizeOptions);
    verify(mThumbnailProducers[2]).canProvideImageForSize(resizeOptions);
    verifyNoMoreInteractions((Object[]) mThumbnailProducers);
  }

  @Test
  public void testNullReturnedIfAllProducersFailOrReturnNullEndingWithNull() {
    int width = THUMBNAIL_WIDTHS[0] - 10;
    int height = THUMBNAIL_HEIGHTS[0] - 10;
    mockRequestWithResizeOptions(width, height);

    mockProducersToProduce(null, THROW_FAILURE, null);

    mProducer.produceResults(mImageConsumer, mProducerContext);

    verify(mImageConsumer).onNewResult(null, Consumer.IS_LAST);
    verifyAllProducersRequestedForResults();
  }

  @Test
  public void testFailureReturnedIfAllProducersFailOrReturnNullEndingWithFailure() {
    int width = THUMBNAIL_WIDTHS[0];
    int height = THUMBNAIL_HEIGHTS[0];
    mockRequestWithResizeOptions(width, height);

    mockProducersToProduce(null, null, THROW_FAILURE);

    mProducer.produceResults(mImageConsumer, mProducerContext);

    verify(mImageConsumer).onFailure(any(Throwable.class));
    verifyAllProducersRequestedForResults();
  }

  @Test
  public void testFinalProducerUsedIfFirstTwoReturnTooSmallImages() {
    int desiredWidth = THUMBNAIL_WIDTHS[0] - 50;
    int desiredHeight = THUMBNAIL_HEIGHTS[0] - 50;
    mockRequestWithResizeOptions(desiredWidth, desiredHeight);

    EncodedImage thirdImage = mockEncodedImage(THUMBNAIL_WIDTHS[2], THUMBNAIL_HEIGHTS[2], 0);
    mockProducersToProduce(
        mockEncodedImage(desiredWidth / 2, desiredHeight / 2, 0),
        mockEncodedImage(desiredWidth / 2, desiredHeight / 2, 0),
        thirdImage);

    mProducer.produceResults(mImageConsumer, mProducerContext);

    verify(mImageConsumer).onNewResult(thirdImage, Consumer.IS_LAST);
    verifyAllProducersRequestedForResults();
  }

  @Test
  public void testSecondProducerUsedIfImageBigEnoughWhenRotated() {
    mockRequestWithResizeOptions(THUMBNAIL_WIDTHS[1], THUMBNAIL_HEIGHTS[1]);

    EncodedImage secondImage =
        mockEncodedImage(THUMBNAIL_HEIGHTS[1] * 3 / 4, THUMBNAIL_WIDTHS[1] * 3 / 4, 90);
    mockProducersToProduce(
        mockEncodedImage(THUMBNAIL_WIDTHS[0], THUMBNAIL_HEIGHTS[0], 0),
        secondImage);

    mProducer.produceResults(mImageConsumer, mProducerContext);

    verify(mImageConsumer).onNewResult(secondImage, Consumer.IS_LAST);
    verifyZeroInteractions(mThumbnailProducers[2]);
  }

  @Test
  public void testNullReturnedIfLastImageNotBigEnoughWhenRotated() {
    mockRequestWithResizeOptions(THUMBNAIL_WIDTHS[2], THUMBNAIL_HEIGHTS[2]);

    mockProducersToProduce(
        mockEncodedImage(THUMBNAIL_WIDTHS[0], THUMBNAIL_HEIGHTS[0], 0),
        mockEncodedImage(THUMBNAIL_WIDTHS[1], THUMBNAIL_HEIGHTS[1], 0),
        mockEncodedImage(THUMBNAIL_HEIGHTS[2] / 2, THUMBNAIL_WIDTHS[2] / 2, 90));

    mProducer.produceResults(mImageConsumer, mProducerContext);

    verify(mImageConsumer).onNewResult(null, Consumer.IS_LAST);
    verify(mThumbnailProducers[2]).produceResults(any(Consumer.class), any(ProducerContext.class));
  }

  private void mockRequestWithResizeOptions(int width, int height) {
    ResizeOptions resizeOptions = new ResizeOptions(width, height);
    when(mImageRequest.getResizeOptions()).thenReturn(resizeOptions);
  }

  private static EncodedImage mockEncodedImage(int width, int height, int rotationAngle) {
    EncodedImage mockImage = mock(EncodedImage.class);
    when(mockImage.getWidth()).thenReturn(width);
    when(mockImage.getHeight()).thenReturn(height);
    when(mockImage.getRotationAngle()).thenReturn(rotationAngle);
    return mockImage;
  }

  private static void mockProducerToSupportSize(
      ThumbnailProducer<EncodedImage> mockProducer,
      final int width,
      final int height) {
    when(mockProducer.canProvideImageForSize(any(ResizeOptions.class))).then(new Answer<Boolean>() {
      @Override
      public Boolean answer(InvocationOnMock invocation) throws Throwable {
        ResizeOptions resizeOptions = (ResizeOptions) invocation.getArguments()[0];
        return resizeOptions.width <= width && resizeOptions.height <= height;
      }
    });
  }

  private void mockProducersToProduce(final EncodedImage... images) {
    for (int i = 0; i < images.length; i++) {
      final EncodedImage image = images[i];
      whenProduceResultsCalledTrigger(mThumbnailProducers[i], new ConsumerCallback() {
        @Override
        public void callback(Consumer<EncodedImage> consumer) {
          if (image == THROW_FAILURE) {
            consumer.onFailure(new IOException("IMAGE FAILED"));
          } else {
            consumer.onNewResult(image, Consumer.IS_LAST);
          }
        }
      });
    }
  }

  private static void whenProduceResultsCalledTrigger(
      ThumbnailProducer<EncodedImage> mockProducer,
      final ConsumerCallback callback) {
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Consumer<EncodedImage> consumer = (Consumer<EncodedImage>) invocation.getArguments()[0];
        callback.callback(consumer);
        return null;
      }
    }).when(mockProducer).produceResults(any(Consumer.class), any(ProducerContext.class));
  }

  private void verifyAllProducersRequestedForResults() {
    verify(mThumbnailProducers[0]).produceResults(any(Consumer.class), any(ProducerContext.class));
    verify(mThumbnailProducers[1]).produceResults(any(Consumer.class), any(ProducerContext.class));
    verify(mThumbnailProducers[2]).produceResults(any(Consumer.class), any(ProducerContext.class));
  }

  private interface ConsumerCallback {

    void callback(Consumer<EncodedImage> consumer);
  }
}
