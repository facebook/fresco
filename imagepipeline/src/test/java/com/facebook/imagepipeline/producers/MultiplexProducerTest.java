/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.BitmapMemoryCacheKey;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Checks basic properties of the multiplex producer, that is:
 *   - identical keys should be combined into the same request.
 *   - non-identical keys get their own request.
 *   - requests should only be cancelled if all underlying requests are cancelled.
 *   - requests should be cleared when they finish.
 *
 * <p>This test happens to use {@link BitmapMemoryCacheKeyMultiplexProducer}. The subclasses are so
 * similar that it's not worth doing a separate test for each one.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class MultiplexProducerTest {

  /**
   * An extra flag to check we maintain other flags than just whether this is the last result
   */
  private final @Consumer.Status int TEST_FLAG = 1 << 11;

  @Mock public CacheKeyFactory mCacheKeyFactory;
  @Mock public Producer mInputProducer;
  @Mock public Exception mException;
  @Mock public ProducerListener mProducerListener;
  @Mock public Object mCallerContext;
  private SettableProducerContext mProducerContext1;
  private SettableProducerContext mProducerContext2;
  private SettableProducerContext mProducerContext3;
  private ImageRequest mImageRequest1;
  private ImageRequest mImageRequest2;
  private BitmapMemoryCacheKey mBitmapMemoryCacheKey1;
  private BitmapMemoryCacheKey mBitmapMemoryCacheKey2;
  private Consumer<CloseableReference<CloseableImage>> mConsumer1;
  private Consumer<CloseableReference<CloseableImage>> mConsumer2;
  private Consumer<CloseableReference<CloseableImage>> mConsumer3;
  private Consumer<CloseableReference<CloseableImage>> mForwardingConsumer1;
  private Consumer<CloseableReference<CloseableImage>> mForwardingConsumer2;
  private BaseProducerContext mMultiplexedContext1;
  private BaseProducerContext mMultiplexedContext2;
  private CloseableImage mFinalCloseableImage1;
  private CloseableImage mFinalCloseableImage2;
  private CloseableImage mIntermediateCloseableImage1;
  private CloseableImage mIntermediateCloseableImage2;
  private CloseableReference<CloseableImage> mFinalImageReference1;
  private CloseableReference<CloseableImage> mFinalImageReference2;
  private CloseableReference<CloseableImage> mIntermediateImageReference1;
  private CloseableReference<CloseableImage> mIntermediateImageReference2;
  private BitmapMemoryCacheKeyMultiplexProducer mMultiplexProducer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mMultiplexProducer =
        new BitmapMemoryCacheKeyMultiplexProducer(mCacheKeyFactory, mInputProducer);
    mImageRequest1 = mock(ImageRequest.class);
    mImageRequest2 = mock(ImageRequest.class);
    mProducerContext1 = new SettableProducerContext(
        mImageRequest1,
        "id1",
        mProducerListener,
        mCallerContext,
        ImageRequest.RequestLevel.FULL_FETCH,
        false,
        true,
        Priority.MEDIUM);
    mProducerContext2 = new SettableProducerContext(
        mImageRequest1,
        "id2",
        mProducerListener,
        mCallerContext,
        ImageRequest.RequestLevel.FULL_FETCH,
        false,
        true,
        Priority.MEDIUM);
    mProducerContext3 = new SettableProducerContext(
        mImageRequest2,
        "id3",
        mProducerListener,
        mCallerContext,
        ImageRequest.RequestLevel.FULL_FETCH,
        false,
        true,
        Priority.MEDIUM);
    mBitmapMemoryCacheKey1 = mock(BitmapMemoryCacheKey.class);
    mBitmapMemoryCacheKey2 = mock(BitmapMemoryCacheKey.class);
    mConsumer1 = mock(Consumer.class);
    mConsumer2 = mock(Consumer.class);
    mConsumer3 = mock(Consumer.class);
    mFinalCloseableImage1 = mock(CloseableImage.class);
    mFinalCloseableImage2 = mock(CloseableImage.class);
    mIntermediateCloseableImage1 = mock(CloseableImage.class);
    mIntermediateCloseableImage2 = mock(CloseableImage.class);
    mFinalImageReference1 = CloseableReference.of(mFinalCloseableImage1);
    mFinalImageReference2 = CloseableReference.of(mFinalCloseableImage2);
    mIntermediateImageReference1 = CloseableReference.of(mIntermediateCloseableImage1);
    mIntermediateImageReference2 = CloseableReference.of(mIntermediateCloseableImage2);

    when(mCacheKeyFactory.getBitmapCacheKey(mImageRequest1, mCallerContext))
        .thenReturn(mBitmapMemoryCacheKey1);
    when(mCacheKeyFactory.getBitmapCacheKey(mImageRequest2, mCallerContext))
        .thenReturn(mBitmapMemoryCacheKey2);

    doAnswer(
        new Answer() {
          @Override
          public Void answer(InvocationOnMock invocation) throws Throwable {
            if (mForwardingConsumer1 == null) {
              mForwardingConsumer1 = (Consumer) invocation.getArguments()[0];
              mMultiplexedContext1 =
                  (BaseProducerContext) invocation.getArguments()[1];
            } else {
              mForwardingConsumer2 = (Consumer) invocation.getArguments()[0];
              mMultiplexedContext2 =
                  (BaseProducerContext) invocation.getArguments()[1];
            }
            return null;
          }
        }).when(mInputProducer).produceResults(any(Consumer.class), any(ProducerContext.class));
  }

  @Test
  public void testSingleRequest() {
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mForwardingConsumer1.onNewResult(mIntermediateImageReference1, Consumer.NO_FLAGS);
    verify(mConsumer1).onNewResult(mIntermediateImageReference1, Consumer.NO_FLAGS);
    mForwardingConsumer1.onNewResult(mIntermediateImageReference2, Consumer.NO_FLAGS);
    verify(mConsumer1).onNewResult(mIntermediateImageReference2, Consumer.NO_FLAGS);
    mForwardingConsumer1.onNewResult(mFinalImageReference1, Consumer.IS_LAST);
    verify(mConsumer1).onNewResult(mFinalImageReference1, Consumer.IS_LAST);
    assertTrue(mMultiplexProducer.mMultiplexers.isEmpty());
  }

  @Test
  public void testNewRequestGetsIntermediateResult() {
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mForwardingConsumer1.onNewResult(mIntermediateImageReference1, Consumer.NO_FLAGS);
    mForwardingConsumer1.onNewResult(mIntermediateImageReference2, TEST_FLAG);
    ArgumentCaptor<CloseableReference> imageReferenceCaptor =
        ArgumentCaptor.forClass(CloseableReference.class);
    mMultiplexProducer.produceResults(mConsumer2, mProducerContext2);
    verify(mConsumer2).onNewResult(imageReferenceCaptor.capture(), eq(TEST_FLAG));
    assertEquals(
        imageReferenceCaptor.getValue().getUnderlyingReferenceTestOnly(),
        mIntermediateImageReference2.getUnderlyingReferenceTestOnly());
  }

  @Test
  public void testTwoIdenticalRequestAndOneDifferent() {
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer2, mProducerContext2);
    mForwardingConsumer1.onNewResult(mIntermediateImageReference1, Consumer.NO_FLAGS);

    mMultiplexProducer.produceResults(mConsumer3, mProducerContext3);
    verify(mConsumer3, never()).onNewResult(mIntermediateImageReference1, Consumer.NO_FLAGS);

    mForwardingConsumer2.onNewResult(mIntermediateImageReference2, Consumer.NO_FLAGS);
    verify(mConsumer3).onNewResult(mIntermediateImageReference2, Consumer.NO_FLAGS);
    verify(mConsumer1, never()).onNewResult(mIntermediateImageReference2, Consumer.NO_FLAGS);
    verify(mConsumer2, never()).onNewResult(mIntermediateImageReference2, Consumer.NO_FLAGS);
    assertTrue(mMultiplexProducer.mMultiplexers.size() == 2);

    mForwardingConsumer1.onNewResult(mFinalImageReference1, Consumer.IS_LAST);
    verify(mConsumer1).onNewResult(mFinalImageReference1, Consumer.IS_LAST);
    verify(mConsumer2).onNewResult(mFinalImageReference1, Consumer.IS_LAST);
    verify(mConsumer3, never()).onNewResult(mFinalImageReference1, Consumer.IS_LAST);
    assertTrue(mMultiplexProducer.mMultiplexers.size() == 1);

    mForwardingConsumer2.onNewResult(mFinalImageReference2, Consumer.IS_LAST);
    verify(mConsumer3).onNewResult(mFinalImageReference2, Consumer.IS_LAST);
    verify(mConsumer1, never()).onNewResult(mFinalImageReference2, Consumer.IS_LAST);
    verify(mConsumer2, never()).onNewResult(mFinalImageReference2, Consumer.IS_LAST);
    assertTrue(mMultiplexProducer.mMultiplexers.isEmpty());
  }

  @Test
  public void testMultiplexRequestFailure() {
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer2, mProducerContext1);
    mForwardingConsumer1.onFailure(mException);
    verify(mConsumer1).onFailure(mException);
    verify(mConsumer2).onFailure(mException);
    assertTrue(mMultiplexProducer.mMultiplexers.isEmpty());
  }

  @Test
  public void testTwoIdenticalInSequence() {
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mForwardingConsumer1.onNewResult(mFinalImageReference1, Consumer.IS_LAST);
    assertTrue(mMultiplexProducer.mMultiplexers.isEmpty());

    mMultiplexProducer.produceResults(mConsumer2, mProducerContext2);
    mForwardingConsumer2.onNewResult(mFinalImageReference2, Consumer.IS_LAST);
    verify(mConsumer2).onNewResult(mFinalImageReference2, Consumer.IS_LAST);
    verify(mConsumer1, never()).onNewResult(mFinalImageReference2, Consumer.IS_LAST);
    assertTrue(mMultiplexProducer.mMultiplexers.isEmpty());
  }

  @Test
  public void testCancelSingleRequest() {
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mProducerContext1.cancel();
    assertTrue(mMultiplexedContext1.isCancelled());
    mForwardingConsumer1.onCancellation();
    verify(mConsumer1).onCancellation();
    assertTrue(mMultiplexProducer.mMultiplexers.isEmpty());
    verifyNoMoreInteractions(mConsumer1);
  }

  @Test
  public void testCancelMultiplexRequest() {
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer2, mProducerContext2);
    mMultiplexProducer.produceResults(mConsumer3, mProducerContext3);

    mProducerContext1.cancel();
    mForwardingConsumer1.onNewResult(mIntermediateImageReference1, TEST_FLAG);
    verify(mConsumer1, never()).onNewResult(mIntermediateImageReference1, TEST_FLAG);
    verify(mConsumer2).onNewResult(mIntermediateImageReference1, TEST_FLAG);
    verify(mConsumer3, never()).onNewResult(mIntermediateImageReference1, TEST_FLAG);
    assertTrue(mMultiplexProducer.mMultiplexers.size() == 2);

    mForwardingConsumer2.onNewResult(mIntermediateImageReference2, Consumer.NO_FLAGS);
    verify(mConsumer3).onNewResult(mIntermediateImageReference2, Consumer.NO_FLAGS);
    verify(mConsumer1, never()).onNewResult(mIntermediateImageReference2, Consumer.NO_FLAGS);
    verify(mConsumer2, never()).onNewResult(mIntermediateImageReference2, Consumer.NO_FLAGS);
    assertTrue(mMultiplexProducer.mMultiplexers.size() == 2);

    mProducerContext3.cancel();
    mForwardingConsumer2.onCancellation();
    assertTrue(mMultiplexProducer.mMultiplexers.size() == 1);

    mProducerContext2.cancel();
    mForwardingConsumer1.onCancellation();
    assertTrue(mMultiplexProducer.mMultiplexers.size() == 0);
  }

  @Test
  public void testOnFailureThenCancel() {
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mForwardingConsumer1.onFailure(mException);
    assertTrue(mMultiplexProducer.mMultiplexers.isEmpty());

    mMultiplexProducer.produceResults(mConsumer2, mProducerContext2);
    assertTrue(mMultiplexProducer.mMultiplexers.size() == 1);
    mProducerContext1.cancel();
    mForwardingConsumer1.onCancellation();
    assertTrue(mMultiplexProducer.mMultiplexers.size() == 1);
  }

  @Test
  public void testCancelThenOnLastResult() {
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mProducerContext1.cancel();
    assertFalse(mMultiplexProducer.mMultiplexers.isEmpty());
    mForwardingConsumer1.onCancellation();
    assertTrue(mMultiplexProducer.mMultiplexers.isEmpty());
    mMultiplexProducer.produceResults(mConsumer2, mProducerContext2);
    assertTrue(mMultiplexProducer.mMultiplexers.size() == 1);
    mForwardingConsumer1.onNewResult(mFinalImageReference1, Consumer.IS_LAST);
    assertTrue(mMultiplexProducer.mMultiplexers.size() == 1);
  }

  @Test
  public void testRestartProducerOnLateCancellationCallback() {
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    verify(mInputProducer).produceResults(mForwardingConsumer1, mMultiplexedContext1);
    mProducerContext1.cancel();
    verify(mConsumer1).onCancellation();
    mMultiplexProducer.produceResults(mConsumer2, mProducerContext2);
    verify(mInputProducer).produceResults(any(Consumer.class), any(ProducerContext.class));
    mForwardingConsumer1.onCancellation();
    assertEquals(1, mMultiplexProducer.mMultiplexers.size());
    verify(mInputProducer).produceResults(mForwardingConsumer2, mMultiplexedContext2);
    mForwardingConsumer2.onNewResult(mFinalImageReference1, Consumer.IS_LAST);
    verify(mConsumer2).onNewResult(mFinalImageReference1, Consumer.IS_LAST);
    assertTrue(mMultiplexProducer.mMultiplexers.isEmpty());
  }

  @Test
  public void testIsPrefetchTrue() {
    mProducerContext1.setIsPrefetch(true);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    assertTrue(mMultiplexedContext1.isPrefetch());
  }

  @Test
  public void testIsPrefetchFalse() {
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    assertFalse(mMultiplexedContext1.isPrefetch());
  }

  @Test
  public void testCancelChangesIsPrefetchIfNoMoreNonPrefetch() {
    mProducerContext1.setIsPrefetch(true);
    mProducerContext2.setIsPrefetch(false);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertFalse(mMultiplexedContext1.isPrefetch());
    ProducerContextCallbacks callbacks = mock(ProducerContextCallbacks.class);
    mMultiplexedContext1.addCallbacks(callbacks);
    mProducerContext2.cancel();
    assertTrue(mMultiplexedContext1.isPrefetch());
    verify(callbacks).onIsPrefetchChanged();
  }

  @Test
  public void testCancelDoesNotChangeIsPrefetchIfOtherIsNotPrefetch() {
    mProducerContext1.setIsPrefetch(false);
    mProducerContext2.setIsPrefetch(false);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertFalse(mMultiplexedContext1.isPrefetch());
    mProducerContext2.cancel();
    assertFalse(mMultiplexedContext1.isPrefetch());
  }

  @Test
  public void testCancelDoesNotChangeIsPrefetchOtherIfAlreadyPrefetch() {
    mProducerContext1.setIsPrefetch(true);
    mProducerContext2.setIsPrefetch(true);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertTrue(mMultiplexedContext1.isPrefetch());
    mProducerContext2.cancel();
    assertTrue(mMultiplexedContext1.isPrefetch());
  }

  @Test
  public void testAddConsumerChangesIsPrefetch() {
    mProducerContext1.setIsPrefetch(true);
    mProducerContext2.setIsPrefetch(false);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    assertTrue(mMultiplexedContext1.isPrefetch());
    ProducerContextCallbacks callbacks = mock(ProducerContextCallbacks.class);
    mMultiplexedContext1.addCallbacks(callbacks);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertFalse(mMultiplexedContext1.isPrefetch());
    verify(callbacks).onIsPrefetchChanged();
  }

  @Test
  public void testAddConsumerDoesNotChangeIsPrefetchIfAlreadyPrefetch() {
    mProducerContext1.setIsPrefetch(true);
    mProducerContext2.setIsPrefetch(true);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    assertTrue(mMultiplexedContext1.isPrefetch());
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertTrue(mMultiplexedContext1.isPrefetch());
  }

  @Test
  public void testAddConsumerDoesNotChangeIsPrefetchIfPrefetch() {
    mProducerContext1.setIsPrefetch(false);
    mProducerContext2.setIsPrefetch(true);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    assertFalse(mMultiplexedContext1.isPrefetch());
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertFalse(mMultiplexedContext1.isPrefetch());
  }

  @Test
  public void testNoLongerPrefetchWhenCurrentlyPrefetch() {
    mProducerContext1.setIsPrefetch(true);
    mProducerContext2.setIsPrefetch(true);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertTrue(mMultiplexedContext1.isPrefetch());
    mProducerContext1.setIsPrefetch(false);
    assertFalse(mMultiplexedContext1.isPrefetch());
  }

  @Test
  public void testNowPrefetchWhenNotPrefetchBefore() {
    mProducerContext1.setIsPrefetch(true);
    mProducerContext2.setIsPrefetch(false);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertFalse(mMultiplexedContext1.isPrefetch());
    mProducerContext2.setIsPrefetch(true);
    assertTrue(mMultiplexedContext1.isPrefetch());
  }

  @Test
  public void testBothNotPrefetchThenOneBecomesPrefetch() {
    mProducerContext1.setIsPrefetch(false);
    mProducerContext2.setIsPrefetch(false);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertFalse(mMultiplexedContext1.isPrefetch());
    mProducerContext2.setIsPrefetch(true);
    assertFalse(mMultiplexedContext1.isPrefetch());
  }

  @Test
  public void testIsIntermediateResultExpectedTrue() {
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    assertTrue(mMultiplexedContext1.isIntermediateResultExpected());
  }

  @Test
  public void testIsIntermediateResultExpectedFalse() {
    mProducerContext1.setIsIntermediateResultExpected(false);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    assertFalse(mMultiplexedContext1.isIntermediateResultExpected());
  }

  @Test
  public void testCancelChangesIsIntermediateResultExpectedIfNeeded() {
    mProducerContext1.setIsIntermediateResultExpected(true);
    mProducerContext2.setIsIntermediateResultExpected(false);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertTrue(mMultiplexedContext1.isIntermediateResultExpected());
    ProducerContextCallbacks callbacks = mock(ProducerContextCallbacks.class);
    mMultiplexedContext1.addCallbacks(callbacks);
    mProducerContext1.cancel();
    assertFalse(mMultiplexedContext1.isIntermediateResultExpected());
    verify(callbacks).onIsIntermediateResultExpectedChanged();
  }

  @Test
  public void testCancelDoesNotChangeIsIntermediateResultExpectedIfNotNeeded() {
    mProducerContext1.setIsIntermediateResultExpected(false);
    mProducerContext2.setIsIntermediateResultExpected(false);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertFalse(mMultiplexedContext1.isIntermediateResultExpected());
    mProducerContext2.cancel();
    assertFalse(mMultiplexedContext1.isIntermediateResultExpected());
  }

  @Test
  public void testCancelDoesNotChangeIsIntermediateResultExpectedIfNotNeeded2() {
    mProducerContext1.setIsIntermediateResultExpected(true);
    mProducerContext2.setIsIntermediateResultExpected(true);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertTrue(mMultiplexedContext1.isIntermediateResultExpected());
    mProducerContext2.cancel();
    assertTrue(mMultiplexedContext1.isIntermediateResultExpected());
  }

  @Test
  public void testAddConsumerChangesIsIntermediateResultExpectedIfNeeded() {
    mProducerContext1.setIsIntermediateResultExpected(false);
    mProducerContext2.setIsIntermediateResultExpected(true);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    assertFalse(mMultiplexedContext1.isIntermediateResultExpected());
    ProducerContextCallbacks callbacks = mock(ProducerContextCallbacks.class);
    mMultiplexedContext1.addCallbacks(callbacks);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertTrue(mMultiplexedContext1.isIntermediateResultExpected());
    verify(callbacks).onIsIntermediateResultExpectedChanged();
  }

  @Test
  public void testAddConsumerDoesNotChangeIsIntermediateResultExpectedIfNotNeeded() {
    mProducerContext1.setIsIntermediateResultExpected(false);
    mProducerContext2.setIsIntermediateResultExpected(false);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    assertFalse(mMultiplexedContext1.isIntermediateResultExpected());
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertFalse(mMultiplexedContext1.isIntermediateResultExpected());
  }

  @Test
  public void testAddConsumerDoesNotChangeIsIntermediateResultExpectedIfNotNeeded2() {
    mProducerContext1.setIsIntermediateResultExpected(true);
    mProducerContext2.setIsIntermediateResultExpected(false);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    assertTrue(mMultiplexedContext1.isIntermediateResultExpected());
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertTrue(mMultiplexedContext1.isIntermediateResultExpected());
  }

  @Test
  public void testPropagatesIsIntermediateResultExpectedChangeIfNeeded() {
    mProducerContext1.setIsIntermediateResultExpected(false);
    mProducerContext2.setIsIntermediateResultExpected(false);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertFalse(mMultiplexedContext1.isIntermediateResultExpected());
    mProducerContext1.setIsIntermediateResultExpected(true);
    assertTrue(mMultiplexedContext1.isIntermediateResultExpected());
  }

  @Test
  public void testPropagatesIsIntermediateResultExpectedChangeIfNeeded2() {
    mProducerContext1.setIsIntermediateResultExpected(true);
    mProducerContext2.setIsIntermediateResultExpected(false);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertTrue(mMultiplexedContext1.isIntermediateResultExpected());
    mProducerContext1.setIsIntermediateResultExpected(false);
    assertFalse(mMultiplexedContext1.isIntermediateResultExpected());
  }

  @Test
  public void testDoesNotPropagatesIsIntermediateResultExpectedChangeIfNotNeeded() {
    mProducerContext1.setIsIntermediateResultExpected(true);
    mProducerContext2.setIsIntermediateResultExpected(true);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertTrue(mMultiplexedContext1.isIntermediateResultExpected());
    mProducerContext2.setIsIntermediateResultExpected(false);
    assertTrue(mMultiplexedContext1.isIntermediateResultExpected());
  }

  @Test
  public void testGetPriority() {
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    assertEquals(Priority.MEDIUM, mMultiplexedContext1.getPriority());
  }

  @Test
  public void testAddHigherPriorityIncreasesPriority() {
    mProducerContext1.setPriority(Priority.MEDIUM);
    mProducerContext2.setPriority(Priority.HIGH);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    assertEquals(Priority.MEDIUM, mMultiplexedContext1.getPriority());
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertEquals(Priority.HIGH, mMultiplexedContext1.getPriority());
  }

  @Test
  public void testAddLowerPriorityDoesNotChangePriority() {
    mProducerContext1.setPriority(Priority.MEDIUM);
    mProducerContext2.setPriority(Priority.LOW);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    assertEquals(Priority.MEDIUM, mMultiplexedContext1.getPriority());
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertEquals(Priority.MEDIUM, mMultiplexedContext1.getPriority());
  }

  @Test
  public void testCancelHighestPriorityLowersPriority() {
    mProducerContext1.setPriority(Priority.MEDIUM);
    mProducerContext2.setPriority(Priority.HIGH);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertEquals(Priority.HIGH, mMultiplexedContext1.getPriority());
    mProducerContext2.cancel();
    assertEquals(Priority.MEDIUM, mMultiplexedContext1.getPriority());
  }

  @Test
  public void testCancelLowerPriorityDoesNotChangePriority() {
    mProducerContext1.setPriority(Priority.MEDIUM);
    mProducerContext2.setPriority(Priority.HIGH);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertEquals(Priority.HIGH, mMultiplexedContext1.getPriority());
    mProducerContext1.cancel();
    assertEquals(Priority.HIGH, mMultiplexedContext1.getPriority());
  }

  @Test
  public void testChangeHighestPriorityLowersPriority() {
    mProducerContext1.setPriority(Priority.HIGH);
    mProducerContext2.setPriority(Priority.MEDIUM);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertEquals(Priority.HIGH, mMultiplexedContext1.getPriority());
    mProducerContext1.setPriority(Priority.LOW);
    assertEquals(Priority.MEDIUM, mMultiplexedContext1.getPriority());
  }

  @Test
  public void testChangeToHighestPriorityHighersPriority() {
    mProducerContext1.setPriority(Priority.LOW);
    mProducerContext2.setPriority(Priority.MEDIUM);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertEquals(Priority.MEDIUM, mMultiplexedContext1.getPriority());
    mProducerContext1.setPriority(Priority.HIGH);
    assertEquals(Priority.HIGH, mMultiplexedContext1.getPriority());
  }

  @Test
  public void testChangeToNonHighestPriorityDoesNotChangePriority() {
    mProducerContext1.setPriority(Priority.LOW);
    mProducerContext2.setPriority(Priority.HIGH);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertEquals(Priority.HIGH, mMultiplexedContext1.getPriority());
    mProducerContext1.setPriority(Priority.MEDIUM);
    assertEquals(Priority.HIGH, mMultiplexedContext1.getPriority());
  }

  @Test
  public void testChangeNonHighestToLowerPriorityDoesNotChangePriority() {
    mProducerContext1.setPriority(Priority.HIGH);
    mProducerContext2.setPriority(Priority.HIGH);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext1);
    mMultiplexProducer.produceResults(mConsumer1, mProducerContext2);
    assertEquals(Priority.HIGH, mMultiplexedContext1.getPriority());
    mProducerContext1.setPriority(Priority.MEDIUM);
    assertEquals(Priority.HIGH, mMultiplexedContext1.getPriority());
  }
}
